package ic2.core.item.armor.jetpack;

import ic2.core.IC2;
import ic2.core.item.armor.ItemArmorJetpack;
import ic2.core.ref.Ic2SoundEvents;
import ic2.core.sound.Sound;
import ic2.core.util.StackUtil;
import ic2.mixin.ServerGamePacketListenerAccessor;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class JetpackLogic {
  private static Sound jetpackSound;
  private static SoundEvent jetpackSoundEvent;
  private static final Map<Player, Boolean> hoverToggleHeld = new WeakHashMap<>();

  public static boolean useJetpack(
      Player player, boolean hoverMode, IJetpack jetpack, ItemStack stack) {

    if (jetpack.getChargeLevel(stack) <= 0.0) {
      return false;
    }

    IBoostingJetpack iBoostingJetpack =
        jetpack instanceof IBoostingJetpack ? (IBoostingJetpack) jetpack : null;
    float power = jetpack.getPower(stack);
    float dropPercentage = jetpack.getDropPercentage(stack);
    if (jetpack.getChargeLevel(stack) <= dropPercentage) {
      power = (float) (power * (jetpack.getChargeLevel(stack) / dropPercentage));
    }

    Level world = player.level();
    Vec3 motion = player.getDeltaMovement();
    double motionX = motion.x;
    double motionY = motion.y;
    double motionZ = motion.z;

    if (IC2.keyboard.isForwardKeyDown(player)) {
      float thruster, boost;
      if (iBoostingJetpack != null) {
        thruster = iBoostingJetpack.getBaseThrust(stack, hoverMode);
        boost = iBoostingJetpack.getBoostThrust(player, stack, hoverMode);
      } else {
        thruster = hoverMode ? 1.0F : 0.15F;
        boost = 0.0F;
      }

      float forwarder = power * thruster * 2.0F;
      if (forwarder > 0.0F) {
        float yaw = player.getYRot() * (float) Math.PI / 180.0F;
        float forward = 0.4F * forwarder + boost;
        float thrust = forward * (0.02F + boost);
        motionX += -Math.sin(yaw) * thrust;
        motionZ += Math.cos(yaw) * thrust;
        if (boost != 0.0F && !player.onGround()) {
          iBoostingJetpack.useBoostPower(stack, boost);
        }
      }
    }

    int worldHeight = world.getMaxBuildHeight();
    int maxFlightHeight = (int) (worldHeight / jetpack.getWorldHeightDivisor(stack));
    double y = player.getY();
    if (y > maxFlightHeight - 25) {
      if (y > maxFlightHeight) {
        y = maxFlightHeight;
      }

      power = (float) (power * ((maxFlightHeight - y) / 25.0));
    }

    double prevMotionY = motionY;
    motionY = Math.min(motionY + power * 0.2F, 0.6F);
    if (hoverMode) {
      float maxHoverY = 0.0F;
      if (IC2.keyboard.isJumpKeyDown(player)) {
        maxHoverY += jetpack.getHoverMultiplier(stack, true);
        if (iBoostingJetpack != null) {
          maxHoverY *= iBoostingJetpack.getHoverBoost(player, stack, true);
        }
      }

      if (IC2.keyboard.isSneakKeyDown(player)) {
        maxHoverY -= jetpack.getHoverMultiplier(stack, false);
        if (iBoostingJetpack != null) {
          maxHoverY *= iBoostingJetpack.getHoverBoost(player, stack, false);
        }
      }

      if (motionY > maxHoverY) {
        motionY = maxHoverY;
        if (prevMotionY > motionY) {
          motionY = prevMotionY;
        }
      }
    }

    player.setDeltaMovement(motionX, motionY, motionZ);

    int consume = hoverMode ? 1 : 2;
    if (!player.onGround()) {
      jetpack.drainEnergy(stack, consume);
    }

    player.fallDistance = 0.0F;
    player.resetFallDistance();
    if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
      var connection = (ServerGamePacketListenerAccessor) serverPlayer.connection;
      connection.ic2$setFloatingTicks(0);
      connection.ic2$setClientFloating(false);
    }
    return true;
  }

  public static void onArmorTick(Level world, Player player, ItemStack stack, IJetpack jetpack) {
    boolean toggleHover = false;
    if (!world.isClientSide()) {
      boolean keysHeld =
          IC2.keyboard.isJumpKeyDown(player) && IC2.keyboard.isModeSwitchKeyDown(player);
      boolean previouslyHeld = Boolean.TRUE.equals(hoverToggleHeld.put(player, keysHeld));
      toggleHover = keysHeld && !previouslyHeld;
    }

    if (stack != null && jetpack.isJetpackActive(stack)) {
      boolean hoverMode = StackUtil.getOrCreateNbtData(stack).getBoolean("hover_mode");
      boolean jetpackUsed = false;

      if (toggleHover) {
        hoverMode = !hoverMode;
        boolean enabled = hoverMode;
        CustomData.update(
            DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("hover_mode", enabled));
        IC2.sideProxy.messagePlayer(
            player, hoverMode ? "ic2.hover_mode.enabled" : "ic2.hover_mode.disabled");
      }

      if (IC2.keyboard.isJumpKeyDown(player) || (hoverMode && !player.onGround())) {
        jetpackUsed = useJetpack(player, hoverMode, jetpack, stack);
      }

      updateJetpackSound(player, jetpackUsed, jetpack);

      if (jetpackUsed || toggleHover) {
        player.inventoryMenu.broadcastChanges();
      }
    } else {
      stopJetpackSound(player);
    }
  }

  public static void clearHoverToggle(Player player) {
    if (!player.level().isClientSide()) {
      hoverToggleHeld.remove(player);
    }
  }

  private static void updateJetpackSound(Player player, boolean jetpackUsed, IJetpack jetpack) {
    if (!IC2.sideProxy.isRendering() || player != IC2.sideProxy.getPlayerInstance()) {
      return;
    }

    if (jetpackUsed) {
      SoundEvent soundEvent =
          jetpack instanceof ItemArmorJetpack
              ? Ic2SoundEvents.ITEM_JETPACK_FIRE
              : Ic2SoundEvents.ITEM_JETPACK_LOOP;
      if (jetpackSound == null || jetpackSoundEvent != soundEvent) {
        stopJetpackSound(player);
        jetpackSoundEvent = soundEvent;
        jetpackSound =
            IC2.soundManager.createSound(
                player, soundEvent, SoundSource.PLAYERS, player, 1.0F, 1.0F);
        if (jetpackSound != null) {
          jetpackSound.play();
        }
      } else if (jetpackSound != null && !jetpackSound.isPlaying()) {
        // Resume only if the engine dropped the instance; avoid re-play every tick.
        jetpackSound.play();
      }
    } else {
      stopJetpackSound(player);
    }
  }

  /** Stops the local jetpack loop. Safe to call for non-local players (no-op) and on disconnect. */
  public static void stopJetpackSound(Player player) {
    if (!IC2.sideProxy.isRendering()) {
      return;
    }

    // Jetpack sound is a single client-global instance for the local player only.
    // Remote player ticks must not stop or clear it (multiplayer orphan / stuck-loop bug).
    Player local = IC2.sideProxy.getPlayerInstance();
    if (player != null && local != null && player != local) {
      return;
    }

    if (jetpackSound != null) {
      Object owner = local != null ? local : player;
      if (owner != null) {
        IC2.soundManager.removeSound(owner, jetpackSound);
      } else {
        jetpackSound.stop();
      }
      jetpackSound = null;
    }

    jetpackSoundEvent = null;
  }
}
