package com.trevorschoeny.savvycoppergolems.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** YACL screen: one tab, a group for where golems sort and a group for how far they look. */
public final class SavvyConfigScreen {

    private SavvyConfigScreen() {}

    private static final String SERVER_NOTE = " On a dedicated server this is the server's setting.";

    public static Screen create(Screen parent) {
        OptionGroup targets = OptionGroup.createBuilder()
                .name(Component.literal("Where golems sort"))
                .description(OptionDescription.of(Component.literal(
                        "Golems always sort into chests, and into the shulker boxes stored in them.")))
                .option(booleanOption("Barrels",
                        "Golems sort into placed barrels the same way they do into chests, "
                                + "shulker boxes stored inside included." + SERVER_NOTE,
                        true, SavvyConfig::barrels, SavvyConfig::setBarrels))
                .option(booleanOption("Placed shulker boxes",
                        "Golems sort into shulker boxes placed as blocks. A golem never puts a "
                                + "shulker box into one." + SERVER_NOTE,
                        true, SavvyConfig::placedShulkers, SavvyConfig::setPlacedShulkers))
                .build();

        OptionGroup limits = OptionGroup.createBuilder()
                .name(Component.literal("How far golems look"))
                .description(OptionDescription.of(Component.literal(
                        "Vanilla's numbers are the defaults. Bigger numbers mean more work for the server.")))
                .option(intOption("Containers checked",
                        "How many containers a golem checks without finding a place for its item "
                                + "before it rests and starts over. Vanilla: "
                                + SavvyConfig.VANILLA_MAX_VISITS + "." + SERVER_NOTE,
                        SavvyConfig.VANILLA_MAX_VISITS, SavvyConfig.MAX_VISITS_CAP,
                        SavvyConfig::maxVisits, SavvyConfig::setMaxVisits))
                .option(intOption("Sideways range",
                        "How far a golem looks for containers in each direction, in blocks. Vanilla: "
                                + SavvyConfig.VANILLA_HORIZONTAL + "." + SERVER_NOTE,
                        SavvyConfig.VANILLA_HORIZONTAL, SavvyConfig.HORIZONTAL_CAP,
                        SavvyConfig::horizontalRange, SavvyConfig::setHorizontalRange))
                .option(intOption("Up and down range",
                        "How far above and below itself a golem looks for containers, in blocks. Vanilla: "
                                + SavvyConfig.VANILLA_VERTICAL + "." + SERVER_NOTE,
                        SavvyConfig.VANILLA_VERTICAL, SavvyConfig.VERTICAL_CAP,
                        SavvyConfig::verticalRange, SavvyConfig::setVerticalRange))
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Savvy Copper Golems"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Golems"))
                        .group(targets)
                        .group(limits)
                        .build())
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> booleanOption(String name, String description, boolean defaultValue,
                                                 Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(BooleanControllerBuilder::create)
                .build();
    }

    private static Option<Integer> intOption(String name, String description, int defaultValue, int max,
                                             Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, max).step(1))
                .build();
    }
}
