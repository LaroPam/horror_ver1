package com.laropam.cornerofeye.registry;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.world.VictimCampFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** World generation: the camps of those who came before you (placed by data/cornerofeye/forge/biome_modifier). */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, CornerOfTheEye.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> VICTIM_CAMP = FEATURES.register("victim_camp",
            () -> new VictimCampFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }
}
