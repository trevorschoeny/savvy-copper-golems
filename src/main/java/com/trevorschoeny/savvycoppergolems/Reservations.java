package com.trevorschoeny.savvycoppergolems;

import com.trevlar.slotkit.api.window.Automation;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * What a golem may put where, with SlotKit's automation rules when SlotKit is installed (a
 * slot reserved for an item, a capped slot, a locked one) and vanilla's when it isn't.
 * SlotKit is optional: without it nothing can reserve or cap a slot, so vanilla's answer is
 * the whole answer. SlotKit itself makes a chest with a reserved slot match at vanilla's
 * golem, and fills reserved slots first; this lets Savvy Copper Golems' own home search
 * count the same room the put will find.
 */
public final class Reservations {
    private Reservations() {}

    private static final boolean SLOTKIT = FabricLoader.getInstance().isModLoaded("slotkit");

    /** How many of {@code held} the chest's slot takes. */
    static int room(Container chest, int slot, ItemStack held) {
        return SLOTKIT ? SlotKitSide.room(chest, slot, held) : vanillaRoom(chest.getItem(slot), held);
    }

    /** How many of {@code held} slot {@code i} of {@code box} (a shulker box item; {@code contents} its slots) takes. */
    static int room(ItemStack box, SimpleContainer contents, int i, ItemStack held) {
        return SLOTKIT ? SlotKitSide.room(box, i, held) : vanillaRoom(contents.getItem(i), held);
    }

    /** Whether a slot reserved for {@code held}, in the container or a box in it, has room for some of it. */
    public static boolean wanted(Container container, ItemStack held) {
        return SLOTKIT && SlotKitSide.wants(container, held);
    }

    // Vanilla's per-slot fill: an empty slot takes the stack, a matching one tops up to full.
    private static int vanillaRoom(ItemStack there, ItemStack held) {
        if (there.isEmpty()) return held.getCount();
        if (!ItemStack.isSameItemSameComponents(there, held)) return 0;
        return Math.max(0, there.getMaxStackSize() - there.getCount());
    }

    /** Every SlotKit reference, in a class that only loads when SlotKit is there. */
    private static final class SlotKitSide {
        static int room(Container chest, int slot, ItemStack held) {
            return Automation.room(chest, slot, held);
        }

        static int room(ItemStack box, int i, ItemStack held) {
            return Automation.room(box, i, held);
        }

        static boolean wants(Container container, ItemStack held) {
            return Automation.wants(container, held);
        }
    }
}
