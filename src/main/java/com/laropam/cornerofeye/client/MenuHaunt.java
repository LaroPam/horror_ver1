package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.ClientConfig;
import com.laropam.cornerofeye.CornerOfTheEye;
import com.laropam.cornerofeye.item.LanternOfRevelationItem;
import com.laropam.cornerofeye.mixin.TitleScreenAccessor;
import com.laropam.cornerofeye.network.HorrorPacket;
import com.laropam.cornerofeye.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * It follows you out of the world. The game remembers the worst it ever got (config/cornerofeye-memory.txt),
 * and the title screen shows it: a wrong splash text, then cracks across the screen as if it were glass,
 * more of them the worse it got, some spreading while you watch, and at the end buttons that say the wrong
 * thing for a moment. Inside a world, from stage 2, the pause menu and the inventory crack too, and items
 * whisper in their tooltips.
 */
@Mod.EventBusSubscriber(modid = CornerOfTheEye.MODID, value = Dist.CLIENT)
public final class MenuHaunt {
    private static final ResourceLocation VIGNETTE = CornerOfTheEye.id("textures/gui/dread_vignette.png");
    private static final int SPLASHES = 10;
    private static final int GLITCHES = 8;
    private static final int WHISPERS = 8;
    private static final RandomSource RANDOM = RandomSource.create();

    /** Worst stage it has ever reached in any world on this computer; -1 until read from disk. */
    private static int memory = -1;
    /** Decides where the title screen's cracks are, so they are the same every time the game starts. */
    private static long seed;
    @Nullable
    private static String splash;

    /** The title screen's cracks, built for {@link #titleLevel}. */
    private static final Layer TITLE = new Layer();
    private static int titleLevel = -1;
    /** This session's cracks in the pause menu and in the inventory panel, built up to {@link #worldLevel}. */
    private static final Layer PAUSE = new Layer();
    private static final Layer PANEL = new Layer();
    private static int worldLevel;
    /** When the next crack starts to spread on the screen that is open, or 0 for none. */
    private static long nextCrack;
    private static int freshCracks;

    @Nullable
    private static Button glitched;
    @Nullable
    private static Component glitchedMessage;
    private static long glitchUntil;
    private static long nextGlitch;

    /** Cracks drawn over one screen; a crack added while open spreads instead of just being there. */
    private static final class Layer {
        private record Entry(Cracks.Crack crack, long start) {
        }

        private final List<Entry> entries = new ArrayList<>();

        void settle(Cracks.Crack crack) {
            this.entries.add(new Entry(crack, 0L));
        }

        void spread(Cracks.Crack crack) {
            this.entries.add(new Entry(crack, Util.getMillis()));
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(ModSounds.CRACK.get(), 0.85F + RANDOM.nextFloat() * 0.3F, 0.6F));
        }

        void clear() {
            this.entries.clear();
        }

        int size() {
            return this.entries.size();
        }

