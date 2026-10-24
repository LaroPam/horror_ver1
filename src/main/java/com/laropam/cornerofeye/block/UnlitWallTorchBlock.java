package com.laropam.cornerofeye.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A wall torch it put out. Strike flint over it to light it again. */
public class UnlitWallTorchBlock extends WallTorchBlock {
    public UnlitWallTorchBlock(Properties properties) {
        super(properties, ParticleTypes.SMOKE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return Relight.torch(level, pos, player, hand, Blocks.WALL_TORCH.defaultBlockState().setValue(FACING, state.getValue(FACING)));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            Direction opposite = state.getValue(FACING).getOpposite();
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D + 0.27D * opposite.getStepX(), pos.getY() + 0.85D,
                    pos.getZ() + 0.5D + 0.27D * opposite.getStepZ(), 0.0D, 0.01D, 0.0D);
        }
    }
}
