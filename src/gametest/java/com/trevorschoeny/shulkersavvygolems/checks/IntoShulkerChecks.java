package com.trevorschoeny.shulkersavvygolems.checks;

import com.google.gson.JsonArray;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
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

/**
 * Checks for the into-shulkers scene (test-gallery/scenes/shulker-savvy-golems/into-shulkers.json).
 *
 * <p>Every station has a real copper golem holding its item and a chest at (2, 0, 4), boxed
 * in glass so a golem can't reach another station's chest. The golems all work at once, so
 * the test waits for every one that should empty its hand, then reads each chest.
 */
public class IntoShulkerChecks implements FabricClientGameTest {
    private static final String[] EMPTIES = {"ssg_into", "ssg_loose", "ssg_shulker", "ssg_full", "ssg_empty", "ssg_nest"};

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "shulker-savvy-golems", "into-shulkers")) {
            // A golem waits 60 ticks at a chest before it acts; give them all a generous minute.
            for (int t = 0; t < 60 && !scene.world().getServer().computeOnServer(IntoShulkerChecks::allEmptied); t++) ctx.waitTicks(20);
            ctx.waitTicks(100);   // the no-match golem has had its look at its chest too
            scene.world().getServer().runOnServer(s -> {
                check(s, scene, "shulkersavvygolems:into_shulker", 0, 21, -1, 0);
                check(s, scene, "shulkersavvygolems:loose_first", 1, 5, 0, 21);
                check(s, scene, "shulkersavvygolems:shulker_first", 0, 21, 1, 5);
                check(s, scene, "shulkersavvygolems:full_falls_through", 1, 21, -1, 0);
                check(s, scene, "shulkersavvygolems:empty_shulker", 0, 16, -1, 0);
                noMatch(s, scene);
                noNesting(s, scene);
            });
        }
    }

    private static boolean allEmptied(MinecraftServer s) {
        for (String tag : EMPTIES) if (!hand(s, tag).isEmpty()) return false;
        return true;
    }

    /**
     * The chest's shulker in {@code shulkerSlot} holds {@code inShulker} cobblestone, and the
     * loose stack in {@code looseSlot} (if any, else -1) holds {@code loose}. Nothing else in
     * the chest is loose cobblestone.
     */
    private static void check(MinecraftServer s, SceneWorld.Opened scene, String id, int shulkerSlot, int inShulker, int looseSlot, int loose) {
        Container chest = chest(s, scene, id);
        int got = cobble(contents(chest.getItem(shulkerSlot)));
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
        String id = "shulkersavvygolems:no_match";
        ItemStack held = hand(s, "ssg_nomatch");
        if (!held.is(Items.COBBLESTONE) || held.getCount() != 16) throw new AssertionError(id + ": golem should still hold 16 cobblestone; holds " + held);
        Container chest = chest(s, scene, id);
        if (cobble(contents(chest.getItem(0))) != 0) throw new AssertionError(id + ": cobblestone went into the dirt shulker");
    }

    /** Station no_nesting: a held shulker box goes in the first free chest slot, never inside a shulker. */
    private static void noNesting(MinecraftServer s, SceneWorld.Opened scene) {
        String id = "shulkersavvygolems:no_nesting";
        Container chest = chest(s, scene, id);
        if (!contents(chest.getItem(0)).isEmpty()) throw new AssertionError(id + ": the shulker in slot 0 should still be empty");
        if (!chest.getItem(1).is(Items.SHULKER_BOX)) throw new AssertionError(id + ": slot 1 should hold the golem's shulker box; holds " + chest.getItem(1));
    }

    private static Container chest(MinecraftServer s, SceneWorld.Opened scene, String id) {
        JsonArray o = scene.station(id).getAsJsonArray("origin");
        BlockPos pos = new BlockPos(o.get(0).getAsInt() + 2, o.get(1).getAsInt(), o.get(2).getAsInt() + 4);
        if (!(s.overworld().getBlockEntity(pos) instanceof Container c)) throw new AssertionError(id + ": no chest at " + pos);
        return c;
    }

    private static ItemStack hand(MinecraftServer s, String tag) {
        for (Entity e : s.overworld().getAllEntities())
            if (e.entityTags().contains(tag) && e instanceof LivingEntity l) return l.getItemBySlot(EquipmentSlot.MAINHAND);
        throw new AssertionError("no golem tagged " + tag);
    }

    private static List<ItemStack> contents(ItemStack shulker) {
        List<ItemStack> out = new ArrayList<>();
        shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().forEach(out::add);
        return out;
    }

    private static int cobble(List<ItemStack> stacks) {
        return stacks.stream().filter(st -> st.is(Items.COBBLESTONE)).mapToInt(ItemStack::getCount).sum();
    }
}
