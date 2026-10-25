package com.laropam.cornerofeye.block;

import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A little bell on a thread, hung from the top of a doorway or a tunnel. Nothing a person walking through notices;
 * but when it passes, the bell rings, and you know where it is.
 */
public class BellStringBlock extends Block {
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
    private static final VoxelShape SHAPE = Block.box(6.5D, 4.0D, 6.5D, 9.5D, 16.0D, 9.5D);

    public BellStringBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(RINGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RINGING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** It hangs from whatever is above it. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** It brushed past. The bell swings and rings, and everyone near enough hears where. */
    public static void ring(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BellStringBlock) || state.getValue(RINGING)) {
            return;
        }
        level.setBlock(pos, state.setValue(RINGING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), 30);
        level.playSound(null, pos, ModSounds.BELL_STRING.get(), SoundSource.BLOCKS, 1.6F, 1.0F + level.random.nextFloat() * 0.15F);
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 40.0D * 40.0D) {
                HorrorNetwork.send(player, HorrorPacket.Type.DREAD, 30);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RINGING)) {
            level.setBlock(pos, state.setValue(RINGING, false), Block.UPDATE_ALL);
        }
    }
}
