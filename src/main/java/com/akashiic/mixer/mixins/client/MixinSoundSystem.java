package com.akashiic.mixer.mixins.client;

import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashiic.mixer.client.OpenAlEngine;

import paulscode.sound.CommandObject;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

/**
 * Crash guard for a sound engine whose OpenAL library has no usable context. Two ways to get there, both seen when an
 * audio device goes away: {@code AL.create} fails (no device) after resetting the native stubs, or a second sound
 * reload starts while the first one is still loading on its own thread, so the new library's {@code AL.create} meets
 * the context of the previous one ({@code IllegalStateException}) and its listener buffers are never allocated.
 * paulscode keeps such a library as the active one, and every queued command then ends in
 * {@code UnsatisfiedLinkError} ({@code alListenerf}) or {@code NullPointerException} ({@code setListenerPosition}): on
 * the paulscode thread that kills the engine; when a mod flushes the queue on the client thread (Dynamic Surroundings'
 * {@code SoundManager.flushSound}) it crashes the game. Forge has no hook here.
 * <p>
 * While the active library is such a dead OpenAL library, {@code CommandQueue(null)} drops the queued commands instead
 * of running them (a queued {@code NEW_LIBRARY} still runs). Adding commands is untouched. {@code SoundEngineWatchdog}
 * restarts the engine.
 */
@Mixin(value = SoundSystem.class, remap = false)
public abstract class MixinSoundSystem {

    @Shadow
    protected Library soundLibrary;

    @Shadow
    protected List<CommandObject> commandQueue;

    @Shadow
    private List<CommandObject> sourcePlayList;

    @Inject(method = "CommandQueue", at = @At("HEAD"), cancellable = true)
    private void akashicmixer$skipDeadEngine(CommandObject command, CallbackInfoReturnable<Boolean> cir) {
        if (command != null || !OpenAlEngine.isDead(soundLibrary)) return;
        synchronized (SoundSystemConfig.THREAD_SYNC) {
            if (!OpenAlEngine.isDead(soundLibrary)) return;
            if (commandQueue != null) {
                for (Iterator<CommandObject> it = commandQueue.iterator(); it.hasNext();) {
                    CommandObject queued = it.next();
                    if (queued != null && queued.Command == CommandObject.NEW_LIBRARY) return;
                    it.remove();
                }
            }
            if (sourcePlayList != null) sourcePlayList.clear();
            OpenAlEngine.droppedCommands();
            cir.setReturnValue(false);
        }
    }
}
