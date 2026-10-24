package com.laropam.cornerofeye.entity;

import com.laropam.cornerofeye.haunt.Animals;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Given to every animal: when it feels the thing nearby, it stops what it was doing and looks at it.
 * Dogs growl (whine when it is close), cats hiss and run, horses rear, everything else stares and then
 * backs off once it comes too close. For a stare at nothing they only stand and look.
 */
public class SenseItGoal extends Goal {
    private final PathfinderMob mob;
    @Nullable
    private Animals.Sense sense;
    private int nextCheck;
    private int recheck;
    private int soundCooldown;

    public SenseItGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Goals are offered a start only every other tick; this keeps the lookup to twice a second.
        if (this.mob.tickCount < this.nextCheck) {
            return false;
        }
        this.nextCheck = this.mob.tickCount + 10;
        this.sense = Animals.find(this.mob);
        return this.sense != null && !this.ownerWandersOff();
    }

    @Override
    public boolean canContinueToUse() {
        return this.sense != null && !this.ownerWandersOff();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        this.recheck = 10;
        this.soundCooldown = this.mob.getRandom().nextInt(20);
        if (this.sense != null && this.sense.real() && (this.mob instanceof Cat || this.mob instanceof Ocelot)) {
            this.mob.playSound(SoundEvents.CAT_HISS, 1.0F, 1.0F);
            this.soundCooldown = 60 + this.mob.getRandom().nextInt(80);
        }
    }

    @Override
    public void stop() {
        this.sense = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (--this.recheck <= 0) {
            this.recheck = 10;
            this.sense = Animals.find(this.mob);
        }
        if (this.sense == null) {
            return;
        }
        Vec3 at = this.sense.pos();
        this.mob.getLookControl().setLookAt(at.x, at.y, at.z, 30.0F, 30.0F);
        double dist = Math.sqrt(this.mob.distanceToSqr(at));
        this.soundCooldown--;

        if (this.mob instanceof Wolf wolf) {
            if (this.soundCooldown <= 0) {
                this.soundCooldown = 40 + wolf.getRandom().nextInt(60);
                boolean close = this.sense.real() && dist < 8.0D;
                wolf.playSound(close ? SoundEvents.WOLF_WHINE : SoundEvents.WOLF_GROWL, 1.0F, close ? 1.0F : 0.8F);
            }
            this.fleeOrHold(at, this.sense.real() && dist < 6.0D && !wolf.isOrderedToSit(), 1.3D);
        } else if (this.mob instanceof Cat || this.mob instanceof Ocelot) {
            if (this.sense.real() && this.soundCooldown <= 0) {
                this.soundCooldown = 60 + this.mob.getRandom().nextInt(80);
                this.mob.playSound(SoundEvents.CAT_HISS, 1.0F, 1.0F);
            }
            boolean sitting = this.mob instanceof Cat cat && cat.isOrderedToSit();
            this.fleeOrHold(at, this.sense.real() && !sitting, 1.5D);
        } else if (this.mob instanceof AbstractHorse horse) {
            if (this.sense.real() && dist < 12.0D && this.soundCooldown <= 0) {
                this.soundCooldown = 60 + horse.getRandom().nextInt(100);
                horse.makeMad();
            }
            this.fleeOrHold(at, this.sense.real() && dist < 8.0D && !horse.isVehicle(), 1.4D);
        } else {
            this.fleeOrHold(at, this.sense.real() && dist < 9.0D, 1.4D);
        }
    }

    private void fleeOrHold(Vec3 from, boolean flee, double speed) {
        if (!flee) {
            this.mob.getNavigation().stop();
        } else if (this.mob.getNavigation().isDone()) {
            Vec3 away = DefaultRandomPos.getPosAway(this.mob, 16, 7, from);
            if (away != null) {
                this.mob.getNavigation().moveTo(away.x, away.y, away.z, speed);
            }
        }
    }

    /** A pet whose owner is walking away goes after them; being left behind is worse. */
    private boolean ownerWandersOff() {
        if (this.mob instanceof TamableAnimal pet && !pet.isOrderedToSit()) {
            LivingEntity owner = pet.getOwner();
            return owner != null && pet.distanceToSqr(owner) > 100.0D;
        }
        return false;
    }
}
