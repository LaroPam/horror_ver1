package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.HorrorConfig;
import com.laropam.cornerofeye.entity.Challenge;
import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.TheOneEntity;
import com.laropam.cornerofeye.item.LanternOfRevelationItem;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModBlocks;
import com.laropam.cornerofeye.registry.ModEntities;
import com.laropam.cornerofeye.registry.ModItems;
import com.laropam.cornerofeye.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Nightmares. Asleep from stage 2 on, you may wake up somewhere that is almost your room: a copy of everything
 * around your bed, in a place where it is always night, with every light out and things not quite right. Your
 * bed is not where you left it. Find it within a minute and a half, before it finds you, and you wake up in
 * the morning with a scrap in your hand. Run out of time, fall, or let it touch you, and you wake up
 * screaming in the middle of the night. Nobody dies in a dream, nothing is lost, nothing can be built or
 * broken there, and the Lantern of Revelation doesn't light.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID)
public final class Nightmares {
    public static final ResourceKey<Level> NIGHTMARE = ResourceKey.create(Registries.DIMENSION, CornerOfTheEye.id("nightmare"));
    private static final int RADIUS = 14;
    private static final int BELOW = 3;
    private static final int ABOVE = 8;
    private static final int LENGTH = 1800;
    private static final int STALK_AT = 300;
    private static final int HUNT_AT = 1100;
    private static final String TAG = "cornerofeye.dream";

    private static final Map<UUID, Dream> DREAMS = new HashMap<>();

    private static final class Dream {
        final BlockPos origin;
        final BlockPos goal;
        final int stage;
        int ticks;

        Dream(BlockPos origin, BlockPos goal, int stage) {
            this.origin = origin;
            this.goal = goal;
            this.stage = stage;
        }
    }

    public static boolean isDream(Level level) {
        return level.dimension() == NIGHTMARE;
    }

    /** Takes a sleeping player into a nightmare. False if it couldn't be set up here. */
    public static boolean begin(ServerPlayer player, int stage) {
        MinecraftServer server = player.server;
        ServerLevel overworld = player.serverLevel();
        ServerLevel dream = server.getLevel(NIGHTMARE);
        if (!HorrorConfig.NIGHTMARES.get() || dream == null || overworld.dimension() != Level.OVERWORLD || DREAMS.containsKey(player.getUUID())) {
            return false;
        }
        BlockPos bed = player.getSleepingPos().orElse(player.blockPosition());
        BlockPos corner = bed.offset(-RADIUS, -BELOW, -RADIUS);
        Vec3i size = new Vec3i(RADIUS * 2 + 1, BELOW + ABOVE + 1, RADIUS * 2 + 1);
        // Everyone dreams in their own corner of the place.
        int slot = Math.floorMod(player.getUUID().hashCode(), 64);
        BlockPos origin = new BlockPos((slot % 8) * 256, 96, (slot / 8) * 256);

        for (Entity entity : dream.getEntities((Entity) null, new AABB(origin).inflate(64.0D), entity -> !(entity instanceof Player))) {
            entity.discard();
        }
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(overworld, corner, size, false, null);
        template.placeInWorld(dream, origin, origin, new StructurePlaceSettings(), dream.random, Block.UPDATE_CLIENTS);
        distort(dream, origin, size, dream.random);

        BlockPos start = origin.offset(bed.subtract(corner));
        BlockPos goal = placeBed(dream, origin, size, start, dream.random);
        if (goal == null) {
            return false;
        }

        CompoundTag back = new CompoundTag();
        back.putInt("X", bed.getX());
        back.putInt("Y", bed.getY());
        back.putInt("Z", bed.getZ());
        back.putInt("Mode", player.gameMode.getGameModeForPlayer().getId());
        persisted(player).put(TAG, back);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted(player));

        player.stopSleepInBed(false, false);
        player.teleportTo(dream, start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, player.getYRot(), 0.0F);
        player.setGameMode(GameType.ADVENTURE);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
        DREAMS.put(player.getUUID(), new Dream(origin, goal, stage));
        HorrorNetwork.send(player, HorrorPacket.Type.CHALLENGE, new BlockPos(LENGTH, 0, 0), Challenge.FIND_BED.ordinal());
        player.sendSystemMessage(Component.translatable("cornerofeye.message.dream_start").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return true;
    }

