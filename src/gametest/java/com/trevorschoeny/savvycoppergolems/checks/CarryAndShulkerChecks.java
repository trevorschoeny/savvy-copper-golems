package com.trevorschoeny.savvycoppergolems.checks;

import com.trevorschoeny.savvycoppergolems.config.SavvyConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import testgallery.SceneWorld;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks for the carry-and-shulkers scene (test-gallery/scenes/savvy-copper-golems/carry-and-shulkers.json):
 * the "Items carried" and "Shulker boxes in chests and barrels" settings. Like {@link SettingsChecks},
 * the scene runs once with the defaults and once with the settings changed, and what's compared is
 * each golem's first decision: the first stack it picks up, and what it holds the first time it rests.
 */
public class CarryAndShulkerChecks implements FabricClientGameTest {
    private static final String CARRY = "savvycoppergolems:carry";
    private static final String SHULKERS = "savvycoppergolems:shulkers_inside_off";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try {
            defaults();
            Map<String, String> first = run(ctx);
            expect(first, CARRY, "picked up 16", "with default settings");
            expect(first, SHULKERS, "delivered, shulker 21", "with default settings");

            SavvyConfig.setCarry(40);
            SavvyConfig.setShulkersInside(false);
            first = run(ctx);
            expect(first, CARRY, "picked up 40", "with Items carried at 40");
            expect(first, SHULKERS, "rested holding 16, shulker 5", "with shulker boxes in chests off");
        } finally {
            defaults();
        }
    }

    private static void defaults() {
        SavvyConfig.setCarry(SavvyConfig.VANILLA_CARRY);
        SavvyConfig.setShulkersInside(true);
    }

    /** Opens the scene, summons both golems, and records each one's first decision (up to a minute). */
    private static Map<String, String> run(ClientGameTestContext ctx) {
        Map<String, String> first = new HashMap<>();
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "carry-and-shulkers")) {
            scene.world().getServer().runOnServer(s -> {
                Stations.summon(s, scene, CARRY, "scs_carry", 0, null);
                Stations.summon(s, scene, SHULKERS, "scs_shulkers", 0, "minecraft:cobblestone 16");
            });
            for (int t = 0; t < 120 && first.size() < 2; t++) {
                scene.world().getServer().runOnServer(s -> {
                    ItemStack carried = Stations.hand(s, "scs_carry");
                    if (!first.containsKey(CARRY) && !carried.isEmpty()) first.put(CARRY, "picked up " + carried.getCount());

                    ItemStack held = Stations.hand(s, "scs_shulkers");
                    int inShulker = Stations.cobble(Stations.contents(Stations.container(s, scene, SHULKERS).getItem(0)));
                    if (!first.containsKey(SHULKERS) && held.isEmpty()) first.put(SHULKERS, "delivered, shulker " + inShulker);
                    if (!first.containsKey(SHULKERS) && Stations.resting(s, "scs_shulkers"))
                        first.put(SHULKERS, "rested holding " + (held.is(Items.COBBLESTONE) ? held.getCount() : 0) + ", shulker " + inShulker);
                });
                ctx.waitTicks(10);
            }
        }
        return first;
    }

    private static void expect(Map<String, String> first, String id, String want, String when) {
        String got = first.getOrDefault(id, "nothing within a minute");
        if (!got.equals(want)) throw new AssertionError(id + " " + when + ": the golem should have " + want + "; it " + got);
    }
}
