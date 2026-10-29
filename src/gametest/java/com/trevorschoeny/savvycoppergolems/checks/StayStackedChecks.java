package com.trevorschoeny.savvycoppergolems.checks;

import com.google.gson.JsonArray;
import com.trevorschoeny.savvycoppergolems.config.SavvyConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.MinecraftServer;
import testgallery.SceneWorld;

/**
 * Checks for the stay-stacked scene (test-gallery/scenes/savvy-copper-golems/stay-stacked.json).
 *
 * <p>Two copper golems that can't walk (movement speed 0, so nothing but pushing moves them; a
 * NoAI golem doesn't move at all, pushed or not) are summoned 0.05 blocks
 * apart. With the setting off they push apart, as vanilla; with it on they stay where they are.
 * The setting goes back to its default whatever happens.
 */
public class StayStackedChecks implements FabricClientGameTest {
    private static final String ID = "savvycoppergolems:stay_stacked";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try {
            SavvyConfig.setStayStacked(false);
            double apart = gapAfterASecond(ctx);
            if (apart < 0.2) throw new AssertionError(ID + " with Stay stacked off: the golems should push apart as vanilla; gap " + apart);

            SavvyConfig.setStayStacked(true);
            double stacked = gapAfterASecond(ctx);
            if (stacked > 0.06) throw new AssertionError(ID + " with Stay stacked on: the golems should stay 0.05 apart; gap " + stacked);
        } finally {
            SavvyConfig.setStayStacked(true);
        }
    }

    /** Summons the pair, waits a second, and returns the horizontal gap between them. */
    private static double gapAfterASecond(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "stay-stacked")) {
            scene.world().getServer().runOnServer(s -> {
                summon(s, scene, "scs_stack_a", 2.5);
                summon(s, scene, "scs_stack_b", 2.55);
            });
            ctx.waitTicks(20);
            return scene.world().getServer().computeOnServer(s -> {
                var a = Stations.golem(s, "scs_stack_a").position();
                var b = Stations.golem(s, "scs_stack_b").position();
                return Math.hypot(a.x - b.x, a.z - b.z);
            });
        }
    }

    private static void summon(MinecraftServer s, SceneWorld.Opened scene, String tag, double x) {
        JsonArray o = scene.station(ID).getAsJsonArray("origin");
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), "summon minecraft:copper_golem "
                + (o.get(0).getAsInt() + x) + " " + o.get(1).getAsInt() + " " + (o.get(2).getAsInt() + 2.5)
                + " {attributes:[{id:\"minecraft:movement_speed\",base:0.0}],Tags:[\"scs\",\"" + tag + "\"]}");
    }
}
