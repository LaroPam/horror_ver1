package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.ClientConfig;
import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.entity.Challenge;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.haunt.HauntData;
import com.laropam.cornerofeye.haunt.Nights;
import com.laropam.cornerofeye.haunt.LightSuppression;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * Everything that only happens on your side of the screen: footsteps behind you, knocking, mining in the walls,
 * heartbeat, darkening edges, fog, the lantern flash and jumpscares.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID, value = Dist.CLIENT)
public final class ClientHorror {
    private static final ResourceLocation JUMPSCARE = CornerOfTheEye.id("textures/gui/jumpscare.png");
    private static final ResourceLocation JUMPSCARE_LATE = CornerOfTheEye.id("textures/gui/jumpscare_late.png");
    private static final ResourceLocation VIGNETTE = CornerOfTheEye.id("textures/gui/dread_vignette.png");
    private static final RandomSource RANDOM = RandomSource.create();

    private static int stage;

    private static int stepsLeft;
    private static int stepTimer;
    private static double stepDistance;
    private static float stepYaw;
    /** Echo steps: while this runs, every step you take is answered by one behind you, a moment late. */
    private static int echoTicks;
    private static int echoLastStep;
    private static int echoDue = -1;
    private static int echoIdle;
    private static boolean echoWalked;
    private static int echoFinalDue = -1;
    /** Where the last of them came from: look that way and they stop. */
    private static Vec3 echoFrom = Vec3.ZERO;

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
    private static float dreadSpike;
    private static float beat;
    private static float beatO;
    private static float fog;
    private static float fogO;
    private static int heartbeatTimer;
    private static int breathTimer;
    private static int tinnitusCooldown;
    private static int lastGlimpsedId = -1;
    private static long lastGlimpseTime;
    private static DreadLoopSound dreadLoop;

    private static final int CARD_LENGTH = 150;
    private static int cardNight;
    /** Nights.CARD_*: an ordinary night, a blood moon, the last night, or the end of it all. */
    private static int cardKind;
    private static int cardTicks;
    private static boolean bloodMoon;
    private static float blood;
    private static float bloodO;

    /** Sleep paralysis: ticks left of not being able to move or look away, and where you are stuck looking. */
    private static int paralysisTicks;
    private static float frozenYaw;
    private static float frozenPitch;
    /** The last few ticks of it: your eyes close. */
    private static final int BLINK = 8;

    private static int challengeType = -1;
    private static int challengeTicks;
    private static int challengeLength = 1;
    private static int challengeLightNeeded;

    /** The haunt stage of the world we are in, {@link HorrorPacket#DEFEATED}, or 0 outside a world. */
    static int stage() {
        return stage;
    }

    // ------------------------------------------------------------- packets

