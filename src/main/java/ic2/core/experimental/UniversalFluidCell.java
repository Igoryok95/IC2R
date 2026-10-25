package ic2.core.experimental;

import ic2.core.fluid.Ic2FluidStack;
import ic2.core.fluid.StandardFluidItem;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/** A reusable Experimental cell; partial contents remain in the same item. */
public final class UniversalFluidCell extends Item implements StandardFluidItem {
  public UniversalFluidCell() {
    super(new Properties().stacksTo(1));
    net.minecraft.world.level.block.DispenserBlock.registerBehavior(
        this, net.neoforged.neoforge.fluids.DispenseFluidContainer.getInstance());
  }

  @Override
  public int getCapacityMb(ItemStack stack) {
    return 1000;
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    var player = context.getPlayer();
    if (player == null) return InteractionResult.PASS;
    var level = context.getLevel();
    if (!level.mayInteract(player, context.getClickedPos())
        || !player.mayUseItemAt(
            context.getClickedPos(), context.getClickedFace(), context.getItemInHand()))
      return InteractionResult.FAIL;
    if (level.getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
            context.getClickedPos(),
            context.getClickedFace())
        == null) return InteractionResult.PASS;
    if (level.isClientSide) return InteractionResult.SUCCESS;
    if (FluidUtil.interactWithFluidHandler(
        player, context.getHand(), level, context.getClickedPos(), context.getClickedFace()))
      return InteractionResult.SUCCESS;
    return InteractionResult.PASS;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
    if (hit.getType() != HitResult.Type.BLOCK
        || !level.mayInteract(player, hit.getBlockPos())
        || !player.mayUseItemAt(hit.getBlockPos(), hit.getDirection(), stack))
      return InteractionResultHolder.pass(stack);
    if (level.isClientSide) return InteractionResultHolder.success(stack);
    var stored = getFluidStack(stack);
    var state = level.getBlockState(hit.getBlockPos());
    var fluid = state.getFluidState();
    if (stored.isEmpty() && fluid.isSource() && state.getBlock() instanceof BucketPickup pickup) {
      var value = Ic2FluidStack.create(fluid.getType(), 1000);
      if (fillMb(stack, value, true, null) == 1000
          && !pickup.pickupBlock(player, level, hit.getBlockPos(), state).isEmpty()) {
        fillMb(stack, value, false, null);
        player.containerMenu.broadcastChanges();
        return InteractionResultHolder.success(stack);
      }
    } else if (!stored.isEmpty()
        && stored.getAmountMb() >= 1000
        && stored.getFluid().getBucket() instanceof BucketItem bucket) {
      var target =
          state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlockContainer
              ? hit.getBlockPos()
              : hit.getBlockPos().relative(hit.getDirection());
      if (level.mayInteract(player, target)
          && player.mayUseItemAt(target, hit.getDirection(), stack)
          && bucket.emptyContents(player, level, target, hit)) {
        drainMb(stack, 1000, false, null);
        player.containerMenu.broadcastChanges();
        return InteractionResultHolder.success(stack);
      }
    }
    return InteractionResultHolder.pass(stack);
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    var fluid = getFluidStack(stack);
    tooltip.add(
        fluid.isEmpty()
            ? Component.translatable("ic2.item.fluid_container.empty")
            : Component.translatable(
                "ic2.item.fluid_container.with_fluid",
                Component.translatable(fluid.getFluidTypeKey()),
                fluid.getAmountMb()));
  }
}
