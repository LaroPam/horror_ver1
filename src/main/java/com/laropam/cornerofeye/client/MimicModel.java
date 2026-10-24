package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.entity.MimicEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** A player, almost: it waves the way people wave, and when it stops pretending its head turns over. */
public class MimicModel extends PlayerModel<MimicEntity> {
    public MimicModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(MimicEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity.isWaving()) {
            this.rightArm.xRot = -2.7F;
            this.rightArm.zRot = -0.25F + Mth.sin(ageInTicks * 0.55F) * 0.35F;
        }
        float twist = entity.getTwist(ageInTicks - entity.tickCount);
        if (twist > 0.0F) {
            // In jerks, the way something turns that has no business turning that far.
            float eased = twist < 1.0F ? (float) Math.floor(twist * 7.0F) / 7.0F + Mth.sin(ageInTicks * 3.1F) * 0.02F : 1.0F;
            this.head.zRot = eased * Mth.PI;
            this.head.y -= eased * 1.5F;
            this.rightArm.xRot = 0.0F;
            this.leftArm.xRot = 0.0F;
        }
        this.hat.copyFrom(this.head);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
    }
}
