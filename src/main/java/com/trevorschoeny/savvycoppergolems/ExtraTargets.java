package com.trevorschoeny.savvycoppergolems;

import com.trevorschoeny.savvycoppergolems.config.SavvyConfig;
import com.trevorschoeny.savvycoppergolems.mixin.ShulkerBoxBlockInvoker;

import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The containers a golem may drop items into on top of vanilla's chests:
 * placed barrels and placed shulker boxes, each behind its own setting, both on by default.
 * Ender chests are deliberately not here: a golem has no player whose ender chest it would fill.
 *
 * <p>Copper chests, where golems pick items up, are untouched; these are destinations only.
 */
public final class ExtraTargets {
    private ExtraTargets() {}

    /** Whether any extra target is switched on; when not, every hook is exactly vanilla. */
    public static boolean anyOn() {
        return SavvyConfig.barrels() || SavvyConfig.placedShulkers();
    }

    /** The block-state half of the destination rule (vanilla: chest or trapped chest). */
    public static boolean wanted(BlockState state) {
        return SavvyConfig.barrels() && state.is(Blocks.BARREL)
                || SavvyConfig.placedShulkers() && state.getBlock() instanceof ShulkerBoxBlock;
    }

    /** The block-entity half: vanilla's target scan only looks at chest block entities. */
    public static boolean isExtra(BlockEntity be) {
        return SavvyConfig.barrels() && be instanceof BarrelBlockEntity
                || SavvyConfig.placedShulkers() && be instanceof ShulkerBoxBlockEntity;
    }

    /**
     * Whether the golem can't open the target right now. Vanilla asks the chest question (a
     * solid block or a cat on top). A barrel opens whatever is around it, as it does for a
     * player; a shulker box needs room for its lid in the way it faces, as it does for a player.
     */
    public static boolean blocked(Level level, TransportItemTarget target, boolean vanilla) {
        if (target.blockEntity() instanceof BarrelBlockEntity) return false;
        if (target.blockEntity() instanceof ShulkerBoxBlockEntity box) {
            return !ShulkerBoxBlockInvoker.savvycoppergolems$canOpen(target.state(), level, target.pos(), box);
        }
        return vanilla;
    }

    /**
     * Shulker boxes don't nest. A placed shulker box refuses one from a hopper (by face), but a
     * golem writes slots directly, so the rule is enforced here: a golem holding a shulker box
     * never counts a placed shulker box as a match.
     */
    public static boolean refuses(Container container, ItemStack held) {
        return container instanceof ShulkerBoxBlockEntity && !held.getItem().canFitInsideContainerItems();
    }
}
