package com.gst.world;

import com.gst.GalacticSpaceTravel;
import com.gst.network.GstNetworking;
import com.gst.world.planet.PlanetGridManager;
import com.gst.world.star.PlanetApproachHandler;
import com.gst.world.star.PlanetNaming;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class PlanetEntryServerHandler {

    private PlanetEntryServerHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!UniverseSeedManager.isInitialized()) {
                return;
            }

            long universeSeed = UniverseSeedManager.getUniverseSeed();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getServerWorld().getRegistryKey() != SpaceDimensions.SPACE_WORLD_KEY) {
                    continue;
                }

                long worldTime = player.getServerWorld().getTime();

                PlanetApproachHandler.NearestPlanet nearest = PlanetApproachHandler.findNearest(
                        universeSeed, player.getX(), player.getY(), player.getZ(), worldTime
                );
                if (nearest == null) {
                    continue;
                }

                double progress = PlanetApproachHandler.getApproachProgress(
                        nearest.distance(), nearest.orbit().visualRadius()
                );

                if (progress < 1.0) {
                    continue;
                }

                ServerWorld planetsWorld = server.getWorld(PlanetDimensions.PLANETS_WORLD_KEY);
                if (planetsWorld == null) {
                    GalacticSpaceTravel.LOGGER.warn("[GST-DEBUG] gst:planets boyutu bulunamadi! Teleport iptal edildi.");
                    continue;
                }

                int cellX = nearest.orbit().terrainCellX();
                int cellZ = nearest.orbit().terrainCellZ();

                double targetX = cellX * (double) PlanetGridManager.CELL_SIZE + PlanetGridManager.CELL_SIZE / 2.0;
                double targetZ = cellZ * (double) PlanetGridManager.CELL_SIZE + PlanetGridManager.CELL_SIZE / 2.0;
                double targetY = 900.0;

                player.setVelocity(Vec3d.ZERO);
                player.teleport(planetsWorld, targetX, targetY, targetZ, player.getYaw(), player.getPitch());
                player.fallDistance = 0.0f;

                String planetName = PlanetNaming.getPlanetName(nearest.system(), nearest.orbit());
                sendPlanetName(player, planetName);
            }
        });
    }

    public static void sendPlanetName(ServerPlayerEntity player, String name) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(name);
        ServerPlayNetworking.send(player, GstNetworking.PLANET_NAME_SYNC, buf);
    }
}