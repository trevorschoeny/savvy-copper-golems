package com.trevorschoeny.savvycoppergolems.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.trevorschoeny.savvycoppergolems.ExtraTargets;
import com.trevorschoeny.savvycoppergolems.ShulkerSort;
import com.trevorschoeny.savvycoppergolems.config.SavvyConfig;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The copper golem's sorting behaviour. Five things change:
 * <ul>
 *   <li>On arrival at a container, the shulkers stored in it count too (see {@link ShulkerSort}).</li>
 *   <li>Placed barrels and shulker boxes are destinations as well as chests (see {@link ExtraTargets}).</li>
 *   <li>An empty container doesn't take an item that has a container of its own in range.</li>
 *   <li>The search range and the number of containers checked before giving up are settings.</li>
 *   <li>How many items a golem takes from a copper chest at once is a setting.</li>
 * </ul>
 * Everything else (pathing, queuing, the 60-tick look, memory of visited containers) is vanilla.
 */
@Mixin(TransportItemsBetweenContainers.class)
abstract class TransportItemsBetweenContainersMixin {

    @Shadow private static boolean isPickingUpItems(PathfinderMob body) { throw new AssertionError(); }
    @Shadow private static boolean hasItemMatchingHandItem(PathfinderMob body, Container container) { throw new AssertionError(); }
    @Shadow private static Set<GlobalPos> getVisitedPositions(PathfinderMob mob) { throw new AssertionError(); }
    @Shadow private static Set<GlobalPos> getUnreachablePositions(PathfinderMob mob) { throw new AssertionError(); }
    @Shadow private AABB getTargetSearchArea(PathfinderMob mob) { throw new AssertionError(); }
    @Shadow private int getHorizontalSearchDistance(PathfinderMob mob) { throw new AssertionError(); }
    @Shadow private @Nullable TransportItemTarget isTargetValidToPick(PathfinderMob body, Level level, BlockEntity blockEntity,
            Set<GlobalPos> visitedPositions, Set<GlobalPos> unreachablePositions, AABB targetBlockSearchArea) { throw new AssertionError(); }

    // ---- What goes into a container -------------------------------------------------------

    /**
     * A container also matches when a shulker in it would take the held item. A placed shulker
     * box never matches for a golem holding a shulker box.
     */
    @ModifyReturnValue(method = "matchesLeavingItemsRequirement", at = @At("RETURN"))
    private static boolean savvycoppergolems$orAShulkerTakes(boolean vanilla, PathfinderMob body, Container container) {
        ItemStack held = body.getMainHandItem();
        if (ExtraTargets.refuses(container, held)) return false;
        return vanilla || SavvyConfig.shulkersInside() && ShulkerSort.aShulkerTakes(container, held);
    }

    /** Fill the container in strict slot order, shulker slots included. Vanilla when it holds no shulker, or the setting is off. */
    @Inject(method = "addItemsToContainer", at = @At("HEAD"), cancellable = true)
    private static void savvycoppergolems$strictSlotWalk(PathfinderMob body, Container container, CallbackInfoReturnable<ItemStack> cir) {
        if (SavvyConfig.shulkersInside()) cir.setReturnValue(ShulkerSort.insert(container, body.getMainHandItem()));
    }

    /** How many items a golem takes from a copper chest at once (vanilla: 16). */
    @ModifyExpressionValue(method = "pickupItemFromContainer", at = @At(value = "CONSTANT", args = "intValue=16"))
    private static int savvycoppergolems$carry(int vanilla) {
        return SavvyConfig.carry();
    }

    // ---- Which containers count -----------------------------------------------------------

    /** Barrels and placed shulker boxes are destinations too (vanilla: chests and trapped chests). */
    @ModifyReturnValue(method = "isWantedBlock", at = @At("RETURN"))
    private boolean savvycoppergolems$orExtraTarget(boolean vanilla, PathfinderMob mob, BlockState block) {
        return vanilla || !isPickingUpItems(mob) && ExtraTargets.wanted(block);
    }

    /** A barrel is never blocked; a shulker box is when its lid has no room. Chests as vanilla. */
    @ModifyReturnValue(method = "isTargetBlocked", at = @At("RETURN"))
    private boolean savvycoppergolems$extraBlocked(boolean vanilla, Level level, TransportItemTarget target) {
        return ExtraTargets.blocked(level, target, vanilla);
    }

