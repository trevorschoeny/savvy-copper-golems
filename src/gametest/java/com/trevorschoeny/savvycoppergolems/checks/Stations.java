package com.trevorschoeny.savvycoppergolems.checks;

import com.google.gson.JsonArray;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import testgallery.SceneWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * What every scene in this mod shares: each station has one copper golem, tagged, and one
 * container (chest, barrel or placed shulker box) at (2, 0, 4), boxed in glass so a golem can't
 * reach another station's container. These read the server's world after the golems have worked.
 */
final class Stations {
    private Stations() {}

    /** The golems all work at once: wait (up to a minute) until every tagged one has emptied its hand. */
    static void waitForEmptyHands(ClientGameTestContext ctx, SceneWorld.Opened scene, String... tags) {
        waitUntil(ctx, scene, s -> allEmpty(s, tags));
    }

    /** Waits up to a minute for the server's world to reach a state; the checks after it say whether it did. */
    static void waitUntil(ClientGameTestContext ctx, SceneWorld.Opened scene, Predicate<MinecraftServer> done) {
        for (int t = 0; t < 60 && !scene.world().getServer().computeOnServer(done::test); t++) ctx.waitTicks(20);
    }

    private static boolean allEmpty(MinecraftServer s, String[] tags) {
        for (String tag : tags) if (!hand(s, tag).isEmpty()) return false;
        return true;
    }

    /** The station's container at (2, 0, 4). */
    static Container container(MinecraftServer s, SceneWorld.Opened scene, String id) {
        JsonArray o = scene.station(id).getAsJsonArray("origin");
        BlockPos pos = new BlockPos(o.get(0).getAsInt() + 2, o.get(1).getAsInt(), o.get(2).getAsInt() + 4);
        if (!(s.overworld().getBlockEntity(pos) instanceof Container c)) throw new AssertionError(id + ": no container at " + pos);
        return c;
    }

    /** What the golem with this tag holds. */
    static ItemStack hand(MinecraftServer s, String tag) {
        for (Entity e : s.overworld().getAllEntities())
            if (e.entityTags().contains(tag) && e instanceof LivingEntity l) return l.getItemBySlot(EquipmentSlot.MAINHAND);
        throw new AssertionError("no golem tagged " + tag);
    }

    /** A shulker box item's contents (empty for anything else). */
    static List<ItemStack> contents(ItemStack shulker) {
        List<ItemStack> out = new ArrayList<>();
        shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().forEach(out::add);
        return out;
    }

    static int cobble(List<ItemStack> stacks) {
        return stacks.stream().filter(st -> st.is(Items.COBBLESTONE)).mapToInt(ItemStack::getCount).sum();
    }

    /** Loose cobblestone in the container's own slots. */
    static int looseCobble(Container c) {
        int n = 0;
        for (ItemStack st : c) if (st.is(Items.COBBLESTONE)) n += st.getCount();
        return n;
    }
}