    /** Every light out, the windows black, the chests empty, the beds gone, and things written on the walls. */
    private static void distort(ServerLevel level, BlockPos origin, Vec3i size, RandomSource random) {
        BlockPos far = origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        for (BlockPos pos : BlockPos.betweenClosed(origin, far)) {
            BlockState state = level.getBlockState(pos);
            BlockState changed = null;
            if (state.getBlock() instanceof BedBlock) {
                changed = Blocks.AIR.defaultBlockState();
            } else if (state.getBlock() instanceof WallTorchBlock && Lights.isTorch(state)) {
                changed = ModBlocks.UNLIT_WALL_TORCH.get().defaultBlockState().setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
            } else if (state.getBlock() instanceof TorchBlock && Lights.isTorch(state)) {
                changed = ModBlocks.UNLIT_TORCH.get().defaultBlockState();
            } else if (state.getLightEmission(level, pos) > 0) {
                if (state.hasProperty(BlockStateProperties.LIT)) {
                    changed = state.setValue(BlockStateProperties.LIT, false);
                } else {
                    changed = state.isCollisionShapeFullBlock(level, pos) ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                }
            } else if (state.is(Tags.Blocks.GLASS_PANES)) {
                changed = Blocks.BLACK_STAINED_GLASS_PANE.defaultBlockState();
            } else if (state.is(Tags.Blocks.GLASS)) {
                changed = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
            } else if (state.isCollisionShapeFullBlock(level, pos) && random.nextFloat() < 0.035F) {
                changed = random.nextBoolean() ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.NETHER_WART_BLOCK.defaultBlockState();
            }
            if (changed != null) {
                level.setBlock(pos, changed, Block.UPDATE_CLIENTS);
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof Container container) {
                // Nothing to take out of a dream.
                container.clearContent();
            }
        }
        BlockPos center = origin.offset(size.getX() / 2, BELOW, size.getZ() / 2);
        for (int i = 0; i < 5; i++) {
            BlockPos at = center.offset(random.nextInt(21) - 10, 0, random.nextInt(21) - 10);
            BlockPos ground = SpotFinder.findGround(level, at, 4);
            if (ground != null) {
                Blood.splatter(level, ground, 3 + random.nextInt(4), 2);
            }
        }
        Marks.writing(level, center, 2, 12);
        Marks.writing(level, center, 2, 12);
        Marks.scratches(level, center);
    }

