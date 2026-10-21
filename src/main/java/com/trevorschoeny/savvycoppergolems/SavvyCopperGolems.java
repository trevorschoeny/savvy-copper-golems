package com.trevorschoeny.savvycoppergolems;

import com.trevorschoeny.savvycoppergolems.config.SavvyConfig;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry point. Loads the settings so they're known before any golem goes looking. */
public class SavvyCopperGolems implements ModInitializer {

    public static final String MOD_ID = "savvycoppergolems";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        SavvyConfig.load();
        LOGGER.info("[savvycoppergolems] init: barrels {}, placed shulkers {}, {} containers, range {}x{}",
                SavvyConfig.barrels() ? "on" : "off", SavvyConfig.placedShulkers() ? "on" : "off",
                SavvyConfig.maxVisits(), SavvyConfig.horizontalRange(), SavvyConfig.verticalRange());
    }
}
