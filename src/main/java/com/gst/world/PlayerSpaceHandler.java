package com.gst.world;

import com.gst.SpaceTravelConstants;
import com.gst.entity.SpacePodEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class PlayerSpaceHandler {

    private PlayerSpaceHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerWorld currentWorld = player.getServerWorld();
                boolean isInSpace = currentWorld.getRegistryKey() == SpaceDimensions.SPACE_WORLD_KEY;
                boolean isInOverworld = currentWorld.getRegistryKey() == World.OVERWORLD;

                // 1. Yerçekimi Kontrolü (Uzaydaysa düşmeyi tamamen durdur)
                if (isInSpace) {
                    if (!player.hasNoGravity()) {
                        player.setNoGravity(true);
                    }
                } else {
                    // Uzayda değilse (overworld VEYA gezegen yüzeyi) yerçekimi hala kapalıysa tekrar aç
                    if (player.hasNoGravity() && !player.isSpectator() && !player.isCreative()) {
                        player.setNoGravity(false);
                    }
                }

                // 2. Overworld -> Uzay Geçiş Tetikleyicisi
                // ÖNEMLİ: Sadece gerçekten OVERWORLD'deyken tetiklenmeli. Önceden "!isInSpace"
                // kontrolü vardı, bu da gst:planets'teki (zemini Y=900'de olan) oyuncuları da
                // yanlışlıkla "overworld'den çıkıyor" sanıp tekrar uzaya fırlatıyordu.
                if (isInOverworld && player.getY() >= SpaceTravelConstants.FADE_END_Y) {
                    ServerWorld spaceWorld = server.getWorld(SpaceDimensions.SPACE_WORLD_KEY);
                    if (spaceWorld == null) continue;

                    Entity vehicle = player.getVehicle();
                    double targetX = player.getX();
                    double targetY = 500.0;
                    double targetZ = player.getZ();

                    if (vehicle instanceof SpacePodEntity pod) {
                        player.stopRiding();

                        pod.setVelocity(Vec3d.ZERO);
                        player.setVelocity(Vec3d.ZERO);

                        SpacePodEntity spacePodInSpace = (SpacePodEntity) pod.moveToWorld(spaceWorld);

                        if (spacePodInSpace != null) {
                            spacePodInSpace.refreshPositionAndAngles(targetX, targetY, targetZ, player.getYaw(), player.getPitch());
                            player.teleport(spaceWorld, targetX, targetY + 0.5, targetZ, player.getYaw(), player.getPitch());
                            player.startRiding(spacePodInSpace, true);
                        }
                    } else {
                        player.setVelocity(Vec3d.ZERO);
                        player.teleport(spaceWorld, targetX, targetY, targetZ, player.getYaw(), player.getPitch());
                    }

                    // Uzaya geçer geçmez yerçekimini ve düşüş ivmesini sıfırla
                    player.setNoGravity(true);
                    player.fallDistance = 0.0f;
                }
            }
        });
    }
}