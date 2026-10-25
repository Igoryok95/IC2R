package ic2.core.experimental;

import ic2.core.IC2;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ExperimentalDynamite {
  public static final PlantedDynamiteBlock BLOCK = new PlantedDynamiteBlock();
  public static final EntityType<DynamiteProjectile> ENTITY =
      EntityType.Builder.<DynamiteProjectile>of(DynamiteProjectile::new, MobCategory.MISC)
          .sized(.25F, .25F)
          .clientTrackingRange(8)
          .updateInterval(1)
          .build("ic2:dynamite");
  public static final Item DYNAMITE = ExperimentalItems.register("dynamite", dynamite(false));
  public static final Item STICKY = ExperimentalItems.register("dynamite_sticky", dynamite(true));
  public static final Item REMOTE =
      ExperimentalItems.register(
          "remote",
          new Item(new Item.Properties().stacksTo(1)) {
            @Override
            public InteractionResult useOn(UseOnContext context) {
              var level = context.getLevel();
              var player = context.getPlayer();
              var pos = context.getClickedPos();
              if (player == null || !level.getBlockState(pos).is(BLOCK))
                return InteractionResult.PASS;
              if (!level.mayInteract(player, pos)
                  || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
                return InteractionResult.FAIL;
              if (!level.isClientSide) {
                var data = StackUtil.getOrCreateNbtData(context.getItemInHand());
                var list = data.getList("links", 10);
                String dimension = level.dimension().location().toString();
                int index = -1;
                for (int i = 0; i < list.size(); i++)
                  if (list.getCompound(i).getLong("pos") == pos.asLong()
                      && list.getCompound(i).getString("dimension").equals(dimension)) {
                    index = i;
                    break;
                  }
                boolean linked = level.getBlockState(pos).getValue(PlantedDynamiteBlock.LINKED);
                if (linked && index < 0) {
                  player.displayClientMessage(
                      Component.translatable("message.ic2.experimental.remote_wrong"), false);
                  return InteractionResult.SUCCESS;
                }
                if (index >= 0) list.remove(index);
                else {
                  var link = new CompoundTag();
                  link.putLong("pos", pos.asLong());
                  link.putString("dimension", dimension);
                  list.add(link);
                }
                data.put("links", list);
                StackUtil.setTag(context.getItemInHand(), data);
                level.setBlockAndUpdate(
                    pos, level.getBlockState(pos).setValue(PlantedDynamiteBlock.LINKED, !linked));
                player.displayClientMessage(
                    Component.translatable(
                        linked
                            ? "message.ic2.experimental.remote_unlinked"
                            : "message.ic2.experimental.remote_linked"),
                    true);
              }
              return InteractionResult.sidedSuccess(level.isClientSide);
            }

            @Override
            public InteractionResultHolder<ItemStack> use(
                Level level, Player player, InteractionHand hand) {
              ItemStack stack = player.getItemInHand(hand);
              if (!level.isClientSide) {
                var data = StackUtil.getOrCreateNbtData(stack);
                var links = data.getList("links", 10);
                var remaining = new ListTag();
                level.playSound(
                    null,
                    player.blockPosition(),
                    ic2.core.ref.Ic2SoundEvents.ITEM_DYNAMITE_REMOTE,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1,
                    1);
                for (int i = 0; i < links.size(); i++) {
                  var link = links.getCompound(i);
                  var pos = BlockPos.of(link.getLong("pos"));
                  if (!link.getString("dimension").equals(level.dimension().location().toString())
                      || !level.hasChunkAt(pos)) {
                    remaining.add(link.copy());
                    continue;
                  }
                  var state = level.getBlockState(pos);
                  if (state.is(BLOCK)
                      && state.getValue(PlantedDynamiteBlock.LINKED)
                      && level.mayInteract(player, pos)) BLOCK.prime(level, pos, player, 40);
                }
                data.put("links", remaining);
                StackUtil.setTag(stack, data);
              }
              return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            @Override
            public void appendHoverText(
                ItemStack stack, TooltipContext context, List<Component> text, TooltipFlag flag) {
              text.add(
                  Component.translatable(
                      "tooltip.ic2.experimental.remote_links",
                      StackUtil.getOrCreateNbtData(stack).getList("links", 10).size()));
            }
          });

  static {
    IC2.envProxy.registerBlock(IC2.getIdentifier("dynamite_placed"), BLOCK);
    IC2.envProxy.registerEntity(IC2.getIdentifier("dynamite"), ENTITY);
    for (Item item : List.of(DYNAMITE, STICKY))
      net.minecraft.world.level.block.DispenserBlock.registerBehavior(
          item,
          new net.minecraft.core.dispenser.DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(
                net.minecraft.core.dispenser.BlockSource source, ItemStack stack) {
              var direction =
                  source.state().getValue(net.minecraft.world.level.block.DispenserBlock.FACING);
              var entity = new DynamiteProjectile(ENTITY, source.level());
              entity.setItem(new ItemStack(item));
              entity.setPos(
                  Vec3.atCenterOf(source.pos())
                      .add(Vec3.atLowerCornerOf(direction.getNormal()).scale(.7)));
              entity.shoot(
                  direction.getStepX(), direction.getStepY() + .1, direction.getStepZ(), 1.1F, 6);
              source.level().addFreshEntity(entity);
              stack.shrink(1);
              return stack;
            }
          });
  }

  private static Item dynamite(boolean sticky) {
    return new Item(new Item.Properties().stacksTo(16)) {
      @Override
      public InteractionResult useOn(UseOnContext context) {
        if (sticky) return InteractionResult.PASS;
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos().relative(context.getClickedFace());
        if (player == null
            || !level.mayInteract(player, pos)
            || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
          return InteractionResult.FAIL;
        var state =
            BLOCK
                .defaultBlockState()
                .setValue(PlantedDynamiteBlock.FACING, context.getClickedFace());
        if (!level.isEmptyBlock(pos) || !state.canSurvive(level, pos))
          return InteractionResult.FAIL;
        if (!level.isClientSide) {
          level.setBlockAndUpdate(pos, state);
          context.getItemInHand().consume(1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
      }

      @Override
      public InteractionResultHolder<ItemStack> use(
          Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
          var projectile = new DynamiteProjectile(ENTITY, level);
          projectile.setOwner(player);
          projectile.setItem(new ItemStack(sticky ? STICKY : DYNAMITE));
          projectile.setPos(player.getX(), player.getEyeY() - .1, player.getZ());
          projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5F, 1);
          level.addFreshEntity(projectile);
          stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
      }
    };
  }

  public static void init() {}

  private ExperimentalDynamite() {}
}
