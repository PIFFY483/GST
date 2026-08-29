package com.gst.client;

import com.gst.client.network.ClientPlanetNameCache;
import com.gst.client.network.ClientUniverseSeedCache;
import com.gst.client.render.PlanetHud;
import com.gst.client.render.PlanetLodRenderer;
import com.gst.client.render.SpacePodEntityRenderer;
import com.gst.entity.ModEntities;
import com.gst.network.GstNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.gst.GalacticSpaceTravel.MOD_ID;

public class GalacticSpaceTravelClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Galactic Space Travel client tarafi baslatiliyor...");

        EntityRendererRegistry.register(ModEntities.SPACE_POD, SpacePodEntityRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(GstNetworking.UNIVERSE_SEED_SYNC, (client, handler, buf, responseSender) -> {
            long seed = buf.readLong();
            client.execute(() -> ClientUniverseSeedCache.set(seed));
        });

        ClientPlayNetworking.registerGlobalReceiver(GstNetworking.PLANET_NAME_SYNC, (client, handler, buf, responseSender) -> {
            String name = buf.readString();
            client.execute(() -> ClientPlanetNameCache.set(name));
        });

        PlanetLodRenderer.register();
        PlanetHud.register();
    }
}