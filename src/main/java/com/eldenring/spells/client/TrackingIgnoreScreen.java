package com.eldenring.spells.client;

import com.eldenring.spells.network.TrackingIgnorePrefsPayload;
import com.eldenring.spells.tracking.TrackingIgnorePrefs;
import com.eldenring.spells.tracking.TrackingIgnorePrefsCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 辉石追踪排除设置：四个「不追踪」勾选，改动后立即 C2S 写入玩家 Attachment。
 * <p>
 * 独立 Screen，不嵌入其它 mod 界面。未识别类别的模组生物不会被这些排除项踢掉。
 */
public class TrackingIgnoreScreen extends Screen {
    private static final int PANEL_WIDTH_PIXELS = 220;
    private static final int CHECKBOX_LEFT_PADDING_PIXELS = 12;
    private static final int TITLE_TOP_OFFSET_PIXELS = 20;
    private static final int FIRST_CHECKBOX_TOP_OFFSET_PIXELS = 48;
    private static final int CHECKBOX_ROW_SPACING_PIXELS = 24;
    private static final int DONE_BUTTON_WIDTH_PIXELS = 100;
    private static final int DONE_BUTTON_BOTTOM_MARGIN_PIXELS = 28;

    public TrackingIgnoreScreen() {
        super(Component.translatable("screen.iss_elden_ring.tracking_ignore"));
    }

    @Override
    protected void init() {
        TrackingIgnorePrefs prefs = TrackingIgnorePrefsCache.get();
        int panelLeft = (this.width - PANEL_WIDTH_PIXELS) / 2;
        int checkboxX = panelLeft + CHECKBOX_LEFT_PADDING_PIXELS;
        int rowY = TITLE_TOP_OFFSET_PIXELS + FIRST_CHECKBOX_TOP_OFFSET_PIXELS;

        addCheckbox(
                checkboxX,
                rowY,
                "screen.iss_elden_ring.tracking_ignore.ignore_players",
                prefs.ignorePlayers(),
                selected -> applyPrefs(TrackingIgnorePrefsCache.get().withIgnorePlayers(selected))
        );
        rowY += CHECKBOX_ROW_SPACING_PIXELS;
        addCheckbox(
                checkboxX,
                rowY,
                "screen.iss_elden_ring.tracking_ignore.ignore_peaceful",
                prefs.ignorePeaceful(),
                selected -> applyPrefs(TrackingIgnorePrefsCache.get().withIgnorePeaceful(selected))
        );
        rowY += CHECKBOX_ROW_SPACING_PIXELS;
        addCheckbox(
                checkboxX,
                rowY,
                "screen.iss_elden_ring.tracking_ignore.ignore_neutral",
                prefs.ignoreNeutral(),
                selected -> applyPrefs(TrackingIgnorePrefsCache.get().withIgnoreNeutral(selected))
        );
        rowY += CHECKBOX_ROW_SPACING_PIXELS;
        addCheckbox(
                checkboxX,
                rowY,
                "screen.iss_elden_ring.tracking_ignore.ignore_hostile",
                prefs.ignoreHostile(),
                selected -> applyPrefs(TrackingIgnorePrefsCache.get().withIgnoreHostile(selected))
        );

        int doneX = (this.width - DONE_BUTTON_WIDTH_PIXELS) / 2;
        int doneY = this.height - DONE_BUTTON_BOTTOM_MARGIN_PIXELS - 20;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(doneX, doneY, DONE_BUTTON_WIDTH_PIXELS, 20)
                .build());
    }

    private void addCheckbox(
            int x,
            int y,
            String translationKey,
            boolean selected,
            java.util.function.Consumer<Boolean> onToggle
    ) {
        Checkbox checkbox = Checkbox.builder(Component.translatable(translationKey), this.font)
                .pos(x, y)
                .selected(selected)
                .onValueChange((box, value) -> onToggle.accept(value))
                .build();
        addRenderableWidget(checkbox);
    }

    private void applyPrefs(TrackingIgnorePrefs prefs) {
        TrackingIgnorePrefsCache.optimisticLocal(prefs);
        TrackingIgnorePrefsPayload.sendToServer(prefs);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                TITLE_TOP_OFFSET_PIXELS,
                0xFFFFFF
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.iss_elden_ring.tracking_ignore.hint"),
                this.width / 2,
                TITLE_TOP_OFFSET_PIXELS + 14,
                0xA0A0A0
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
