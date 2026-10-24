package com.laropam.cornerofeye.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Something it left on a wall or floor: claw gouges, a long-fingered handprint, a smear. */
public class MarkBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final int SCRATCH = 0;
    public static final int HAND = 1;
    public static final int SMEAR = 2;
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 2);

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.UP, Block.box(0, 0, 0, 16, 0.5, 16));
        SHAPES.put(Direction.DOWN, Block.box(0, 15.5, 0, 16, 16, 16));
        SHAPES.put(Direction.NORTH, Block.box(0, 0, 15.5, 16, 16, 16));
        SHAPES.put(Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 0.5));
        SHAPES.put(Direction.WEST, Block.box(15.5, 0, 0, 16, 16, 16));
        SHAPES.put(Direction.EAST, Block.box(0, 0, 0, 0.5, 16, 16));
    }

    public MarkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(KIND, SCRATCH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, KIND);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** It sits on the block behind it, facing out. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !this.canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
