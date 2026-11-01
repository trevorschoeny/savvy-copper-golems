package com.trevorschoeny.savvycoppergolems.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A golem removed mid-visit (killed, discarded, its chunk unloading) closes what it had open. */
@Mixin(Entity.class)
abstract class EntityRemovalMixin {

    @Inject(method = "onRemoval", at = @At("HEAD"))
    private void savvycoppergolems$golemCloses(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof CopperGolem golem) golem.clearOpenedChestPos();
    }
}
