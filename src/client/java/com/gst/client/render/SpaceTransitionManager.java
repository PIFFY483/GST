package com.gst.client.render;

public final class SpaceTransitionManager {

    private static boolean transitioning = false;
    private static float fadeOpacity = 1.0f;

    private SpaceTransitionManager() {}

    public static void startTransition() {
        transitioning = true;
        fadeOpacity = 1.0f; // Ekran kararmış/dondurulmuş durumda başlar
    }

    public static void stopTransition() {
        transitioning = false;
    }

    public static boolean isTransitioning() {
        return transitioning;
    }

    public static float getFadeOpacity() {
        return fadeOpacity;
    }

    public static void setFadeOpacity(float opacity) {
        fadeOpacity = opacity;
    }
}