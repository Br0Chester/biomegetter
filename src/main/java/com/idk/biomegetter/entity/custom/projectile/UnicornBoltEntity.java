package com.idk.biomegetter.entity.custom.projectile;

import com.idk.biomegetter.entity.custom.UnicornEntity;
import com.idk.biomegetter.entity.custom.ally.SummonedAlly;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class UnicornBoltEntity extends Projectile {

    private static final float SPEED = 1.2F;
    private static final float EXPLOSION_RADIUS = 3.0F;
    private static final int MAX_LIFETIME_TICKS = 100; // ~5 сек — на случай, если ни во что не попал

    public UnicornBoltEntity(EntityType<? extends UnicornBoltEntity> type, Level level) {
        super(type, level);
    }

    public void shootTowards(Vec3 targetPos) {
        Vec3 direction = targetPos.subtract(this.position()).normalize();
        this.setDeltaMovement(direction.scale(SPEED));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {

    }

    @Override
    public void tick() {
        super.tick();

        Vec3 start = this.position();
        Vec3 end = start.add(this.getDeltaMovement());

        HitResult blockHit = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
        ));
        if (blockHit.getType() != HitResult.Type.MISS) {
            this.onHit(blockHit);
            return;
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                this.level(), this, start, end,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0),
                this::canHitEntity
        );
        if (entityHit != null) {
            this.onHit(entityHit);
            return;
        }

        this.setPos(end.x, end.y, end.z);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
        }

        if (this.tickCount > MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    public boolean canHitEntity(Entity target) {
        return target instanceof LivingEntity
                && !(target instanceof UnicornEntity)
                && !(target instanceof SummonedAlly)
                && target != this.getOwner();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        this.explode();
    }

    private void explode() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }

        this.discard();
    }
}