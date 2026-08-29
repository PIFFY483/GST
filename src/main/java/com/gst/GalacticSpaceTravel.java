package com.gst;

import com.gst.command.SpacePodCommands;
import com.gst.entity.ModEntities;
import com.gst.network.UniverseSeedSyncHandler;
import com.gst.world.PlanetDimensions;
import com.gst.world.PlanetEntryServerHandler;
import com.gst.world.PlanetGravityHandler;
import com.gst.world.PlayerSpaceHandler;
import com.gst.world.SpaceDimensions;
import com.gst.world.gen.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalacticSpaceTravel implements ModInitializer {

    public static final String MOD_ID = "gst";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Galactic Space Travel mod baslatiliyor...");

        ModEntities.register();
        SpacePodCommands.register();
        SpaceDimensions.register();
        PlanetDimensions.register();
        ModWorldGen.register();
        PlayerSpaceHandler.register();
        PlanetEntryServerHandler.register();
        PlanetGravityHandler.register();
        UniverseSeedSyncHandler.register();
    }
}