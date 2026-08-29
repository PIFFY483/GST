package com.gst.entity;

import com.gst.world.SpaceDimensions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class SpacePodEntity extends Entity {

    public SpacePodEntity(EntityType<?> type, World world) {
        super(type, world);
        this.setNoGravity(false);
    }

    @Override
    public void tick() {
        super.tick();

        // 1. Yükselme ve İtki Mantığı
        if (this.hasPassengers()) {
            boolean inSpaceWorld = this.getWorld().getRegistryKey() == SpaceDimensions.SPACE_WORLD_KEY;
            if (!inSpaceWorld) {
                // Overworld'de dikey tırmanış
                this.setVelocity(this.getVelocity().x * 0.9, 0.8, this.getVelocity().z * 0.9);
            } else {
                // Uzay boyutunda süzülme
                this.setVelocity(this.getVelocity().x * 0.98, this.getVelocity().y * 0.95, this.getVelocity().z * 0.98);
            }
        }

        // 2. Fizik Yönetimi
        if (!this.getWorld().isClient()) {
            boolean isSpace = this.getWorld().getRegistryKey() == SpaceDimensions.SPACE_WORLD_KEY;
            this.setNoGravity(isSpace);

            this.move(MovementType.SELF, this.getVelocity());
        }
    }

    @Override
    public void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        if (this.hasPassenger(passenger)) {
            positionUpdater.accept(passenger, this.getX(), this.getY() + 0.5D, this.getZ());
        }
    }

    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.shouldCancelInteraction()) {
            return ActionResult.PASS;
        }

        if (!this.getWorld().isClient()) {
            return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
        }

        return ActionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty();
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}