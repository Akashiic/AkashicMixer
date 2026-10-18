package com.akashiic.mixer.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import com.akashiic.mixer.api.AkashicMixerApi;
import com.akashiic.mixer.api.MixerChannel;

/**
 * The Akashic Sounds Mixer: the Akashic master slider, one slider and mute button per registered channel, and the
 * Minecraft music settings. Laid out like vanilla's Music and Sounds screen. Saves on close.
 */
public final class GuiAkashicMixer extends GuiScreen {

    private static final int DONE = 0;
    private static final int DUCK = 1;
    private static final int MUTE_BASE = 100;

    private final GuiScreen parent;
    private final List<MixerChannel> rows = new ArrayList<>();

    public GuiAkashicMixer(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        buttonList.clear();
        rows.clear();
        rows.add(AkashicMixerApi.master());
        rows.addAll(AkashicMixerApi.channels());
        int left = width / 2 - 155;
        int y = height / 6 - 12;
        for (int i = 0; i < rows.size(); i++) {
            final MixerChannel ch = rows.get(i);
            buttonList.add(new MixerSlider(MUTE_BASE + 1000 + i, left, y, 250, ch.volumePercent(), percent -> {
                ch.setVolumePercent(percent);
                return label(ch);
            }));
            buttonList.add(new GuiButton(MUTE_BASE + i, left + 255, y, 55, 20, muteLabel(ch)));
            y += 24;
        }
        y += 6;
        buttonList.add(new GuiButton(DUCK, left, y, 310, 20, duckLabel()));
        y += 24;
        buttonList.add(new MixerSlider(2, left, y, 310, MixerConfig.duckFloorPercent, percent -> {
            MixerConfig.duckFloorPercent = percent;
            return floorLabel();
        }));
        buttonList
            .add(new GuiButton(DONE, width / 2 - 100, Math.max(y + 30, height / 6 + 168), I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        if (button.id == DONE) {
            mc.displayGuiScreen(parent);
        } else if (button.id == DUCK) {
            MixerConfig.duckEnabled = !MixerConfig.duckEnabled;
            button.displayString = duckLabel();
        } else if (button.id >= MUTE_BASE && button.id < MUTE_BASE + rows.size()) {
            MixerChannel ch = rows.get(button.id - MUTE_BASE);
            ch.setMuted(!ch.isMuted());
            button.displayString = muteLabel(ch);
            for (Object o : buttonList) {
                GuiButton b = (GuiButton) o;
                if (b.id == MUTE_BASE + 1000 + rows.indexOf(ch)) b.displayString = label(ch);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        MixerConfig.save();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, I18n.format("akashicmixer.gui.title"), width / 2, 15, 0xFFFFFF);
        if (rows.size() <= 1) {
            drawCenteredString(
                fontRendererObj,
                I18n.format("akashicmixer.gui.none"),
                width / 2,
                height / 6 + 16,
                0xA0A0A0);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static String label(MixerChannel ch) {
        String name = I18n.format(ch.translationKey());
        String value = ch.isMuted() ? I18n.format("akashicmixer.gui.muted")
            : ch.volumePercent() == 0 ? I18n.format("options.off") : ch.volumePercent() + "%";
        return name + ": " + value;
    }

    private static String muteLabel(MixerChannel ch) {
        return ch.isMuted() ? EnumChatFormatting.RED + I18n.format("akashicmixer.gui.unmute")
            : I18n.format("akashicmixer.gui.mute");
    }

    private static String duckLabel() {
        return I18n
            .format("akashicmixer.gui.duck", I18n.format(MixerConfig.duckEnabled ? "options.on" : "options.off"));
    }

    private static String floorLabel() {
        int p = MixerConfig.duckFloorPercent;
        return I18n.format("akashicmixer.gui.duckFloor", p == 0 ? I18n.format("akashicmixer.gui.silent") : p + "%");
    }
}
