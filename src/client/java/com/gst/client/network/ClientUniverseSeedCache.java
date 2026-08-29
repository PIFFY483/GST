package com.gst.client.network;

/**
 * Server'dan senkronize edilen tek evren seed'ini tutar. Login anında dolar.
 * Doldurulmadan (ör. dünyaya daha yeni girildiği ilk birkaç tick) gezegen
 * hesaplamaları yapılmamalı - isReceived() ile kontrol et.
 */
public final class ClientUniverseSeedCache {

    private static long seed = 0L;
    private static boolean received = false;

    private ClientUniverseSeedCache() {
    }

    public static void set(long value) {
        seed = value;
        received = true;
    }

    public static long get() {
        return seed;
    }

    public static boolean isReceived() {
        return received;
    }

    /** Dünyadan çıkışta/disconnect'te çağrılabilir, zorunlu değil ama temiz durur. */
    public static void reset() {
        seed = 0L;
        received = false;
    }
}