package com.gst.client.render;

import com.gst.client.network.ClientPlanetNameCache;
import com.gst.world.PlanetDimensions;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class PlanetHud {

    private PlanetHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(PlanetHud::render);
    }

    private static void render(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        if (client.world.getRegistryKey() != PlanetDimensions.PLANETS_WORLD_KEY) return;
        if (client.options.hudHidden) return;

        String name = ClientPlanetNameCache.get();
        if (name == null) return;

        int screenHeight = client.getWindow().getScaledHeight();
        int x = 4;
        int y = screenHeight - 14;

        drawContext.drawTextWithShadow(client.textRenderer, Text.literal(name), x, y, 0xFFFFFF);
    }
}