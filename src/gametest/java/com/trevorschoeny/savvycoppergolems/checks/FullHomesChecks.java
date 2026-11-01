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
import net.minecraft.world.level.block.Blocks;
import testgallery.SceneWorld;

/**
 * Check on the storage-room scene (test-gallery/scenes/savvy-copper-golems/storage-room.json),
 * reworked on the server: all 16 containers are filled to the brim with dirt, and an empty chest is
 * placed at (2, 0, 4), the nearest container to the golem, which holds 16 dirt.
 *
 * <p>Vanilla puts the dirt in the empty chest. An empty container waits only for a container that
 * holds the item and has room for it; full chests of dirt are no home, so with the default search
 * limit the dirt still goes in the empty chest instead of the golem checking full chests and resting.
 */
public class FullHomesChecks implements FabricClientGameTest {
    private static final String ID = "savvycoppergolems:storage_room";
    private static final String TAG = "scs_full";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        SavvyConfig.setMaxVisits(SavvyConfig.DEFAULT_MAX_VISITS);
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "storage-room")) {
            scene.world().getServer().runOnServer(s -> {
                BlockPos o = origin(scene);
                for (int x = 4; x <= 10; x += 2) for (int z = 4; z <= 10; z += 2)
                    if (s.overworld().getBlockEntity(o.offset(x, 0, z)) instanceof Container c)
                        for (int slot = 0; slot < c.getContainerSize(); slot++)
                            if (c.getItem(slot).isEmpty() || c.getItem(slot).is(Items.DIRT)) c.setItem(slot, new ItemStack(Items.DIRT, 64));
                s.overworld().setBlockAndUpdate(o.offset(2, 0, 4), Blocks.CHEST.defaultBlockState());
                Stations.summon(s, scene, ID, TAG, 0, "minecraft:dirt 16");
            });
            Stations.waitUntil(ctx, scene, 120, s -> Stations.hand(s, TAG).isEmpty() || Stations.resting(s, TAG));
            String got = scene.world().getServer().computeOnServer(s -> {
                ItemStack held = Stations.hand(s, TAG);
                return "golem " + (held.is(Items.DIRT) ? held.getCount() : 0) + ", empty chest " + dirt(Stations.container(s, scene, ID));
            });
            if (!got.equals("golem 0, empty chest 16")) throw new AssertionError(ID + " with full chests of dirt: the dirt should go in the empty chest (golem 0, empty chest 16); was " + got);
        }
    }

    private static BlockPos origin(SceneWorld.Opened scene) {
        JsonArray o = scene.station(ID).getAsJsonArray("origin");
        return new BlockPos(o.get(0).getAsInt(), o.get(1).getAsInt(), o.get(2).getAsInt());
    }

    private static int dirt(Container c) {
        int n = 0;
        for (ItemStack st : c) if (st.is(Items.DIRT)) n += st.getCount();
        return n;
    }
}
