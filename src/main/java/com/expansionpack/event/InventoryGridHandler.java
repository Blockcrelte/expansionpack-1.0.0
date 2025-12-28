package com.expansionpack.event;

import com.expansionpack.ExpansionPack;
import com.expansionpack.config.ItemSizeConfig;
import com.expansionpack.config.ModConfig;
import com.expansionpack.init.ModItems;
import com.expansionpack.networking.ModMessages;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.*;

@Mod.EventBusSubscriber(modid = ExpansionPack.MOD_ID)
public class InventoryGridHandler {
    public static final String IS_SLAVE = "expansionpack.is_slave";
    public static final String MASTER_SLOT = "expansionpack.master_slot";
    public static final String IS_ROTATED = "expansionpack.is_rotated";
    private static final int GRID_WIDTH = 9;

    public record ItemDim(int w, int h) { public boolean is1x1() { return w == 1 && h == 1; } }

    public static boolean isIrregular(ItemStack stack) { return false; }

    public static boolean isItemPart(ItemStack stack, int dx, int dy, boolean rotated) {
        return !stack.isEmpty();
    }

    public static ItemDim getBaseDim(ItemStack stack) {
        if (stack.isEmpty()) return new ItemDim(1, 1);
        Item item = stack.getItem();
        ItemDim configSize = ItemSizeConfig.getSize(item);
        if (configSize != null) {
            if (stack.hasTag() && stack.getTag().getBoolean(IS_ROTATED)) return new ItemDim(configSize.h, configSize.w);
            return configSize;
        }

        ItemDim base = new ItemDim(1, 1);
        if (item instanceof PickaxeItem || item instanceof FishingRodItem ||
                item instanceof SwordItem || item instanceof DiggerItem || item instanceof HoeItem) base = new ItemDim(1, 2);
        else if (item instanceof MinecartItem) base = new ItemDim(2, 2);
        else if (item instanceof ArmorItem armor) {
            if (armor.getType() == ArmorItem.Type.CHESTPLATE || armor.getType() == ArmorItem.Type.LEGGINGS) base = new ItemDim(2, 2);
            else base = new ItemDim(2, 1);
        }
        else if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof DoorBlock) base = new ItemDim(2, 3);
            else if (block instanceof ShulkerBoxBlock) base = new ItemDim(3, 3);
            else if (block instanceof ChestBlock || block == Blocks.ENDER_CHEST) base = new ItemDim(2, 2);
            else if (block instanceof AbstractFurnaceBlock || block instanceof CraftingTableBlock ||
                    block instanceof EnchantmentTableBlock || block instanceof AnvilBlock ||
                    block instanceof SmithingTableBlock || block instanceof LoomBlock) base = new ItemDim(2, 2);
        }

        if (stack.hasTag() && stack.getTag().getBoolean(IS_ROTATED)) return new ItemDim(base.h, base.w);
        return base;
    }

    public static ItemDim getActualDim(ItemStack stack, Slot slot, boolean isCreative) {
        if (stack.isEmpty() || slot == null) return new ItemDim(1, 1);
        boolean isHotbar = (slot.container instanceof Inventory) && (slot.getContainerSlot() < 9);
        boolean isGridCompatible = (slot.container instanceof Inventory) || (slot.container.getContainerSize() >= 27);
        if (isCreative || !isGridCompatible) return new ItemDim(1, 1);
        ItemDim base = getBaseDim(stack);
        if (isHotbar) {
            int index = slot.getContainerSlot();
            List<? extends String> rules = ModConfig.SPEC.isLoaded() ? ModConfig.HOTBAR_RULES.get() : Arrays.asList("0-2:ANY", "3-6:GRID", "7-8:FOOD");
            for (String rule : rules) {
                try {
                    String[] parts = rule.split(":");
                    String[] range = parts[0].split("-");
                    int start = Integer.parseInt(range[0]), end = Integer.parseInt(range[1]);
                    if (index >= start && index <= end) {
                        String mode = parts[1].toUpperCase();
                        if (mode.equals("ANY")) return new ItemDim(1, 1);
                        if (mode.equals("FOOD")) return (stack.getItem().isEdible() || base.is1x1()) ? new ItemDim(1, 1) : base;
                        if (mode.equals("GRID")) break;
                    }
                } catch (Exception ignored) {}
            }
        }
        return base;
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (isSlave(event.getEntity().getItem())) { event.setCanceled(true); event.getEntity().discard(); }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.isCreative()) return;
        Player player = event.player;
        AbstractContainerMenu menu = player.containerMenu;

        ItemStack carried = menu.getCarried();
        if (isSlave(carried)) {
            if (carried.hasTag() && carried.getTag().contains(MASTER_SLOT)) {
                int masterId = carried.getTag().getInt(MASTER_SLOT);
                if (masterId >= 0 && masterId < menu.slots.size()) {
                    Slot masterSlot = menu.slots.get(masterId);
                    if (!masterSlot.getItem().isEmpty() && !isSlave(masterSlot.getItem())) {
                        menu.setCarried(masterSlot.getItem()); masterSlot.set(ItemStack.EMPTY);
                    } else menu.setCarried(ItemStack.EMPTY);
                } else menu.setCarried(ItemStack.EMPTY);
            } else menu.setCarried(ItemStack.EMPTY);
        }

        Map<Container, List<Slot>> groups = new LinkedHashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                if (slot.getContainerSlot() < 36) groups.computeIfAbsent(slot.container, k -> new ArrayList<>()).add(slot);
                else if (isSlave(slot.getItem())) slot.set(ItemStack.EMPTY);
            } else if (slot.container.getContainerSize() >= 9) groups.computeIfAbsent(slot.container, k -> new ArrayList<>()).add(slot);
        }

        boolean changed = false;
        for (List<Slot> group : groups.values()) { if (processGrid(player, group)) changed = true; }
        if (changed) menu.broadcastChanges();
    }

    private static boolean processGrid(Player player, List<Slot> slots) {
        int size = slots.size(), width = GRID_WIDTH, totalRows = (size + width - 1) / width;
        int[] ownerMap = new int[size]; Arrays.fill(ownerMap, -1);
        boolean changed = false;

        for (int i = 0; i < size; i++) {
            Slot slot = slots.get(i); ItemStack stack = slot.getItem();
            if (stack.isEmpty() || isSlave(stack)) continue;

            ItemDim dim = getActualDim(stack, slot, false);
            boolean rotated = stack.hasTag() && stack.getTag().getBoolean(IS_ROTATED);
            int sc = i % width, sr = i / width;
            boolean conflict = (sc + dim.w > width || sr + dim.h > totalRows);

            if (!conflict) {
                boolean isOriginHotbar = (slot.container instanceof Inventory) && (slot.getContainerSlot() < 9);
                for (int dx = 0; dx < dim.w; dx++) {
                    for (int dy = 0; dy < dim.h; dy++) {
                        int sid = (sr + dy) * width + (sc + dx);
                        if (sid >= size || ownerMap[sid] != -1) { conflict = true; break; }
                        if (((slots.get(sid).container instanceof Inventory) && (slots.get(sid).getContainerSlot() < 9)) != isOriginHotbar) { conflict = true; break; }
                    }
                    if (conflict) break;
                }
            }

            if (conflict) {
                boolean moved = false;
                for (int j = 0; j < size; j++) {
                    if (slots.get(j).getItem().isEmpty() && canPlaceItem(j, dim, width, totalRows, size, ownerMap, slots, stack, rotated)) {
                        slots.get(j).set(stack.copy()); slot.set(ItemStack.EMPTY); moved = true;
                        int tsc = j % width, tsr = j / width;
                        for (int dx = 0; dx < dim.w; dx++) {
                            for (int dy = 0; dy < dim.h; dy++) {
                                int sid = (tsr + dy) * width + (tsc + dx); if (sid < size) ownerMap[sid] = j;
                            }
                        }
                        break;
                    }
                }
                if (!moved) { player.drop(stack.copy(), false); slot.set(ItemStack.EMPTY); }
                changed = true;
            } else {
                for (int dx = 0; dx < dim.w; dx++) {
                    for (int dy = 0; dy < dim.h; dy++) {
                        int sid = (sr + dy) * width + (sc + dx); if (sid < size) ownerMap[sid] = i;
                    }
                }
            }
        }

        for (int i = 0; i < size; i++) {
            int mLocal = ownerMap[i]; Slot slot = slots.get(i);
            if (mLocal == -1) { if (isSlave(slot.getItem())) { slot.set(ItemStack.EMPTY); changed = true; } }
            else if (mLocal != i) {
                int mGlobal = slots.get(mLocal).index;
                if (!isSlave(slot.getItem()) || slot.getItem().getOrCreateTag().getInt(MASTER_SLOT) != mGlobal) {
                    slot.set(createSlave(mGlobal)); changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean canPlaceItem(int index, ItemDim dim, int width, int totalRows, int size, int[] ownerMap, List<Slot> slots, ItemStack stack, boolean rotated) {
        int c = index % width, r = index / width;
        if (c + dim.w > width || r + dim.h > totalRows) return false;
        boolean startHotbar = (slots.get(index).container instanceof Inventory) && (slots.get(index).getContainerSlot() < 9);
        for (int dx = 0; dx < dim.w; dx++) {
            for (int dy = 0; dy < dim.h; dy++) {
                int sid = (r + dy) * width + (c + dx);
                if (sid >= size || ownerMap[sid] != -1 || !slots.get(sid).getItem().isEmpty()) return false;
                if (((slots.get(sid).container instanceof Inventory) && (slots.get(sid).getContainerSlot() < 9)) != startHotbar) return false;
            }
        }
        return true;
    }

    private static ItemStack createSlave(int m) {
        ItemStack s = new ItemStack(ModItems.BLOCKED_SLOT.get());
        s.getOrCreateTag().putBoolean(IS_SLAVE, true); s.getOrCreateTag().putInt(MASTER_SLOT, m);
        return s;
    }

    public static boolean isSlave(ItemStack s) { return !s.isEmpty() && s.hasTag() && s.getTag().getBoolean(IS_SLAVE); }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        GuiGraphics gui = event.getGuiGraphics();
        boolean isCreative = screen instanceof CreativeModeInventoryScreen;
        Set<Integer> renderedArea = new HashSet<>();

        for (Slot slot : screen.getMenu().slots) {
            if (renderedArea.contains(slot.index)) continue;
            //创造模式不渲染dwd
            if (isCreative || (slot.container instanceof Inventory && slot.getContainerSlot() >= 36)) continue;
            ItemStack stack = slot.getItem();
            int x = screen.getGuiLeft() + slot.x, y = screen.getGuiTop() + slot.y;
            if (isSlave(stack)) {
                renderedArea.add(slot.index);
                gui.pose().pushPose(); gui.pose().translate(0, 0, 350);
                gui.fill(x, y, x + 16, y + 16, 0xAAFFFFFF); gui.pose().popPose();
                continue;
            }

            //处理特殊槽位
            if (!stack.isEmpty()) {
                ItemDim dim = getActualDim(stack, slot, false);
                boolean rotated = stack.hasTag() && stack.getTag().getBoolean(IS_ROTATED);

                //如果物品太宽超过容器边框，回退到 1x1 渲染
                if (slot.getContainerSlot() % 9 + dim.w > 9) dim = new ItemDim(1, 1);

                if (!dim.is1x1()) {
                    int tw = 16 + (dim.w - 1) * 18, th = 16 + (dim.h - 1) * 18;
                    for (int dx = 0; dx < dim.w; dx++) {
                        for (int dy = 0; dy < dim.h; dy++) {
                            renderedArea.add(slot.index + dy * 9 + dx);
                        }
                    }

                    // 绘制一大坨背景墙
                    gui.pose().pushPose(); gui.pose().translate(0, 0, 360);
                    gui.fill(x-1, y-1, x+tw+1, y+th+1, 0xFFC6C6C6);
                    gui.fill(x, y, x+tw, y+th, 0xFFB0B0B0);
                    int b = 0xFF373737;
                    gui.fill(x-1, y-1, x+tw+1, y, b); gui.fill(x-1, y+th, x+tw+1, y+th+1, b);
                    gui.fill(x-1, y, x, y+th, b); gui.fill(x+tw, y, x+tw+1, y+th, b);
                    gui.pose().popPose();

                    // 绘制超大图标(不要看我)
                    gui.pose().pushPose();
                    gui.pose().translate(x + tw / 2.0f, y + th / 2.0f, 400);
                    float s = Math.min(tw / 16.0f, th / 16.0f) * 0.85f;
                    gui.pose().scale(s, s, 1.0f);
                    if (rotated) gui.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90));
                    gui.pose().translate(-8, -8, 0);
                    RenderSystem.enableBlend(); Lighting.setupForFlatItems();
                    gui.renderFakeItem(stack, 0, 0); Lighting.setupFor3DItems();
                    gui.pose().popPose();
                    // ai救救我= =
                    if (stack.getCount() > 1) {
                        String txt = String.valueOf(stack.getCount());
                        gui.pose().pushPose(); gui.pose().translate(0, 0, 700); // 提升层级到 700
                        gui.drawString(Minecraft.getInstance().font, txt, x + tw - Minecraft.getInstance().font.width(txt) - 1, y + th - 9, 0xFFFFFFFF, true);
                        gui.pose().popPose();
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() == GLFW.GLFW_KEY_R && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            ItemStack carried = screen.getMenu().getCarried();
            if (!carried.isEmpty()) {
                ModMessages.sendToServer(new ModMessages.RotationPacket());
                Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.4f, 1.1f);
                event.setCanceled(true);
            }
        }
    }
}