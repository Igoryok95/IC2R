package ic2.core.experimental;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class DynamiteProjectile extends ThrowableItemProjectile {
  public int fuse = 100;
  private BlockPos attached;
  private Vec3 anchor;
  private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> STUCK =
      net.minecraft.network.syncher.SynchedEntityData.defineId(
          DynamiteProjectile.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

  @Override
  protected void defineSynchedData(
      net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(STUCK, false);
  }

  public DynamiteProjectile(EntityType<? extends DynamiteProjectile> type, Level level) {
    super(type, level);
  }

  @Override
  protected Item getDefaultItem() {
    return ExperimentalDynamite.DYNAMITE;
  }

  @Override
  protected void onHitBlock(BlockHitResult hit) {
    if (getItem().is(ExperimentalDynamite.STICKY)) {
      attached = hit.getBlockPos();
      setDeltaMovement(Vec3.ZERO);
      setNoGravity(true);
      anchor =
          hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(.02));
      setPos(anchor);
      entityData.set(STUCK, true);
    } else {
      var motion = getDeltaMovement();
      setDeltaMovement(
          switch (hit.getDirection().getAxis()) {
            case X -> motion.multiply(-.4, .8, .8);
            case Y -> motion.multiply(.8, -.4, .8);
            case Z -> motion.multiply(.8, .8, -.4);
          });
    }
  }

  @Override
  public void tick() {
    if (!level().isClientSide) {
      if (attached != null && level().getBlockState(attached).isAir()) {
        attached = null;
        anchor = null;
        entityData.set(STUCK, false);
      }
    }
    boolean stuck = entityData.get(STUCK);
    setNoGravity(stuck);
    if (stuck) setDeltaMovement(Vec3.ZERO);
    super.tick();
    if (entityData.get(STUCK)) {
      if (anchor != null) setPos(anchor);
      setDeltaMovement(Vec3.ZERO);
    }
    if (level().isClientSide) return;
    if (isInWater()) fuse = (int) Math.min(Integer.MAX_VALUE, (long) fuse + 2000);
    if ((fuse -= onGround() ? 4 : 1) <= 0) {
      // The original point blast affects the immediate 3x3x3 neighborhood only.
      ExperimentalPointExplosion.explode(this);
      discard();
    }
  }

  @Override
  public void addAdditionalSaveData(CompoundTag nbt) {
    super.addAdditionalSaveData(nbt);
    nbt.putInt("fuse", fuse);
    if (attached != null) nbt.putLong("attached", attached.asLong());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag nbt) {
    super.readAdditionalSaveData(nbt);
    fuse = nbt.contains("fuse") ? nbt.getInt("fuse") : 100;
    attached = nbt.contains("attached") ? BlockPos.of(nbt.getLong("attached")) : null;
    anchor = attached == null ? null : position();
    entityData.set(STUCK, attached != null);
  }
}
