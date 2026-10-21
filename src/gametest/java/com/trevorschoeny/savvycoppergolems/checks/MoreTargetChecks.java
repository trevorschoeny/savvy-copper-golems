package com.trevorschoeny.savvycoppergolems.checks;

import com.google.gson.JsonArray;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import testgallery.SceneWorld;

/**
 * Checks for the more-targets scene (test-gallery/scenes/savvy-copper-golems/more-targets.json):
 * placed barrels and shulker boxes as destinations, with both settings at their default (on).
 */
public class MoreTargetChecks implements FabricClientGameTest {
    private static final String[] EMPTIES = {"scg_barrel", "scg_barrel_shulker", "scg_covered", "scg_placed"};

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "more-targets")) {
            Stations.waitForEmptyHands(ctx, scene, EMPTIES);
            Stations.waitUntil(ctx, scene, s -> Stations.looseCobble(barrelOf(s, scene)) == 6);
            ctx.waitTicks(100);   // the golems that should keep their item have had their look too
            scene.world().getServer().runOnServer(s -> {
                loose(s, scene, "savvycoppergolems:barrel", 21);
                loose(s, scene, "savvycoppergolems:barrel_covered", 21);
                loose(s, scene, "savvycoppergolems:placed_shulker", 21);
                shulkerInBarrel(s, scene);
                lidBlocked(s, scene);
                placedNoNesting(s, scene);
                copperChestToBarrel(s, scene);
            });
        }
    }

    /** The container's own slots hold {@code want} cobblestone in all. */
    private static void loose(MinecraftServer s, SceneWorld.Opened scene, String id, int want) {
        int have = Stations.looseCobble(Stations.container(s, scene, id));
        if (have != want) throw new AssertionError(id + ": should hold " + want + " cobblestone; holds " + have);
    }

    /** Station shulker_in_barrel: the shulker in slot 0 took all 16, the barrel has no loose cobblestone. */
    private static void shulkerInBarrel(MinecraftServer s, SceneWorld.Opened scene) {
        String id = "savvycoppergolems:shulker_in_barrel";
        Container barrel = Stations.container(s, scene, id);
        int inShulker = Stations.cobble(Stations.contents(barrel.getItem(0)));
        if (inShulker != 21) throw new AssertionError(id + ": shulker should hold 21 cobblestone; holds " + inShulker);
        if (Stations.looseCobble(barrel) != 0) throw new AssertionError(id + ": cobblestone went loose into the barrel");
    }

    /** Station lid_blocked: a shulker box that can't open is skipped; the golem keeps its 16. */
    private static void lidBlocked(MinecraftServer s, SceneWorld.Opened scene) {
        String id = "savvycoppergolems:lid_blocked";
        ItemStack held = Stations.hand(s, "scg_lid");
        if (!held.is(Items.COBBLESTONE) || held.getCount() != 16) throw new AssertionError(id + ": golem should still hold 16 cobblestone; holds " + held);
        loose(s, scene, id, 5);
    }

    /** Station placed_no_nesting: the golem keeps its shulker box and the placed one stays empty. */
    private static void placedNoNesting(MinecraftServer s, SceneWorld.Opened scene) {
        String id = "savvycoppergolems:placed_no_nesting";
        if (!Stations.hand(s, "scg_nest").is(Items.SHULKER_BOX)) throw new AssertionError(id + ": golem should still hold its shulker box");
        if (!Stations.container(s, scene, id).isEmpty()) throw new AssertionError(id + ": the placed shulker box should still be empty");
    }

    /**
     * Station copper_chest_to_barrel: picking up still works as vanilla (barrels are never a
     * source), and what the golem picked up goes into the barrel.
     */
    private static void copperChestToBarrel(MinecraftServer s, SceneWorld.Opened scene) {
        String id = "savvycoppergolems:copper_chest_to_barrel";
        int inBarrel = Stations.looseCobble(barrelOf(s, scene));
        if (inBarrel != 6) throw new AssertionError(id + ": barrel should hold 6 cobblestone; holds " + inBarrel);
        if (!copperChestOf(s, scene).isEmpty()) throw new AssertionError(id + ": the copper chest should be empty");
    }

    private static Container barrelOf(MinecraftServer s, SceneWorld.Opened scene) { return at(s, scene, 3); }
    private static Container copperChestOf(MinecraftServer s, SceneWorld.Opened scene) { return at(s, scene, 1); }

    /** The loop station's container at (x, 0, 4). */
    private static Container at(MinecraftServer s, SceneWorld.Opened scene, int x) {
        JsonArray o = scene.station("savvycoppergolems:copper_chest_to_barrel").getAsJsonArray("origin");
        BlockPos pos = new BlockPos(o.get(0).getAsInt() + x, o.get(1).getAsInt(), o.get(2).getAsInt() + 4);
        if (!(s.overworld().getBlockEntity(pos) instanceof Container c)) throw new AssertionError("copper_chest_to_barrel: no container at " + pos);
        return c;
    }
}
