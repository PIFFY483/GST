package com.gst.world;

import com.gst.GalacticSpaceTravel;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class SpaceDimensions {

    public static final RegistryKey<World> SPACE_WORLD_KEY = RegistryKey.of(
            RegistryKeys.WORLD,
            new Identifier(GalacticSpaceTravel.MOD_ID, "space")
    );

    public static final RegistryKey<DimensionType> SPACE_TYPE_KEY = RegistryKey.of(
            RegistryKeys.DIMENSION_TYPE,
            new Identifier(GalacticSpaceTravel.MOD_ID, "space_type")
    );

    public static void register() {
        GalacticSpaceTravel.LOGGER.info("Uzay boyutu anahtarlari kaydedildi.");
    }
}