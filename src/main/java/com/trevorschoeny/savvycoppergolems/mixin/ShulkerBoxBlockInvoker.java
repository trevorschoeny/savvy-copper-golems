package com.trevorschoeny.savvycoppergolems.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's own "is there room for the lid" check, so a golem is held to the player's rule. */
@Mixin(ShulkerBoxBlock.class)
public interface ShulkerBoxBlockInvoker {
    @Invoker("canOpen")
    static boolean savvycoppergolems$canOpen(BlockState state, Level level, BlockPos pos, ShulkerBoxBlockEntity blockEntity) {
        throw new AssertionError();
    }
}
