package com.trevorschoeny.savvycoppergolems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.trevorschoeny.savvycoppergolems.SavvyCopperGolems;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Savvy Copper Golems settings: sorting into shulkers stored in containers
 * and the two extra targets, all on by default; crammed golems staying stacked (on); how much a golem carries and its three search
 * limits, defaulting to vanilla's numbers. Same static-field + JSON shape as the other house
 * mods, persisted to {@code config/savvycoppergolems/config.json}.
 *
 * <p>Everything here is read on the server, where the golem's AI runs. On a dedicated server
 * the server's file decides; a client's copy only drives its settings screen.
 */
public final class SavvyConfig {

    private SavvyConfig() {}

    private static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Vanilla's values (TransportItemsBetweenContainers and CopperGolemAi, 26.3).
    public static final int VANILLA_MAX_VISITS = 10;
    public static final int VANILLA_HORIZONTAL = 32;
    public static final int VANILLA_VERTICAL = 8;
    public static final int VANILLA_CARRY = 16;

    // Caps keep a typo from making every golem scan hundreds of chunks per search.
    public static final int MAX_VISITS_CAP = 64;
    public static final int HORIZONTAL_CAP = 128;
    public static final int VERTICAL_CAP = 64;
    // A full stack; the golem never takes more than the stack in the slot anyway.
    public static final int CARRY_CAP = 64;

    // Golems look inside the shulker boxes stored in a chest or barrel. Off: vanilla's chest rule.
    private static boolean shulkersInside = true;

    // Crammed golems stay stacked: two copper golems within 0.1 blocks of each other (vanilla:
    // 0.01) don't push each other apart, so a nudge doesn't scatter a crowd around its block.
    private static boolean stayStacked = true;

    // Golems sort into placed barrels, the same way they do into chests.
    private static boolean barrels = true;
    // Golems sort into placed shulker boxes (never a shulker box into one).
    private static boolean placedShulkers = true;
    // Containers a golem checks without finding a match before it rests and starts over.
    private static int maxVisits = VANILLA_MAX_VISITS;
    // How far a golem looks for containers, sideways and up or down, in blocks.
    private static int horizontalRange = VANILLA_HORIZONTAL;
    private static int verticalRange = VANILLA_VERTICAL;
    // How many items a golem takes from a copper chest at once.
    private static int carry = VANILLA_CARRY;

    private static boolean loaded = false;

    private static Path filePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(SavvyCopperGolems.MOD_ID)
                .resolve("config.json");
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        Path path = filePath();
        if (!Files.exists(path)) {
            SavvyCopperGolems.LOGGER.info("[config] no config at {}, using defaults", path);
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            shulkersInside = readBool(root, "shulkersInside", shulkersInside);
            stayStacked = readBool(root, "stayStacked", stayStacked);
            barrels = readBool(root, "barrels", barrels);
            placedShulkers = readBool(root, "placedShulkers", placedShulkers);
            maxVisits = readInt(root, "maxVisits", maxVisits, MAX_VISITS_CAP);
            horizontalRange = readInt(root, "horizontalRange", horizontalRange, HORIZONTAL_CAP);
            verticalRange = readInt(root, "verticalRange", verticalRange, VERTICAL_CAP);
            carry = readInt(root, "carry", carry, CARRY_CAP);
            SavvyCopperGolems.LOGGER.info("[config] loaded from {}", path);
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            SavvyCopperGolems.LOGGER.error("[config] failed to read {}, using defaults", path, e);
        }
    }

    private static boolean readBool(JsonObject root, String key, boolean fallback) {
        return root.has(key) && root.get(key).isJsonPrimitive()
                ? root.get(key).getAsBoolean() : fallback;
    }

    /** A hand-edited file can hold anything; out-of-range numbers are clamped, not rejected. */
    private static int readInt(JsonObject root, String key, int fallback, int cap) {
        if (!root.has(key) || !root.get(key).isJsonPrimitive()) return fallback;
        try {
            return clamp(root.get(key).getAsInt(), cap);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int v, int cap) { return Math.max(1, Math.min(cap, v)); }

    private static void save() {
        Path path = filePath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", CURRENT_VERSION);
            root.addProperty("shulkersInside", shulkersInside);
            root.addProperty("stayStacked", stayStacked);
            root.addProperty("barrels", barrels);
            root.addProperty("placedShulkers", placedShulkers);
            root.addProperty("maxVisits", maxVisits);
            root.addProperty("horizontalRange", horizontalRange);
            root.addProperty("verticalRange", verticalRange);
            root.addProperty("carry", carry);
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException e) {
            SavvyCopperGolems.LOGGER.error("[config] failed to write {}, changes won't persist", path, e);
        }
    }

    public static boolean shulkersInside() { return shulkersInside; }
    public static void setShulkersInside(boolean v) { shulkersInside = v; save(); }

    public static boolean stayStacked() { return stayStacked; }
    public static void setStayStacked(boolean v) { stayStacked = v; save(); }

    public static boolean barrels() { return barrels; }
    public static void setBarrels(boolean v) { barrels = v; save(); }

    public static boolean placedShulkers() { return placedShulkers; }
    public static void setPlacedShulkers(boolean v) { placedShulkers = v; save(); }

    public static int maxVisits() { return maxVisits; }
    public static void setMaxVisits(int v) { maxVisits = clamp(v, MAX_VISITS_CAP); save(); }

    public static int horizontalRange() { return horizontalRange; }
    public static void setHorizontalRange(int v) { horizontalRange = clamp(v, HORIZONTAL_CAP); save(); }

    public static int verticalRange() { return verticalRange; }
    public static void setVerticalRange(int v) { verticalRange = clamp(v, VERTICAL_CAP); save(); }

    public static int carry() { return carry; }
    public static void setCarry(int v) { carry = clamp(v, CARRY_CAP); save(); }
}
