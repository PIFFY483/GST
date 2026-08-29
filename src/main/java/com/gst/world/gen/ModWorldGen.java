package com.gst.world.gen;

import com.gst.GalacticSpaceTravel;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModWorldGen {

    private ModWorldGen() {
    }

    public static void register() {
        Registry.register(Registries.CHUNK_GENERATOR,
                new Identifier(GalacticSpaceTravel.MOD_ID, "planet_generator"),
                PlanetChunkGenerator.CODEC);
    }
}