package com.expansionpack;

import com.expansionpack.config.ItemSizeConfig;
import com.expansionpack.config.ModConfig;
import com.expansionpack.init.ModEnchantments;
import com.expansionpack.init.ModItems;
import com.expansionpack.networking.ModMessages;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ExpansionPack.MOD_ID)
public class ExpansionPack {
    public static final String MOD_ID = "expansionpack";

    public ExpansionPack() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 必须先注册配置
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);

        ItemSizeConfig.load();
        ModItems.register(modEventBus);
        ModEnchantments.register(modEventBus);
        ModMessages.register();

        MinecraftForge.EVENT_BUS.register(this);
    }
}