        void draw(GuiGraphics graphics, int left, int top, int width, int height) {
            long now = Util.getMillis();
            for (Entry entry : this.entries) {
                float grown = entry.start() == 0L ? Float.MAX_VALUE : (now - entry.start()) / 1000.0F * Cracks.GROWTH_PER_SECOND;
                entry.crack().draw(graphics, left, top, width, height, grown);
            }
        }
    }

    // ------------------------------------------------------------ memory

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("cornerofeye-memory.txt");
    }

    static int memory() {
        if (memory < 0) {
            memory = 0;
            seed = RANDOM.nextLong();
            try {
                if (Files.exists(file())) {
                    String[] parts = Files.readString(file()).trim().split("\\s+");
                    memory = Mth.clamp(Integer.parseInt(parts[0]), 0, 3);
                    if (parts.length > 1) {
                        seed = Long.parseLong(parts[1]);
                    }
                }
            } catch (IOException | NumberFormatException ignored) {
                // Unreadable: start clean.
            }
        }
        return memory;
    }

    /** Called with every stage the server tells us about. Banishing it for good wipes the memory. */
    static void remember(int stage) {
        int value = stage == HorrorPacket.DEFEATED ? 0 : Mth.clamp(stage, 0, 3);
        if (value == memory() || (stage != HorrorPacket.DEFEATED && value < memory())) {
            return;
        }
        memory = value;
        if (value == 0) {
            // A clean screen, and new cracks if it ever starts again.
            seed = RANDOM.nextLong();
        }
        try {
            Files.writeString(file(), value + " " + seed);
        } catch (IOException ignored) {
            // Then it only remembers until the game closes.
        }
    }

    private static boolean enabled() {
        return ClientConfig.MENU_CORRUPTION.get();
    }

    /** Stage of the world we are in, 0 outside one or once it is gone. */
    private static int worldStage() {
        int stage = ClientHorror.stage();
        return stage == HorrorPacket.DEFEATED || Minecraft.getInstance().level == null ? 0 : stage;
    }

    // ------------------------------------------------------------ screens

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        restoreGlitch();
        long now = Util.getMillis();
        Screen screen = event.getScreen();
        nextGlitch = now + 4000 + RANDOM.nextInt(8000);
        nextCrack = 0L;
        freshCracks = 0;
        if (!enabled()) {
            return;
        }
        if (screen instanceof TitleScreen title && memory() >= 1) {
            ((TitleScreenAccessor) title).cornerofeye$setSplash(null);
            splash = I18n.get("cornerofeye.splash." + RANDOM.nextInt(SPLASHES));
            buildTitle();
            if (memory() >= 2) {
                nextCrack = now + 12000 + RANDOM.nextInt(18000);
            }
        } else if (screen instanceof PauseScreen && worldStage() >= 2) {
            buildWorld();
            if (RANDOM.nextFloat() < (worldStage() >= 3 ? 0.8F : 0.4F)) {
                nextCrack = now + 4000 + RANDOM.nextInt(6000);
            }
        } else if (screen instanceof InventoryScreen && worldStage() >= 2) {
            buildWorld();
            if (worldStage() >= 3 && PANEL.size() < 7 && RANDOM.nextFloat() < 0.35F) {
                nextCrack = now + 1000 + RANDOM.nextInt(2500);
            }
        }
    }

    @SubscribeEvent
    public static void onClose(ScreenEvent.Closing event) {
        restoreGlitch();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PAUSE.clear();
        PANEL.clear();
        worldLevel = 0;
    }

    @SubscribeEvent
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        boolean title = screen instanceof TitleScreen && memory() >= 3;
        boolean pause = screen instanceof PauseScreen && worldStage() >= 3;
        if (enabled() && (title || pause)) {
            glitchButtons(screen);
        }
    }

    /** Over everything, like the glass in front of it: the title screen and the pause menu. */
    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!enabled()) {
            return;
        }
        Screen screen = event.getScreen();
        GuiGraphics graphics = event.getGuiGraphics();
        if (screen instanceof TitleScreen && memory() >= 1) {
            if (memory() >= 2) {
                RenderSystem.enableBlend();
                graphics.setColor(1.0F, 1.0F, 1.0F, memory() >= 3 ? 0.85F : 0.5F);
                graphics.blit(VIGNETTE, 0, 0, screen.width, screen.height, 0.0F, 0.0F, 256, 256, 256, 256);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
            }
            if (splash != null) {
                drawSplash(graphics, screen);
            }
            maybeSpread(TITLE, 4, 0.3F, 0.55F);
            overEverything(graphics, () -> TITLE.draw(graphics, 0, 0, screen.width, screen.height));
        } else if (screen instanceof PauseScreen && worldStage() >= 2) {
            maybeSpread(PAUSE, 9, 0.3F, 0.5F);
            overEverything(graphics, () -> PAUSE.draw(graphics, 0, 0, screen.width, screen.height));
        }
    }

    /** Buttons draw a little in front of the screen; the glass is in front of them. */
    private static void overEverything(GuiGraphics graphics, Runnable draw) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        draw.run();
        graphics.pose().popPose();
    }

    /** In the inventory the panel itself is cracked: under the items, never outside its edges. */
    @SubscribeEvent
    public static void onInventoryBackground(ContainerScreenEvent.Render.Background event) {
        if (!enabled() || !(event.getContainerScreen() instanceof InventoryScreen screen) || worldStage() < 2) {
            return;
        }
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        maybeSpread(PANEL, 7, 0.35F, 0.7F);
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.enableScissor(left, top, left + screen.getXSize(), top + screen.getYSize());
        PANEL.draw(graphics, left, top, screen.getXSize(), screen.getYSize());
        graphics.disableScissor();
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!enabled() || event.getEntity() == null || worldStage() < 2) {
            return;
        }
        ItemStack stack = event.getItemStack();
        // Stable for twenty seconds at a time, so a line doesn't flicker on and off while you look.
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        long window = Util.getMillis() / 20000L;
        int roll = Math.floorMod(Objects.hash(item, window), 100);
        int line = Math.floorMod(Objects.hash(window, item, 7), 24);
        String key = null;
        if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
            key = roll < 40 ? "cornerofeye.tooltip.torch." + line % 3 : null;
        } else if (stack.getItem() instanceof LanternOfRevelationItem) {
            key = roll < 40 ? "cornerofeye.tooltip.lantern." + line % 3 : null;
        } else if (roll < (worldStage() >= 3 ? 8 : 4)) {
            key = "cornerofeye.tooltip.any." + line % WHISPERS;
        }
        if (key != null) {
            event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }

    // ------------------------------------------------------------ pieces

    /** The title screen's cracks come from the remembered seed: the same ones every time, more as it got worse. */
    private static void buildTitle() {
        if (titleLevel == memory()) {
            return;
        }
        titleLevel = memory();
        TITLE.clear();
        RandomSource random = RandomSource.create(seed);
        TITLE.settle(Cracks.fromRandomEdge(random, 0.35F));
        if (titleLevel >= 2) {
            TITLE.settle(Cracks.fromRandomEdge(random, 0.3F + random.nextFloat() * 0.25F));
            TITLE.settle(Cracks.fromRandomEdge(random, 0.3F + random.nextFloat() * 0.25F));
        }
        if (titleLevel >= 3) {
            float x = random.nextBoolean() ? 0.12F + random.nextFloat() * 0.13F : 0.75F + random.nextFloat() * 0.13F;
            TITLE.settle(Cracks.impact(random, x, 0.55F + random.nextFloat() * 0.3F, 0.3F));
            TITLE.settle(Cracks.fromRandomEdge(random, 0.4F + random.nextFloat() * 0.3F));
            TITLE.settle(Cracks.fromRandomEdge(random, 0.4F + random.nextFloat() * 0.3F));
        }
    }

    /** In a world the cracks belong to this session and only ever add up. */
    private static void buildWorld() {
        int stage = worldStage();
        if (stage >= 2 && worldLevel < 2) {
            PAUSE.settle(Cracks.fromRandomEdge(RANDOM, 0.3F + RANDOM.nextFloat() * 0.2F));
            PAUSE.settle(Cracks.fromRandomEdge(RANDOM, 0.3F + RANDOM.nextFloat() * 0.2F));
            PANEL.settle(Cracks.fromRandomEdge(RANDOM, 0.4F + RANDOM.nextFloat() * 0.3F));
        }
        if (stage >= 3 && worldLevel < 3) {
            PAUSE.settle(Cracks.impact(RANDOM, RANDOM.nextBoolean() ? 0.15F : 0.85F, 0.3F + RANDOM.nextFloat() * 0.5F, 0.28F));
            PAUSE.settle(Cracks.fromRandomEdge(RANDOM, 0.4F + RANDOM.nextFloat() * 0.3F));
            PANEL.settle(Cracks.fromRandomEdge(RANDOM, 0.5F + RANDOM.nextFloat() * 0.3F));
            PANEL.settle(Cracks.fromRandomEdge(RANDOM, 0.5F + RANDOM.nextFloat() * 0.3F));
        }
        worldLevel = Math.max(worldLevel, stage);
    }

    /** When its time comes, a new crack runs across the open screen, with the sound of it. */
    private static void maybeSpread(Layer layer, int max, float minLength, float maxLength) {
        long now = Util.getMillis();
        if (nextCrack == 0L || now < nextCrack) {
            return;
        }
        nextCrack = 0L;
        if (layer.size() >= max || freshCracks >= 3) {
            return;
        }
        freshCracks++;
        layer.spread(Cracks.fromRandomEdge(RANDOM, minLength + RANDOM.nextFloat() * (maxLength - minLength)));
        if (layer == TITLE) {
            // The title screen keeps going for as long as you sit there.
            nextCrack = now + 20000 + RANDOM.nextInt(25000);
        }
    }

    /** Where the yellow splash would be, but it isn't yellow and it doesn't bounce. */
    private static void drawSplash(GuiGraphics graphics, Screen screen) {
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(screen.width / 2.0F + 123.0F, 69.0F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
        float scale = 1.8F * 100.0F / (font.width(splash) + 32);
        graphics.pose().scale(scale, scale, scale);
        graphics.drawCenteredString(font, splash, 0, -8, 0xFFA01212);
        graphics.pose().popPose();
    }

    /** Every so often a button says something else, for a few frames. Clicking it still does what it should. */
    private static void glitchButtons(Screen screen) {
        long now = Util.getMillis();
        if (glitched != null && now >= glitchUntil) {
            restoreGlitch();
        }
        if (glitched != null || now < nextGlitch) {
            return;
        }
        nextGlitch = now + 6000 + RANDOM.nextInt(14000);
        List<Button> buttons = screen.children().stream()
                .filter(child -> child instanceof Button button && button.visible && button.getWidth() >= 98)
                .map(child -> (Button) child).toList();
        if (buttons.isEmpty()) {
            return;
        }
        glitched = buttons.get(RANDOM.nextInt(buttons.size()));
        glitchedMessage = glitched.getMessage();
        glitched.setMessage(Component.translatable("cornerofeye.glitch." + RANDOM.nextInt(GLITCHES)).withStyle(ChatFormatting.DARK_RED));
        glitchUntil = now + 120 + RANDOM.nextInt(160);
    }

    private static void restoreGlitch() {
        if (glitched != null && glitchedMessage != null) {
            glitched.setMessage(glitchedMessage);
        }
        glitched = null;
        glitchedMessage = null;
    }

    private MenuHaunt() {
    }
}
