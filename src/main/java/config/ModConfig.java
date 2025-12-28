package com.expansionpack.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HOTBAR_RULES;

    static {
        BUILDER.push("hotbar_settings");

        HOTBAR_RULES = BUILDER
                .comment("定义快捷栏规则。格式: '起始-结束:模式'", "模式: ANY, GRID, FOOD")
                .defineList("hotbar_rules",
                        Arrays.asList("0-6:ANY", "9:GRID", "7-8:FOOD"),
                        obj -> obj instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}