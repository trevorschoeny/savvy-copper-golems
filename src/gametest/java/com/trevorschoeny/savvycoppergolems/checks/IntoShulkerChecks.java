package com.trevorschoeny.savvycoppergolems.checks;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import testgallery.SceneWorld;

/**
 * Checks for the into-shulkers scene (test-gallery/scenes/savvy-copper-golems/into-shulkers.json).
 *
 * <p>Every station has a real copper golem holding its item and a chest at (2, 0, 4), boxed
 * in glass so a golem can't reach another station's chest. The golems all work at once, so
 * the test waits for every one that should empty its hand, then reads each chest.
 */
public class IntoShulkerChecks implements FabricClientGameTest {
    private static final String[] EMPTIES = {"ssg_into", "ssg_loose", "ssg_shulker", "ssg_full", "ssg_empty", "ssg_nest"};

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "into-shulkers")) {
            // A golem waits 60 ticks at a chest before it acts; give them all a generous minute.
            Stations.waitForEmptyHands(ctx, scene, EMPTIES);
            ctx.waitTicks(100);   // the no-match golem has had its look at its chest too
            scene.world().getServer().runOnServer(s -> {
                check(s, scene, "savvycoppergolems:into_shulker", 0, 21, -1, 0);
                check(s, scene, "savvycoppergolems:loose_first", 1, 5, 0, 21);
                check(s, scene, "savvycoppergolems:shulker_first", 0, 21, 1, 5);
                check(s, scene, "savvycoppergolems:full_falls_through", 1, 21, -1, 0);
                check(s, scene, "savvycoppergolems:empty_shulker", 0, 16, -1, 0);
                noMatch(s, scene);
                noNesting(s, scene);
            });
        }
    }

    /**
     * The chest's shulker in {@code shulkerSlot} holds {@code inShulker} cobblestone, and the
     * loose stack in {@code looseSlot} (if any, else -1) holds {@code loose}. Nothing else in
     * the chest is loose cobblestone.
     */
    private static void check(MinecraftServer s, SceneWorld.Opened scene, String id, int shulkerSlot, int inShulker, int looseSlot, int loose) {
        Container chest = Stations.container(s, scene, id);
        int got = Stations.cobble(Stations.contents(chest.getItem(shulkerSlot)));
        if (got != inShulker) throw new AssertionError(id + ": shulker in slot " + shulkerSlot + " should hold " + inShulker + " cobblestone; holds " + got);
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack st = chest.getItem(i);
            int want = i == looseSlot ? loose : 0;
            int have = st.is(Items.COBBLESTONE) ? st.getCount() : 0;
            if (have != want) throw new AssertionError(id + ": loose slot " + i + " should hold " + want + " cobblestone; holds " + have);
        }
    }

    /** Station no_match: a shulker of dirt doesn't make the chest a match, so the golem keeps its 16. */
    private static void noMatch(MinecraftServer s, SceneWorld.Opened scene) {
        String id = "savvycoppergolems:no_match";
        ItemStack held = Stations.hand(s, "ssg_nomatch");
        if (!held.is(Items.COBBLESTONE) || held.getCount() != 16) throw new AssertionError(id + ": golem should still hold 16 cobblestone; holds " + held);
        Container chest = Stations.container(s, scene, id);
        if (Stations.cobble(Stations.contents(chest.getItem(0))) != 0) throw new AssertionError(id + ": cobblestone went into the dirt shulker");
    }

    /** Station no_nesting: a held shulker box goes in the first free chest slot, never inside a shulker. */
    private static void noNesting(MinecraftServer s, SceneWorld.Opened scene) {
        String id = "savvycoppergolems:no_nesting";
        Container chest = Stations.container(s, scene, id);
        if (!Stations.contents(chest.getItem(0)).isEmpty()) throw new AssertionError(id + ": the shulker in slot 0 should still be empty");
        if (!chest.getItem(1).is(Items.SHULKER_BOX)) throw new AssertionError(id + ": slot 1 should hold the golem's shulker box; holds " + chest.getItem(1));
    }
}
