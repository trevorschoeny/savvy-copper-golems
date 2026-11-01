package com.trevorschoeny.savvycoppergolems.checks;

import com.google.gson.JsonArray;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import testgallery.SceneWorld;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Not a check: a long diagnostic run of the sorter cell (scene chest-wall), twelve golems
 * stacked on the stair, the copper chest refilled with the wall's items whenever it runs dry.
 * Logs every target a golem picks (with its position and the distance), every time it queues
 * behind another golem, every container it marks unreachable, every rest, and counts what lands
 * in each container. Run it alone by listing it as the only gametest entrypoint.
 */
public class ChestWallProbe implements FabricClientGameTest {
    private static final String ID = "savvycoppergolems:chest_wall";
    private static final int GOLEMS = 12, TICKS = 20 * 300;
    private static final String[] LOAD = {"stick", "copper_block", "acacia_planks", "jungle_door", "oak_planks", "oak_log",
            "oak_door", "spruce_planks", "spruce_log", "dark_oak_log", "dark_oak_planks"};

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "savvy-copper-golems", "chest-wall")) {
            JsonArray o = scene.station(ID).getAsJsonArray("origin");
            BlockPos origin = new BlockPos(o.get(0).getAsInt(), o.get(1).getAsInt(), o.get(2).getAsInt());
            Map<BlockPos, String> names = names(origin);
            scene.world().getServer().runOnServer(s -> {
                for (int i = 0; i < GOLEMS; i++)
                    s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), "summon minecraft:copper_golem "
                            + (origin.getX() + 3.5) + " " + (origin.getY() + 1) + " " + (origin.getZ() + 3.5) + " {Tags:[\"scs\",\"scs_probe\"]}");
                for (LivingEntity g : golems(s)) g.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
            });
            Map<String, Integer> delivered = new TreeMap<>();
            Map<UUID, String> last = new HashMap<>();
            Map<UUID, Set<GlobalPos>> lastUnreach = new HashMap<>();
            Map<BlockPos, Map<String, Integer>> lastCounts = new HashMap<>();
            int[] queued = {0}, unreachable = {0}, rests = {0};
            for (int t = 0; t < TICKS; t += 2) {
                ctx.waitTicks(2);
                int tick = t;
                scene.world().getServer().runOnServer(s -> {
                    Container copper = (Container) s.overworld().getBlockEntity(origin.offset(2, 1, 2));
                    if (copper.isEmpty()) for (int i = 0; i < 27; i++)
                        copper.setItem(i, new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(LOAD[i % LOAD.length])), 4));
                    List<LivingEntity> gs = golems(s);
                    for (int i = 0; i < gs.size(); i++) {
                        LivingEntity g = gs.get(i);
                        String[] st = behaviour(g);
                        BlockPos tp = st[1] == null ? null : BlockPos.of(Long.parseLong(st[1]));
                        String now = st[0] + " " + (tp == null ? "-" : names.getOrDefault(tp, tp.subtract(origin).toShortString()));
                        if (!now.equals(last.get(g.getUUID()))) {
                            String d2 = tp == null ? "" : String.format(" d2=%.4f", tp.distToCenterSqr(g.position()));
                            if (st[0].equals("QUEUING")) queued[0]++;
                            log(tick, i, g, origin, now + d2 + (st[0].equals("QUEUING") ? " (open by " + openers(s, tp) + ")" : ""));
                            last.put(g.getUUID(), now);
                        }
                        Set<GlobalPos> un = g.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS).orElse(Set.of());
                        for (GlobalPos p : un) if (!lastUnreach.getOrDefault(g.getUUID(), Set.of()).contains(p)) {
                            unreachable[0]++;
                            log(tick, i, g, origin, "UNREACHABLE " + names.getOrDefault(p.pos(), p.pos().subtract(origin).toShortString()));
                        }
                        lastUnreach.put(g.getUUID(), new HashSet<>(un));
                        if (g.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).filter(c -> c == 140).isPresent()) {
                            rests[0]++;
                            log(tick, i, g, origin, "REST (nothing left to try)");
                        }
                    }
                    for (Map.Entry<BlockPos, String> e : names.entrySet()) {
                        if (!(s.overworld().getBlockEntity(e.getKey()) instanceof Container c)) continue;
                        Map<String, Integer> counts = new HashMap<>();
                        List<Container> parts = new ArrayList<>(List.of(c));
                        if (e.getValue().contains("-") && s.overworld().getBlockEntity(e.getKey().south()) instanceof Container back) parts.add(back);
                        for (Container part : parts) for (ItemStack it : part)
                            if (!it.isEmpty()) counts.merge(it.getItem().toString().replace("minecraft:", ""), it.getCount(), Integer::sum);
                        Map<String, Integer> before = lastCounts.getOrDefault(e.getKey(), counts);
                        for (Map.Entry<String, Integer> k : counts.entrySet()) {
                            int d = k.getValue() - before.getOrDefault(k.getKey(), 0);
                            if (d > 0) {
                                delivered.merge(e.getValue() + " <- " + k.getKey(), d, Integer::sum);
                                System.out.println("PROBE t=" + tick + " DELIVERED " + d + " " + k.getKey() + " -> " + e.getValue());
                            }
                        }
                        lastCounts.put(e.getKey(), counts);
                    }
                });
            }
            StringBuilder sb = new StringBuilder("PROBE SUMMARY queued=" + queued[0] + " unreachable=" + unreachable[0] + " rests=" + rests[0] + "\n");
            delivered.forEach((k, v) -> sb.append("PROBE   ").append(k).append(": ").append(v).append('\n'));
            System.out.println(sb);
        }
    }

    private static void log(int tick, int i, LivingEntity g, BlockPos origin, String what) {
        System.out.printf("PROBE t=%d g%d at (%.4f, %.4f, %.4f) hand %s: %s%n", tick, i, g.getX() - origin.getX(), g.getY() - origin.getY(),
                g.getZ() - origin.getZ(), g.getMainHandItem().isEmpty() ? "-" : g.getMainHandItem().getItem().toString().replace("minecraft:", ""), what);
    }

    /** Readable names for the cell's containers: the nine wall chests (front halves), the input chest, the copper chest, the dropper. */
    private static Map<BlockPos, String> names(BlockPos o) {
        Map<BlockPos, String> m = new LinkedHashMap<>();
        String[] row = {"bottom", "middle", "top"}, col = {"left", "centre", "right"};
        for (int y = 0; y <= 2; y++) for (int x = 2; x <= 4; x++) m.put(o.offset(x, y, 4), row[y] + "-" + col[x - 2]);
        m.put(o.offset(4, 2, 2), "INPUT chest");
        m.put(o.offset(2, 1, 2), "copper chest");
        m.put(o.offset(5, 1, 2), "dropper (from input chest)");
        return m;
    }

    private static List<LivingEntity> golems(MinecraftServer s) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : s.overworld().getAllEntities()) if (e.entityTags().contains("scs_probe") && e instanceof LivingEntity l) out.add(l);
        out.sort(Comparator.comparing(Entity::getUUID));
        return out;
    }

    private static String openers(MinecraftServer s, BlockPos p) {
        return s.overworld().getBlockEntity(p) instanceof ChestBlockEntity c ? String.valueOf(c.getEntitiesWithContainerOpen().size()) : "?";
    }

    /** The golem's running transport behaviour: {state, target pos as long or null}. */
    private static String[] behaviour(LivingEntity g) {
        for (var b : g.getBrain().getRunningBehaviors()) if (b instanceof TransportItemsBetweenContainers tb) {
            try {
                Field state = TransportItemsBetweenContainers.class.getDeclaredField("state");
                Field target = TransportItemsBetweenContainers.class.getDeclaredField("target");
                state.setAccessible(true);
                target.setAccessible(true);
                Object tg = target.get(tb);
                return new String[]{String.valueOf(state.get(tb)), tg == null ? null : String.valueOf(((TransportItemsBetweenContainers.TransportItemTarget) tg).pos().asLong())};
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
        return new String[]{"IDLE", null};
    }
}
