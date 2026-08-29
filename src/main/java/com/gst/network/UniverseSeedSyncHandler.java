package com.gst.network;

import com.gst.world.UniverseSeedManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public final class UniverseSeedSyncHandler {

    private UniverseSeedSyncHandler() {
    }

    public static void register() {
        // Sunucu başlarken evren seed'ini bir kere hesapla
        ServerLifecycleEvents.SERVER_STARTED.register(UniverseSeedManager::initialize);

        // Her oyuncu bağlandığında ona seed'i gönder
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendTo(handler.getPlayer()));
    }

    public static void sendTo(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeLong(UniverseSeedManager.getUniverseSeed());
        ServerPlayNetworking.send(player, GstNetworking.UNIVERSE_SEED_SYNC, buf);
    }
}