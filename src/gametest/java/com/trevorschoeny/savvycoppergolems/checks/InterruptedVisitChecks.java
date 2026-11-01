package com.trevorschoeny.savvycoppergolems.checks;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import testgallery.SceneWorld;

import java.util.function.Consumer;

/**
 * A golem interrupted mid-visit still closes the container. Uses the more-targets scene's
 * placed_shulker station: once the golem has the box open, the player hits it (it panics and
 * drops the visit) or it is removed, and the lid has to close.
 */
public class InterruptedVisitChecks implements FabricClientGameTest {
    private static final String ID = "savvycoppergolems:placed_shulker";
    private static final String TAG = "scg_placed";

    @Override
    public void runTest(ClientGameTestContext ctx) {
        // A hit from the player: only damage with an attacker makes a golem panic.
        interrupt(ctx, "hurt", g -> g.hurtServer((ServerLevel) g.level(),
                g.damageSources().playerAttack(g.level().getServer().getPlayerList().getPlayers().getFirst()), 1));
        interrupt(ctx, "removed", LivingEntity::discard);
    }

    private static void interrupt(ClientGameTestContext ctx, String how, Consumer<LivingEntity> interruption) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "more-targets")) {
            Stations.waitUntil(ctx, scene, s -> !box(s, scene).isClosed());
            if (scene.world().getServer().computeOnServer(s -> box(s, scene).isClosed())) throw new AssertionError(ID + ": the golem never opened the box");
            scene.world().getServer().runOnServer(s -> interruption.accept(Stations.golem(s, TAG)));
            ctx.waitTicks(30);   // the lid takes 10 ticks to close; well short of the golem's next visit
            if (!scene.world().getServer().computeOnServer(s -> box(s, scene).isClosed()))
                throw new AssertionError(ID + ": golem " + how + " mid-visit; the box should close, but its lid is still up");
        }
    }

    private static ShulkerBoxBlockEntity box(MinecraftServer s, SceneWorld.Opened scene) {
        return (ShulkerBoxBlockEntity) Stations.container(s, scene, ID);
    }
}
