package com.trevorschoeny.savvycoppergolems.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** ModMenu entry point: opens the settings screen from the mods list. */
public final class SavvyConfigModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SavvyConfigScreen::create;
    }
}
