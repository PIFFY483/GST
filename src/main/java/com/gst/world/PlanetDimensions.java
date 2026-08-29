package com.gst.world;

import com.gst.GalacticSpaceTravel;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

/**
 * Oyuncunun gerçekten "ayak bastığı" gezegen yüzeyi boyutu.
 * Hangi gezegende olduğun, bu tek boyutun hangi hücresinde (PlanetGridManager) durduğuna göre belirlenir.
 */
public class PlanetDimensions {

    public static final RegistryKey<World> PLANETS_WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            new Identifier(GalacticSpaceTravel.MOD_ID, "planets")
    );

    public static final RegistryKey<DimensionType> PLANETS_TYPE_KEY = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(GalacticSpaceTravel.MOD_ID, "planets_type")
    );

    public static void register() {
        GalacticSpaceTravel.LOGGER.info("Gezegen yuzeyi boyutu anahtarlari kaydedildi.");
    }
}