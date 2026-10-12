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

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Config screen opened from Mod Menu: three buttons that cycle MINIMUM → NORMAL → MAXIMUM, for
 * the pricing mode, the level of traded enchanted books and the strength of traded enchanted gear,
 * and two on/off buttons that bring back vanilla demand and vanilla wandering trader prices.
 *
 * <p>Each choice is saved as soon as it changes. It applies to trades generated from then on;
 * offers a villager already has are stored in the world and keep their price and level.
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
        contents.addChild(modeButton("pricing", VillagerBargainsConfig.pricingMode(),
                VillagerBargainsConfig::setPricingMode));
        contents.addChild(modeButton("bookLevels", VillagerBargainsConfig.bookLevelMode(),
                VillagerBargainsConfig::setBookLevelMode));
        contents.addChild(modeButton("gearStrength", VillagerBargainsConfig.gearStrengthMode(),
                VillagerBargainsConfig::setGearStrengthMode));
        contents.addChild(flagButton("vanillaDemand", VillagerBargainsConfig.vanillaDemand(),
                VillagerBargainsConfig::setVanillaDemand));
        contents.addChild(flagButton("vanillaWanderingTrader", VillagerBargainsConfig.vanillaWanderingTrader(),
                VillagerBargainsConfig::setVanillaWanderingTrader));
        contents.addChild(new MultiLineTextWidget(Component.translatable("villagerbargains.config.note"), font)
                .setMaxWidth(CONTENT_WIDTH)
                .setCentered(true));

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(BUTTON_WIDTH).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    /**
     * A button for one setting. Its label is {@code villagerbargains.config.<setting>}, each value
     * is named by {@code villagerbargains.<setting>.<mode>} with a {@code .description} tooltip.
     */
    private CycleButton<PricingMode> modeButton(String setting, PricingMode current, Consumer<PricingMode> onChange) {
        return CycleButton.builder((PricingMode mode) -> modeText(setting, mode, ""), current)
                .withValues(PricingMode.values())
                .withTooltip(mode -> Tooltip.create(modeText(setting, mode, ".description")))
                .create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("villagerbargains.config." + setting),
                        (button, mode) -> onChange.accept(mode));
    }

    /**
     * An on/off button for one setting. Its label is {@code villagerbargains.config.<setting>},
     * its tooltip is {@code villagerbargains.<setting>.on.description} or {@code .off.description}.
     */
    private CycleButton<Boolean> flagButton(String setting, boolean current, Consumer<Boolean> onChange) {
        return CycleButton.onOffBuilder(current)
                .withTooltip(on -> Tooltip.create(
                        Component.translatable("villagerbargains." + setting + (on ? ".on" : ".off") + ".description")))
                .create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("villagerbargains.config." + setting),
                        (button, on) -> onChange.accept(on));
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static Component modeText(String setting, PricingMode mode, String suffix) {
        return Component.translatable("villagerbargains." + setting + "." + mode.name().toLowerCase(Locale.ROOT) + suffix);
    }
}
