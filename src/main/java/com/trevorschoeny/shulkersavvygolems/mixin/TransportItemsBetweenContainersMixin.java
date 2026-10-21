package com.trevorschoeny.shulkersavvygolems.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.trevorschoeny.shulkersavvygolems.ShulkerSort;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The copper golem's sorting behaviour. It still picks the nearest unvisited chest and
 * visits it once; on arrival, the shulkers in that chest count too (see {@link ShulkerSort}).
 */
@Mixin(TransportItemsBetweenContainers.class)
abstract class TransportItemsBetweenContainersMixin {

    /** A chest also matches when a shulker in it would take the held item. */
    @ModifyReturnValue(method = "matchesLeavingItemsRequirement", at = @At("RETURN"))
    private static boolean shulkersavvygolems$orAShulkerTakes(boolean vanilla, PathfinderMob body, Container container) {
        return vanilla || ShulkerSort.aShulkerTakes(container, body.getMainHandItem());
    }

    /** Fill the chest in strict slot order, shulker slots included. Vanilla when it holds no shulker. */
    @Inject(method = "addItemsToContainer", at = @At("HEAD"), cancellable = true)
    private static void shulkersavvygolems$strictSlotWalk(PathfinderMob body, Container container, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(ShulkerSort.insert(container, body.getMainHandItem()));
    }
}
