package com.gst.world;

import com.gst.world.planet.PlanetData;
import com.gst.world.planet.PlanetGridManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Her gezegenin kendine has yerçekimi katsayısını (PlanetData#gravityFactor)
 * uygular. Vanilla'nın yerçekimini tamamen değiştirmiyoruz (bu, LivingEntity#travel
 * içine mixin gerektirirdi, riskli) - bunun yerine vanilla uyguladıktan SONRA
 * farkı telafi ediyoruz. Tam fizik doğruluğu değil ama hissedilir bir fark yaratır.
 */
public final class PlanetGravityHandler {

    /** Vanilla'nın normal düşüş ivmesi (blok/tick^2), yaklaşık değer. */
    private static final double VANILLA_GRAVITY = 0.08;

    private PlanetGravityHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!UniverseSeedManager.isInitialized()) {
                return;
            }
            long universeSeed = UniverseSeedManager.getUniverseSeed();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getServerWorld().getRegistryKey() != PlanetDimensions.PLANETS_WORLD_KEY) {
                    continue;
                }
                if (player.isOnGround() || player.getAbilities().flying) {
                    continue;
                }

                PlanetData data = PlanetGridManager.getPlanetAt(universeSeed, player.getBlockX(), player.getBlockZ());
                float gravityFactor = data.gravityFactor();

                if (Math.abs(gravityFactor - 1.0f) < 0.01f) {
                    continue; // vanilla'ya çok yakınsa dokunmaya gerek yok
                }

                // Vanilla bu tick zaten ~VANILLA_GRAVITY kadar düşürdü, farkı telafi ediyoruz
                double correction = (1.0 - gravityFactor) * VANILLA_GRAVITY;

                Vec3d v = player.getVelocity();
                player.setVelocity(v.x, v.y + correction, v.z);
                player.velocityModified = true;
            }
        });
    }
}