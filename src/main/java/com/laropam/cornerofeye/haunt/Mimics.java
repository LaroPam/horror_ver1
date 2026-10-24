package com.laropam.cornerofeye.haunt;

import com.laropam.cornerofeye.entity.MimicEntity;
import com.laropam.cornerofeye.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Puts a mimic out in the dark where the player will notice it. */
public final class Mimics {
    /**
     * Wears the face of a friend who can't possibly be there (far away, or in another dimension); playing
     * alone, it wears yours.
     */
    static boolean appear(HauntContext c) {
        ServerLevel level = c.level();
        ServerPlayer victim = c.player();
        if ((!c.night() && !c.dark() && !c.forced()) || !level.getEntities(ModEntities.MIMIC.get(), mimic -> true).isEmpty()) {
            return false;
        }
        List<ServerPlayer> faraway = new ArrayList<>();
        for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
            if (other != victim && (other.level() != level || other.distanceTo(victim) > 48.0D)) {
                faraway.add(other);
            }
        }
        ServerPlayer copy = faraway.isEmpty() ? victim : faraway.get(c.random().nextInt(faraway.size()));
        // Off to one side, far enough that it takes a while to get there, in plain view if you turn.
        Vec3 spot = SpotFinder.find(level, victim, 16.0D, 28.0D, 35.0F, 25.0F, 6, 24,
                pos -> Sight.clearLine(level, victim, pos.add(0.0D, 1.6D, 0.0D)));
        return spot != null && MimicEntity.spawn(level, victim, copy, spot) != null;
    }

    private Mimics() {
    }
}
