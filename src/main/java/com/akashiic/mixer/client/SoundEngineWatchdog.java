package com.akashiic.mixer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;

import com.akashiic.mixer.AkashicMixer;
import com.akashiic.mixer.core.DefaultDeviceFollower;
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
 * <li>With {@code followDefaultDevice} too: every {@link #DEVICE_PROBE_NANOS} compares Windows' default output with the
 * engine's device ({@link DefaultDeviceFollower}) and restarts the engine on the new default, so every Minecraft
 * sound follows the headset.</li>
 * <li>After a restart of its own, if background music was playing, the next track starts within
 * {@link MusicResume#DELAY_TICKS} ticks instead of after vanilla's 10-20 minute pause.</li>
 * </ul>
 */
public final class SoundEngineWatchdog {

    private static final long PROBE_NANOS = 500_000_000L;
    static final long DEVICE_PROBE_NANOS = 2_000_000_000L;
    private static final long SLOW_DEVICE_PROBE_NANOS = 10_000_000_000L;
    /** Two device enumerations in a row slower than this: check every {@link #SLOW_DEVICE_PROBE_NANOS} instead. */
    private static final long SLOW_ENUMERATION_NANOS = 8_000_000L;
    private static final long MUSIC_RESUME_WINDOW_NANOS = 15_000_000_000L;
    private static final ReloadGate GATE = new ReloadGate();
    private static boolean deferLogged;

    private final boolean recover;
    private final boolean followDefault;
    private final EngineRecovery recovery = new EngineRecovery();
    private final DefaultDeviceFollower follower = new DefaultDeviceFollower();
    private long nextProbeNanos;
    private long nextDeviceProbeNanos;
    private long deviceProbeIntervalNanos = DEVICE_PROBE_NANOS;
    private boolean defaultMoved;
    private Object probedEngine;
    private int slowProbes;
    private String[] lastDevices;
    private boolean musicWasPlaying;
    private long resumeMusicUntilNanos;
    private boolean disabled;

    public SoundEngineWatchdog(boolean recover, boolean followDefault) {
        this.recover = recover;
        this.followDefault = recover && followDefault;
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
        Minecraft mc = Minecraft.getMinecraft();
        SoundHandler handler = mc.getSoundHandler();
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
        if (resumeMusicUntilNanos != 0) {
            if (MusicResume.resumeSoon(mc) || now - resumeMusicUntilNanos > 0) resumeMusicUntilNanos = 0;
        }
        if (!recover || now - nextProbeNanos < 0) return;
        nextProbeNanos = now + PROBE_NANOS;

        Health health;
        Object engine;
        try {
            health = OpenAlEngine.health(manager);
            engine = OpenAlEngine.system(manager);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            disabled = true;
            AkashicMixer.LOG.warn("AkashicMixer: sound engine watchdog disabled, cannot read the engine state", e);
            return;
        }
        if (health == Health.HEALTHY) health = followDefault(engine, now);

        if (health == Health.HEALTHY) {
            OpenAlEngine.healthy();
            if (!recovery.failing()) musicWasPlaying = MusicResume.playing(mc);
        }
        boolean wasFailing = recovery.failing();
        int restarts = recovery.restarts();
        if (!recovery.update(health, now)) {
            if (wasFailing && !recovery.failing() && restarts > 0) {
                AkashicMixer.LOG.info("AkashicMixer: sound engine is back after {} restart(s)", restarts);
                if (musicWasPlaying) resumeMusicUntilNanos = now + MUSIC_RESUME_WINDOW_NANOS;
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

    /** HEALTHY, or DEFAULT_CHANGED when Windows' default output moved to another device. */
    private Health followDefault(Object engine, long now) {
        if (!followDefault) return Health.HEALTHY;
        if (engine != probedEngine || now - nextDeviceProbeNanos >= 0) {
            probedEngine = engine;
            long start = System.nanoTime();
            String[] devices = OpenAlEngine.devices();
            long took = System.nanoTime() - start;
            slowProbes = took > SLOW_ENUMERATION_NANOS ? slowProbes + 1 : 0;
            if (slowProbes >= 2 && deviceProbeIntervalNanos != SLOW_DEVICE_PROBE_NANOS) {
                deviceProbeIntervalNanos = SLOW_DEVICE_PROBE_NANOS;
                AkashicMixer.LOG.info(
                    "AkashicMixer: listing audio devices took {} ms; checking the Windows default every 10 s",
                    took / 1_000_000L);
            }
            nextDeviceProbeNanos = now + deviceProbeIntervalNanos;
            lastDevices = devices;
            defaultMoved = devices != null && follower.moved(engine, devices[0], devices[1]);
        }
        return defaultMoved ? Health.DEFAULT_CHANGED : Health.HEALTHY;
    }

    private String describe(Health health) {
        if (health == Health.DEFAULT_CHANGED && lastDevices != null) {
            return "is on '" + lastDevices[1] + "' but Windows now plays on '" + lastDevices[0] + "'";
        }
        return health == Health.DEAD ? "has no usable audio device" : "lost its audio device";
    }
}
