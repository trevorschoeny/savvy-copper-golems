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

/**
 * Checks for the storage-room scene (test-gallery/scenes/savvy-copper-golems/storage-room.json):
 * 15 chests and barrels of dirt, and the chest whose shulker holds cobblestone in the far corner,
 * so it is the 16th container from the golem.
 *
 * <p>A golem checks containers nearest first and gives up after the search limit, forgetting
 * what it checked. At vanilla's 10 it never reaches the far chest; at this mod's default (32) it
 * does. Barrels count towards the limit, since this mod makes
 * them destinations.
 */
public class StorageRoomChecks implements FabricClientGameTest {
    private static final String ID = "savvycoppergolems:storage_room";
    private static final String TAG = "scs_room";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try {
            SavvyConfig.setMaxVisits(SavvyConfig.VANILLA_MAX_VISITS);
            try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "storage-room")) {
                scene.world().getServer().runOnServer(s -> Stations.summon(s, scene, ID, TAG, 0, "minecraft:cobblestone 16"));
                // Ten 3-second looks plus the walking: wait for the golem to give up the first time.
                Stations.waitUntil(ctx, scene, 120, s -> Stations.resting(s, TAG));
                String got = scene.world().getServer().computeOnServer(s -> state(s, scene));
                if (!got.equals("golem 16, shulker 5")) throw new AssertionError(ID + " with vanilla's limit: when the golem first rested it should be golem 16, shulker 5; was " + got);
            }

            SavvyConfig.setMaxVisits(SavvyConfig.DEFAULT_MAX_VISITS);
            try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "storage-room")) {
                scene.world().getServer().runOnServer(s -> Stations.summon(s, scene, ID, TAG, 0, "minecraft:cobblestone 16"));
                Stations.waitUntil(ctx, scene, 150, s -> Stations.hand(s, TAG).isEmpty());
                String got = scene.world().getServer().computeOnServer(s -> state(s, scene));
                if (!got.equals("golem 0, shulker 21")) throw new AssertionError(ID + " with the default limit: should end golem 0, shulker 21; was " + got);
            }
        } finally {
            SavvyConfig.setMaxVisits(SavvyConfig.DEFAULT_MAX_VISITS);
        }
    }

    private static String state(MinecraftServer s, SceneWorld.Opened scene) {
        ItemStack held = Stations.hand(s, TAG);
        return "golem " + (held.is(Items.COBBLESTONE) ? held.getCount() : 0) + ", shulker " + Stations.cobble(Stations.contents(target(s, scene).getItem(5)));
    }

    /** The far-corner chest at (10, 0, 10). */
    private static Container target(MinecraftServer s, SceneWorld.Opened scene) {
        JsonArray o = scene.station(ID).getAsJsonArray("origin");
        BlockPos pos = new BlockPos(o.get(0).getAsInt() + 10, o.get(1).getAsInt(), o.get(2).getAsInt() + 10);
        if (!(s.overworld().getBlockEntity(pos) instanceof Container c)) throw new AssertionError(ID + ": no container at " + pos);
        return c;
    }
}
