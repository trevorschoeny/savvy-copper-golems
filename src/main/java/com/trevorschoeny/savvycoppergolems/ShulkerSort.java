package com.trevorschoeny.savvycoppergolems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * How a copper golem drops its item into a chest that holds shulker boxes.
 *
 * <p>The rule: every shulker in the chest is treated exactly like a chest
 * the golem just opened, and the chest's own loose slots are one more "chest". The golem
 * walks the chest in strict slot order: a loose slot is filled the vanilla way if the chest
 * itself would take the item, a shulker slot is filled the vanilla way if that shulker would
 * take it, and then it carries on with the next slot until its hand is empty.
 *
 * <p>Vanilla's rules, mirrored from {@code TransportItemsBetweenContainers} (26.3):
 * <ul>
 *   <li>A container takes the item if it is empty, or anything in it is the same item
 *       (components ignored).</li>
 *   <li>Filling walks the slots in order: a same-item, same-components stack that isn't full
 *       is topped up; the first empty slot takes everything left.</li>
 * </ul>
 * "The chest itself" is judged on the whole chest, as vanilla does, so a chest of shulkers
 * is not empty and only takes loose items when it already holds a loose stack of them.
 * With no shulker in the chest this is exactly vanilla.
 */
public final class ShulkerSort {
    private ShulkerSort() {}

    /** Vanilla's leaving-items rule: empty, or holds the same item somewhere. */
    public static boolean takes(Iterable<ItemStack> container, ItemStack held) {
        boolean empty = true;
        for (ItemStack stack : container) {
            if (ItemStack.isSameItem(stack, held)) return true;
            if (!stack.isEmpty()) empty = false;
        }
        return empty;
    }

    /** Whether some shulker in the chest would take the item: makes the chest a match. */
    public static boolean aShulkerTakes(Container chest, ItemStack held) {
        if (!held.getItem().canFitInsideContainerItems()) return false;   // shulker boxes don't nest
        for (ItemStack stack : chest) {
            SimpleContainer box = contents(stack);
            if (box != null && takes(box, held)) return true;
        }
        return false;
    }

    /**
     * Puts the held stack into the chest, slot by slot as described above, and returns what is
     * left for the golem's hand (empty when it all went in). Only called once the chest has
     * matched, by vanilla's rule or {@link #aShulkerTakes}.
     */
    public static ItemStack insert(Container chest, ItemStack held) {
        ItemStack left = held.copy();
        boolean chestTakes = takes(chest, held);
        boolean fitsInShulker = held.getItem().canFitInsideContainerItems();

        for (int slot = 0; slot < chest.getContainerSize() && !left.isEmpty(); slot++) {
            ItemStack inSlot = chest.getItem(slot);
            SimpleContainer box = fitsInShulker ? contents(inSlot) : null;
            if (box != null) {
                // A shulker slot: searched as a chest of its own, right here in the walk.
                if (!takes(box, left)) continue;
                for (int i = 0; i < box.getContainerSize() && !left.isEmpty(); i++) left = fill(box, i, left);
                inSlot.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(box.getItems()));
                chest.setItem(slot, inSlot);
            } else if (chestTakes) {
                // A loose slot, part of "the chest itself".
                left = fill(chest, slot, left);
            }
        }
        return left;
    }

    /** Vanilla's per-slot fill: an empty slot takes it all, a matching stack is topped up. */
    private static ItemStack fill(Container container, int slot, ItemStack stack) {
        ItemStack there = container.getItem(slot);
        if (there.isEmpty()) {
            container.setItem(slot, stack);
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < there.getMaxStackSize()) {
            int moved = Math.min(there.getMaxStackSize() - there.getCount(), stack.getCount());
            there.grow(moved);
            stack.shrink(moved);
            container.setItem(slot, there);
        }
        return stack;
    }

    /** A shulker box stack's contents as a container, or null if the stack isn't a shulker box. */
    private static SimpleContainer contents(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock)) return null;
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        // 27 slots, or more if the box already holds more (Shulker Palette's extra rows).
        SimpleContainer box = new SimpleContainer(Math.max(27, contents.size()));
        contents.copyInto(box.getItems());
        return box;
    }
}
