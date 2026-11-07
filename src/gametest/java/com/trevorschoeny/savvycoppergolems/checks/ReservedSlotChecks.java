package com.trevorschoeny.savvycoppergolems.checks;

import com.trevlar.menukit.api.panel.ModeScope;
import com.trevlar.slotkit.api.item.ItemContents;
import com.trevlar.slotkit.api.window.SlotOperations;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A slot reserved for an item (a SlotKit {@link SlotOperations.Preference}, as Inventory Plus
 * registers for its reservations; here a stand-in) makes its chest a home for that item in
 * Savvy Copper Golems' search, ahead of a nearer plain empty chest that vanilla would pick:
 * <ul>
 *   <li>an empty chest with slot 13 reserved for cobblestone;</li>
 *   <li>a chest of dirt with a shulker box (of dirt) whose slot 5 is reserved;</li>
 *   <li>a chest of dirt whose reserved slot 13 is capped at 8, under the stack: still a home. 8 go in
 *       slot 13, and then the chest holds cobblestone, so the walk puts the rest in its loose slots.</li>
 * </ul>
 * A golem holding 16 cobblestone stands between the plain empty chest (2 blocks) and the
 * reserved one (3 blocks).
 */
public class ReservedSlotChecks implements ModInitializer, FabricClientGameTest {

    enum Phase { NONE, CHEST, BOX, CAPPED }

    static volatile Phase phase = Phase.NONE;
    static volatile BlockPos reserved = BlockPos.ZERO;

    @Override
    public void onInitialize() {
        SlotOperations.prefer(ModeScope.BOTH, (ref, group, incoming) -> incoming.is(Items.COBBLESTONE)
                && (phase != Phase.BOX && isReservedChestSlot(ref.container(), ref.containerSlot())
                    || phase == Phase.BOX && ref.container() instanceof ItemContents && ref.containerSlot() == 5));
        SlotOperations.limit(ModeScope.BOTH, (ref, group, incoming) ->
                phase == Phase.CAPPED && isReservedChestSlot(ref.container(), ref.containerSlot()) ? 8 : SlotOperations.NO_LIMIT);
    }

    private static boolean isReservedChestSlot(Container container, int slot) {
        return phase != Phase.NONE && slot == 13 && container instanceof ChestBlockEntity chest && chest.getBlockPos().equals(reserved);
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        List<String> failures = new ArrayList<>();
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            TestServerContext server = world.getServer();
            BlockPos golem = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).blockPosition().offset(6, 0, 0));
            BlockPos plain = golem.offset(2, 0, 0);
            reserved = golem.offset(-3, 0, 0);
            ctx.waitTicks(2);

            run(ctx, server, Phase.CHEST, golem, plain, "slot 13 16, plain chest 0",
                    s -> "slot 13 " + count(chest(s, reserved).getItem(13)) + ", plain chest " + cobble(chest(s, plain)), failures);
            run(ctx, server, Phase.BOX, golem, plain, "box slot 5 16, plain chest 0",
                    s -> "box slot 5 " + count(boxSlot5(chest(s, reserved))) + ", plain chest " + cobble(chest(s, plain)), failures);
            run(ctx, server, Phase.CAPPED, golem, plain, "slot 13 8, chest 16, plain chest 0",
                    s -> "slot 13 " + count(chest(s, reserved).getItem(13)) + ", chest " + cobble(chest(s, reserved))
                            + ", plain chest " + cobble(chest(s, plain)), failures);
        } finally {
            phase = Phase.NONE;
        }
        if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }

    /** Sets the phase up, summons the golem with 16 cobblestone, waits up to 60 s for its hand to empty, and compares. */
    private static void run(ClientGameTestContext ctx, TestServerContext server, Phase p, BlockPos golem, BlockPos plain,
                            String want, java.util.function.Function<MinecraftServer, String> got, List<String> failures) {
        // A tag per phase: the last phase's golem, killed with an empty hand, can still be found for a tick.
        String tag = "scs_reserved_" + p.name().toLowerCase();
        server.runOnServer(s -> {
            phase = p;
            for (BlockPos pos : List.of(plain, reserved)) {
                // Emptied first: a chest broken by setBlock drops what it holds.
                if (s.overworld().getBlockEntity(pos) instanceof Container old) old.clearContent();
                s.overworld().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                s.overworld().setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            }
            Container target = chest(s, reserved);
            if (p == Phase.BOX) {
                ItemStack box = new ItemStack(Items.SHULKER_BOX);
                box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIRT))));
                target.setItem(0, box);
            }
            if (p != Phase.CHEST) target.setItem(1, new ItemStack(Items.DIRT, 4));
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), "summon minecraft:copper_golem "
                    + golem.getX() + " " + golem.getY() + " " + golem.getZ()
                    + " {Tags:[\"" + tag + "\"],equipment:{mainhand:{id:\"minecraft:cobblestone\",count:16}}}");
        });
        for (int i = 0; i < 60 && !server.computeOnServer(s -> Stations.hand(s, tag).isEmpty()); i++) ctx.waitTicks(20);
        String result = server.computeOnServer(got::apply);
        if (!result.equals(want)) failures.add(p + ": want " + want + "; was " + result);
        server.runOnServer(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), "kill @e[type=copper_golem]"));
    }

    private static Container chest(MinecraftServer s, BlockPos pos) {
        return (Container) s.overworld().getBlockEntity(pos);
    }

    private static ItemStack boxSlot5(Container chest) {
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        chest.getItem(0).getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items.get(5);
    }

    private static int count(ItemStack stack) {
        return stack.is(Items.COBBLESTONE) ? stack.getCount() : 0;
    }

    private static int cobble(Container c) {
        int n = 0;
        for (ItemStack st : c) n += count(st);
        return n;
    }
}
