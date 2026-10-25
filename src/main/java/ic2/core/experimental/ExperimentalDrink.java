package ic2.core.experimental;

import ic2.core.ref.Ic2Items;
import ic2.core.util.StackUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Experimental beer/rum effects, including the original escalating intoxication. */
public final class ExperimentalDrink extends Item {
  private static final int[] DURATION = {300, 600, 900, 1200, 1600, 2000, 2400};
  private static final float[] INTENSITY = {.4F, .75F, 1, 1.5F, 2};

  public ExperimentalDrink() {
    super(new Properties().stacksTo(1));
  }

  @Override
  public UseAnim getUseAnimation(ItemStack stack) {
    return UseAnim.DRINK;
  }

  @Override
  public int getUseDuration(ItemStack stack, LivingEntity entity) {
    return 32;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(player.getItemInHand(hand));
  }

  @Override
  public Component getName(ItemStack stack) {
    int value = StackUtil.getOrCreateNbtData(stack).getInt("value");
    if ((value & 3) == 2) return Component.translatable("item.ic2.experimental.rum");
    int quality = Math.min(5, value >>> 13 & 7);
    if (quality == 5) return Component.translatable("item.ic2.experimental.beer.quality.5");
    var name = Component.empty();
    int solids = Math.min(6, value >>> 7 & 7), hops = Math.min(6, value >>> 10 & 7);
    if (solids != 3)
      name.append(Component.translatable("item.ic2.experimental.beer.solids." + solids));
    if (hops != 3) name.append(Component.translatable("item.ic2.experimental.beer.hops." + hops));
    return name.append(Component.translatable("item.ic2.experimental.beer.quality." + quality));
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
    int value = StackUtil.getOrCreateNbtData(stack).getInt("value");
    if (!level.isClientSide) {
      if ((value & 3) == 1) {
        int quality = Math.min(5, value >>> 13 & 7);
        int solids = Math.min(6, value >>> 7 & 7), alcohol = Math.min(6, value >>> 10 & 7);
        if (quality == 5) badDrink(entity);
        else {
          int duration = DURATION[solids];
          float intensity = INTENSITY[quality];
          if (entity instanceof Player player) player.getFoodData().eat(6 - alcohol, solids * .15F);
          int max = (int) (intensity * alcohol * .5F);
          int previous =
              entity.hasEffect(MobEffects.DIG_SLOWDOWN)
                  ? entity.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()
                  : -1;
          amplify(entity, MobEffects.DIG_SLOWDOWN, max, intensity, duration);
          if (previous >= 0) amplify(entity, MobEffects.DAMAGE_BOOST, max, intensity, duration);
          if (previous > 0)
            amplify(entity, MobEffects.MOVEMENT_SLOWDOWN, max / 2, intensity, duration);
          if (previous > 1)
            amplify(entity, MobEffects.DAMAGE_RESISTANCE, max - 1, intensity, duration);
          if (previous > 2) amplify(entity, MobEffects.CONFUSION, 0, intensity, duration);
          if (previous > 3)
            entity.addEffect(new MobEffectInstance(MobEffects.HARM, 1, level.random.nextInt(3)));
        }
      } else if ((value & 3) == 2) {
        if ((value >>> 7 & 127) < 100) badDrink(entity);
        else {
          int previous =
              entity.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                  ? entity.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()
                  : -1;
          amplify(entity, MobEffects.FIRE_RESISTANCE, 0, 2, 600);
          amplify(entity, MobEffects.DAMAGE_RESISTANCE, 2, 2, 600);
          if (previous >= 0) amplify(entity, MobEffects.BLINDNESS, 0, 2, 600);
          if (previous >= 1) amplify(entity, MobEffects.CONFUSION, 0, 2, 600);
        }
      }
    }
    if (entity instanceof Player player && player.getAbilities().instabuild) return stack;
    stack.shrink(1);
    ItemStack empty = new ItemStack(Ic2Items.EMPTY_MUG);
    if (stack.isEmpty()) return empty;
    if (entity instanceof Player player && !player.getInventory().add(empty))
      player.drop(empty, false);
    return stack;
  }

  private static void amplify(
      LivingEntity entity, Holder<MobEffect> effect, int max, float intensity, int duration) {
    var old = entity.getEffect(effect);
    if (old == null) {
      entity.addEffect(new MobEffectInstance(effect, duration, 0));
      return;
    }
    int addition = Math.max(0, (int) (duration * (1 + intensity * 2) - old.getDuration()) / 2);
    duration = old.getDuration() + Math.min(duration, addition);
    int amplifier = old.getAmplifier();
    if (amplifier < max) amplifier++;
    entity.addEffect(new MobEffectInstance(effect, duration, amplifier));
  }

  private static void badDrink(LivingEntity entity) {
    switch (entity.getRandom().nextInt(6)) {
      case 1 -> entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200));
      case 2 -> entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 2400));
      case 3 -> entity.addEffect(new MobEffectInstance(MobEffects.POISON, 2400));
      case 4 -> entity.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
      case 5 ->
          entity.addEffect(
              new MobEffectInstance(MobEffects.HARM, 1, entity.getRandom().nextInt(4)));
      default -> {}
    }
  }

  private static void instantHarm(LivingEntity entity, int level) {
    MobEffects.HARM.value().applyInstantenousEffect(null, null, entity, level, .25);
  }
}
