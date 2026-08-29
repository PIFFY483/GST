package com.gst.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Tüm evren üretiminin (yıldız/gezegen yerleşimi VE gezegen terrain'i) tek bir
 * ortak seed'e dayanmasını garanti eden merkezi sınıf.
 *
 * ÖNEMLİ: Hiçbir yerde ServerWorld#getSeed() DOĞRUDAN kullanılmamalı - her
 * dimension'ın (gst:space, gst:planets, overworld) kendi seed'i farklı olabilir.
 * StarSystemGenerator (yörünge/konum hesabı) ile PlanetGridManager (gerçek
 * terrain hesabı) aynı gezegen için farklı seed görürse ikisi birbirini tutmaz.
 * Bu yüzden HER YERDE (server VE client) sadece bu sınıftan okunan tek seed
 * kullanılmalı.
 */
public final class UniverseSeedManager {

    private static long universeSeed = 0L;
    private static boolean initialized = false;

    private UniverseSeedManager() {
    }

    /** Sunucu başlarken bir kere çağrılır (bkz. GalacticSpaceTravel / UniverseSeedSyncHandler). */
    public static void initialize(MinecraftServer server) {
        // NOT: getSeed() sadece ServerWorld'de var, temel World sınıfında değil -
        // bu yüzden değişken tipi World değil ServerWorld olmalı.
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        universeSeed = overworld != null ? overworld.getSeed() : 0L;
        initialized = true;
    }

    public static long getUniverseSeed() {
        return universeSeed;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}