    /** Your bed, somewhere it shouldn't be, with the only light there is: barely enough to see it by. */
    @Nullable
    private static BlockPos placeBed(ServerLevel level, BlockPos origin, Vec3i size, BlockPos start, RandomSource random) {
        for (int attempt = 0; attempt < 200; attempt++) {
            BlockPos at = origin.offset(2 + random.nextInt(size.getX() - 4), BELOW, 2 + random.nextInt(size.getZ() - 4));
            BlockPos head = SpotFinder.findGround(level, at, 4);
            if (head == null || head.distSqr(start) < 10 * 10) {
                continue;
            }
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos foot = head.relative(facing.getOpposite());
            if (!level.getBlockState(head).isAir() || !level.getBlockState(foot).isAir() || !level.getBlockState(head.above()).isAir()
                    || !level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)) {
                continue;
            }
            BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, facing);
            level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
            level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
            level.setBlock(head.above(), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 5), Block.UPDATE_ALL);
            return head;
        }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || DREAMS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (Iterator<Map.Entry<UUID, Dream>> it = DREAMS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Dream> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !isDream(player.level())) {
                // Logged out mid-dream: they are brought back when they log in again.
                it.remove();
                continue;
            }
            Dream dream = entry.getValue();
            dream.ticks++;
            ServerLevel level = player.serverLevel();
            if (dream.ticks == STALK_AT) {
                // Something else is in the room.
                Vec3 spot = SpotFinder.find(level, player, 10.0D, 16.0D, 120.0F, 50.0F, 6, 24, pos -> true);
                TheOneEntity one = spot == null ? null : TheOneEntity.spawn(level, player, spot, Math.max(dream.stage, HauntData.APPROACHING), Phase.STALK);
                if (one != null) {
                    one.holdPhase(Phase.STALK, HUNT_AT - STALK_AT);
                }
            } else if (dream.ticks == HUNT_AT) {
                for (TheOneEntity one : level.getEntities(ModEntities.THE_ONE.get(), one -> one.distanceTo(player) < 96.0D)) {
                    one.holdPhase(Phase.HUNT, LENGTH);
                    HorrorNetwork.playSoundTo(player, ModSounds.DRONE.get(), SoundSource.HOSTILE, player.getEyePosition(), 0.8F, 0.7F);
                }
            }
            if (dream.ticks >= LENGTH || player.getY() < dream.origin.getY() - 12) {
                it.remove();
                finish(player, dream, false);
            }
        }
    }

    /** Lying down in the bed you were looking for. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isDream(player.level())) {
            return;
        }
        event.setCanceled(true);
        if (player.level().getBlockState(event.getPos()).getBlock() instanceof BedBlock) {
            end(player, true);
        }
    }

    /** Wakes the player up, either way. */
    public static void end(ServerPlayer player, boolean won) {
        Dream dream = DREAMS.remove(player.getUUID());
        if (dream != null) {
            finish(player, dream, won);
        }
    }

    private static void finish(ServerPlayer player, Dream dream, boolean won) {
        ServerLevel level = player.serverLevel();
        for (TheOneEntity one : level.getEntities(ModEntities.THE_ONE.get(), one -> one.distanceTo(player) < 128.0D)) {
            one.discard();
        }
        HorrorNetwork.send(player, HorrorPacket.Type.CHALLENGE_END, won ? 1 : 0);
        wakeUp(player);
        if (won) {
            ServerLevel overworld = player.server.overworld();
            boolean othersAwake = overworld.players().stream().anyMatch(other -> other != player && !other.isSleeping() && !other.isSpectator());
            if (!othersAwake) {
                overworld.setDayTime(overworld.getDayTime() + (24000L - overworld.getDayTime() % 24000L));
            }
            ItemStack scrap = new ItemStack(ModItems.SHADOW_SCRAP.get());
            if (!player.getInventory().add(scrap)) {
                player.drop(scrap, false);
            }
            player.sendSystemMessage(Component.translatable("cornerofeye.message.dream_won").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else {
            HorrorNetwork.send(player, HorrorPacket.Type.JUMPSCARE, 18);
            HorrorNetwork.playSoundTo(player, ModSounds.SCREAM.get(), SoundSource.HOSTILE, player.getEyePosition(), 1.2F, 1.0F);
            player.setHealth(Math.max(1.0F, player.getHealth() / 2.0F));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
            player.sendSystemMessage(Component.translatable("cornerofeye.message.dream_lost").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }

    /** Back in the real bed, in the real world, in the game mode they had. */
    private static void wakeUp(ServerPlayer player) {
        CompoundTag back = persisted(player).getCompound(TAG);
        ServerLevel overworld = player.server.overworld();
        BlockPos bed = back.contains("X") ? new BlockPos(back.getInt("X"), back.getInt("Y"), back.getInt("Z")) : overworld.getSharedSpawnPos();
        player.teleportTo(overworld, bed.getX() + 0.5D, bed.getY() + 0.6D, bed.getZ() + 0.5D, player.getYRot(), 0.0F);
        player.setGameMode(back.contains("Mode") ? GameType.byId(back.getInt("Mode")) : GameType.SURVIVAL);
        CompoundTag persisted = persisted(player);
        persisted.remove(TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static CompoundTag persisted(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** Logged out (or crashed) in a dream: wake up quietly. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && persisted(player).contains(TAG)) {
            wakeUp(player);
        }
    }

    /** Nobody gets hurt in a dream. Its touch is handled by TheOneEntity, and falling ends the dream. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isDream(player.level())) {
            event.setCanceled(true);
        }
    }

    /** Nothing gets dropped and lost in a dream. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (isDream(event.getPlayer().level())) {
            event.setCanceled(true);
            event.getPlayer().getInventory().add(event.getEntity().getItem());
        }
    }

    /** The lantern doesn't light in a dream. */
    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (isDream(event.getLevel()) && event.getItemStack().getItem() instanceof LanternOfRevelationItem) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("cornerofeye.message.dream_lantern").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DREAMS.clear();
    }

    private Nightmares() {
    }
}
