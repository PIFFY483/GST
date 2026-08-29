package com.gst.client.network;

public final class ClientPlanetNameCache {

    private static String currentName = null;

    private ClientPlanetNameCache() {
    }

    public static void set(String name) {
        currentName = name;
    }

    public static String get() {
        return currentName;
    }

    public static void clear() {
        currentName = null;
    }
}