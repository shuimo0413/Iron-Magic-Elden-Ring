package com.eldenring.spells.client.malilib;

import com.eldenring.spells.EldenRingSpellsMod;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.Collections;
import java.util.List;
import net.neoforged.fml.ModList;

/**
 * MaLiLib / Tweakerge 同款配置界面：顶部标题 + 右上角模组切换下拉 + 选项列表。
 * <p>
 * 有 MaFgLib 时由 {@link TrackingIgnoreMalilibBootstrap} 注册进 {@code Registry.CONFIG_SCREEN}，
 * 可从 Tweakerge 配置界面右上角切到本模组。
 */
public class TrackingIgnoreMalilibGui extends GuiConfigsBase {
    private static ConfigGuiTab currentTab = ConfigGuiTab.TRACKING_IGNORE;

    public TrackingIgnoreMalilibGui() {
        super(
                10,
                50,
                EldenRingSpellsMod.MOD_ID,
                null,
                "elden_ring_spells.config.title",
                resolveModVersion()
        );
    }

    private static String resolveModVersion() {
        return ModList.get()
                .getModContainerById(EldenRingSpellsMod.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("?");
    }

    @Override
    public void initGui() {
        TrackingIgnoreMalilibConfigs.pullFromCache();
        super.initGui();
        this.clearOptions();

        int buttonX = 10;
        int buttonY = 26;
        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            buttonX += createTabButton(buttonX, buttonY, -1, tab) + 2;
        }
    }

    private int createTabButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(currentTab != tab);
        this.addButton(button, new TabButtonListener(tab, this));
        return button.getWidth();
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        if (currentTab == ConfigGuiTab.TRACKING_IGNORE) {
            List<? extends IConfigBase> configs = TrackingIgnoreMalilibConfigs.OPTIONS;
            return ConfigOptionWrapper.createFor(configs);
        }
        return Collections.emptyList();
    }

    private static final class TabButtonListener implements IButtonActionListener {
        private final ConfigGuiTab tab;
        private final TrackingIgnoreMalilibGui parent;

        private TabButtonListener(ConfigGuiTab tab, TrackingIgnoreMalilibGui parent) {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            currentTab = this.tab;
            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }

    /**
     * 配置页签。目前只有「追踪排除」一页，样式对齐 Tweakerge 顶栏按钮。
     */
    public enum ConfigGuiTab {
        TRACKING_IGNORE("elden_ring_spells.config.tab.tracking_ignore");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
        }
    }
}
