package ic2.core.experimental;

import ic2.api.event.ExplosionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;

/** Experimental PointExplosion: resistance <10, immediate 3x3x3 blocks, 20 entity damage. */
public final class ExperimentalPointExplosion {
  public static void explode(DynamiteProjectile dynamite) {
    var level = dynamite.level();
    var owner = dynamite.getOwner() instanceof LivingEntity living ? living : null;
    if (NeoForge.EVENT_BUS
        .post(new ExplosionEvent(level, dynamite, dynamite.position(), 1, owner, 0, 1))
        .isCanceled()) return;
    var explosion =
        new Explosion(
            level,
            owner,
            dynamite.getX(),
            dynamite.getY(),
            dynamite.getZ(),
            1,
            true,
            Explosion.BlockInteraction.DESTROY);
    var center = dynamite.blockPosition();
    for (var pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
      var state = level.getBlockState(pos);
      if (state.getBlock().getExplosionResistance() < 10)
        explosion.getToBlow().add(pos.immutable());
    }
    for (var entity :
        level.getEntities(
            owner, new AABB(dynamite.position().add(-2, -2, -2), dynamite.position().add(2, 2, 2))))
      entity.hurt(level.damageSources().explosion(explosion), 20);
    explosion.finalizeExplosion(true);
    level.playSound(
        null,
        dynamite.blockPosition(),
        net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
        net.minecraft.sounds.SoundSource.BLOCKS,
        4,
        1);
    if (level instanceof net.minecraft.server.level.ServerLevel server)
      server.sendParticles(
          net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
          dynamite.getX(),
          dynamite.getY(),
          dynamite.getZ(),
          1,
          0,
          0,
          0,
          0);
  }

  private ExperimentalPointExplosion() {}
}
