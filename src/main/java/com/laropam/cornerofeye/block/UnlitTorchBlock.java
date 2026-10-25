package com.laropam.cornerofeye.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A torch it put out. Picking it up and putting it back won't light it: strike flint over it. */
public class UnlitTorchBlock extends TorchBlock {
    /** It was a soul torch, and lit again it is one again. */
    public static final BooleanProperty SOUL = BooleanProperty.create("soul");

    public UnlitTorchBlock(Properties properties) {
        super(properties, ParticleTypes.SMOKE);
        this.registerDefaultState(this.stateDefinition.any().setValue(SOUL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOUL);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return Relight.torch(level, pos, player, hand, (state.getValue(SOUL) ? Blocks.SOUL_TORCH : Blocks.TORCH).defaultBlockState());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D, 0.0D, 0.01D, 0.0D);
        }
    }
}
