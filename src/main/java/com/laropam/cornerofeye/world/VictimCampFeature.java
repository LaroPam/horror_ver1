package com.laropam.cornerofeye.world;

import com.laropam.cornerofeye.block.MarkBlock;
import com.laropam.cornerofeye.registry.ModBlocks;
import com.laropam.cornerofeye.registry.ModItems;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Where someone camped before you, and didn't leave. A dead campfire, torches burnt down to stubs around it, a tent
 * half torn down, a bedroll with blood on it, claw marks on the log they sat on, handprints leading off into the
 * dark. In the chest, their notes, which stop in the middle of a word, and a few of the scraps it left them.
 */
public class VictimCampFeature extends Feature<NoneFeatureConfiguration> {
    public static final int DIARIES = 3;
    public static final int PAGES = 5;

    public VictimCampFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        // Flat, dry, open ground: no camps in the sea, on cliffs or under the trees' roots.
        int base = origin.getY();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos column = origin.offset(dx, 0, dz);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, column.getX(), column.getZ());
                BlockPos groundPos = new BlockPos(column.getX(), top - 1, column.getZ());
                BlockState ground = level.getBlockState(groundPos);
                if (Math.abs(top - base) > 1 || !ground.isSolidRender(level, groundPos) || !ground.getFluidState().isEmpty()
                        || ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS)) {
                    return false;
                }
            }
        }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction side = facing.getClockWise();
        BlockPos fire = surface(level, origin);
        set(level, fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.FACING, facing));

        // Burnt down to stubs, all the way round. They did try.
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 2.0D * i / 4.0D + random.nextDouble() * 0.6D;
            BlockPos at = surface(level, fire.offset((int) Math.round(Math.cos(angle) * 3.0D), 0, (int) Math.round(Math.sin(angle) * 3.0D)));
            if (level.getBlockState(at).isAir()) {
                set(level, at, ModBlocks.UNLIT_TORCH.get().defaultBlockState());
            }
        }

        // The log they sat on, gouged.
        BlockPos log = surface(level, fire.relative(side.getOpposite(), 2));
        for (int k = -1; k <= 1; k++) {
            BlockPos at = log.relative(facing, k);
            set(level, at, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, facing.getAxis()));
            if (random.nextInt(3) != 0) {
                set(level, at.relative(side), mark(side, MarkBlock.SCRATCH));
            }
        }

        // The tent, half pulled down: two poles, a roof with holes torn in it.
        BlockPos tent = surface(level, fire.relative(facing, 3));
        for (int k : new int[]{-1, 1}) {
            BlockPos pole = tent.relative(side, k);
            set(level, pole, Blocks.OAK_FENCE.defaultBlockState());
            if (k == -1) {
                set(level, pole.above(), Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        for (int k = -1; k <= 1; k++) {
            for (int depth = 0; depth <= 1; depth++) {
                BlockPos cloth = tent.relative(side, k).relative(facing, depth).above(k == -1 ? 2 : 1);
                if (random.nextInt(4) != 0 && level.getBlockState(cloth).isAir()) {
                    set(level, cloth, (k == 1 ? Blocks.BROWN_CARPET : Blocks.BROWN_WOOL).defaultBlockState());
                }
            }
        }
        // The bedroll under it, and the blood on it and round it.
        BlockPos bedroll = tent.relative(facing);
        set(level, bedroll, Blocks.RED_CARPET.defaultBlockState());
        set(level, bedroll.relative(side), Blocks.WHITE_CARPET.defaultBlockState());
        for (int i = 0; i < 3; i++) {
            BlockPos at = surface(level, bedroll.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2));
            if (level.getBlockState(at).isAir()) {
                set(level, at, mark(Direction.UP, random.nextBoolean() ? MarkBlock.SMEAR : MarkBlock.HAND));
            }
        }

        // Its handprints, leading away from the bedroll into the dark. Something was dragged.
        Direction away = random.nextBoolean() ? side : side.getOpposite();
        for (int step = 2; step <= 12; step += 2) {
            BlockPos at = surface(level, bedroll.relative(away, step).relative(facing, random.nextInt(3) - 1));
            if (level.getBlockState(at).isAir()) {
                set(level, at, mark(Direction.UP, step % 4 == 0 ? MarkBlock.SMEAR : MarkBlock.HAND));
            }
        }

        // Their things.
        BlockPos chestPos = surface(level, fire.relative(side, 2).relative(facing, -1));
        set(level, chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, side.getOpposite()));
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            int slot = 0;
            chest.setItem(slot++, diary(random.nextInt(DIARIES)));
            chest.setItem(slot++, new ItemStack(ModItems.SHADOW_SCRAP.get(), 1 + random.nextInt(2)));
            chest.setItem(slot++, new ItemStack(ModItems.UNLIT_TORCH.get(), 3 + random.nextInt(6)));
            chest.setItem(slot++, new ItemStack(Items.BREAD, 1 + random.nextInt(3)));
            ItemStack flint = new ItemStack(Items.FLINT_AND_STEEL);
            // A strike or two left in it.
            flint.setDamageValue(Math.max(0, flint.getMaxDamage() - 1 - random.nextInt(2)));
            chest.setItem(slot + random.nextInt(20), flint);
        }
        return true;
    }

    /** One of their notebooks: pages in whatever language you read, ending in the middle of a word. */
    public static ItemStack diary(int which) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString(WrittenBookItem.TAG_TITLE, "...");
        tag.putString(WrittenBookItem.TAG_AUTHOR, "?");
        tag.putBoolean(WrittenBookItem.TAG_RESOLVED, true);
        ListTag pages = new ListTag();
        for (int i = 0; i < PAGES; i++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("cornerofeye.camp.diary." + which + "." + i))));
        }
        tag.put(WrittenBookItem.TAG_PAGES, pages);
        book.setHoverName(Component.translatable("cornerofeye.camp.diary.title").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return book;
    }

    private static BlockState mark(Direction facing, int kind) {
        return ModBlocks.MARK.get().defaultBlockState().setValue(MarkBlock.FACING, facing).setValue(MarkBlock.KIND, kind);
    }

    /** The first open block above the ground at this column. */
    private static BlockPos surface(WorldGenLevel level, BlockPos pos) {
        return new BlockPos(pos.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pos.getX(), pos.getZ()), pos.getZ());
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
