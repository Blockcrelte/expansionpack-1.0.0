package com.expansionpack.config;

import com.expansionpack.event.InventoryGridHandler.ItemDim;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ItemSizeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, SizeDef> ITEM_SIZES = new HashMap<>();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("expansionpack-sizes.json");

    private static class SizeDef {
        int w; int h;
        public SizeDef(int w, int h) { this.w = w; this.h = h; }
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Type type = new TypeToken<Map<String, SizeDef>>(){}.getType();
                Map<String, SizeDef> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    ITEM_SIZES.clear();
                    ITEM_SIZES.putAll(loaded);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            generateDefaults();
            save();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(ITEM_SIZES, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ItemDim getSize(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key != null && ITEM_SIZES.containsKey(key.toString())) {
            SizeDef def = ITEM_SIZES.get(key.toString());
            return new ItemDim(def.w, def.h);
        }
        return null;
    }

    private static void generateDefaults() {
        add(Items.SHIELD, 1, 2);
        add(Items.TOTEM_OF_UNDYING, 1, 2);
        add(Items.CROSSBOW, 3, 1);
        add(Items.BOW, 1, 3);
        add(Items.TRIDENT, 1, 3);

        add(Items.COAL_BLOCK, 3, 3);
        add(Items.DIAMOND_BLOCK, 3, 3);
        add(Items.GOLD_BLOCK, 3, 3);
        add(Items.EMERALD_BLOCK, 3, 3);
        add(Items.IRON_BLOCK, 3, 3);
        add(Items.RAW_IRON_BLOCK, 3, 3);
        add(Items.RAW_COPPER_BLOCK, 3, 3);
        add(Items.RAW_GOLD_BLOCK, 3, 3);
        add(Items.COPPER_BLOCK, 3, 3);

        add(Items.LAPIS_BLOCK, 2, 2);
        add(Items.REDSTONE_BLOCK, 2, 2);

        add(Items.CHICKEN, 2, 2);
        add(Items.COOKED_CHICKEN, 2, 2);
        add(Items.RABBIT, 2, 2);
        add(Items.COOKED_RABBIT, 2, 2);
        add(Items.CAKE, 2, 2);

        add(Items.BEEF, 1, 2);
        add(Items.COOKED_BEEF, 1, 2);
        add(Items.PORKCHOP, 1, 2);
        add(Items.COOKED_PORKCHOP, 1, 2);

        add(Items.MELON_SLICE, 2, 1);
        add(Items.MUTTON, 2, 1);
        add(Items.COOKED_MUTTON, 2, 1);
        add(Items.BREAD, 2, 1);

        add(Items.COAL, 2, 2);
        add(Items.CHARCOAL, 2, 2);
        add(Items.RAW_IRON, 2, 2);
        add(Items.RAW_COPPER, 2, 2);
        add(Items.RAW_GOLD, 2, 2);
        add(Items.EMERALD, 2, 2);
        add(Items.DIAMOND, 2, 2);
        add(Items.REDSTONE, 2, 2);
        add(Items.QUARTZ, 2, 2);
        add(Items.ANCIENT_DEBRIS, 2, 3);

        add(Items.IRON_INGOT, 2, 1);
        add(Items.GOLD_INGOT, 2, 1);
        add(Items.COPPER_INGOT, 2, 1);
        add(Items.NETHERITE_INGOT, 2, 1);
    }

    private static void add(Item item, int w, int h) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key != null) ITEM_SIZES.put(key.toString(), new SizeDef(w, h));
    }
}