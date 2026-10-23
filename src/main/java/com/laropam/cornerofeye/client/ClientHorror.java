package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.ClientConfig;
import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Everything that only happens on your side of the screen: footsteps behind you, knocking, mining in the walls,
 * heartbeat, darkening edges, fog, the lantern flash and jumpscares.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID, value = Dist.CLIENT)
public final class ClientHorror {
    private static final ResourceLocation JUMPSCARE = CornerOfTheEye.id("textures/gui/jumpscare.png");
    private static final ResourceLocation VIGNETTE = CornerOfTheEye.id("textures/gui/dread_vignette.png");
    private static final RandomSource RANDOM = RandomSource.create();

    private static int stage;

    private static int stepsLeft;
    private static int stepTimer;
    private static double stepDistance;
    private static float stepYaw;

    private static BlockPos miningPos = BlockPos.ZERO;
    private static int miningBlocksLeft;
    private static int miningTicks;

    private static BlockPos knockPos = BlockPos.ZERO;
    private static int knockTicks = -1;

    private static int jumpscareTicks;
    private static int jumpscareLength = 1;
    private static float flash;
    private static float flashO;

    private static float dread;
    private static float dreadO;
    private static float fog;
    private static float fogO;
    private static int heartbeatTimer;

    // ------------------------------------------------------------- packets

