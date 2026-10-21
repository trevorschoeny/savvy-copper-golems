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

/**
 * The copper golem's sorting behaviour. Four things change:
 * <ul>
 *   <li>On arrival at a container, the shulkers stored in it count too (see {@link ShulkerSort}).</li>
 *   <li>Placed barrels and shulker boxes are destinations as well as chests (see {@link ExtraTargets}).</li>
 *   <li>The search range and the number of containers checked before giving up are settings.</li>
 *   <li>How many items a golem takes from a copper chest at once is a setting.</li>
 * </ul>
 * Everything else (pathing, queuing, the 60-tick look, memory of visited containers) is vanilla.
 */
@Mixin(TransportItemsBetweenContainers.class)
abstract class TransportItemsBetweenContainersMixin {

    @Shadow private static boolean isPickingUpItems(PathfinderMob body) { throw new AssertionError(); }
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
     */
    @ModifyReturnValue(method = "getTransportTarget", at = @At("RETURN"))
    private Optional<TransportItemTarget> savvycoppergolems$orNearerExtra(Optional<TransportItemTarget> vanilla, ServerLevel level, PathfinderMob body) {
        if (!ExtraTargets.anyOn() || isPickingUpItems(body)) return vanilla;

        AABB area = getTargetSearchArea(body);
        Set<GlobalPos> visited = getVisitedPositions(body);
        Set<GlobalPos> unreachable = getUnreachablePositions(body);
        TransportItemTarget best = vanilla.orElse(null);
        double bestDistance = best == null ? Float.MAX_VALUE : best.pos().distToCenterSqr(body.position());

        int chunkRadius = Math.floorDiv(getHorizontalSearchDistance(body), 16) + 1;
        for (ChunkPos chunkPos : ChunkPos.rangeClosed(ChunkPos.containing(body.blockPosition()), chunkRadius).toList()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (chunk == null) continue;
            for (BlockEntity candidate : chunk.getBlockEntities().values()) {
                if (!ExtraTargets.isExtra(candidate)) continue;
                double distance = candidate.getBlockPos().distToCenterSqr(body.position());
                if (distance >= bestDistance) continue;
                TransportItemTarget target = isTargetValidToPick(body, level, candidate, visited, unreachable, area);
                if (target != null) {
                    best = target;
                    bestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(best);
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
