package com.laropam.cornerofeye.block;

import com.laropam.cornerofeye.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Which of its messages is written here, and how the letters fell (a seed, so every client sees the same mess). */
public class WritingBlockEntity extends BlockEntity {
    private int message;
    private long seed;

    public WritingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WRITING_ENTITY.get(), pos, state);
    }

    public void setMessage(int message, long seed) {
        this.message = message;
        this.seed = seed;
        this.setChanged();
    }

    public int getMessage() {
        return this.message;
    }

    public long getSeed() {
        return this.seed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Message", this.message);
        tag.putLong("Seed", this.seed);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.message = tag.getInt("Message");
        this.seed = tag.getLong("Seed");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The words run well past this one block. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(3.0D);
    }
}