    public static void handle(HorrorPacket msg) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        switch (msg.type()) {
            case SYNC_STAGE -> {
                stage = msg.data();
                MenuHaunt.remember(stage);
            }
            case FOOTSTEPS -> {
                if (player != null) {
                    stepsLeft = msg.data();
                    stepTimer = 0;
                    stepDistance = 7.0D;
                    stepYaw = player.getYRot() + 180.0F + (RANDOM.nextFloat() - 0.5F) * 50.0F;
                    echoTicks = 300 + msg.data() * 30;
                    echoLastStep = (int) player.moveDist;
                    echoDue = -1;
                    echoIdle = 0;
                    echoWalked = false;
                    echoFinalDue = -1;
                    double rad = Math.toRadians(stepYaw);
                    echoFrom = new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
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
                dreadSpike = 1.0F;
            }
            case FLASH -> flash = Math.max(flash, msg.data() / 100.0F);
            case DREAD -> dreadSpike = Math.min(1.0F, dreadSpike + msg.data() / 100.0F);
            case CHALLENGE -> {
                challengeType = msg.data();
                challengeLength = Math.max(1, msg.pos().getX());
                challengeLightNeeded = msg.pos().getY();
                challengeTicks = challengeLength;
                dreadSpike = Math.min(1.0F, dreadSpike + 0.5F);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.GLIMPSE.get(), 0.8F, 0.8F));
            }
            case CHALLENGE_END -> challengeType = -1;
            case NIGHT_CARD -> {
                cardNight = msg.data();
                cardKind = msg.pos().getX();
                cardTicks = CARD_LENGTH;
            }
            case BLOOD_MOON -> bloodMoon = msg.data() == 1;
            case PARALYSIS -> {
                paralysisTicks = msg.data();
                if (player != null) {
                    frozenYaw = player.getYRot();
                    frozenPitch = player.getXRot();
                }
                dreadSpike = 1.0F;
            }
            case SMOTHER -> {
                if (LightSuppression.clientUpdate(msg.pos(), msg.data()) && minecraft.level != null) {
                    BlockPos pos = msg.pos();
                    minecraft.level.getChunkSource().getLightEngine().checkBlock(pos);
                    // Redraw it (and its neighbours' faces) without its own glow.
                    minecraft.levelRenderer.setBlocksDirty(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                }
            }
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
        if (cardTicks > 0) {
            cardTicks--;
        }
        bloodO = blood;
        long dayTime = level.getDayTime() % 24000L;
        boolean nightSky = dayTime >= 12500L && dayTime < 23200L;
        float bloodTarget = bloodMoon && nightSky && level.dimension() == Level.OVERWORLD ? 1.0F : 0.0F;
        blood += Mth.clamp(bloodTarget - blood, -0.01F, 0.01F);
        if (jumpscareTicks > 0) {
            jumpscareTicks--;
        }
        tickChallenge(minecraft);
        if (paralysisTicks > 0) {
            // You can't move. You can't even look away.
            paralysisTicks--;
            player.setYRot(frozenYaw);
            player.setXRot(frozenPitch);
            player.yRotO = frozenYaw;
            player.xRotO = frozenPitch;
            player.setYHeadRot(frozenYaw);
            dreadSpike = Math.max(dreadSpike, 0.9F);
        }
        tickFootsteps(level, player);
        tickMining(level);
        tickKnock(level);
        tickDread(minecraft, level, player);
    }

