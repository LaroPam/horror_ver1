package com.laropam.cornerofeye.registry;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.block.BellStringBlock;
import com.laropam.cornerofeye.block.BloodBlock;
import com.laropam.cornerofeye.block.MarkBlock;
import com.laropam.cornerofeye.block.WritingBlock;
import com.laropam.cornerofeye.block.WritingBlockEntity;
import com.laropam.cornerofeye.block.UnlitTorchBlock;
import com.laropam.cornerofeye.block.UnlitWallTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CornerOfTheEye.MODID);

    public static final RegistryObject<Block> BLOOD = BLOCKS.register("blood",
            () -> new BloodBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .replaceable()
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> UNLIT_TORCH = BLOCKS.register("unlit_torch",
            () -> new UnlitTorchBlock(torchProperties()));
    public static final RegistryObject<Block> UNLIT_WALL_TORCH = BLOCKS.register("unlit_wall_torch",
            () -> new UnlitWallTorchBlock(torchProperties().dropsLike(UNLIT_TORCH.get())));

    public static final RegistryObject<Block> MARK = BLOCKS.register("mark",
            () -> new MarkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).noCollission().noOcclusion().instabreak()
                    .replaceable().noLootTable().pushReaction(PushReaction.DESTROY).sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<Block> WRITING = BLOCKS.register("writing",
            () -> new WritingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).noCollission().noOcclusion().instabreak()
                    .replaceable().noLootTable().pushReaction(PushReaction.DESTROY).sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> BELL_STRING = BLOCKS.register("bell_string",
            () -> new BellStringBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).noCollission().noOcclusion().instabreak()
                    .pushReaction(PushReaction.DESTROY).sound(SoundType.WOOL)));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CornerOfTheEye.MODID);
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<WritingBlockEntity>> WRITING_ENTITY = BLOCK_ENTITIES.register("writing",
            () -> BlockEntityType.Builder.of(WritingBlockEntity::new, WRITING.get()).build(null));

    private static BlockBehaviour.Properties torchProperties() {
        return BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY);
    }

    private ModBlocks() {
    }
}
