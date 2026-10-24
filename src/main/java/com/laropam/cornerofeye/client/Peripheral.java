package com.laropam.cornerofeye.client;

import com.laropam.cornerofeye.entity.Phase;
import com.laropam.cornerofeye.entity.Scene;
import com.laropam.cornerofeye.entity.TheOneEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The core rule: it can only be seen from the corner of your eye. Works out where on the screen it would be
 * and fades it out the closer it gets to the middle. Every player computes this for their own camera,
 * so in co-op one of you can see it while the other can't.
 */
public final class Peripheral {
    /** Screen-space distance from the center (0) to the edge (1) where it starts to show... */
    private static final double FADE_START = 0.45D;
    /** ...and where it is fully visible. */
    private static final double FADE_END = 0.75D;

    private static double fov = 70.0D;

    static void setFov(double value) {
        fov = value;
    }

    public static float visibility(TheOneEntity entity, float partialTick) {
        if (entity.getScene() == Scene.MASSACRE) {
            // In the village it is never seen at all.
            return 0.0F;
        }
        if (entity.getPhase() == Phase.REVEALED || entity.getPhase() == Phase.SCENE) {
            // Caught, or one of its moments: it wants to be seen.
            return 1.0F;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        double tanV = Math.tan(Math.toRadians(fov) / 2.0D);
        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());

        Vec3 base = entity.getPosition(partialTick);
        double closest = Double.MAX_VALUE;
        // The part of it nearest the middle of the screen decides: no peeking at its feet while staring at its face.
        for (double h = 0.2D; h <= 3.0D; h += 0.7D) {
            Vec3 d = base.add(0.0D, h * entity.getBbHeight() / 3.2D, 0.0D).subtract(eye);
            double forward = d.x * look.x() + d.y * look.y() + d.z * look.z();
            if (forward <= 0.05D) {
                continue;
            }
            double x = (d.x * left.x() + d.y * left.y() + d.z * left.z()) / forward / (tanV * aspect);
            double y = (d.x * up.x() + d.y * up.y() + d.z * up.z()) / forward / tanV;
            closest = Math.min(closest, Math.max(Math.abs(x), Math.abs(y)));
        }
        if (closest == Double.MAX_VALUE) {
            return 1.0F;
        }
        double t = Mth.clamp((closest - FADE_START) / (FADE_END - FADE_START), 0.0D, 1.0D);
        return (float) (t * t * (3.0D - 2.0D * t));
    }

    private Peripheral() {
    }
}
