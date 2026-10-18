package com.akashiic.mixer.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;

import com.akashiic.mixer.core.ButtonSlot;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Adds the "Akashic Sounds Mixer" button to Minecraft's Music and Sounds screen, next to whatever other mods put there
 * (first free slot, {@link ButtonSlot}), and opens {@link GuiAkashicMixer} from it.
 */
public final class SoundOptionsButton {

    /** Unlikely to collide with vanilla or other mods' button ids ("AKM"). */
    static final int BUTTON_ID = 0x414B4D;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiScreenOptionsSounds)) return;
        @SuppressWarnings("unchecked")
        List<GuiButton> buttons = event.buttonList;
        List<int[]> taken = new ArrayList<>();
        for (GuiButton b : buttons) {
            if (b.visible) taken.add(new int[] { b.xPosition, b.yPosition, b.width, b.height });
        }
        int[] slot = ButtonSlot.find(taken, event.gui.width, event.gui.height);
        buttons.add(
            new GuiButton(
                BUTTON_ID,
                slot[0],
                slot[1],
                slot[2],
                ButtonSlot.HEIGHT,
                I18n.format("akashicmixer.gui.open")));
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.gui instanceof GuiScreenOptionsSounds) || event.button.id != BUTTON_ID) return;
        Minecraft.getMinecraft()
            .displayGuiScreen(new GuiAkashicMixer(event.gui));
        event.setCanceled(true);
    }
}
