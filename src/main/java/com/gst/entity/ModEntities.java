package com.gst.entity;

import com.gst.GalacticSpaceTravel;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {

    public static final EntityType<SpacePodEntity> SPACE_POD = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(GalacticSpaceTravel.MOD_ID, "space_pod"),
            FabricEntityTypeBuilder.<SpacePodEntity>create(SpawnGroup.MISC, SpacePodEntity::new)
                    .dimensions(EntityDimensions.fixed(1.4f, 1.6f))
                    .trackRangeChunks(10)
                    .build()
    );

    private ModEntities() {
    }

    public static void register() {
        // Bu metodun cagrilmasi, sinifin yuklenmesini ve yukaridaki static
        // alanin (SPACE_POD) kaydedilmesini garanti eder.
    }
}