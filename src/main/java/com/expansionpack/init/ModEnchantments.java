package com.expansionpack.init;

import com.expansionpack.ExpansionPack;
import com.expansionpack.enchantment.ExtraDamageEnchantment;
import com.expansionpack.enchantment.RedstoneOverdriveEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ExpansionPack.MOD_ID);
    public static final RegistryObject<Enchantment> FIXED_STRIKE = ENCHANTMENTS.register("fixed_strike", ExtraDamageEnchantment::new);
    public static final RegistryObject<Enchantment> REDSTONE_OVERDRIVE = ENCHANTMENTS.register("redstone_overdrive", RedstoneOverdriveEnchantment::new);
    public static void register(IEventBus bus) { ENCHANTMENTS.register(bus); }
}