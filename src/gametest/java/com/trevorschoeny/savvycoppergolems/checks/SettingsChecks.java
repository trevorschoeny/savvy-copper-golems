package com.trevorschoeny.savvycoppergolems.checks;

import com.google.gson.JsonArray;
import com.trevorschoeny.savvycoppergolems.config.SavvyConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import testgallery.SceneWorld;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks for the settings scene (test-gallery/scenes/savvy-copper-golems/settings.json).
 *
 * <p>The scene runs twice. With the default settings every golem must put its 16 cobblestone
 * away (so each layout really is reachable); with the limits turned down every golem must still
 * hold it when it first gives up and rests. Only the settings differ between the passes, so each
 * "keeps it" is that setting's doing.
 * Settings are global and saved, so they go back to the defaults whatever happens.
 */
public class SettingsChecks implements FabricClientGameTest {
    private static final String[] TAGS = {"scs_barrel", "scs_placed", "scs_far", "scs_low", "scs_visits"};

    // Station id, golem tag, where its cobblestone container sits relative to the station origin,
    // and the height the golem starts at (it starts at (2.5, golemY, 2.5)).
    private record Spot(String id, String tag, int x, int y, int z, int golemY) {}
    private static final Spot[] SPOTS = {
            new Spot("savvycoppergolems:barrels_off", "scs_barrel", 2, 0, 4, 0),
            new Spot("savvycoppergolems:placed_off", "scs_placed", 2, 0, 4, 0),
            new Spot("savvycoppergolems:horizontal_range", "scs_far", 2, 0, 8, 0),
            new Spot("savvycoppergolems:vertical_range", "scs_low", 2, 0, 5, 2),
            new Spot("savvycoppergolems:max_visits", "scs_visits", 2, 0, 5, 0),
    };

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try {
            defaults();
            try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "settings")) {
                scene.world().getServer().runOnServer(s -> summonGolems(s, scene));
                Stations.waitForEmptyHands(ctx, scene, TAGS);
                scene.world().getServer().runOnServer(s -> {
                    for (Spot spot : SPOTS) expect(s, scene, spot, 21, 0, "with default settings");
                });
            }

            SavvyConfig.setBarrels(false);
            SavvyConfig.setPlacedShulkers(false);
            SavvyConfig.setHorizontalRange(3);
            SavvyConfig.setVerticalRange(1);
            SavvyConfig.setMaxVisits(1);
            try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "settings")) {
                // A golem that finds no container rests (vanilla's 140-tick cooldown) and wanders
                // a little meanwhile, which can bring a container into a short range. So what's
                // checked is the decision: what each golem holds the first time it rests.
                scene.world().getServer().runOnServer(s -> summonGolems(s, scene));
                Map<String, String> atRest = new HashMap<>();
                for (int t = 0; t < 120 && atRest.size() < SPOTS.length; t++) {
                    scene.world().getServer().runOnServer(s -> {
                        for (Spot spot : SPOTS) if (!atRest.containsKey(spot.id()) && Stations.resting(s, spot.tag())) atRest.put(spot.id(), state(s, scene, spot));
                    });
                    ctx.waitTicks(10);
                }
                for (Spot spot : SPOTS) {
                    String want = "golem 16, container 5";
                    String got = atRest.getOrDefault(spot.id(), "never rested");
                    if (!got.equals(want)) throw new AssertionError(spot.id() + " with the limits turned down: when the golem first rested it should be " + want + "; was " + got);
                }
            }
        } finally {
            defaults();
        }
    }

    private static void defaults() {
        SavvyConfig.setBarrels(true);
        SavvyConfig.setPlacedShulkers(true);
        SavvyConfig.setHorizontalRange(SavvyConfig.VANILLA_HORIZONTAL);
        SavvyConfig.setVerticalRange(SavvyConfig.VANILLA_VERTICAL);
        SavvyConfig.setMaxVisits(SavvyConfig.VANILLA_MAX_VISITS);
    }

    /** The golems are summoned by the test, not the scene; see {@link Stations#summon}. */
    private static void summonGolems(MinecraftServer s, SceneWorld.Opened scene) {
        for (Spot spot : SPOTS) Stations.summon(s, scene, spot.id(), spot.tag(), spot.golemY(), "minecraft:cobblestone 16");
    }

    private static String state(MinecraftServer s, SceneWorld.Opened scene, Spot spot) {
        ItemStack held = Stations.hand(s, spot.tag());
        return "golem " + (held.is(Items.COBBLESTONE) ? held.getCount() : 0) + ", container " + Stations.looseCobble(container(s, scene, spot));
    }

    private static Container container(MinecraftServer s, SceneWorld.Opened scene, Spot spot) {
        JsonArray o = scene.station(spot.id()).getAsJsonArray("origin");
        BlockPos pos = new BlockPos(o.get(0).getAsInt() + spot.x(), o.get(1).getAsInt() + spot.y(), o.get(2).getAsInt() + spot.z());
        if (!(s.overworld().getBlockEntity(pos) instanceof Container c)) throw new AssertionError(spot.id() + ": no container at " + pos);
        return c;
    }

    /** The station's cobblestone container holds {@code inContainer}, and its golem {@code inHand}. */
    private static void expect(MinecraftServer s, SceneWorld.Opened scene, Spot spot, int inContainer, int inHand, String when) {
        int have = Stations.looseCobble(container(s, scene, spot));
        if (have != inContainer) throw new AssertionError(spot.id() + " " + when + ": container should hold " + inContainer + " cobblestone; holds " + have);
        ItemStack held = Stations.hand(s, spot.tag());
        int holding = held.is(Items.COBBLESTONE) ? held.getCount() : 0;
        if (holding != inHand) throw new AssertionError(spot.id() + " " + when + ": golem should hold " + inHand + " cobblestone; holds " + held);
    }
}
