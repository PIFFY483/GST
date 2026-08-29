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
 *
 * ARŞİMET NOTU: Oyuncu suda/lavdayken bu düzeltmeyi UYGULAMIYORUZ. Kaldırma
 * kuvveti F = ρ·V·g ve ağırlık m·g olduğundan, batma/yüzme oranı g'den
 * bağımsızdır (g'ler sadeleşir) - vanilla'nın sıvı fiziği zaten bunu doğru
 * yapıyor. Buraya ekstra bir düzeltme eklemek, düşük/yüksek g gezegenlerinde
 * batma hızını yanlış şekilde değiştirirdi.
 */
public final class PlanetGravityHandler {

    /** Vanilla'nın normal düşüş ivmesi (blok/tick^2), yaklaşık değer. */
    private static final double VANILLA_GRAVITY = 0.08;

    /**
     * Vanilla'nın havadayken her tick uyguladığı yatay sürtünme (drag) katsayısı:
     * LivingEntity#travel içinde v.x,v.z *= 0.91 (bkz. minecraft.wiki/w/Entity).
     * Yerçekiminden tamamen bağımsız, sabit bir değer.
     */
    private static final double VANILLA_HORIZONTAL_DRAG = 0.91;

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
                if (player.isTouchingWater() || player.isInLava()) {
                    continue; // Arşimet: kaldırma kuvveti g'den bağımsız, yukarıdaki nota bak
                }

                PlanetData data = PlanetGridManager.getPlanetAt(universeSeed, player.getBlockX(), player.getBlockZ());
                float gravityFactor = data.gravityFactor();

                if (Math.abs(gravityFactor - 1.0f) < 0.01f) {
                    continue; // vanilla'ya çok yakınsa dokunmaya gerek yok
                }

                // Vanilla bu tick zaten ~VANILLA_GRAVITY kadar düşürdü, farkı telafi ediyoruz
                double correction = (1.0 - gravityFactor) * VANILLA_GRAVITY;

                // Yatay sürtünmeyi de g ile yumuşatıyoruz. Sebep: vanilla'nın 0.91 sürtünmesi
                // yerçekiminden bağımsız olduğu için, sadece dikey ivmeyi telafi edersek düşük
                // g'de havada kalış süresi çok uzasa da yatay hız birkaç tick içinde (geometrik
                // seri ile) sıfıra yakınsıyor - "planör menzili" sadece kalkış hızına bağlı kalıyor,
                // havada kalma süresine değil, ve zıplama yüksekliği artsa da ileri mesafe hep aynı
                // hissettiriyor. Etkin sürtünmeyi effectiveDrag = 1 - g*(1-0.91) olacak şekilde
                // hesaplıyoruz: g=1'de vanilla ile birebir aynı (çarpan=1), g→0'da sürtünme neredeyse
                // tamamen kayboluyor (uzayda kayma hissi), böylece uzayan havada kalış süresi artık
                // gerçekten ekstra ileri mesafeye dönüşüyor.
                double effectiveDrag = 1.0 - gravityFactor * (1.0 - VANILLA_HORIZONTAL_DRAG);
                double horizontalMultiplier = effectiveDrag / VANILLA_HORIZONTAL_DRAG;

                Vec3d v = player.getVelocity();
                player.setVelocity(v.x * horizontalMultiplier, v.y + correction, v.z * horizontalMultiplier);
                player.velocityModified = true;
            }
        });
    }
}