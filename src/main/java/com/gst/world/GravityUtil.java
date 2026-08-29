package com.gst.world;

import com.gst.world.planet.PlanetData;
import com.gst.world.planet.PlanetGridManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Zıplama (LivingEntityMixin#jump), düşme hasarı (LivingEntityMixin#computeFallDamage)
 * ve mermi balistiği (PersistentProjectileEntityMixin#tick) mixinlerinin ortak olarak
 * kullandığı, "bu pozisyondaki yerçekimi çarpanı nedir" sorusuna tek noktadan cevap
 * veren yardımcı sınıf. PlanetGravityHandler'ın (düşüş sırasındaki ivme telafisi)
 * kullandığı aynı PlanetData#gravityFactor kaynağını kullanır.
 *
 * SADECE SUNUCU TARAFINDA anlamlı sonuç döner: client'ta universe seed'i (bkz.
 * client kaynak setindeki ClientUniverseSeedCache) main kaynak setinden erişilemez,
 * bu yüzden client çağrılarında güvenle vanilla (1.0) döneriz - sunucu zaten doğru
 * değeri hesaplayıp velocity/hasar paketiyle client'a senkronize edecek.
 */
public final class GravityUtil {

    private GravityUtil() {
    }

    /** Verilen dünya/pozisyondaki yerçekimi çarpanını döner (1.0 = vanilla). */
    public static float getGravityFactor(World world, BlockPos pos) {
        if (world == null || world.isClient()) {
            return 1.0f;
        }
        if (world.getRegistryKey() != PlanetDimensions.PLANETS_WORLD_KEY) {
            return 1.0f;
        }
        if (!UniverseSeedManager.isInitialized()) {
            return 1.0f;
        }

        long universeSeed = UniverseSeedManager.getUniverseSeed();
        PlanetData data = PlanetGridManager.getPlanetAt(universeSeed, pos.getX(), pos.getZ());
        return data.gravityFactor();
    }

    /** g vanilla'ya çok yakınsa (fark < %1) dokunmaya gerek yok - erken çıkış için. */
    public static boolean isVanillaGravity(float factor) {
        return Math.abs(factor - 1.0f) < 0.01f;
    }

    /** LivingEntityMixin#jump ile aynı taban zıplama hızı (LivingEntity#getJumpVelocity vanilla değeri). */
    private static final double VANILLA_JUMP_VELOCITY = 0.42;
    /** PlanetGravityHandler#VANILLA_GRAVITY ile aynı yaklaşık değer - değiştirilirse orada da güncellenmeli. */
    private static final double VANILLA_GRAVITY = 0.08;

    /**
     * Verilen yerçekimi çarpanına göre, bir canlının (jump boost gibi ek efektler
     * olmadan) düz zıplayarak ulaşabileceği maksimum yüksekliği (blok) döner.
     *
     * Zıplama hızı v = 0.42 * sqrt(1/g) ile ölçeklendiği (bkz. LivingEntityMixin#jump)
     * VE düşüş ivmesi de aynı oranda a = 0.08*g ile telafi edildiği (bkz.
     * PlanetGravityHandler) için, kinematik h = v^2 / (2*a) formülü şu sonucu verir:
     *   h = (0.42^2 / g) / (2 * 0.08 * g) = h_vanilla / g^2
     * yani düşük g'de yükseklik g^2 oranında artar (Ay'da zıplamanın çok daha
     * yüksek hissettirmesiyle örtüşür), yüksek g'de aynı oranda azalır.
     */
    public static double getMaxJumpHeight(float gravityFactor) {
        if (gravityFactor <= 0.0f) {
            return Double.MAX_VALUE; // yerçekimi yok/negatif - sınır tanımsız, engelleme
        }
        double vanillaMaxHeight = (VANILLA_JUMP_VELOCITY * VANILLA_JUMP_VELOCITY) / (2.0 * VANILLA_GRAVITY);
        return vanillaMaxHeight / ((double) gravityFactor * gravityFactor);
    }
}
