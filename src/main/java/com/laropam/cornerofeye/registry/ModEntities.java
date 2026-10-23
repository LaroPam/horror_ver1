package com.laropam.cornerofeye.registry;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.TheOneEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CornerOfTheEye.MODID);

    public static final RegistryObject<EntityType<TheOneEntity>> THE_ONE = ENTITIES.register("the_one",
            () -> EntityType.Builder.of(TheOneEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 3.2F)
                    .fireImmune()
                    // Tracked far enough to be glimpsed across a dark field.
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("the_one"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(THE_ONE.get(), TheOneEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
