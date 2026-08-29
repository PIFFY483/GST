package com.gst.client.render;

import com.gst.client.network.ClientUniverseSeedCache;
import com.gst.world.SpaceDimensions;
import com.gst.world.planet.PlanetData;
import com.gst.world.planet.PlanetGridManager;
import com.gst.world.planet.PlanetType;
import com.gst.world.star.PlanetOrbit;
import com.gst.world.star.StarSystemData;
import com.gst.world.star.StarSystemGenerator;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * GEÇİCİ / PLACEHOLDER RENDERER.
 *
 * ÖNEMLİ MİMARİ NOKTA: Gezegenler/yıldızlar gerçek dünya koordinatlarında
 * (yüzbinlerce/milyonlarca blok uzakta) çizilmiyor - çizilirlerse Minecraft'ın
 * far-clip (görüş kesme) mesafesi yüzünden HİÇBİR ZAMAN görünmezler, motor
 * onları daha en baştan eler.
 *
 * Bunun yerine vanilla'nın güneş/ay'ı çizdiği teknik kullanılıyor: gerçek
 * yön vektörü hesaplanıp, kameraya göre SABİT yakın bir mesafeye (RENDER_DISTANCE)
 * projekte ediliyor. Görünen boyut, gerçek (mesafe/yarıçap) oranı korunarak
 * ölçekleniyor - yani açısal büyüklük doğru kalıyor, sadece "nerede çizildiği"
 * sahte. Bu, "Dual Camera" / skybox render tekniğinin basit bir versiyonu.
 *
 * Bu hala düz renkli, düşük poligonlu bir küre - atmosfer parıltısı, gürültü
 * dokusu yok. Sadece mekanizmanın çalıştığını görebilmen için.
 */
public final class PlanetLodRenderer {

    /** Nesnelerin GERÇEKTE çizildiği sabit mesafe (blok). Far-clip'in güvenle içinde kalmalı. */
    private static final float RENDER_DISTANCE = 300f;

    /** Bu gerçek mesafeden daha uzaktaki nesneler için oran hesaplanmaz (bölme hatası/aşırı küçülme önlenir). */
    private static final double MIN_TRUE_DISTANCE = 1.0;

    private static final int LAT_SEGMENTS = 10;
    private static final int LON_SEGMENTS = 14;
    private static final List<Vector3f[]> UNIT_SPHERE_TRIANGLES = buildUnitSphereTriangles();

    private PlanetLodRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(PlanetLodRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        Entity cameraEntity = client.getCameraEntity();
        if (cameraEntity == null) return;
        if (cameraEntity.getWorld().getRegistryKey() != SpaceDimensions.SPACE_WORLD_KEY) return;
        if (!ClientUniverseSeedCache.isReceived()) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        long universeSeed = ClientUniverseSeedCache.get();
        Vec3d camPos = context.camera().getPos();
        long worldTime = cameraEntity.getWorld().getTime();

        List<StarSystemData> systems = StarSystemGenerator.getNearbySystems(universeSeed, camPos.x, camPos.z, 1);
        if (systems.isEmpty()) return;

        setupRenderState();

        for (StarSystemData system : systems) {
            drawCelestialObject(matrices, camPos, system.starX(), system.starY(), system.starZ(),
                    system.starVisualRadius(), system.starColorRgb());

            for (PlanetOrbit orbit : system.planets()) {
                double px = orbit.getX(system.starX(), worldTime);
                double pz = orbit.getZ(system.starZ(), worldTime);
                double py = system.starY();

                PlanetData data = PlanetGridManager.getPlanetAtCell(universeSeed, orbit.terrainCellX(), orbit.terrainCellZ());
                int color = colorFor(data.type());

                drawCelestialObject(matrices, camPos, px, py, pz, orbit.visualRadius(), color);
            }
        }

        teardownRenderState();
    }

    private static void setupRenderState() {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(true);
    }

