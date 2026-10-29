package com.trevorschoeny.savvycoppergolems.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.trevorschoeny.savvycoppergolems.config.SavvyConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Crammed golems stay stacked. Vanilla skips the push between two entities closer than 0.01
 * blocks on both axes; past that the push is strongest up close, so one small nudge scatters a
 * whole crowd. Between two copper golems the cutoff is 0.1 blocks instead, so a stacked crowd
 * absorbs small nudges. Every other pair of entities pushes as vanilla.
 */
@Mixin(Entity.class)
abstract class EntityPushMixin {

    @ModifyExpressionValue(method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "CONSTANT", args = "doubleValue=0.009999999776482582"))
    private double savvycoppergolems$golemCutoff(double vanilla, Entity other) {
        return SavvyConfig.stayStacked() && (Object) this instanceof CopperGolem && other instanceof CopperGolem ? 0.1 : vanilla;
    }
}
