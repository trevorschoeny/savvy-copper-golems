package com.trevorschoeny.savvycoppergolems.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every end of a visit closes the container. Vanilla closes it only when the 60-tick look
 * finishes; any other end (hurt, leashed, walked off, target changed) just forgets which
 * container was open, so the lid stays up. Every one of those ends, and the normal one, goes
 * through {@code clearOpenedChestPos}, as does the golem's removal (see
 * {@link EntityRemovalMixin}), so the close goes there. After a normal visit the container
 * no longer lists the golem and this does nothing.
 */
@Mixin(CopperGolem.class)
abstract class CopperGolemCloseMixin {
    @Shadow private @Nullable BlockPos openedChestPos;

    @Inject(method = "clearOpenedChestPos", at = @At("HEAD"))
    private void savvycoppergolems$closeWhatIsOpen(CallbackInfo ci) {
        CopperGolem golem = (CopperGolem) (Object) this;
        // An unloaded container needs nothing: its open count isn't saved. isLoaded keeps this from loading a chunk.
        if (openedChestPos == null || !(golem.level() instanceof ServerLevel level) || !level.isLoaded(openedChestPos)) return;
        if (level.getBlockEntity(openedChestPos) instanceof Container container
                && container.getEntitiesWithContainerOpen().contains(golem)) container.stopOpen(golem);
    }
}
