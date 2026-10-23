package com.laropam.cornerofeye.haunt;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.RotationSegment;

/** Signs nobody placed. Text lives in the lang files as cornerofeye.sign.N.L */
public final class Signs {
    /** Number of lines in each message. */
    private static final int[] LINES = {1, 3, 2, 2, 1, 2, 3, 2, 2, 3};
    public static final int WATCHED_YOU_SLEEP = 7;

    public static int randomMessage(RandomSource random) {
        int message;
        do {
            message = random.nextInt(LINES.length);
        } while (message == WATCHED_YOU_SLEEP);
        return message;
    }

    /** Places a waxed sign readable by someone standing at {@code viewerYaw} from it. */
    public static boolean place(ServerLevel level, BlockPos pos, float viewerYaw, int message) {
        int rotation = RotationSegment.convertToSegment(viewerYaw + 180.0F);
        level.setBlock(pos, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return false;
        }
        int lines = LINES[message];
        int first = (4 - lines) / 2;
        SignText text = sign.getFrontText();
        for (int i = 0; i < lines; i++) {
            text = text.setMessage(first + i, Component.translatable("cornerofeye.sign." + message + "." + i));
        }
        sign.setText(text, true);
        sign.setWaxed(true);
        return true;
    }

    private Signs() {
    }
}
