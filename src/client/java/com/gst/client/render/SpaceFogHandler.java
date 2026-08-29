package com.gst.client.render;

import com.gst.SpaceTravelConstants;

public final class SpaceFogHandler {

    private SpaceFogHandler() {
    }

    /**
     * Sisin ekranı kapatma (perde) oranı:
     * 650-850 arası: 0.0 -> 1.0 (Ekran kararır/sislenir)
     * 850-900 arası: 1.0 -> 0.0 (Sis dağılır, uzay açılır)
     * 900 ve üstü: 0.0 (Sis tamamen yok, berrak uzay)
     */
    public static double getCurtainProgress(double y) {
        double start = SpaceTravelConstants.FADE_START_Y; // 650
        double end = SpaceTravelConstants.FADE_END_Y;     // 850
        double space = SpaceTravelConstants.SPACE_PHYSICS_START_Y; // 900

        if (y <= start) return 0.0;

        // 1. AŞAMA: Atmosferden çıkış (Sis kapanıyor)
        if (y > start && y <= end) {
            return (y - start) / (end - start);
        }

        // 2. AŞAMA: Uzaya giriş (Sis açılıyor)
        if (y > end && y <= space) {
            return 1.0 - ((y - end) / (space - end));
        }

        // 3. AŞAMA: Derin uzay (Sis sıfır)
        return 0.0;
    }

    /**
     * Arka planın (gökyüzünün) ne kadar siyaha döneceğini belirler.
     * Y >= 850 olduğunda gökyüzü %100 zifiri karanlık uzay olur.
     */
    public static double getSpaceBlackoutProgress(double y) {
        double start = SpaceTravelConstants.FADE_START_Y;
        double end = SpaceTravelConstants.FADE_END_Y;

        if (y <= start) return 0.0;
        if (y >= end) return 1.0;
        return (y - start) / (end - start);
    }
}