package com.akashiic.mixer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;

import com.akashiic.mixer.AkashicMixer;
import com.akashiic.mixer.core.EngineRecovery;
import com.akashiic.mixer.core.EngineRecovery.Health;
import com.akashiic.mixer.core.ReloadGate;
import com.akashiic.mixer.mixins.client.AccessorSoundHandler;
import com.akashiic.mixer.mixins.client.AccessorSoundManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Keeps Minecraft's sound engine usable when the audio device comes and goes (headphones unplugged, Bluetooth off).
 * Client thread.
 * <ul>
 * <li>Always: runs a reload that {@code MixinSoundManager} deferred because the previous one was still loading
 * ({@link ReloadGate}).</li>
 * <li>With {@code recoverEngine}: twice a second reads {@link OpenAlEngine#health}; {@link EngineRecovery} decides when
 * to restart, and the restart is {@code SoundManager.reloadSoundSystem()} only: the engine, not the sound registry, so
 * no resource reload and no long freeze. The sound comes back without Refresh Sounds.</li>
 * </ul>
 */
public final class SoundEngineWatchdog {

    private static final long PROBE_NANOS = 500_000_000L;
    private static final ReloadGate GATE = new ReloadGate();
    private static boolean deferLogged;

    private final boolean recover;
    private final EngineRecovery recovery = new EngineRecovery();
    private long nextProbeNanos;
    private boolean disabled;

    public SoundEngineWatchdog(boolean recover) {
        this.recover = recover;
    }

    /** {@code MixinSoundManager}: may {@code reloadSoundSystem} run now? False defers it. Client thread. */
    public static boolean tryStartReload(boolean loaded) {
        if (GATE.tryStart(loaded, System.nanoTime())) return true;
        if (!deferLogged) {
            deferLogged = true;
            AkashicMixer.LOG.info("AkashicMixer: sound reload asked for while the last one is still loading; deferred");
        }
        return false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || disabled) return;
        SoundHandler handler = Minecraft.getMinecraft()
            .getSoundHandler();
        if (handler == null) return;
        long now = System.nanoTime();

        SoundManager manager;
        try {
            manager = ((AccessorSoundHandler) handler).akashicmixer$getSndManager();
            if (manager != null && GATE.tick(((AccessorSoundManager) manager).akashicmixer$isLoaded(), now)) {
                deferLogged = false;
                manager.reloadSoundSystem();
                return;
            }
        } catch (LinkageError | RuntimeException e) {
            disabled = true;
            AkashicMixer.LOG.warn("AkashicMixer: sound engine watchdog disabled", e);
            return;
        }
        if (!recover || now - nextProbeNanos < 0) return;
        nextProbeNanos = now + PROBE_NANOS;

        Health health;
        try {
            health = OpenAlEngine.health(manager);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            disabled = true;
            AkashicMixer.LOG.warn("AkashicMixer: sound engine watchdog disabled, cannot read the engine state", e);
            return;
        }

        if (health == Health.HEALTHY) OpenAlEngine.healthy();
        boolean wasFailing = recovery.failing();
        int restarts = recovery.restarts();
        if (!recovery.update(health, now)) {
            if (wasFailing && !recovery.failing() && restarts > 0) {
                AkashicMixer.LOG.info("AkashicMixer: sound engine is back after {} restart(s)", restarts);
            }
            return;
        }
        int attempt = recovery.restarts();
        if (attempt <= 3 || attempt % 10 == 0) {
            AkashicMixer.LOG
                .info("AkashicMixer: sound engine {}; restarting it (attempt {})", describe(health), attempt);
        }
        try {
            manager.reloadSoundSystem();
        } catch (RuntimeException e) {
            AkashicMixer.LOG.warn("AkashicMixer: sound engine restart failed; will retry", e);
        }
    }

    private static String describe(Health health) {
        return health == Health.DEAD ? "has no usable audio device" : "lost its audio device";
    }
}
