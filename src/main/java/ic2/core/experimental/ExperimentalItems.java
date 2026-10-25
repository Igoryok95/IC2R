package ic2.core.experimental;

import ic2.core.IC2;
import ic2.core.Ic2ItemGroupType;
import ic2.core.Ic2Potion;
import ic2.core.block.wiring.tileentity.TileEntityElectricBlock;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Restored Experimental utilities; no Classic materials or electrical tiers. */
public final class ExperimentalItems {
  public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
  public static final Item SOLAR_HELMET =
      register("solar_helmet", new ExperimentalChargingArmor(true));
  public static final Item STATIC_BOOTS =
      register("static_boots", new ExperimentalChargingArmor(false));
  public static final Item CONTAINMENT_BOX = register("containment_box", new ContainmentBox());
  public static final Item FLUID_CELL = register("fluid_cell", new UniversalFluidCell());
  public static final Item IODINE_TABLET =
      register(
          "iodine_tablet",
          new Item(new Item.Properties()) {
            @Override
            public InteractionResultHolder<ItemStack> use(
                Level level, Player player, InteractionHand hand) {
              ItemStack stack = player.getItemInHand(hand);
              var effect = player.getEffect(Ic2Potion.radiationHolder());
              if (effect == null || effect.getDuration() < 20)
                return InteractionResultHolder.pass(stack);
              if (!level.isClientSide) {
                int count = Math.min(stack.getCount(), effect.getDuration() / 20);
                player.removeEffect(Ic2Potion.radiationHolder());
                int remaining = (effect.getDuration() / 20 - count) * 20;
                if (remaining > 0) Ic2Potion.radiation.applyTo(player, remaining, 0);
                stack.consume(count, player);
                level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.GENERIC_EAT,
                    SoundSource.PLAYERS,
                    1,
                    1);
              }
              return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
          });
  public static final Item MFSU_UPGRADE_KIT =
      register(
          "mfsu_upgrade_kit",
          new Item(new Item.Properties()) {
            @Override
            public InteractionResult useOn(UseOnContext context) {
              var level = context.getLevel();
              var pos = context.getClickedPos();
              var state = level.getBlockState(pos);
              var target =
                  state.is(Ic2Blocks.MFE)
                      ? Ic2Blocks.MFSU
                      : state.is(Ic2Blocks.MFE_CHARGEPAD) ? Ic2Blocks.MFSU_CHARGEPAD : null;
              if (target == null
                  || !(level.getBlockEntity(pos) instanceof TileEntityElectricBlock original))
                return InteractionResult.PASS;
              Player player = context.getPlayer();
              if (player == null
                  || !level.mayInteract(player, pos)
                  || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
                return InteractionResult.FAIL;
              if (!level.isClientSide) {
                var saved = original.saveWithFullMetadata(level.registryAccess());
                var replacement = target.defaultBlockState();
                for (var property : state.getProperties()) {
                  if (replacement.hasProperty(property))
                    replacement = copyProperty(replacement, state, property);
                }
                original.clearContent();
                if (!level.setBlockAndUpdate(pos, replacement)) {
                  original.loadWithComponents(saved, level.registryAccess());
                  return InteractionResult.FAIL;
                }
                if (!(level.getBlockEntity(pos) instanceof TileEntityElectricBlock upgraded))
                  throw new IllegalStateException("Missing upgraded storage entity");
                upgraded.loadWithComponents(saved, level.registryAccess());
                upgraded.setChanged();
                context.getItemInHand().consume(1, player);
                player.displayClientMessage(
                    Component.translatable("message.ic2.experimental.storage_upgraded"), true);
              }
              return InteractionResult.sidedSuccess(level.isClientSide);
            }
          });

  private static <T extends Comparable<T>>
      net.minecraft.world.level.block.state.BlockState copyProperty(
          net.minecraft.world.level.block.state.BlockState target,
          net.minecraft.world.level.block.state.BlockState source,
          net.minecraft.world.level.block.state.properties.Property<T> property) {
    return target.setValue(property, source.getValue(property));
  }

  public static Item register(String name, Item item) {
    ITEMS.put(name, item);
    IC2.envProxy.registerItem(IC2.getIdentifier(name), item);
    Ic2Items.CREATIVE_TAB_ITEMS
        .computeIfAbsent(Ic2ItemGroupType.TOOLS_AND_UTILITIES, key -> new ArrayList<>())
        .add(() -> item);
    return item;
  }

  public static void init() {}

  private ExperimentalItems() {}
}
