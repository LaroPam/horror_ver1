package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.entity.scene.PeekScene;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * Over three blocks tall, all bones. Stands hunched with its head cocked and fingers brushing the ground
 * while it watches; drops onto all fours, knees up like a spider and head upside down, when it moves.
 * Texture layout is mirrored in tools/gen_textures.py.
 */
public class TheOneModel extends HierarchicalModel<TheOneEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(CornerOfTheEye.id("the_one"), "main");

    private static final float PI = (float) Math.PI;

    private final ModelPart root;
    private final ModelPart hips;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart rightThigh;
    private final ModelPart rightShin;
    private final ModelPart leftThigh;
    private final ModelPart leftShin;
    private float alpha = 1.0F;

    public TheOneModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.hips = root.getChild("hips");
        this.body = this.hips.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.rightArm = this.body.getChild("right_arm");
        this.rightForearm = this.rightArm.getChild("forearm");
        this.leftArm = this.body.getChild("left_arm");
        this.leftForearm = this.leftArm.getChild("forearm");
        this.rightThigh = this.hips.getChild("right_thigh");
        this.rightShin = this.rightThigh.getChild("shin");
        this.leftThigh = this.hips.getChild("left_thigh");
        this.leftShin = this.leftThigh.getChild("shin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition hips = root.addOrReplaceChild("hips", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, 0.0F));
        PartDefinition body = hips.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(28, 0).addBox(-3.5F, -16.0F, -2.0F, 7, 16, 4), PartPose.ZERO);

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -11.0F, -3.5F, 7, 10, 7), PartPose.offset(0.0F, -16.0F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-3.0F, 0.0F, -3.5F, 6, 2, 6), PartPose.offset(0.0F, -1.0F, 0.0F));

        addArm(body, "right_arm", -4.5F, 100, 116);
        addArm(body, "left_arm", 4.5F, 108, 100);

        addLeg(hips, "right_thigh", -1.9F, 52, 76);
        addLeg(hips, "left_thigh", 1.9F, 64, 88);

        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addArm(PartDefinition body, String name, float x, int upperU, int forearmU) {
        PartDefinition arm = body.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(upperU, 0).addBox(-1.0F, -1.0F, -1.0F, 2, 17, 2), PartPose.offset(x, -15.0F, 0.0F));
        int forearmV = forearmU == 100 ? 20 : 0;
        PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create()
                .texOffs(forearmU, forearmV).addBox(-1.0F, 0.0F, -1.0F, 2, 17, 2), PartPose.offset(0.0F, 16.0F, 0.0F));
        for (int i = 0; i < 3; i++) {
            float splay = (i - 1) * 0.22F;
            forearm.addOrReplaceChild("finger" + i, CubeListBuilder.create()
                            .texOffs(108, 20).addBox(-0.5F, 0.0F, -0.5F, 1, 8, 1),
                    PartPose.offsetAndRotation((i - 1) * 0.7F, 16.5F, -0.3F, -0.1F, 0.0F, splay));
        }
    }

    private static void addLeg(PartDefinition hips, String name, float x, int thighU, int shinU) {
        PartDefinition thigh = hips.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(thighU, 0).addBox(-1.5F, 0.0F, -1.5F, 3, 13, 3), PartPose.offset(x, 0.0F, 0.0F));
        thigh.addOrReplaceChild("shin", CubeListBuilder.create()
                .texOffs(shinU, 0).addBox(-1.5F, 0.0F, -1.5F, 3, 13, 3), PartPose.offset(0.0F, 13.0F, 0.0F));
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public float getAlpha() {
        return this.alpha;
    }

    @Override
    public void setupAnim(TheOneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float crawl = entity.getCrawlAmount(ageInTicks - entity.tickCount);
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        float pitch = headPitch * Mth.DEG_TO_RAD;
        float twitch = Mth.sin(ageInTicks * 0.9F) * Mth.sin(ageInTicks * 0.13F) > 0.92F ? 0.25F : 0.0F;

        if (entity.getPhase() == Phase.REVEALED) {
            this.poseRevealed(ageInTicks);
            return;
        }

        // Standing: hunched, head cocked too far, fingers brushing the ground.
        float bodyStand = 0.28F;
        float headTilt = 0.55F + twitch + Mth.sin(ageInTicks * 0.03F) * 0.05F;

        // Crawling: torso flat, knees above the hips, elbows high, head upside down.
        float bodyCrawl = 1.4F;
        float swing = Mth.cos(limbSwing * 0.8F) * Mth.clamp(limbSwingAmount * 1.6F, 0.0F, 1.0F);

        this.hips.y = Mth.lerp(crawl, -2.0F, 18.5F);
        this.body.xRot = Mth.lerp(crawl, bodyStand, bodyCrawl);

        this.head.xRot = Mth.lerp(crawl, -bodyStand, -bodyCrawl) + pitch;
        this.head.yRot = yaw;
        this.head.zRot = Mth.lerp(crawl, headTilt, PI + twitch);
        this.jaw.xRot = Mth.lerp(crawl, 0.12F, 0.45F) + twitch;

        float armStand = -bodyStand + 0.05F;
        this.rightArm.xRot = Mth.lerp(crawl, armStand + Mth.sin(ageInTicks * 0.04F) * 0.03F, -3.7F + swing * 0.5F);
        this.leftArm.xRot = Mth.lerp(crawl, armStand + Mth.sin(ageInTicks * 0.04F + 1.0F) * 0.03F, -3.7F - swing * 0.5F);
        this.rightArm.zRot = Mth.lerp(crawl, 0.14F, 0.45F);
        this.leftArm.zRot = -this.rightArm.zRot;
        this.rightForearm.xRot = Mth.lerp(crawl, -0.08F, 1.68F - swing * 0.3F);
        this.leftForearm.xRot = Mth.lerp(crawl, -0.08F, 1.68F + swing * 0.3F);

        this.rightThigh.xRot = Mth.lerp(crawl, -0.05F, 2.2F - swing * 0.4F);
        this.leftThigh.xRot = Mth.lerp(crawl, -0.05F, 2.2F + swing * 0.4F);
        this.rightThigh.zRot = Mth.lerp(crawl, 0.0F, 0.35F);
        this.leftThigh.zRot = -this.rightThigh.zRot;
        this.rightShin.xRot = Mth.lerp(crawl, 0.1F, -2.05F + swing * 0.3F);
        this.leftShin.xRot = Mth.lerp(crawl, 0.1F, -2.05F - swing * 0.3F);

        float sceneTime = entity.getAnimTime(ageInTicks - entity.tickCount);
        switch (entity.getScene()) {
            case PEEK -> this.posePeek(entity, sceneTime, headTilt, pitch);
            case BEHIND -> this.poseBehind(ageInTicks, pitch);
            case DOOR -> {
                if (entity.getAnim() == 0) {
                    // Squeezing through: the head jerks side to side.
                    this.head.zRot += Mth.sin(ageInTicks * 1.7F) * 0.3F;
                } else if (entity.getAnim() == 2) {
                    this.reach(0.75F, ageInTicks);
                }
            }
            case LIGHTS_OUT -> {
                if (entity.getAnim() == 1) {
                    this.reach(0.5F, ageInTicks);
                }
            }
            case STILL -> {
                // Sniffing for you.
                this.head.xRot += Mth.sin(ageInTicks * 1.3F) * 0.18F;
                this.head.yRot += Mth.sin(ageInTicks * 0.45F) * 0.5F;
                this.jaw.xRot = 0.3F + Mth.sin(ageInTicks * 2.1F) * 0.15F;
            }
            default -> {
            }
        }
    }

    /**
     * Crouched behind a trunk, the whole body bent sideways so only the head and one hand come out past the
     * bark, head tipped over until it is almost lying on its side.
     */
    private void posePeek(TheOneEntity entity, float time, float headTilt, float pitch) {
        float lean = switch (entity.getAnim()) {
            case 0 -> smoothstep(time / PeekScene.LEAN_OUT);
            case 1 -> 1.0F;
            default -> 1.0F - Mth.clamp(time / PeekScene.SNAP_BACK, 0.0F, 1.0F);
        };
        float side = entity.isLeaningLeft() ? 1.0F : -1.0F;
        this.hips.y = Mth.lerp(lean, -2.0F, 4.0F);
        this.rightThigh.xRot = this.leftThigh.xRot = Mth.lerp(lean, -0.05F, -0.75F);
        this.rightShin.xRot = this.leftShin.xRot = Mth.lerp(lean, 0.1F, 1.5F);
        this.body.xRot = Mth.lerp(lean, 0.28F, 0.1F);
        this.body.zRot = side * 0.85F * lean;
        this.head.xRot = -this.body.xRot + pitch;
        this.head.zRot = Mth.lerp(lean, headTilt, side * 1.25F);
        this.jaw.xRot = Mth.lerp(lean, 0.12F, 0.3F);
        ModelPart lead = side > 0.0F ? this.leftArm : this.rightArm;
        ModelPart leadForearm = side > 0.0F ? this.leftForearm : this.rightForearm;
        ModelPart other = side > 0.0F ? this.rightArm : this.leftArm;
        // Fingers hooked around the edge of the bark at head height.
        lead.xRot = Mth.lerp(lean, lead.xRot, -2.5F);
        lead.zRot = Mth.lerp(lean, lead.zRot, -side * 0.35F);
        leadForearm.xRot = Mth.lerp(lean, leadForearm.xRot, 1.25F);
        other.zRot = Mth.lerp(lean, other.zRot, -this.body.zRot);
    }

    /** Bent right over you from behind, jaw hanging, hands either side of your head. */
    private void poseBehind(float ageInTicks, float pitch) {
        this.body.xRot = 0.65F + Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.head.xRot = -0.55F + pitch;
        this.head.zRot = 0.35F;
        this.jaw.xRot = 0.55F + Mth.sin(ageInTicks * 0.3F) * 0.1F;
        this.rightArm.xRot = this.leftArm.xRot = -1.25F;
        this.rightArm.zRot = 0.4F;
        this.leftArm.zRot = -0.4F;
        this.rightForearm.xRot = this.leftForearm.xRot = -0.5F;
    }

    /** Arms coming up towards you, jaw opening. */
    private void reach(float amount, float ageInTicks) {
        this.rightArm.xRot = Mth.lerp(amount, this.rightArm.xRot, -1.4F + Mth.sin(ageInTicks * 0.2F) * 0.05F);
        this.leftArm.xRot = Mth.lerp(amount, this.leftArm.xRot, -1.4F + Mth.sin(ageInTicks * 0.2F + 1.5F) * 0.05F);
        this.rightForearm.xRot = this.leftForearm.xRot = Mth.lerp(amount, 0.0F, -0.3F);
        this.jaw.xRot = Mth.lerp(amount, this.jaw.xRot, 0.7F);
    }

    private static float smoothstep(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Caught in the light: thrown upright, arms flung out, jaw hanging open, shaking. */
    private void poseRevealed(float ageInTicks) {
        float shake = Mth.sin(ageInTicks * 2.7F) * 0.12F;
        this.body.xRot = -0.15F + shake * 0.5F;
        this.head.xRot = -0.5F;
        this.head.zRot = shake * 3.0F;
        this.jaw.xRot = 1.0F;
        this.rightArm.xRot = -0.3F;
        this.leftArm.xRot = -0.3F;
        this.rightArm.zRot = 1.9F + shake;
        this.leftArm.zRot = -1.9F - shake;
        this.rightForearm.xRot = -0.6F;
        this.leftForearm.xRot = -0.6F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha * this.alpha);
    }
}
