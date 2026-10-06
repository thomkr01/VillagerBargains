package com.villagerbargains.client;

import com.villagerbargains.config.PricingMode;
import com.villagerbargains.config.VillagerBargainsConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Config screen opened from Mod Menu: one button that cycles MINIMUM → NORMAL → MAXIMUM.
 *
 * <p>The choice is saved as soon as it changes. It applies to trades generated from then on;
 * offers a villager already has are stored in the world and keep their price.
 */
public final class VillagerBargainsConfigScreen extends Screen {
    private static final int CONTENT_WIDTH = 260;
    private static final int BUTTON_WIDTH = 210;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public VillagerBargainsConfigScreen(Screen parent) {
        super(Component.translatable("villagerbargains.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);

        LinearLayout contents = layout.addToContents(LinearLayout.vertical().spacing(12));
        contents.defaultCellSetting().alignHorizontallyCenter();
        contents.addChild(pricingButton());
        contents.addChild(new MultiLineTextWidget(Component.translatable("villagerbargains.config.note"), font)
                .setMaxWidth(CONTENT_WIDTH)
                .setCentered(true));

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(BUTTON_WIDTH).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    private CycleButton<PricingMode> pricingButton() {
        return CycleButton.builder(VillagerBargainsConfigScreen::modeName, VillagerBargainsConfig.pricingMode())
                .withValues(PricingMode.values())
                .withTooltip(mode -> Tooltip.create(modeDescription(mode)))
                .create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("villagerbargains.config.pricing"),
                        (button, mode) -> VillagerBargainsConfig.setPricingMode(mode));
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static Component modeName(PricingMode mode) {
        return Component.translatable("villagerbargains.pricing." + key(mode));
    }

    private static Component modeDescription(PricingMode mode) {
        return Component.translatable("villagerbargains.pricing." + key(mode) + ".description");
    }

    private static String key(PricingMode mode) {
        return mode.name().toLowerCase(java.util.Locale.ROOT);
    }
}
