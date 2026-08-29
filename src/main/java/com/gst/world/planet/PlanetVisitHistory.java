package com.gst.world.planet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Her oyuncunun en son ışınlandığı gezegenlerin (terrain hücresi bazında,
 * bkz. PlanetGridManager#packCell) kısa geçmişini tutar. "gst goplanet"
 * komutunun art arda aynı gezegene göndermesini engellemek için kullanılır:
 * bir gezegen, oyuncu en az HISTORY_SIZE FARKLI gezegene daha ışınlanana
 * kadar tekrar hedef olarak seçilemez.
 *
 * Sunucu bazında sadece bellekte tutulur (kalıcı/diske yazılmaz) - sunucu
 * yeniden başladığında sıfırlanması sorun değil, amaç sadece "art arda aynı
 * yere ışınlanma" hissini önlemek.
 */
public final class PlanetVisitHistory {

    /** Bir gezegen, en az bu kadar FARKLI gezegene ışınlanana kadar tekrar hedef olamaz. */
    public static final int HISTORY_SIZE = 10;

    private static final Map<UUID, Deque<Long>> HISTORY = new ConcurrentHashMap<>();

    private PlanetVisitHistory() {
    }

    /** Verilen gezegen hücresinin, oyuncunun yakın geçmişinde olup olmadığını döner. */
    public static boolean isRecentlyVisited(UUID playerId, long planetCellKey) {
        Deque<Long> history = HISTORY.get(playerId);
        return history != null && history.contains(planetCellKey);
    }

    /** Oyuncunun yeni ışınlandığı gezegeni geçmişe ekler, geçmişi HISTORY_SIZE ile sınırlı tutar. */
    public static void recordVisit(UUID playerId, long planetCellKey) {
        Deque<Long> history = HISTORY.computeIfAbsent(playerId, id -> new ArrayDeque<>(HISTORY_SIZE + 1));

        // Aynı gezegen zaten geçmişteyse önce çıkar ki en sona (en yeni) taşınsın
        history.remove(planetCellKey);
        history.addLast(planetCellKey);

        while (history.size() > HISTORY_SIZE) {
            history.removeFirst();
        }
    }

    /** Test/debug amaçlı: bir oyuncunun geçmişini temizler. */
    public static void clear(UUID playerId) {
        HISTORY.remove(playerId);
    }
}
