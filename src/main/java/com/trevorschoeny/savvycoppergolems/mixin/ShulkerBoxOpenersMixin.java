package com.trevorschoeny.savvycoppergolems.mixin;

import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A golem closes the placed shulker box it opened. Vanilla's golem only closes a container
 * that lists it among the entities that have it open; chests and barrels keep that list, a
 * shulker box keeps only a count. So the box opened for the golem and never closed: the lid
 * stayed up and the count climbed with every visit. Here the box lists the golems that opened
 * it, so the golem's own close goes through. Players are still only counted, as vanilla.
 */
@Mixin(ShulkerBoxBlockEntity.class)
abstract class ShulkerBoxOpenersMixin {
    // ponytail: a golem interrupted mid-visit (hit, leashed) never closes, so the lid stays up
    // until the chunk reloads; chests close by a range recheck, add one here if that shows up.
    @Unique private final List<ContainerUser> savvycoppergolems$golems = new ArrayList<>();

    @Inject(method = "startOpen", at = @At("TAIL"))
    private void savvycoppergolems$listGolem(ContainerUser user, CallbackInfo ci) {
        if (user instanceof CopperGolem && !((BlockEntity) (Object) this).isRemoved()) savvycoppergolems$golems.add(user);
    }

    @Inject(method = "stopOpen", at = @At("HEAD"))
    private void savvycoppergolems$unlistGolem(ContainerUser user, CallbackInfo ci) {
        savvycoppergolems$golems.remove(user);
    }

    /** Vanilla's default answers an empty list; this is what the golem asks before closing. */
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return List.copyOf(savvycoppergolems$golems);
    }
}
