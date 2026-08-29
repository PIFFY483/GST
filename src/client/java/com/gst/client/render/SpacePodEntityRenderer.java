package com.gst.client.render;

import com.gst.entity.SpacePodEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SpacePodEntityRenderer extends EntityRenderer<SpacePodEntity> {

    public SpacePodEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(SpacePodEntity entity) {
        // Minecraft 1.20.1 için eksik doku Identifier tanımı
        return new Identifier("minecraft", "textures/misc/missing.png");
    }

    @Override
    public void render(SpacePodEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Ana render metodunu çağırıyoruz
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}