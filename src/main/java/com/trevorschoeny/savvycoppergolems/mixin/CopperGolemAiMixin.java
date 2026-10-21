package com.trevorschoeny.savvycoppergolems.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.entity.animal.golem.CopperGolemAi;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

/**
 * A golem waits its turn while a player has a chest open. Vanilla only asks chests; barrels
 * track who has them open the same way, so they're asked too. (A shulker box keeps no such
 * list, so a golem doesn't wait for one, as with any other non-chest in vanilla.)
 */
@Mixin(CopperGolemAi.class)
abstract class CopperGolemAiMixin {

    @ModifyReturnValue(method = "shouldQueueForTarget", at = @At("RETURN"))
    private static Predicate<TransportItemTarget> savvycoppergolems$queueForBarrels(Predicate<TransportItemTarget> vanilla) {
        return vanilla.or(target -> target.blockEntity() instanceof BarrelBlockEntity barrel
                && !barrel.getEntitiesWithContainerOpen().isEmpty());
    }
}
