package com.laropam.cornerofeye.client;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Cracks across a menu, as if the screen itself were glass. Each crack starts at an anchor (a corner, an edge
 * or a point of impact) and branches out from there; it can be drawn partly grown, so new ones spread while
 * you watch. Coordinates are in units of the area's height, measured from the anchor, so they keep their
 * shape when the window is resized.
 */
final class Cracks {
    /** How far a growing crack spreads per second, in heights. */
    static final float GROWTH_PER_SECOND = 0.45F;

    private record Segment(float x1, float y1, float x2, float y2, float at, boolean thick) {
    }

    /** One crack: where it is anchored (fractions of the area) and its segments. */
    static final class Crack {
        private final float anchorX;
        private final float anchorY;
        private final List<Segment> segments = new ArrayList<>();
        private float length;

        private Crack(float anchorX, float anchorY) {
            this.anchorX = anchorX;
            this.anchorY = anchorY;
        }

        float length() {
            return this.length;
        }

        private void add(Segment segment) {
            this.segments.add(segment);
            this.length = Math.max(this.length, segment.at() + (float) Math.hypot(segment.x2() - segment.x1(), segment.y2() - segment.y1()));
        }

        /** Draws it over the area, as far as {@code grown} heights from where it started. */
        void draw(GuiGraphics graphics, int left, int top, int width, int height, float grown) {
            float ox = left + this.anchorX * width;
            float oy = top + this.anchorY * height;
            for (Segment s : this.segments) {
                if (s.at() >= grown) {
                    continue;
                }
                float full = (float) Math.hypot(s.x2() - s.x1(), s.y2() - s.y1());
                float part = Math.min(1.0F, (grown - s.at()) / Math.max(full, 1.0E-4F));
                float x1 = ox + s.x1() * height;
                float y1 = oy + s.y1() * height;
                float len = full * part * height;
                float angle = (float) Math.atan2(s.y2() - s.y1(), s.x2() - s.x1());
                line(graphics, x1, y1, len, angle, s.thick() ? 2 : 1);
            }
        }
    }

    private static void line(GuiGraphics graphics, float x, float y, float len, float angle, int thickness) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotation(angle));
        int l = Mth.ceil(len) + 1;
        // A pale edge where the light catches the break, then the dark line itself.
        graphics.fill(0, 1, l, 1 + thickness, 0x46FFFFFF);
        graphics.fill(0, 0, l, thickness, 0xE0101012);
        graphics.pose().popPose();
    }

    /** A crack running in from the corner or edge at ({@code ax}, {@code ay}), heading roughly along {@code angle}. */
    static Crack fromEdge(RandomSource random, float ax, float ay, float angle, float length) {
        Crack crack = new Crack(ax, ay);
        branch(crack, random, 0.0F, 0.0F, angle, 0.0F, length, 0);
        return crack;
    }

    /** Where something hit the glass: lines bursting outwards, joined by rings. */
    static Crack impact(RandomSource random, float ax, float ay, float radius) {
        Crack crack = new Crack(ax, ay);
        int spokes = 7 + random.nextInt(4);
        float[] angles = new float[spokes];
        float[] lengths = new float[spokes];
        for (int i = 0; i < spokes; i++) {
            angles[i] = (i + random.nextFloat() * 0.6F) * Mth.TWO_PI / spokes;
            lengths[i] = radius * (0.5F + random.nextFloat() * 0.8F);
            branch(crack, random, 0.0F, 0.0F, angles[i], 0.0F, lengths[i], 1);
        }
        for (float ring : new float[]{0.18F, 0.42F, 0.7F}) {
            for (int i = 0; i < spokes; i++) {
                int j = (i + 1) % spokes;
                float r1 = radius * ring * (0.85F + random.nextFloat() * 0.3F);
                float r2 = radius * ring * (0.85F + random.nextFloat() * 0.3F);
                if (r1 > lengths[i] || r2 > lengths[j] || random.nextFloat() < 0.25F) {
                    continue;
                }
                crack.add(new Segment(Mth.cos(angles[i]) * r1, Mth.sin(angles[i]) * r1,
                        Mth.cos(angles[j]) * r2, Mth.sin(angles[j]) * r2, r1, false));
            }
        }
        return crack;
    }

    /** A wandering line that sometimes splits. {@code at} is how far from the start it begins. */
    private static void branch(Crack crack, RandomSource random, float x, float y, float angle, float at, float remaining, int depth) {
        while (remaining > 0.0F) {
            float len = 0.012F + random.nextFloat() * 0.028F;
            angle += (random.nextFloat() - 0.5F) * 0.7F;
            float nx = x + Mth.cos(angle) * len;
            float ny = y + Mth.sin(angle) * len;
            crack.add(new Segment(x, y, nx, ny, at, depth == 0 && at < 0.12F));
            x = nx;
            y = ny;
            at += len;
            remaining -= len;
            if (depth < 3 && random.nextFloat() < 0.16F) {
                float turn = (random.nextBoolean() ? 1.0F : -1.0F) * (0.4F + random.nextFloat() * 0.7F);
                branch(crack, random, x, y, angle + turn, at, remaining * (0.25F + random.nextFloat() * 0.4F), depth + 1);
            }
        }
    }

    /** A crack from a random corner or edge, pointing into the area. */
    static Crack fromRandomEdge(RandomSource random, float length) {
        float ax;
        float ay;
        float angle;
        switch (random.nextInt(6)) {
            case 0 -> { ax = 0.0F; ay = 0.0F; angle = Mth.HALF_PI * 0.5F; }
            case 1 -> { ax = 1.0F; ay = 0.0F; angle = Mth.HALF_PI * 1.5F; }
            case 2 -> { ax = 0.0F; ay = 1.0F; angle = -Mth.HALF_PI * 0.5F; }
            case 3 -> { ax = 1.0F; ay = 1.0F; angle = -Mth.HALF_PI * 1.5F; }
            case 4 -> { ax = random.nextFloat(); ay = 0.0F; angle = Mth.HALF_PI; }
            default -> { ax = random.nextFloat(); ay = 1.0F; angle = -Mth.HALF_PI; }
        }
        return fromEdge(random, ax, ay, angle + (random.nextFloat() - 0.5F) * 0.8F, length);
    }

    private Cracks() {
    }
}
