package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.network.HorrorNetwork;
import com.laropam.cornerofeye.network.HorrorPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lights it has smothered. They stay dark until a player strikes flint over them, and while it is still
 * hunting nearby they can't be lit at all. Read from the lighting threads, so everything is concurrent.
 *
 * <ul>
 *     <li><b>dark</b>: any glowing block (glowstone, lanterns, sea lanterns...) whose light the engine ignores</li>
 *     <li><b>frozen</b>: furnaces, put out and standing still</li>
 *     <li><b>locked</b>: everything it put out during the current hunt, which can't be relit until it leaves</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID)
public final class LightSuppression {
    private static final Map<ResourceKey<Level>, Map<Long, Block>> DARK = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Map<Long, Block>> FROZEN = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> LOCKED = new ConcurrentHashMap<>();
    /** The client only needs to know which positions to leave unlit in its own world. */
    private static final Set<Long> CLIENT_DARK = ConcurrentHashMap.newKeySet();

    // ---------------------------------------------------------------- queries

    /** Called by the light engine on every light source. */
    public static boolean isDark(BlockGetter getter, long pos, BlockState state) {
        if (!(getter instanceof Level level)) {
            return false;
        }
        if (level.isClientSide) {
            return !CLIENT_DARK.isEmpty() && CLIENT_DARK.contains(pos);
        }
        Map<Long, Block> dark = DARK.get(level.dimension());
        return dark != null && !dark.isEmpty() && dark.get(pos) == state.getBlock();
    }

    public static boolean isFrozen(Level level, BlockPos pos, BlockState state) {
        Map<Long, Block> frozen = FROZEN.get(level.dimension());
        return frozen != null && !frozen.isEmpty() && frozen.get(pos.asLong()) == state.getBlock();
    }

