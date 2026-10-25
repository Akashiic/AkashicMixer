package com.akashiic.mixer.client;

import java.lang.reflect.Field;
import java.nio.IntBuffer;

import net.minecraft.client.audio.SoundManager;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCdevice;

import com.akashiic.mixer.AkashicMixer;
import com.akashiic.mixer.core.EngineRecovery.Health;
import com.akashiic.mixer.mixins.client.AccessorLibraryLWJGLOpenAL;
import com.akashiic.mixer.mixins.client.AccessorSoundManager;
import com.akashiic.mixer.mixins.client.AccessorSoundSystem;

import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

/**
 * State of Minecraft's OpenAL sound engine, read by {@code MixinSoundSystem} (any thread) and
 * {@link SoundEngineWatchdog} (client thread). LWJGL 2 has one global OpenAL context, so {@code AL.isCreated()} is
 * the truth about whether any OpenAL call can work. Hodgepodge's library extends {@code LibraryLWJGLOpenAL} and is
 * covered by the same checks.
 */
public final class OpenAlEngine {

    private static volatile boolean fieldCheck = true;
    private static volatile boolean droppedLogged;
    private static final IntBuffer SCRATCH = BufferUtils.createIntBuffer(1);
    private static Field systemField;

    private OpenAlEngine() {}

    /** True when {@code library} is an OpenAL library without a usable context: running its commands would crash. */
    public static boolean isDead(Library library) {
        if (!(library instanceof LibraryLWJGLOpenAL)) return false;
        if (!AL.isCreated()) return true;
        if (!fieldCheck) return false;
        try {
            return ((AccessorLibraryLWJGLOpenAL) (Object) library).akashicmixer$getListenerPosition() == null;
        } catch (LinkageError | ClassCastException e) {
            // Another mod reshaped the class: AL.isCreated() alone still covers the device-loss case.
            fieldCheck = false;
            AkashicMixer.LOG
                .warn("AkashicMixer: cannot read the OpenAL listener buffer ({}); checking AL only", e.toString());
            return false;
        }
    }

    /** Logged once per episode: the guard dropped commands instead of letting them crash. Any thread. */
    public static void droppedCommands() {
        if (droppedLogged) return;
        droppedLogged = true;
        AkashicMixer.LOG.warn(
            "AkashicMixer: Minecraft's sound engine has no usable OpenAL context (audio device lost or overlapping "
                + "sound reloads); dropping its sound commands until it is restarted");
    }

    /**
     * Health of Minecraft's engine. DEAD also covers paulscode's silent fallback ({@code Library}, "No Sound"), which
     * is where a reload ends when OpenAL cannot open any device. Client thread.
     */
    static Health health(SoundManager manager) throws ReflectiveOperationException {
        if (manager == null || !((AccessorSoundManager) manager).akashicmixer$isLoaded()) return Health.LOADING;
        SoundSystem system = system(manager);
        if (system == null || !SoundSystem.initialized()) return Health.LOADING;
        Library library = ((AccessorSoundSystem) (Object) system).akashicmixer$getLibrary();
        if (library == null) return Health.LOADING;
        if (library.getClass() == Library.class) return Health.DEAD;
        if (!(library instanceof LibraryLWJGLOpenAL)) return Health.HEALTHY;
        if (isDead(library)) return Health.DEAD;
        synchronized (SoundSystemConfig.THREAD_SYNC) {
            if (!AL.isCreated()) return Health.DEAD;
            try {
                return deviceConnected() ? Health.HEALTHY : Health.DISCONNECTED;
            } catch (LinkageError e) {
                // Destroyed under us by a mod outside THREAD_SYNC; the next probe sees DEAD.
                return Health.LOADING;
            }
        }
    }

    /** A healthy engine starts a new episode for the dropped-commands warning. */
    static void healthy() {
        droppedLogged = false;
    }

    /**
     * {@code SoundManager.sndSystem}. Its type is the package-private {@code SoundManager.SoundSystemStarterThread},
     * which an accessor cannot name, so the field is found by type: the only SoundSystem field of SoundManager.
     */
    static SoundSystem system(SoundManager manager) throws ReflectiveOperationException {
        if (systemField == null) {
            for (Field f : SoundManager.class.getDeclaredFields()) {
                if (SoundSystem.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    systemField = f;
                    break;
                }
            }
            if (systemField == null) throw new NoSuchFieldException("SoundManager has no SoundSystem field");
        }
        return (SoundSystem) systemField.get(manager);
    }

    /** {@code ALC_CONNECTED} of the current device; true when the driver cannot tell (no ALC_EXT_disconnect). */
    private static boolean deviceConnected() {
        ALCdevice device = AL.getDevice();
        if (device == null || !ALC10.alcIsExtensionPresent(device, "ALC_EXT_disconnect")) return true;
        int connected = ALC10.alcGetEnumValue(device, "ALC_CONNECTED");
        if (connected == 0) return true;
        SCRATCH.clear();
        ALC10.alcGetInteger(device, connected, SCRATCH);
        return SCRATCH.get(0) != ALC10.ALC_FALSE;
    }
}
