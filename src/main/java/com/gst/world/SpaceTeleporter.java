package com.gst.world;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class SpaceTeleporter {

    public static void teleportToSpace(ServerPlayerEntity player) {
        ServerWorld spaceWorld = player.getServer().getWorld(SpaceDimensions.SPACE_WORLD_KEY);

        if (spaceWorld != null && player.getWorld() != spaceWorld) {
            // Oyuncuyu yukleme ekrani tetiklemeden uzay boyutundaki ayni X, Z koordinatlarina işinliyoruz
            player.teleport(
                    spaceWorld,
                    player.getX(),
                    100.0, // Uzay boyutunda baslangic Y yuksekligi
                    player.getZ(),
                    player.getYaw(),
                    player.getPitch()
            );
        }
    }
}