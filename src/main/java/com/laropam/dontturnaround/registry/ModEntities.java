package com.laropam.dontturnaround.registry;

import com.laropam.dontturnaround.DontTurnAround;
import com.laropam.dontturnaround.entity.FacelessEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DontTurnAround.MODID);

    public static final RegistryObject<EntityType<FacelessEntity>> FACELESS = ENTITIES.register("faceless",
            () -> EntityType.Builder.of(FacelessEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.7F)
                    .fireImmune()
                    // Tracked far enough that a watcher on the horizon is still visible.
                    .clientTrackingRange(10)
                    .build("faceless"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FACELESS.get(), FacelessEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
