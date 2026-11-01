package com.trevorschoeny.savvycoppergolems.checks;

import com.google.gson.JsonArray;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import testgallery.SceneWorld;

import java.util.HashSet;
import java.util.Set;

/**
 * Check for the chest-wall scene (test-gallery/scenes/savvy-copper-golems/chest-wall.json), Trev's
 * 3x3 sorter cell. From the golem's stair the hopper's empty input chest behind it is exactly as
 * far as the two top-corner chests, and vanilla drops an item into the nearest container that is
 * empty or holds it, so on a tie or a hair behind centre the corner's items go to the input chest.
 *
 * <p>A golem holding dark oak logs (the top-right chest's item) has already checked the six nearer
 * wall chests and the top-left one; it stands 0.05 behind centre, so the input chest is strictly
 * nearer than the top-right chest. Vanilla puts the logs in the input chest; this mod sends them on
 * to the top-right chest, since an empty container waits while the item has a home in range.
 */
public class ChestWallChecks implements FabricClientGameTest {
    private static final String ID = "savvycoppergolems:chest_wall";
    private static final String TAG = "scs_wall";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "chest-wall")) {
            BlockPos o = origin(scene);
            scene.world().getServer().runOnServer(s -> {
                s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), "summon minecraft:copper_golem "
                        + (o.getX() + 3.5) + " " + (o.getY() + 1) + " " + (o.getZ() + 3.45)
                        + " {Tags:[\"scs\",\"" + TAG + "\"],equipment:{mainhand:{id:\"minecraft:dark_oak_log\",count:4}}}");
                LivingEntity g = Stations.golem(s, TAG);
                // The nearer wall chests and the top-left one, as if already checked: what's left is the input chest and the top-right.
                Set<GlobalPos> checked = new HashSet<>();
                for (int y = 0; y <= 2; y++) for (int x = 2; x <= 4; x++)
                    if (!(y == 2 && x == 4)) checked.add(GlobalPos.of(s.overworld().dimension(), o.offset(x, y, 4)));
                g.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, checked, 6000L);
                g.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
            });
            Stations.waitUntil(ctx, scene, 30, s -> Stations.hand(s, TAG).isEmpty());
            String got = scene.world().getServer().computeOnServer(s ->
                    "top-right " + logs(s, o.offset(4, 2, 4)) + ", input " + (logs(s, o.offset(4, 2, 2)) + logs(s, o.offset(5, 1, 2))));
            if (!got.equals("top-right 5, input 0")) throw new AssertionError(ID + ": the dark oak logs should reach the top-right chest (top-right 5, input 0); was " + got);
        }
    }

    private static BlockPos origin(SceneWorld.Opened scene) {
        JsonArray o = scene.station(ID).getAsJsonArray("origin");
        return new BlockPos(o.get(0).getAsInt(), o.get(1).getAsInt(), o.get(2).getAsInt());
    }

    /** Dark oak logs at {@code pos} and the block south of it (a wall chest's back half). */
    private static int logs(MinecraftServer s, BlockPos pos) {
        int n = 0;
        for (BlockPos p : new BlockPos[]{pos, pos.south()})
            if (s.overworld().getBlockEntity(p) instanceof Container c) for (ItemStack it : c) if (it.is(Items.DARK_OAK_LOG)) n += it.getCount();
        return n;
    }
}