    /** Put out by it and still waiting for someone to light it again. */
    public static boolean isSmothered(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return CLIENT_DARK.contains(pos.asLong());
        }
        BlockState state = level.getBlockState(pos);
        return isDark(level, pos.asLong(), state) || isFrozen(level, pos, state);
    }

    public static boolean isLocked(Level level, BlockPos pos) {
        Set<Long> locked = LOCKED.get(level.dimension());
        return locked != null && locked.contains(pos.asLong());
    }

    // ---------------------------------------------------------------- changes

    public static void darken(ServerLevel level, BlockPos pos, Block block) {
        DARK.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>()).put(pos.asLong(), block);
        changed(level, pos, true);
    }

    public static void freeze(ServerLevel level, BlockPos pos, Block block) {
        FROZEN.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>()).put(pos.asLong(), block);
        changed(level, pos, true);
    }

    /** Can't be lit again while it is still around. */
    public static void lock(ServerLevel level, BlockPos pos) {
        LOCKED.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet()).add(pos.asLong());
    }

    /** It has left: what it put out can be lit again (by hand). */
    public static void unlock(ServerLevel level, Collection<BlockPos> positions) {
        Set<Long> locked = LOCKED.get(level.dimension());
        if (locked != null) {
            positions.forEach(pos -> locked.remove(pos.asLong()));
        }
    }

    /** Lets the block give light (or burn fuel) again. */
    public static boolean relight(ServerLevel level, BlockPos pos) {
        boolean wasDark = remove(DARK, level, pos);
        boolean wasFrozen = remove(FROZEN, level, pos);
        if (wasDark || wasFrozen) {
            changed(level, pos, false);
        }
        return wasDark || wasFrozen;
    }

    /** It is gone for good: everything it ever put out gives light again. */
    public static void relightAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Set<Long> positions = new HashSet<>();
            for (Map<ResourceKey<Level>, Map<Long, Block>> store : List.of(DARK, FROZEN)) {
                Map<Long, Block> map = store.get(level.dimension());
                if (map != null) {
                    positions.addAll(map.keySet());
                }
            }
            positions.forEach(pos -> relight(level, BlockPos.of(pos)));
            LOCKED.remove(level.dimension());
        }
    }

    private static boolean remove(Map<ResourceKey<Level>, Map<Long, Block>> store, ServerLevel level, BlockPos pos) {
        Map<Long, Block> map = store.get(level.dimension());
        return map != null && map.remove(pos.asLong()) != null;
    }

    private static void changed(ServerLevel level, BlockPos pos, boolean dark) {
        level.getChunkSource().getLightEngine().checkBlock(pos);
        Data.get(level).setDirty();
        for (ServerPlayer player : level.players()) {
            HorrorNetwork.send(player, HorrorPacket.Type.SMOTHER, pos, dark ? 1 : 0);
        }
    }

    // ----------------------------------------------------------------- client

    /** Client side: is this block one it has put out? Called while building chunk meshes. */
    public static boolean isClientDark(BlockPos pos) {
        return !CLIENT_DARK.isEmpty() && CLIENT_DARK.contains(pos.asLong());
    }

    /** Client side: is {@code state} the state of a put-out block right next to {@code pos}? */
    public static boolean isClientDarkNextTo(BlockGetter level, BlockState state, BlockPos pos) {
        if (CLIENT_DARK.isEmpty() || state.getLightEmission(level, pos) <= 0) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (CLIENT_DARK.contains(next.asLong()) && level.getBlockState(next) == state) {
                return true;
            }
        }
        return false;
    }

    /** Client side: applies a change sent by the server. */
    public static boolean clientUpdate(BlockPos pos, int action) {
        if (action == 2) {
            CLIENT_DARK.clear();
            return false;
        }
        if (action == 1) {
            CLIENT_DARK.add(pos.asLong());
        } else {
            CLIENT_DARK.remove(pos.asLong());
        }
        return true;
    }

    // ----------------------------------------------------------------- events

    /** Striking flint over something it put out lights it again, unless it is still hunting nearby. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        if (!flint && !stack.is(Items.FIRE_CHARGE)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        if (level.isClientSide) {
            if (CLIENT_DARK.contains(pos.asLong())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (isLocked(level, pos)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.6F);
            player.displayClientMessage(Component.translatable("cornerofeye.message.cant_relight").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        if (isSmothered(level, pos)) {
            BlockState state = level.getBlockState(pos);
            if (isDark(level, pos.asLong(), state) && !player.getAbilities().instabuild) {
                // A lantern, glowstone, anything that glowed: what burned inside it is gone. A spark isn't enough,
                // it takes a new flame, a torch of yours, given up for it.
                ItemStack torch = findTorch(player);
                if (torch == null) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.2F);
                    player.displayClientMessage(Component.translatable("cornerofeye.message.needs_torch").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
                    return;
                }
                torch.shrink(1);
            }
            relight(serverLevel, pos);
            if (flint) {
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
            } else {
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** A torch from the inventory, to give up for relighting something it put out. */
    @javax.annotation.Nullable
    private static ItemStack findTorch(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
                return stack;
            }
        }
        return null;
    }

    /** What it put out is dead: break it and it crumbles. (A furnace still spills what was inside.) */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isSmothered(level, event.getPos())) {
            return;
        }
        BlockPos pos = event.getPos();
        event.setCanceled(true);
        relight(level, pos);
        level.destroyBlock(pos, false, event.getPlayer());
    }

    /**
     * Something new put down where a dead light used to be (it went some other way than being broken by hand: blown
     * up, pushed, replaced). A fresh light is a fresh light.
     */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && (has(DARK, level, event.getPos()) || has(FROZEN, level, event.getPos()))) {
            relight(level, event.getPos());
        }
    }

    private static boolean has(Map<ResourceKey<Level>, Map<Long, Block>> store, ServerLevel level, BlockPos pos) {
        Map<Long, Block> map = store.get(level.dimension());
        return map != null && map.containsKey(pos.asLong());
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Data.get(level);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sendAll(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendAll(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sendAll(event.getEntity());
    }

    private static void sendAll(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        HorrorNetwork.send(serverPlayer, HorrorPacket.Type.SMOTHER, BlockPos.ZERO, 2);
        ResourceKey<Level> dimension = serverPlayer.level().dimension();
        for (Map<ResourceKey<Level>, Map<Long, Block>> store : List.of(DARK, FROZEN)) {
            Map<Long, Block> map = store.get(dimension);
            if (map != null) {
                map.keySet().forEach(pos -> HorrorNetwork.send(serverPlayer, HorrorPacket.Type.SMOTHER, BlockPos.of(pos), 1));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DARK.clear();
        FROZEN.clear();
        LOCKED.clear();
    }

    // ------------------------------------------------------------ persistence

    /** Smothered lights survive restarts: they stay out until someone lights them. */
    private static final class Data extends SavedData {
        private static final String NAME = "cornerofeye_smothered";
        private final ResourceKey<Level> dimension;

        private Data(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }

        static Data get(ServerLevel level) {
            ResourceKey<Level> dimension = level.dimension();
            return level.getDataStorage().computeIfAbsent(tag -> load(dimension, tag), () -> new Data(dimension), NAME);
        }

        private static Data load(ResourceKey<Level> dimension, CompoundTag tag) {
            read(tag.getList("Dark", Tag.TAG_COMPOUND), DARK.computeIfAbsent(dimension, key -> new ConcurrentHashMap<>()));
            read(tag.getList("Frozen", Tag.TAG_COMPOUND), FROZEN.computeIfAbsent(dimension, key -> new ConcurrentHashMap<>()));
            return new Data(dimension);
        }

        private static void read(ListTag list, Map<Long, Block> into) {
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("Block"));
                Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                if (block != null) {
                    into.put(entry.getLong("Pos"), block);
                }
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.put("Dark", write(DARK.get(this.dimension)));
            tag.put("Frozen", write(FROZEN.get(this.dimension)));
            return tag;
        }

        private static ListTag write(Map<Long, Block> map) {
            ListTag list = new ListTag();
            if (map != null) {
                map.forEach((pos, block) -> {
                    CompoundTag entry = new CompoundTag();
                    entry.putLong("Pos", pos);
                    entry.putString("Block", BuiltInRegistries.BLOCK.getKey(block).toString());
                    list.add(entry);
                });
            }
            return list;
        }
    }

    private LightSuppression() {
    }
}
