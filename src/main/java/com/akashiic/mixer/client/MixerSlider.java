package com.akashiic.mixer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import org.lwjgl.opengl.GL11;

/** A 0..100 slider drawn like Minecraft's own option sliders. */
final class MixerSlider extends GuiButton {

    interface Handler {

        /** Stores the new value and returns the label to show. */
        String changed(int percent);
    }

    private final Handler handler;
    private float value;
    private boolean dragging;

    MixerSlider(int id, int x, int y, int width, int percent, Handler handler) {
        super(id, x, y, width, 20, "");
        this.handler = handler;
        this.value = percent / 100f;
        this.displayString = handler.changed(percent);
    }

    @Override
    public int getHoverState(boolean mouseOver) {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        if (dragging) set(mouseX);
        mc.getTextureManager()
            .bindTexture(buttonTextures);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        int knob = xPosition + (int) (value * (width - 8));
        drawTexturedModalRect(knob, yPosition, 0, 66, 4, 20);
        drawTexturedModalRect(knob + 4, yPosition, 196, 66, 4, 20);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;
        set(mouseX);
        dragging = true;
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }

    private void set(int mouseX) {
        float v = (mouseX - (xPosition + 4)) / (float) (width - 8);
        value = v < 0f ? 0f : (v > 1f ? 1f : v);
        displayString = handler.changed(Math.round(value * 100f));
    }
}