    /**
     * Vanilla's scan for the nearest target only looks at chest block entities, and a pattern
     * match there can't be widened without breaking its cast. So, after vanilla has picked its
     * nearest chest, the same scan runs again over barrels and placed shulker boxes, through
     * vanilla's own validity check, and a nearer one wins. Only while dropping items off: copper
     * chests, the pick-up side, are all chests.
     *
     * <p>Then empty containers wait their turn. Vanilla drops an item into the first container,
     * nearest first, that is empty or already holds it, so an empty chest as near as the item's
     * own chest can take it: in the usual 3x3 chest-wall sorter the hopper's input chest behind the
     * golem is exactly as far as the two top corners, and the corner items end up back in the
     * input. So when the pick is empty and an unchecked container in range already takes the held
     * item, the golem goes there instead. Items with no home still go to the nearest empty one.
     */
    @ModifyReturnValue(method = "getTransportTarget", at = @At("RETURN"))
    private Optional<TransportItemTarget> savvycoppergolems$orNearerExtra(Optional<TransportItemTarget> vanilla, ServerLevel level, PathfinderMob body) {
        if (isPickingUpItems(body)) return vanilla;

        TransportItemTarget best = vanilla.orElse(null);
        if (ExtraTargets.anyOn()) best = nearest(level, body, best, ExtraTargets::isExtra, target -> true);
        if (best == null || !best.container().isEmpty()) return Optional.ofNullable(best);

        TransportItemTarget home = nearest(level, body, null, be -> be instanceof ChestBlockEntity || ExtraTargets.anyOn() && ExtraTargets.isExtra(be),
                target -> !target.container().isEmpty() && holdsAHomeFor(body, target.container()));
        return Optional.of(home != null ? home : best);
    }

    /**
     * The nearest block entity of a {@code kind} in the golem's search area that passes vanilla's
     * validity check and {@code accept} (which sees the whole container, both halves of a double
     * chest), or {@code best} if that is nearer.
     */
    private @Nullable TransportItemTarget nearest(ServerLevel level, PathfinderMob body, @Nullable TransportItemTarget best,
            Predicate<BlockEntity> kind, Predicate<TransportItemTarget> accept) {
        AABB area = getTargetSearchArea(body);
        Set<GlobalPos> visited = getVisitedPositions(body);
        Set<GlobalPos> unreachable = getUnreachablePositions(body);
        double bestDistance = best == null ? Float.MAX_VALUE : best.pos().distToCenterSqr(body.position());

        int chunkRadius = Math.floorDiv(getHorizontalSearchDistance(body), 16) + 1;
        for (ChunkPos chunkPos : ChunkPos.rangeClosed(ChunkPos.containing(body.blockPosition()), chunkRadius).toList()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (chunk == null) continue;
            for (BlockEntity candidate : chunk.getBlockEntities().values()) {
                if (!kind.test(candidate)) continue;
                double distance = candidate.getBlockPos().distToCenterSqr(body.position());
                if (distance >= bestDistance) continue;
                TransportItemTarget target = isTargetValidToPick(body, level, candidate, visited, unreachable, area);
                if (target != null && accept.test(target)) {
                    best = target;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    /** Whether the container already holds what the golem carries: the rule in {@link #savvycoppergolems$orAShulkerTakes}, without vanilla's "or empty". */
    private static boolean holdsAHomeFor(PathfinderMob body, Container container) {
        ItemStack held = body.getMainHandItem();
        if (ExtraTargets.refuses(container, held)) return false;
        return hasItemMatchingHandItem(body, container) || SavvyConfig.shulkersInside() && ShulkerSort.aShulkerTakes(container, held);
    }

    // ---- How far a golem looks ------------------------------------------------------------

    /** Sideways search range; a golem riding something still only looks 1 block, as vanilla. */
    @ModifyReturnValue(method = "getHorizontalSearchDistance", at = @At("RETURN"))
    private int savvycoppergolems$horizontalRange(int vanilla, PathfinderMob mob) {
        return mob.isPassenger() ? vanilla : SavvyConfig.horizontalRange();
    }

    @ModifyReturnValue(method = "getVerticalSearchDistance", at = @At("RETURN"))
    private int savvycoppergolems$verticalRange(int vanilla, PathfinderMob mob) {
        return mob.isPassenger() ? vanilla : SavvyConfig.verticalRange();
    }

    /** Containers checked without a match before the golem rests and forgets them (vanilla: 10). */
    @ModifyExpressionValue(method = "setVisitedBlockPos", at = @At(value = "CONSTANT", args = "intValue=10"))
    private int savvycoppergolems$maxVisits(int vanilla) {
        return SavvyConfig.maxVisits();
    }
}