    public static void handle(HorrorPacket msg) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        switch (msg.type()) {
            case SYNC_STAGE -> stage = msg.data();
            case FOOTSTEPS -> {
                if (player != null) {
                    stepsLeft = msg.data();
                    stepTimer = 0;
                    stepDistance = 7.0D;
                    stepYaw = player.getYRot() + 180.0F + (RANDOM.nextFloat() - 0.5F) * 50.0F;
                }
            }
            case MINING -> {
                miningPos = msg.pos();
                miningBlocksLeft = Math.max(1, msg.data());
                miningTicks = 0;
            }
            case KNOCK -> {
                knockPos = msg.pos();
                knockTicks = 0;
            }
            case JUMPSCARE -> {
                jumpscareLength = Math.max(1, msg.data());
                jumpscareTicks = ClientConfig.JUMPSCARES.get() ? jumpscareLength : 0;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.SCREAM.get(), 1.0F, 1.0F));
            }
            case FLASH -> flash = Math.max(flash, msg.data() / 100.0F);
        }
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }
        flashO = flash;
        flash = Math.max(0.0F, flash - 0.06F);
        if (jumpscareTicks > 0) {
            jumpscareTicks--;
        }
        tickFootsteps(level, player);
        tickMining(level);
        tickKnock(level);
        tickDread(minecraft, level, player);
    }

    /** Steps from behind that get closer, and stop the moment you turn around. */
    private static void tickFootsteps(ClientLevel level, LocalPlayer player) {
        if (stepsLeft <= 0 || --stepTimer > 0) {
            return;
        }
        double rad = Math.toRadians(stepYaw);
        Vec3 dir = new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
        Vec3 look = player.getViewVector(1.0F);
        if (look.x * dir.x + look.z * dir.z > 0.35D) {
            stepsLeft = 0;
            return;
        }
        Vec3 at = player.position().add(dir.scale(stepDistance));
        BlockPos ground = BlockPos.containing(at);
        for (int i = 0; i < 4 && level.getBlockState(ground.below()).isAir(); i++) {
            ground = ground.below();
        }
        BlockState state = level.getBlockState(ground.below());
        if (!state.isAir()) {
            SoundType sound = state.getSoundType();
            level.playLocalSound(at.x, ground.getY(), at.z, sound.getStepSound(), SoundSource.HOSTILE,
                    sound.getVolume() * 0.3F, sound.getPitch() * 0.9F, false);
        }
        stepDistance = Math.max(2.0D, stepDistance - 0.6D);
        stepsLeft--;
        stepTimer = 7 + RANDOM.nextInt(3);
    }

    /** Pickaxe on stone, somewhere inside the walls. */
    private static void tickMining(ClientLevel level) {
        if (miningBlocksLeft <= 0) {
            return;
        }
        BlockState state = level.getBlockState(miningPos);
        SoundType sound = state.getSoundType();
        Vec3 at = Vec3.atCenterOf(miningPos);
        miningTicks++;
        if (miningTicks < 30 && miningTicks % 5 == 0) {
            level.playLocalSound(at.x, at.y, at.z, sound.getHitSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 6.0F, sound.getPitch() * 0.5F, false);
        } else if (miningTicks == 30) {
            level.playLocalSound(at.x, at.y, at.z, sound.getBreakSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 3.0F, sound.getPitch() * 0.8F, false);
        } else if (miningTicks > 50) {
            miningBlocksLeft--;
            miningTicks = 0;
            miningPos = miningPos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM));
        }
    }

    /** Three knocks. Sometimes three more. */
    private static void tickKnock(ClientLevel level) {
        if (knockTicks < 0) {
            return;
        }
        int t = knockTicks++;
        if (t == 0 || t == 7 || t == 14 || t == 60 || t == 67 || t == 74) {
            if (t >= 60 && RANDOM.nextInt(3) != 0) {
                knockTicks = -1;
                return;
            }
            Vec3 at = Vec3.atCenterOf(knockPos);
            level.playLocalSound(at.x, at.y + 0.5D, at.z, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS,
                    0.25F, 1.6F + RANDOM.nextFloat() * 0.15F, false);
        }
        if (t > 80) {
            knockTicks = -1;
        }
    }

    /** How close it is decides the heartbeat, the dark at the edges and the fog. */
    private static void tickDread(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
        float target = 0.0F;
        for (TheOneEntity one : level.getEntitiesOfClass(TheOneEntity.class, player.getBoundingBox().inflate(32.0D))) {
            Phase phase = one.getPhase();
            float range = phase == Phase.WATCH ? 20.0F : 32.0F;
            float closeness = 1.0F - Mth.clamp(one.distanceTo(player) / range, 0.0F, 1.0F);
            target = Math.max(target, phase == Phase.WATCH ? closeness * 0.5F : closeness);
        }
        dreadO = dread;
        dread += (target - dread) * 0.05F;

        if (dread > 0.08F && --heartbeatTimer <= 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1.0F, 0.25F + dread * 0.75F));
            heartbeatTimer = (int) Mth.lerp(dread, 32.0F, 11.0F);
        }

        long time = level.getDayTime() % 24000L;
        boolean night = time > 13000L && time < 23000L;
        float fogTarget = 0.0F;
        if (stage != HorrorPacket.DEFEATED && level.dimension() == Level.OVERWORLD) {
            if (night && stage >= HauntData.HUNTING) {
                fogTarget = 0.85F;
            } else if (night && stage >= HauntData.APPROACHING) {
                fogTarget = 0.6F;
            }
            fogTarget = Math.max(fogTarget, dread);
        }
        fogO = fog;
        fog += (fogTarget - fog) * 0.02F;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        stage = 0;
        stepsLeft = 0;
        miningBlocksLeft = 0;
        knockTicks = -1;
        jumpscareTicks = 0;
        flash = flashO = 0.0F;
        dread = dreadO = 0.0F;
        fog = fogO = 0.0F;
    }

    // ---------------------------------------------------------- viewport

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov()) {
            Peripheral.setFov(event.getFOV());
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float amount = Mth.lerp((float) event.getPartialTick(), fogO, fog);
        if (!ClientConfig.FOG.get() || amount < 0.01F || event.getType() != FogType.NONE) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(amount, event.getFarPlaneDistance(), 14.0F));
        event.setNearPlaneDistance(Mth.lerp(amount, event.getNearPlaneDistance(), 0.0F));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float amount = Mth.lerp((float) event.getPartialTick(), fogO, fog);
        if (!ClientConfig.FOG.get() || amount < 0.01F) {
            return;
        }
        float dim = 1.0F - 0.8F * amount;
        event.setRed(event.getRed() * dim);
        event.setGreen(event.getGreen() * dim);
        event.setBlue(event.getBlue() * dim);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (jumpscareTicks <= 0 || !ClientConfig.SCREEN_EFFECTS.get()) {
            return;
        }
        float shake = 4.0F * jumpscareTicks / jumpscareLength;
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * shake);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * shake);
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * shake * 2.0F);
    }

    // ----------------------------------------------------------- overlays

    static void renderDread(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        float amount = Mth.lerp(partialTick, dreadO, dread);
        if (amount < 0.02F || !ClientConfig.SCREEN_EFFECTS.get()) {
            return;
        }
        // A slow pulse in time with nothing in particular.
        float pulse = 0.85F + 0.15F * Mth.sin((Minecraft.getInstance().level.getGameTime() + partialTick) * 0.25F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, Mth.clamp(amount * 1.1F * pulse, 0.0F, 0.95F));
        graphics.blit(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    static void renderScare(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        float flashAmount = Mth.lerp(partialTick, flashO, flash);
        if (flashAmount > 0.01F && ClientConfig.SCREEN_EFFECTS.get()) {
            int alpha = (int) (Mth.clamp(flashAmount, 0.0F, 1.0F) * 235.0F);
            graphics.fill(0, 0, width, height, (alpha << 24) | 0xF2F0FF);
        }

        if (jumpscareTicks <= 0) {
            return;
        }
        float progress = 1.0F - (jumpscareTicks - partialTick) / jumpscareLength;
        graphics.fill(0, 0, width, height, 0xFF000000);
        // Starts with the whole face filling the screen, then keeps coming closer.
        int size = (int) (Math.min(width, height) * (1.0F + progress * 0.35F));
        int shake = (int) ((1.0F - progress) * 14.0F) + 1;
        int x = (width - size) / 2 + RANDOM.nextInt(shake * 2 + 1) - shake;
        int y = (height - size) / 2 + RANDOM.nextInt(shake * 2 + 1) - shake;
        graphics.blit(JUMPSCARE, x, y, size, size, 0.0F, 0.0F, 64, 64, 64, 64);
        if (progress < 0.2F) {
            graphics.fill(0, 0, width, height, 0x66FF0000);
        }
    }

    private ClientHorror() {
    }
}
