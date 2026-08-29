package com.gst.network;

import com.gst.GalacticSpaceTravel;
import net.minecraft.util.Identifier;

public final class GstNetworking {

    /** Server -> Client: evren seed'ini senkronize eder (login anında bir kere gönderilir). */
    public static final Identifier UNIVERSE_SEED_SYNC = new Identifier(GalacticSpaceTravel.MOD_ID, "universe_seed_sync");

    /** Server -> Client: oyuncu bir gezegene ışınlandığında, o gezegenin adını gönderir (HUD için). */
    public static final Identifier PLANET_NAME_SYNC = new Identifier(GalacticSpaceTravel.MOD_ID, "planet_name_sync");

    private GstNetworking() {
    }
}