    /** The clock ticks every second, then twice a second near the end. Your heart keeps up. */
    private static void tickChallenge(Minecraft minecraft) {
        if (challengeType < 0) {
            return;
        }
        challengeTicks--;
        int interval = challengeTicks < 60 ? 10 : 20;
        if (challengeTicks >= 0 && challengeTicks % interval == 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.TIMER_TICK.get(), challengeTicks < 60 ? 1.25F : 1.0F, 0.9F));
        }
        dreadSpike = Math.max(dreadSpike, 0.45F);
        if (challengeTicks < -60) {
            // The server never answered; don't leave a dead timer on screen.
            challengeType = -1;
        }
    }

    /**
     * Steps behind you. Standing still, you hear them coming closer from behind. Walking, every step you take is
     * answered by one behind you, a moment late, in your rhythm; when you stop, it takes one more step, closer,
     * and then nothing. Turn round and they stop at once.
     */
    private static void tickFootsteps(ClientLevel level, LocalPlayer player) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 back = new Vec3(-look.x, 0.0D, -look.z);
        if (echoTicks > 0) {
            echoTicks--;
            if (look.x * echoFrom.x + look.z * echoFrom.z > 0.35D) {
                echoTicks = 0;
                stepsLeft = 0;
                return;
            }
            int step = (int) player.moveDist;
            if (step != echoLastStep) {
                echoLastStep = step;
                echoIdle = 0;
                echoWalked = true;
                stepsLeft = 0;
                if (echoDue < 0) {
                    echoDue = 4 + RANDOM.nextInt(3);
                }
            } else if (++echoIdle == 8 && echoWalked) {
                // You stopped.
                echoFinalDue = 9 + RANDOM.nextInt(6);
            }
            if (echoDue >= 0 && --echoDue < 0) {
                stepBehind(level, player, back, 4.5D, 0.3F);
            }
            if (echoFinalDue >= 0 && --echoFinalDue < 0) {
                // ...it didn't.
                stepBehind(level, player, back, 3.0D, 0.42F);
                dreadSpike = Math.min(1.0F, dreadSpike + 0.3F);
                echoTicks = 0;
            }
            if (echoWalked) {
                return;
            }
        }
        if (stepsLeft <= 0 || --stepTimer > 0) {
            return;
        }
        double rad = Math.toRadians(stepYaw);
        Vec3 dir = new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
        if (look.x * dir.x + look.z * dir.z > 0.35D) {
            stepsLeft = 0;
            return;
        }
        stepBehind(level, player, dir, stepDistance, 0.3F);
        stepDistance = Math.max(2.0D, stepDistance - 0.6D);
        stepsLeft--;
        stepTimer = 7 + RANDOM.nextInt(3);
    }

    /** One step on whatever the ground is {@code distance} blocks off in {@code direction}. */
    private static void stepBehind(ClientLevel level, LocalPlayer player, Vec3 direction, double distance, float volume) {
        Vec3 at = player.position().add(direction.normalize().scale(distance));
        BlockPos ground = BlockPos.containing(at);
        for (int i = 0; i < 4 && level.getBlockState(ground.below()).isAir(); i++) {
            ground = ground.below();
        }
        BlockState state = level.getBlockState(ground.below());
        if (!state.isAir()) {
            SoundType sound = state.getSoundType();
            level.playLocalSound(at.x, ground.getY(), at.z, sound.getStepSound(), SoundSource.HOSTILE,
                    sound.getVolume() * volume, sound.getPitch() * 0.85F, false);
        }
        echoFrom = direction.normalize();
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

    /**
     * Fear, shown only through the senses: no meter on screen. How close it is, how dark it is around you,
     * and sudden shocks (a light dying next to you, a glimpse of it) decide the heartbeat, your own breathing,
     * the hum under everything, how muffled the world sounds, how narrow and dark your vision gets, and the fog.
     */
    private static void tickDread(Minecraft minecraft, ClientLevel level, LocalPlayer player) {
        float proximity = 0.0F;
        for (TheOneEntity one : level.getEntitiesOfClass(TheOneEntity.class, player.getBoundingBox().inflate(32.0D))) {
            Phase phase = one.getPhase();
            float range = phase == Phase.WATCH ? 20.0F : 32.0F;
            float closeness = 1.0F - Mth.clamp(one.distanceTo(player) / range, 0.0F, 1.0F);
            proximity = Math.max(proximity, phase == Phase.WATCH ? closeness * 0.5F : closeness);
        }

        long time = level.getDayTime() % 24000L;
        boolean night = time > 13000L && time < 23000L;
        boolean haunted = stage != HorrorPacket.DEFEATED && level.dimension() == Level.OVERWORLD;
        // Once it has started coming closer, just standing in the dark is enough to make your heart race.
        float darkness = 0.0F;
        if (haunted && stage >= HauntData.APPROACHING && level.getMaxLocalRawBrightness(player.blockPosition()) < 7) {
            darkness = stage >= HauntData.HUNTING ? 0.22F : 0.12F;
        }

        dreadSpike = Math.max(0.0F, dreadSpike - 0.012F);
        float target = haunted || proximity > 0.0F ? Mth.clamp(Math.max(proximity, darkness) + dreadSpike, 0.0F, 1.0F) : 0.0F;
        dreadO = dread;
        // Fear comes fast and leaves slowly.
        dread += (target - dread) * (target > dread ? 0.12F : 0.03F);

        beatO = beat;
        beat = Math.max(0.0F, beat - 0.12F);
        if (dread > 0.08F && --heartbeatTimer <= 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1.0F, 0.25F + dread * 0.75F));
            heartbeatTimer = (int) Mth.lerp(dread, 32.0F, 10.0F);
            beat = 1.0F;
        }
        if (dread > 0.5F && --breathTimer <= 0) {
            // You can hear yourself breathing.
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PLAYER_BREATH.get(), 0.95F + RANDOM.nextFloat() * 0.1F,
                    (dread - 0.4F) * 0.8F));
            breathTimer = (int) Mth.lerp(dread, 90.0F, 50.0F) + RANDOM.nextInt(20);
        }
        if (tinnitusCooldown > 0) {
            tinnitusCooldown--;
        } else if (dread > 0.85F) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.TINNITUS.get(), 1.0F, 0.35F));
            tinnitusCooldown = 400;
        }
        if (dread > 0.05F && (dreadLoop == null || !minecraft.getSoundManager().isActive(dreadLoop))) {
            dreadLoop = new DreadLoopSound();
            minecraft.getSoundManager().play(dreadLoop);
        }

        float fogTarget = 0.0F;
        if (haunted) {
            if (night && stage >= HauntData.HUNTING) {
                fogTarget = 0.85F;
            } else if (night && stage >= HauntData.APPROACHING) {
                fogTarget = 0.6F;
            }
            fogTarget = Math.max(fogTarget, proximity);
        }
        fogO = fog;
        fog += (fogTarget - fog) * 0.02F;
    }

    /** Current fear, 0-1. */
    static float dreadLevel() {
        return dread;
    }

    /** It just showed up at the edge of your screen. You might not have noticed. Your ears did. */
    static void onGlimpse(TheOneEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (entity.getId() != lastGlimpsedId || now - lastGlimpseTime > 300L) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.GLIMPSE.get(), 0.9F + RANDOM.nextFloat() * 0.2F, 0.6F));
            dreadSpike = Math.min(1.0F, dreadSpike + 0.35F);
        }
        lastGlimpsedId = entity.getId();
        lastGlimpseTime = now;
    }

    /** The world goes quiet and slow when it is near: everything except it, and your heart. */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || dread < 0.1F || !ClientConfig.MUFFLE_WORLD.get() || sound instanceof TickableSoundInstance
                || CornerOfTheEye.MODID.equals(sound.getLocation().getNamespace())) {
            return;
        }
        SoundSource source = sound.getSource();
        if (source == SoundSource.AMBIENT || source == SoundSource.BLOCKS || source == SoundSource.NEUTRAL
                || source == SoundSource.PLAYERS || source == SoundSource.WEATHER) {
            event.setSound(new MuffledSound(sound, 1.0F - 0.75F * dread, 1.0F - 0.15F * dread));
        }
    }

    @SubscribeEvent
    public static void onRespawnOrTravel(ClientPlayerNetworkEvent.Clone event) {
        // A new world on our side; the server sends the dark spots of the new one.
        LightSuppression.clientUpdate(BlockPos.ZERO, 2);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        stage = 0;
        stepsLeft = 0;
        echoTicks = 0;
        miningBlocksLeft = 0;
        cardTicks = 0;
        bloodMoon = false;
        blood = bloodO = 0.0F;
        knockTicks = -1;
        jumpscareTicks = 0;
        paralysisTicks = 0;
        challengeType = -1;
        flash = flashO = 0.0F;
        dread = dreadO = dreadSpike = 0.0F;
        beat = beatO = 0.0F;
        fog = fogO = 0.0F;
        lastGlimpsedId = -1;
        dreadLoop = null;
        LightSuppression.clientUpdate(BlockPos.ZERO, 2);
    }

    // ---------------------------------------------------------- viewport

    /** Fear narrows your vision, which leaves less room at the edges to catch it in. */
    @SubscribeEvent
    public static void onNarrowFov(ViewportEvent.ComputeFov event) {
        float amount = Mth.lerp((float) event.getPartialTick(), dreadO, dread);
        if (event.usedConfiguredFov() && amount > 0.01F && ClientConfig.SCREEN_EFFECTS.get()) {
            event.setFOV(event.getFOV() * (1.0D - 0.08D * amount));
        }
    }

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

    /** 0-1: how red the night is. */
    static float bloodMoonAmount(float partialTick) {
        return Mth.lerp(partialTick, bloodO, blood);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float red = bloodMoonAmount((float) event.getPartialTick());
        if (red > 0.01F) {
            event.setRed(Mth.lerp(red, event.getRed(), Math.max(event.getRed(), 0.22F)));
            event.setGreen(event.getGreen() * (1.0F - 0.75F * red));
            event.setBlue(event.getBlue() * (1.0F - 0.75F * red));
        }
        float amount = Mth.lerp((float) event.getPartialTick(), fogO, fog);
        if (!ClientConfig.FOG.get() || amount < 0.01F) {
            return;
        }
        float dim = 1.0F - 0.8F * amount;
        event.setRed(event.getRed() * dim);
        event.setGreen(event.getGreen() * dim);
        event.setBlue(event.getBlue() * dim);
    }

    /** Paralysed: no walking, no jumping, no sneaking away. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (paralysisTicks > 0) {
            event.getInput().forwardImpulse = 0.0F;
            event.getInput().leftImpulse = 0.0F;
            event.getInput().up = event.getInput().down = event.getInput().left = event.getInput().right = false;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (paralysisTicks > 0) {
            // Stuck looking at it, head tipped on the pillow, the tiniest tremor of trying to move.
            float t = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() + (float) event.getPartialTick() : 0.0F;
            event.setYaw(frozenYaw + Mth.sin(t * 1.7F) * 0.15F);
            event.setPitch(frozenPitch + Mth.sin(t * 2.3F) * 0.1F);
            event.setRoll(event.getRoll() + 9.0F);
        }
        if (!ClientConfig.SCREEN_EFFECTS.get()) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float amount = Mth.lerp(partial, dreadO, dread);
        if (amount > 0.6F && Minecraft.getInstance().level != null) {
            // Hands shaking: a slight unsteady roll that grows with fear.
            float t = Minecraft.getInstance().level.getGameTime() + partial;
            float tremble = (amount - 0.6F) * 2.5F;
            event.setRoll(event.getRoll() + (Mth.sin(t * 0.9F) + Mth.sin(t * 2.3F) * 0.5F) * tremble);
        }
        if (jumpscareTicks <= 0) {
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
        // The dark at the edges throbs with every heartbeat.
        float pulse = 0.8F + 0.35F * Mth.lerp(partialTick, beatO, beat);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, Mth.clamp(amount * 1.1F * pulse, 0.0F, 0.95F));
        graphics.blit(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    static void renderScare(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (cardTicks > 0) {
            renderNightCard(graphics, partialTick, width, height);
        }
        if (challengeType >= 0) {
            renderChallenge(graphics, partialTick, width, height);
        }
        if (paralysisTicks > 0) {
            // Eyes barely open: dark all round, then a blink, and when they open again it is gone.
            int edge = (int) (height * 0.16F);
            graphics.fillGradient(0, 0, width, edge, 0xF0000000, 0x00000000);
            graphics.fillGradient(0, height - edge, width, height, 0x00000000, 0xF0000000);
            if (paralysisTicks <= BLINK) {
                graphics.fill(0, 0, width, height, 0xFF000000);
            }
        }
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
        graphics.blit(stage >= HauntData.HUNTING && stage != HorrorPacket.DEFEATED ? JUMPSCARE_LATE : JUMPSCARE,
                x, y, size, size, 0.0F, 0.0F, 64, 64, 64, 64);
        if (progress < 0.2F) {
            graphics.fill(0, 0, width, height, 0x66FF0000);
        }
    }

    /** "NIGHT 3", fading up out of the dark, trembling a little, then gone. */
    private static void renderNightCard(GuiGraphics graphics, float partialTick, int width, int height) {
        float age = CARD_LENGTH - cardTicks + partialTick;
        float alpha = Mth.clamp(Math.min(age / 30.0F, (CARD_LENGTH - age) / 40.0F), 0.0F, 1.0F);
        if (alpha < 0.02F) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        boolean victory = cardKind == Nights.CARD_VICTORY;
        boolean red = !victory && (cardKind != Nights.CARD_NIGHT || cardNight >= 7);
        int a = (int) (alpha * 255.0F) << 24;
        PoseStack pose = graphics.pose();
        float y = height * 0.38F;
        graphics.fillGradient(0, (int) y - 40, width, (int) y + 46, (int) (alpha * 120) << 24, (int) (alpha * 120) << 24);

        pose.pushPose();
        float jitter = RANDOM.nextFloat() < 0.08F ? (RANDOM.nextFloat() - 0.5F) * 4.0F : 0.0F;
        pose.translate(width / 2.0F + jitter, y, 0.0F);
        pose.scale(victory ? 4.0F : 5.0F, victory ? 4.0F : 5.0F, 1.0F);
        Component title = victory ? Component.translatable("cornerofeye.night.victory") : Component.translatable("cornerofeye.night.title", cardNight);
        graphics.drawCenteredString(font, title, 0, -4, a | (victory ? 0xEADCA6 : red ? 0xC81E1E : 0xD8D4CC));
        pose.popPose();

        Component sub = switch (cardKind) {
            case Nights.CARD_VICTORY -> Component.translatable("cornerofeye.night.victory.sub");
            case Nights.CARD_FINALE -> Component.translatable("cornerofeye.night.finale");
            case Nights.CARD_BLOOD -> Component.translatable("cornerofeye.night.blood");
            default -> Component.translatable(cardNight <= 7 ? "cornerofeye.night.sub." + cardNight : "cornerofeye.night.sub.late");
        };
        pose.pushPose();
        pose.translate(width / 2.0F, y + 26.0F, 0.0F);
        pose.scale(1.5F, 1.5F, 1.0F);
        graphics.drawCenteredString(font, sub.copy().withStyle(ChatFormatting.ITALIC), 0, 0, a | (victory ? 0xB8B0A0 : red ? 0xA02020 : 0x9A9A9A));
        pose.popPose();
    }

    /** The countdown: big seconds, what to do, and a bar running out. */
    private static void renderChallenge(GuiGraphics graphics, float partialTick, int width, int height) {
        Font font = Minecraft.getInstance().font;
        Challenge type = Challenge.byId(challengeType);
        float seconds = Math.max(0.0F, (challengeTicks - partialTick) / 20.0F);
        boolean urgent = seconds < 3.0F;
        boolean blink = urgent && ((int) (seconds * 4.0F)) % 2 == 0;
        float top = height * 0.16F;
        PoseStack pose = graphics.pose();
        // A dark band behind the text so it reads over anything, even over it.
        graphics.fill(width / 2 - 150, (int) top - 26, width / 2 + 150, (int) top + 70, 0x70000000);

        pose.pushPose();
        pose.translate(width / 2.0F, top, 0.0F);
        float scale = 4.0F + (urgent ? 0.6F * (seconds % 0.5F) : 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, String.format(Locale.ROOT, "%.1f", seconds), 0, -4, blink ? 0xFF2A2A : 0xEDEDED);
        pose.popPose();

        pose.pushPose();
        pose.translate(width / 2.0F, top + 24.0F, 0.0F);
        pose.scale(2.0F, 2.0F, 1.0F);
        graphics.drawCenteredString(font, Component.translatable(type.key()).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFD6D6);
        pose.popPose();

        graphics.drawCenteredString(font, Component.translatable(type.key() + ".hint"), width / 2, (int) top + 48, 0xA8A8A8);

        int barWidth = 160;
        int x0 = width / 2 - barWidth / 2;
        int y0 = (int) top + 62;
        float left = Mth.clamp((challengeTicks - partialTick) / challengeLength, 0.0F, 1.0F);
        graphics.fill(x0 - 1, y0 - 1, x0 + barWidth + 1, y0 + 4, 0x90000000);
        graphics.fill(x0, y0, x0 + (int) (barWidth * left), y0 + 3, urgent ? 0xFFD42020 : 0xFFE6E6E6);

        if ((type == Challenge.LIGHT_RUN || type == Challenge.LIGHT_PLACE) && challengeLightNeeded > 0) {
            // How much light is on you right now. A furnace across the room is not enough.
            Minecraft minecraft = Minecraft.getInstance();
            int light = 0;
            if (minecraft.level != null && minecraft.player != null) {
                light = Math.max(minecraft.level.getBrightness(LightLayer.BLOCK, minecraft.player.blockPosition()),
                        minecraft.level.getBrightness(LightLayer.BLOCK, BlockPos.containing(minecraft.player.getEyePosition())));
            }
            boolean enough = light >= challengeLightNeeded;
            graphics.drawCenteredString(font, Component.translatable("cornerofeye.challenge.light_meter", light, challengeLightNeeded),
                    width / 2, y0 + 8, enough ? 0xFFE9B0 : 0x9A9A9A);
        }
    }

    private ClientHorror() {
    }
}
