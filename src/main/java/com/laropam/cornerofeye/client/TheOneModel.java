package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.Burrow;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.entity.scene.PeekScene;
import com.laropam.cornerofeye.entity.scene.WindowScene;
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
    private static final float HALF_PI = PI / 2.0F;

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
        // Its fingers keep growing.
        float claws = 1.0F + 0.7F * entity.getMenace();
        for (ModelPart forearm : new ModelPart[]{this.rightForearm, this.leftForearm}) {
            for (int i = 0; i < 3; i++) {
                forearm.getChild("finger" + i).yScale = claws;
            }
        }
        float crawl = entity.getCrawlAmount(ageInTicks - entity.tickCount);
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        float pitch = headPitch * Mth.DEG_TO_RAD;
        float menace = entity.getMenace();
        // The worse it gets, the more often it jerks.
        float twitch = Mth.sin(ageInTicks * 0.9F) * Mth.sin(ageInTicks * 0.13F) > 0.92F - 0.3F * menace ? 0.25F + 0.15F * menace : 0.0F;

        if (entity.getPhase() == Phase.REVEALED) {
            this.poseRevealed(ageInTicks);
            return;
        }

        // Standing: hunched, head cocked too far, fingers brushing the ground.
        float bodyStand = 0.28F;
        float headTilt = 0.55F + 0.2F * menace + twitch + Mth.sin(ageInTicks * 0.03F) * 0.05F;

        // Crawling: torso nearly flat, knees above the hips, elbows high, head upside down and looking ahead.
        float bodyCrawl = 1.25F;
        float swing = Mth.cos(limbSwing * 0.8F) * Mth.clamp(limbSwingAmount * 1.6F, 0.0F, 1.0F);
        // Upright it takes long, slow strides: legs swing from the hip, the trailing knee bends, arms barely move.
        float stride = Mth.cos(limbSwing * 0.45F) * Mth.clamp(limbSwingAmount * 1.4F, 0.0F, 1.0F);

        this.hips.y = Mth.lerp(crawl, -2.0F, 18.5F);
        this.body.xRot = Mth.lerp(crawl, bodyStand, bodyCrawl);

        // Parts rotate X first, then Z: with the head flipped over (zRot = PI), the same xRot as the body's
        // cancels it out, so the face ends up looking ahead, upside down, hanging below the neck.
        this.head.xRot = Mth.lerp(crawl, -bodyStand, bodyCrawl) + pitch;
        this.head.yRot = yaw;
        this.head.zRot = Mth.lerp(crawl, headTilt, PI + twitch);
        this.jaw.xRot = Mth.lerp(crawl, 0.12F, 0.45F) + 0.3F * menace + twitch;

        float armStand = -bodyStand + 0.05F;
        this.rightArm.xRot = Mth.lerp(crawl, armStand + Mth.sin(ageInTicks * 0.04F) * 0.03F - stride * 0.12F, -3.7F + swing * 0.5F);
        this.leftArm.xRot = Mth.lerp(crawl, armStand + Mth.sin(ageInTicks * 0.04F + 1.0F) * 0.03F + stride * 0.12F, -3.7F - swing * 0.5F);
        this.rightArm.zRot = Mth.lerp(crawl, 0.14F, 0.45F);
        this.leftArm.zRot = -this.rightArm.zRot;
        this.rightForearm.xRot = Mth.lerp(crawl, -0.08F, 1.68F - swing * 0.3F);
        this.leftForearm.xRot = Mth.lerp(crawl, -0.08F, 1.68F + swing * 0.3F);

        this.rightThigh.xRot = Mth.lerp(crawl, -0.05F + stride * 0.42F, 2.2F - swing * 0.4F);
        this.leftThigh.xRot = Mth.lerp(crawl, -0.05F - stride * 0.42F, 2.2F + swing * 0.4F);
        this.rightThigh.zRot = Mth.lerp(crawl, 0.0F, 0.35F);
        this.leftThigh.zRot = -this.rightThigh.zRot;
        this.rightShin.xRot = Mth.lerp(crawl, 0.1F + Math.max(0.0F, -stride) * 0.6F, -2.05F + swing * 0.3F);
        this.leftShin.xRot = Mth.lerp(crawl, 0.1F + Math.max(0.0F, stride) * 0.6F, -2.05F - swing * 0.3F);

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
            case CEILING -> this.poseHanging(ageInTicks, yaw, pitch);
            case UNDERWATER -> {
                // Rising with its arms up, reaching for your feet.
                this.body.xRot = 0.0F;
                this.body.zRot = Mth.sin(ageInTicks * 0.08F) * 0.08F;
                this.head.xRot = pitch;
                this.rightArm.xRot = -2.9F + Mth.sin(ageInTicks * 0.15F) * 0.15F;
                this.leftArm.xRot = -2.9F + Mth.sin(ageInTicks * 0.15F + 2.0F) * 0.15F;
                this.rightArm.zRot = 0.25F;
                this.leftArm.zRot = -0.25F;
                this.rightForearm.xRot = this.leftForearm.xRot = 0.0F;
                this.jaw.xRot = 0.6F;
            }
            case CHASE -> {
                this.jaw.xRot = 0.85F;
                this.head.zRot += Mth.sin(ageInTicks * 2.3F) * 0.2F;
            }
            case WINDOW -> this.poseWindow(entity, sceneTime, ageInTicks, pitch, headTilt);
            case STILL -> {
                // Sniffing for you.
                this.head.xRot += Mth.sin(ageInTicks * 1.3F) * 0.18F;
                this.head.yRot += Mth.sin(ageInTicks * 0.45F) * 0.5F;
                this.jaw.xRot = 0.3F + Mth.sin(ageInTicks * 2.1F) * 0.15F;
            }
            default -> {
            }
        }
        float partial = ageInTicks - entity.tickCount;
        this.fitToRoom(entity, partial, crawl);
        float prone = entity.getProne(partial) * crawl;
        if (prone > 0.0F) {
            this.poseProne(prone, limbSwing, limbSwingAmount, ageInTicks, twitch);
        }
        if (entity.getDigDirection() != 0) {
            this.poseDigging(entity, partial, ageInTicks);
        }
    }

    /**
     * Squeezed into a one-block tunnel: everything has to fit in it. Flat on its belly, torso level, head
     * up and facing forward, hind legs dragging straight out behind, and the long arms stretched out ahead
     * of it, claws hooking into the floor and hauling the rest of it along. Blended in by {@code amount} as
     * it squeezes in or climbs back out.
     */
    private void poseProne(float amount, float limbSwing, float limbSwingAmount, float ageInTicks, float twitch) {
        float moving = Mth.clamp(limbSwingAmount * 1.6F, 0.0F, 1.0F);
        float gait = limbSwing * 0.55F;
        float hipsY = 22.0F;
        float body = HALF_PI + Mth.sin(gait * 2.0F) * 0.04F * moving;
        this.hips.y = Mth.lerp(amount, this.hips.y, hipsY);
        this.hips.z = Mth.lerp(amount, this.hips.z, 0.0F);
        this.body.xRot = Mth.lerp(amount, this.body.xRot, body);
        this.body.zRot = Mth.lerp(amount, this.body.zRot, 0.0F);
        this.head.xRot = Mth.lerp(amount, this.head.xRot, -0.1F - body);
        this.head.zRot = Mth.lerp(amount, this.head.zRot, 0.3F + twitch);
        this.jaw.xRot = Mth.lerp(amount, this.jaw.xRot, 0.3F + 0.1F * Mth.sin(ageInTicks * 0.2F));
        // Arms: each hand reaches out along the floor, hooks in, and pulls back while the other reaches.
        for (int side = 0; side < 2; side++) {
            float phase = gait + side * PI;
            float reach = 0.5F + 0.5F * Mth.sin(phase) * moving;
            boolean lifted = Mth.cos(phase) > 0.0F && moving > 0.1F;
            float[] arm = this.reachFrom(hipsY, body, Mth.lerp(reach, 40.0F, 46.0F), lifted ? 3.5F : 0.8F);
            ModelPart upper = side == 0 ? this.rightArm : this.leftArm;
            ModelPart lower = side == 0 ? this.rightForearm : this.leftForearm;
            upper.xRot = Mth.lerp(amount, upper.xRot, arm[0] - body);
            lower.xRot = Mth.lerp(amount, lower.xRot, arm[1] - arm[0]);
            upper.zRot = Mth.lerp(amount, upper.zRot, side == 0 ? 0.06F : -0.06F);
            this.curl(lower, Mth.lerp(amount, 0.0F, lifted ? -0.2F : 1.1F));
        }
        // Legs straight out behind, one knee drawn up, then the other.
        for (int side = 0; side < 2; side++) {
            float push = Math.max(0.0F, Mth.sin(gait + side * PI + 0.8F)) * moving;
            ModelPart thigh = side == 0 ? this.rightThigh : this.leftThigh;
            ModelPart shin = side == 0 ? this.rightShin : this.leftShin;
            float thighAngle = 1.48F - 0.15F * push;
            thigh.xRot = Mth.lerp(amount, thigh.xRot, thighAngle);
            thigh.zRot = Mth.lerp(amount, thigh.zRot, 0.0F);
            shin.xRot = Mth.lerp(amount, shin.xRot, 1.72F + 0.7F * push - thighAngle);
        }
    }

    /**
     * Tearing through a block, close up: the claws work only at the block it is breaking, never through the
     * walls around it. Positions are in model pixels from its hips (forward, up); the block in front starts
     * {@link #FACE} pixels ahead, where it stopped.
     * <ul>
     *     <li>Forward: hands planted on the face of the block, claws dragged down it hard, one after the other,
     *     head butting and biting at it. At the very end both arms punch through to the other side.</li>
     *     <li>Up: it rears its front up into the block over its head and claws at it, head thrown back.</li>
     *     <li>Down: head down into the hole in front of it, scooping out the block below like a dog.</li>
     *     <li>Through: it lies still in the hole it made and looks at you, one hand reaching in.</li>
     * </ul>
     */
    private void poseDigging(TheOneEntity entity, float partialTick, float ageInTicks) {
        int direction = entity.getDigDirection();
        float time = entity.getDigTime(partialTick);
        float progress = entity.getDigProgress(partialTick);
        float blockPx = 16.0F / entity.getVisualScale();
        float hipsY = this.hips.y;
        int listen = entity.getDigListen();
        if (time < listen) {
            this.poseListening(direction, time, ageInTicks, hipsY, blockPx);
            return;
        }
        time -= listen;
        // It gets more frantic the longer a block holds out.
        float period = Mth.lerp(progress, 11.0F, 6.5F);
        float shiver = Mth.sin(ageInTicks * 3.3F) * 0.03F;
        float floor = 24.0F - hipsY;

        switch (direction) {
            case Burrow.FORWARD -> {
                float body = HALF_PI + shiver;
                this.body.xRot = body;
                if (progress > 0.85F) {
                    // The claws come through to your side first.
                    float jab = Mth.sin(ageInTicks * 2.6F) * 1.5F;
                    this.setArm(true, body, this.reachFrom(hipsY, body, FACE + 14.0F + jab, floor - 1.0F + blockPx * 0.5F));
                    this.setArm(false, body, this.reachFrom(hipsY, body, FACE + 14.0F - jab, floor - 1.0F + blockPx * 0.45F));
                    this.curl(this.rightForearm, -0.5F);
                    this.curl(this.leftForearm, -0.5F);
                } else {
                    for (int side = 0; side < 2; side++) {
                        float p = time / period + side * 0.5F;
                        p -= Mth.floor(p);
                        // Up to the top of the block, slam in, drag down to the floor, back up.
                        float height = p < 0.14F ? Mth.lerp(p / 0.14F, 0.35F, 0.85F)
                                : p < 0.72F ? Mth.lerp((p - 0.14F) / 0.58F, 0.85F, 0.08F)
                                : Mth.lerp((p - 0.72F) / 0.28F, 0.08F, 0.35F);
                        float depth = p < 0.14F ? 1.5F : p < 0.72F ? 0.8F : -2.5F;
                        this.setArm(side == 0, body, this.reachFrom(hipsY, body, FACE + depth, floor - 1.0F + blockPx * height));
                        this.curl(side == 0 ? this.rightForearm : this.leftForearm, p < 0.72F ? 1.1F : -0.3F);
                    }
                    this.hips.z = -0.8F * Mth.abs(Mth.sin(time * PI / period));
                }
                this.head.xRot = -0.05F - body;
                this.head.zRot = 0.3F + Mth.sin(ageInTicks * 1.9F) * 0.15F;
                this.head.yRot = Mth.sin(ageInTicks * 0.7F) * 0.2F;
                this.jaw.xRot = 0.15F + 0.7F * Math.abs(Mth.sin(ageInTicks * 1.35F));
            }
            case Burrow.UP -> {
                // Head lifted, the claws driven up into the block over its hands and dragged back down.
                float body = 1.35F + shiver;
                this.body.xRot = body;
                for (int side = 0; side < 2; side++) {
                    float p = time / period + side * 0.5F;
                    p -= Mth.floor(p);
                    float strike = p < 0.18F ? p / 0.18F : 1.0F - (p - 0.18F) / 0.82F;
                    this.setArm(side == 0, body, this.reachFrom(hipsY, body, FACE - Mth.lerp(strike, 13.0F, 9.0F),
                            floor + blockPx * Mth.lerp(strike, 0.7F, 1.25F), false));
                    this.curl(side == 0 ? this.rightForearm : this.leftForearm, Mth.lerp(strike, 0.2F, 1.1F));
                }
                this.head.xRot = -0.7F - body;
                this.head.zRot = 0.35F + Mth.sin(ageInTicks * 1.7F) * 0.15F;
                this.head.yRot = 0.0F;
                this.jaw.xRot = 0.85F;
            }
            case Burrow.DOWN -> {
                // Head down, scooping out the block under its hands and ripping it back towards itself.
                float body = 1.75F + shiver;
                this.body.xRot = body;
                for (int side = 0; side < 2; side++) {
                    float p = time / period + side * 0.5F;
                    p -= Mth.floor(p);
                    float back = p < 0.6F ? p / 0.6F : 1.0F - (p - 0.6F) / 0.4F;
                    this.setArm(side == 0, body, this.reachFrom(hipsY, body, FACE - Mth.lerp(back, 2.0F, 9.0F),
                            floor - blockPx * Mth.lerp(back, 0.1F, 0.6F)));
                    this.curl(side == 0 ? this.rightForearm : this.leftForearm, Mth.lerp(back, -0.2F, 1.1F));
                }
                this.hips.y += 0.8F * Mth.abs(Mth.sin(time * PI / period));
                this.head.xRot = 0.9F - body;
                this.head.zRot = 0.5F + Mth.sin(ageInTicks * 2.2F) * 0.2F;
                this.head.yRot = 0.0F;
                this.jaw.xRot = 0.2F + 0.6F * Math.abs(Mth.sin(ageInTicks * 1.6F));
            }
            case Burrow.OPEN -> {
                // One hand comes up and lies flat on the door, still, as if it were listening; then it pushes, slowly.
                float reach = Mth.clamp(time / Burrow.OPEN_AT, 0.0F, 1.0F);
                float push = Mth.clamp((time - Burrow.OPEN_AT) / (Burrow.OPEN_LENGTH - Burrow.OPEN_AT), 0.0F, 1.0F);
                float body = this.body.xRot;
                this.setArm(true, body, this.reachFrom(hipsY, body, blockPx * (1.05F + 0.6F * push), floor + blockPx * Mth.lerp(reach, 0.25F, 1.0F)));
                this.curl(this.rightForearm, Mth.lerp(reach, 0.7F, -0.4F));
                this.head.zRot += 0.3F * reach + Mth.sin(ageInTicks * 0.5F) * 0.04F;
                this.jaw.xRot = 0.1F + 0.3F * push;
            }
            case Burrow.THROUGH -> {
                // Still. Breathing. Looking at you through the hole, one hand coming in.
                float t = Mth.clamp(time / 20.0F, 0.0F, 1.0F);
                float body = this.body.xRot;
                this.setArm(true, body, this.reachFrom(hipsY, body, FACE + Mth.lerp(t, 0.0F, 12.0F), floor - 1.0F + blockPx * 0.4F));
                this.curl(this.rightForearm, 0.6F + Mth.sin(ageInTicks * 0.9F) * 0.5F);
                this.head.zRot = 0.3F + 0.5F * t + Mth.sin(ageInTicks * 4.0F) * 0.04F;
                this.jaw.xRot = Mth.lerp(t, 0.2F, 1.0F);
            }
            default -> {
            }
        }
    }

    /**
     * The moment before it starts on a block: still, hands flat on it, fingers slowly spreading. Only the
     * head moves, in small jerks, as if it heard you on the other side.
     */
    private void poseListening(int direction, float time, float ageInTicks, float hipsY, float blockPx) {
        float spread = Mth.clamp(time / Burrow.LISTEN, 0.0F, 1.0F);
        float jerk = Mth.sin(ageInTicks * 0.31F) > 0.85F ? 0.35F : 0.0F;
        float floor = 24.0F - hipsY;
        float body = direction == Burrow.UP ? 1.35F : direction == Burrow.DOWN ? 1.75F : HALF_PI;
        this.body.xRot = body;
        float[] hands = switch (direction) {
            case Burrow.UP -> this.reachFrom(hipsY, body, FACE - 11.0F, floor + blockPx * 0.75F, false);
            case Burrow.DOWN -> this.reachFrom(hipsY, body, FACE - 4.0F, floor + 0.5F);
            default -> this.reachFrom(hipsY, body, FACE, floor - 1.0F + blockPx * 0.45F);
        };
        this.setArm(true, body, hands);
        this.setArm(false, body, hands);
        this.curl(this.rightForearm, 0.8F - 1.2F * spread);
        this.curl(this.leftForearm, 0.8F - 1.2F * spread);
        this.head.xRot = (direction == Burrow.UP ? -0.7F : direction == Burrow.DOWN ? 0.9F : -0.05F) - body;
        this.head.zRot = 0.3F + jerk;
        this.head.yRot = 0.0F;
        this.jaw.xRot = 0.05F;
    }

    /** Where the block it stopped in front of begins, in model pixels ahead of its hips (Burrow.REACH). */
    private static final float FACE = Burrow.REACH * 16.0F;

    /**
     * World angles for upper arm and forearm so the wrist lands at {@code forward} pixels ahead of the hips and
     * {@code up} pixels above the floor, elbow bending upward. The shoulder's place comes from the hips'
     * height and the body's tilt.
     */
    private float[] reachFrom(float hipsY, float body, float forward, float up) {
        return this.reachFrom(hipsY, body, forward, up, true);
    }

    /** As above, with the elbow bending up (over the arm) or down (under it, to reach up without hitting the roof). */
    private float[] reachFrom(float hipsY, float body, float forward, float up, boolean elbowUp) {
        float shoulderForward = -this.hips.z + 15.0F * Mth.sin(body);
        float shoulderUp = (24.0F - hipsY) + 15.0F * Mth.cos(body);
        float f = forward - shoulderForward;
        float u = up - shoulderUp;
        float upper = 16.0F;
        float lower = 16.5F;
        float distance = Mth.clamp(Mth.sqrt(f * f + u * u), Math.abs(upper - lower) + 0.01F, upper + lower - 0.01F);
        float base = (float) Mth.atan2(u, f);
        float bend = (float) Math.acos(Mth.clamp((upper * upper + distance * distance - lower * lower) / (2.0F * upper * distance), -1.0F, 1.0F));
        float elevation = elbowUp ? base + bend : base - bend;
        float elbowForward = upper * Mth.cos(elevation);
        float elbowHeight = upper * Mth.sin(elevation);
        float lowerElevation = (float) Mth.atan2(u - elbowHeight, f - elbowForward);
        return new float[]{-HALF_PI - elevation, -HALF_PI - lowerElevation};
    }

    private void setArm(boolean right, float body, float[] angles) {
        ModelPart arm = right ? this.rightArm : this.leftArm;
        ModelPart forearm = right ? this.rightForearm : this.leftForearm;
        arm.xRot = angles[0] - body;
        forearm.xRot = angles[1] - angles[0];
        arm.zRot = right ? 0.06F : -0.06F;
    }

    /** Fingers hooked down and in (positive) or splayed straight out (negative). */
    private void curl(ModelPart forearm, float amount) {
        for (int i = 0; i < 3; i++) {
            forearm.getChild("finger" + i).xRot = -0.1F + amount;
        }
    }

    /** A low ceiling: upright it hunches over, knees bent, back bowed, head pulled down and claws dragging on the floor. */
    private void fitToRoom(TheOneEntity entity, float partialTick, float crawl) {
        float stoop = entity.getStoop(partialTick) * (1.0F - crawl);
        if (stoop > 0.0F) {
            this.body.xRot += stoop * 0.9F;
            this.head.xRot -= stoop * 0.75F;
            this.hips.y += stoop * 4.9F;
            this.rightThigh.xRot -= stoop * 0.65F;
            this.leftThigh.xRot -= stoop * 0.65F;
            this.rightShin.xRot += stoop * 1.25F;
            this.leftShin.xRot += stoop * 1.25F;
            this.rightArm.xRot -= stoop * 0.55F;
            this.leftArm.xRot -= stoop * 0.55F;
            this.rightForearm.xRot -= stoop * 0.7F;
            this.leftForearm.xRot -= stoop * 0.7F;
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

    /**
     * Drawn upside down by the renderer: legs up in the rock, arms dangling towards the floor, head turning
     * to find you. Rotations are mirrored by the flip, so the head's yaw and pitch are negated.
     */
    private void poseHanging(float ageInTicks, float yaw, float pitch) {
        this.hips.y = -2.0F;
        this.body.xRot = 0.0F;
        this.body.zRot = Mth.sin(ageInTicks * 0.05F) * 0.06F;
        this.head.xRot = -pitch;
        this.head.yRot = -yaw;
        this.head.zRot = 0.4F + Mth.sin(ageInTicks * 0.07F) * 0.15F;
        this.jaw.xRot = 0.5F;
        this.rightArm.xRot = PI + Mth.sin(ageInTicks * 0.09F) * 0.12F;
        this.leftArm.xRot = PI + Mth.sin(ageInTicks * 0.09F + 1.7F) * 0.12F;
        this.rightArm.zRot = -0.12F;
        this.leftArm.zRot = 0.12F;
        this.rightForearm.xRot = this.leftForearm.xRot = 0.15F;
        this.rightThigh.xRot = this.leftThigh.xRot = 0.0F;
        this.rightShin.xRot = this.leftShin.xRot = 0.0F;
    }

    /**
     * Bent double outside a window so that its face is against the glass: knees folded and apart, back bowed
     * forward, head level and cocked, both palms flat on the glass either side of its face with the elbows sticking
     * up and out like a spider's legs. Scraping, the claws drag slowly down the glass. Snapping back, it straightens.
     */
    private void poseWindow(TheOneEntity entity, float time, float ageInTicks, float pitch, float headTilt) {
        float press = entity.getAnim() == 2 ? 1.0F - smoothstep(time / WindowScene.SNAP_BACK) : 1.0F;
        float drag = entity.getAnim() == 1 ? smoothstep(time / WindowScene.SCRAPE) : 0.0F;
        float hipsY = Mth.lerp(press, -2.0F, 9.0F);
        float body = Mth.lerp(press, 0.28F, 1.1F) + Mth.sin(ageInTicks * 0.05F) * 0.02F;
        this.hips.y = hipsY;
        this.body.xRot = body;
        this.rightThigh.xRot = this.leftThigh.xRot = Mth.lerp(press, -0.05F, -1.15F);
        this.rightShin.xRot = this.leftShin.xRot = Mth.lerp(press, 0.1F, 2.1F);
        this.rightThigh.zRot = 0.2F * press;
        this.leftThigh.zRot = -0.2F * press;
        this.head.xRot = -body + pitch * 0.3F;
        this.head.yRot = 0.0F;
        this.head.zRot = Mth.lerp(press, headTilt, 0.55F + Mth.sin(ageInTicks * 0.04F) * 0.1F);
        this.jaw.xRot = 0.25F + 0.15F * Mth.sin(ageInTicks * 0.11F);
        // The wrists stop short of the glass so the claws, hanging on from them, end at it and not through it.
        float glass = (float) (WindowScene.REACH * 16.0D) - 4.5F;
        for (int side = 0; side < 2; side++) {
            // Palms either side of its face; scraping, the claws drag down the glass.
            float up = (side == 0 ? 24.0F : 21.0F) - 8.0F * drag;
            float[] arm = this.reachFrom(hipsY, body, glass, up);
            ModelPart upper = side == 0 ? this.rightArm : this.leftArm;
            ModelPart lower = side == 0 ? this.rightForearm : this.leftForearm;
            upper.xRot = Mth.lerp(press, upper.xRot, arm[0] - body);
            lower.xRot = Mth.lerp(press, lower.xRot, arm[1] - arm[0]);
            upper.zRot = Mth.lerp(press, upper.zRot, side == 0 ? 0.5F : -0.5F);
            this.curl(lower, drag > 0.0F ? 1.0F : -0.35F);
        }
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
