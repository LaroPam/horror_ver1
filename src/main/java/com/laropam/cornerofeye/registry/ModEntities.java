package com.laropam.cornerofeye.registry;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.CorpseEntity;
import com.laropam.cornerofeye.entity.FlareEntity;
import com.laropam.cornerofeye.entity.MimicEntity;
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

    public static final RegistryObject<EntityType<CorpseEntity>> CORPSE = ENTITIES.register("corpse",
            () -> EntityType.Builder.<CorpseEntity>of(CorpseEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.4F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("corpse"));

    public static final RegistryObject<EntityType<FlareEntity>> FLARE = ENTITIES.register("flare",
            () -> EntityType.Builder.<FlareEntity>of(FlareEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build("flare"));

    public static final RegistryObject<EntityType<MimicEntity>> MIMIC = ENTITIES.register("mimic",
            () -> EntityType.Builder.<MimicEntity>of(MimicEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("mimic"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(THE_ONE.get(), TheOneEntity.createAttributes().build());
        event.put(MIMIC.get(), MimicEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
