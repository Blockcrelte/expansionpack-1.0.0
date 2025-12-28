package com.expansionpack.networking;

import com.expansionpack.ExpansionPack;
import com.expansionpack.event.InventoryGridHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ExpansionPack.MOD_ID, "main"),
                () -> "1.0",
                s -> true,
                s -> true
        );
        INSTANCE.messageBuilder(RotationPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RotationPacket::new).encoder(RotationPacket::toBytes)
                .consumerMainThread(RotationPacket::handle).add();
    }

    public static void sendToServer(Object msg) { if (INSTANCE != null) INSTANCE.sendToServer(msg); }

    public static class RotationPacket {
        public RotationPacket() {}
        public RotationPacket(FriendlyByteBuf buf) {}
        public void toBytes(FriendlyByteBuf buf) {}
        public static void handle(RotationPacket msg, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context ctx = supplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    ItemStack carried = player.containerMenu.getCarried();
                    if (!carried.isEmpty()) {
                        boolean rot = carried.getOrCreateTag().getBoolean(InventoryGridHandler.IS_ROTATED);
                        carried.getOrCreateTag().putBoolean(InventoryGridHandler.IS_ROTATED, !rot);
                        player.containerMenu.sendAllDataToRemote();
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}