package ic2.core.item.block;

import ic2.core.block.transport.SteelHopperBlockEntity;
import ic2.core.item.ItemBlockIc2;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public final class SteelHopperItem extends ItemBlockIc2 {
  public SteelHopperItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, tooltip, flag);
    tooltip.add(
        Component.translatable(
            "ic2.tooltip.steel_hopper.rate", SteelHopperBlockEntity.ITEMS_PER_SECOND));
    tooltip.add(Component.translatable("ic2.tooltip.steel_hopper.redstone"));
  }
}