    private static void teardownRenderState() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * worldX/Y/Z: nesnenin GERÇEK (uzak) konumu. trueRadius: gerçek görsel yarıçapı.
     * Bu metod gerçek konumu ASLA doğrudan kullanmaz - sadece yön ve mesafe oranını.
     */
    private static void drawCelestialObject(MatrixStack matrices, Vec3d camPos, double worldX, double worldY, double worldZ, float trueRadius, int argb) {
        double dx = worldX - camPos.x;
        double dy = worldY - camPos.y;
        double dz = worldZ - camPos.z;

        double trueDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (trueDistance < MIN_TRUE_DISTANCE) {
            trueDistance = MIN_TRUE_DISTANCE;
        }

        // Yön vektörü (normalize)
        double dirX = dx / trueDistance;
        double dirY = dy / trueDistance;
        double dirZ = dz / trueDistance;

        // Sahte (render) konum: kameradan sabit RENDER_DISTANCE uzaklıkta, ama GERÇEK yönde
        float relX = (float) (dirX * RENDER_DISTANCE);
        float relY = (float) (dirY * RENDER_DISTANCE);
        float relZ = (float) (dirZ * RENDER_DISTANCE);

        // Açısal büyüklüğü koru: apparentRadius / RENDER_DISTANCE == trueRadius / trueDistance
        float apparentRadius = (float) (RENDER_DISTANCE * (trueRadius / trueDistance));
        // Aşırı küçük noktaları da göz ardı etmeyelim, en az birkaç blok göster
        apparentRadius = Math.max(apparentRadius, 2.0f);

        drawSphere(matrices, relX, relY, relZ, apparentRadius, argb);
    }

    private static void drawSphere(MatrixStack matrices, float relX, float relY, float relZ, float radius, int argb) {
        int a = (argb >> 24) & 0xFF;
        if (a == 0) a = 255;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (Vector3f[] tri : UNIT_SPHERE_TRIANGLES) {
            for (Vector3f v : tri) {
                float x = relX + v.x() * radius;
                float y = relY + v.y() * radius;
                float z = relZ + v.z() * radius;
                buffer.vertex(matrix, x, y, z).color(r, g, b, a).next();
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static int colorFor(PlanetType type) {
        return switch (type) {
            case ROCKY -> 0xFF8C8C8C;
            case ICE -> 0xFFBEEFFF;
            case LAVA -> 0xFFFF4500;
            case BARREN -> 0xFFC97C3D;
            case CRYSTAL -> 0xFFB47CFF;
            case GAS_GIANT -> 0xFFE0D8C0;
            case OCEAN -> 0xFF2E6FA3;
            case EARTH_LIKE -> 0xFF4C9A4C;
            case CHAOTIC -> 0xFF9A6B4C;
        };
    }

    private static List<Vector3f[]> buildUnitSphereTriangles() {
        List<Vector3f[]> triangles = new ArrayList<>();

        for (int lat = 0; lat < LAT_SEGMENTS; lat++) {
            double theta1 = Math.PI * lat / LAT_SEGMENTS;
            double theta2 = Math.PI * (lat + 1) / LAT_SEGMENTS;

            for (int lon = 0; lon < LON_SEGMENTS; lon++) {
                double phi1 = 2 * Math.PI * lon / LON_SEGMENTS;
                double phi2 = 2 * Math.PI * (lon + 1) / LON_SEGMENTS;

                Vector3f p1 = sphericalToCartesian(theta1, phi1);
                Vector3f p2 = sphericalToCartesian(theta2, phi1);
                Vector3f p3 = sphericalToCartesian(theta2, phi2);
                Vector3f p4 = sphericalToCartesian(theta1, phi2);

                triangles.add(new Vector3f[]{p1, p2, p3});
                triangles.add(new Vector3f[]{p1, p3, p4});
            }
        }

        return triangles;
    }

    private static Vector3f sphericalToCartesian(double theta, double phi) {
        float x = (float) (Math.sin(theta) * Math.cos(phi));
        float y = (float) Math.cos(theta);
        float z = (float) (Math.sin(theta) * Math.sin(phi));
        return new Vector3f(x, y, z);
    }
}