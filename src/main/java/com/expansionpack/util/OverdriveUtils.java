package com.expansionpack.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public class OverdriveUtils {
    public static final String ENERGY_KEY = "expansionpack.energy";
    public static final String SHIELD_CD_KEY = "expansionpack.shield_cd";
    public static final String RECHARGE_CD_KEY = "expansionpack.recharge_cd";

    public static float getEnergy(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(ENERGY_KEY)) {
            return stack.getTag().getFloat(ENERGY_KEY);
        }
        return 0f;
    }

    public static void setEnergy(ItemStack stack, float energy) {
        stack.getOrCreateTag().putFloat(ENERGY_KEY, Math.max(0, energy));
    }

    public static int getMaxEnergy(ItemStack stack, int level) {
        if (stack.getItem() instanceof ArmorItem) {
            return level * 15; // 【修改】护甲每级上限改为 15
        }
        return level * 125;
    }

    public static long getLastShieldTime(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getLong(SHIELD_CD_KEY) : 0L;
    }

    public static void setLastShieldTime(ItemStack stack, long time) {
        stack.getOrCreateTag().putLong(SHIELD_CD_KEY, time);
    }

    public static long getLastRechargeTime(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getLong(RECHARGE_CD_KEY) : 0L;
    }

    public static void setLastRechargeTime(ItemStack stack, long time) {
        stack.getOrCreateTag().putLong(RECHARGE_CD_KEY, time);
    }
}