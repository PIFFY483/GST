package com.gst.command;

import com.gst.entity.ModEntities;
import com.gst.world.PlanetDimensions;
import com.gst.world.UniverseSeedManager;
import com.gst.world.planet.PlanetGridManager;
import com.gst.world.star.PlanetApproachHandler;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class SpacePodCommands {

    private SpacePodCommands() {
    }

    public static void register() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) -> {
                    dispatcher.register(CommandManager.literal("gst")
                            .then(CommandManager.literal("spawnpod")
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        ServerPlayerEntity player = source.getPlayerOrThrow();

                                        var pod = ModEntities.SPACE_POD.create(source.getWorld());
                                        if (pod != null) {
                                            pod.refreshPositionAndAngles(
                                                    player.getX() + player.getRotationVector().x * 3,
                                                    player.getY(),
                                                    player.getZ() + player.getRotationVector().z * 3,
                                                    player.getYaw(),
                                                    0.0f
                                            );
                                            source.getWorld().spawnEntity(pod);
                                            source.sendFeedback(() -> net.minecraft.text.Text.literal("Uzay kapsulu olusturuldu."), false);
                                        }
                                        return 1;
                                    }))
                            .then(CommandManager.literal("goplanet")
                                    .executes(context -> {
                                        ServerCommandSource source = context.getSource();
                                        ServerPlayerEntity player = source.getPlayerOrThrow();

                                        if (!UniverseSeedManager.isInitialized()) {
                                            source.sendError(Text.literal("Universe seed henuz hazir degil, biraz bekleyip tekrar dene."));
                                            return 0;
                                        }

                                        long universeSeed = UniverseSeedManager.getUniverseSeed();
                                        long worldTime = player.getServerWorld().getTime();

                                        PlanetApproachHandler.NearestPlanet nearest = PlanetApproachHandler.findNearest(
                                                universeSeed, player.getX(), player.getY(), player.getZ(), worldTime
                                        );

                                        if (nearest == null) {
                                            source.sendError(Text.literal("Yakin sektorlerde hic gezegen bulunamadi. Baska bir konumdan dene."));
                                            return 0;
                                        }

                                        ServerWorld planetsWorld = source.getServer().getWorld(PlanetDimensions.PLANETS_WORLD_KEY);
                                        if (planetsWorld == null) {
                                            source.sendError(Text.literal("gst:planets boyutu yuklenemedi - dimension json kayitlarini kontrol et."));
                                            return 0;
                                        }

                                        int cellX = nearest.orbit().terrainCellX();
                                        int cellZ = nearest.orbit().terrainCellZ();

                                        double targetX = cellX * (double) PlanetGridManager.CELL_SIZE + PlanetGridManager.CELL_SIZE / 2.0;
                                        double targetZ = cellZ * (double) PlanetGridManager.CELL_SIZE + PlanetGridManager.CELL_SIZE / 2.0;
                                        double targetY = 900.0;

                                        player.teleport(planetsWorld, targetX, targetY, targetZ, player.getYaw(), player.getPitch());

                                        String planetName = com.gst.world.star.PlanetNaming.getPlanetName(nearest.system(), nearest.orbit());
                                        com.gst.world.PlanetEntryServerHandler.sendPlanetName(player, planetName);

                                        double distance = nearest.distance();
                                        source.sendFeedback(() -> net.minecraft.text.Text.literal(
                                                planetName + " gezegenine isinlandin (gercek mesafe: " + (long) distance + " blok idi)."
                                        ), false);
                                        return 1;
                                    })));
                });
    }
}