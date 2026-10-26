package ic2.integration.jade;

import ic2.core.IC2;
import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

@WailaPlugin
public final class Ic2JadePlugin implements IWailaPlugin {
  private static final Provider PROVIDER = new Provider();

  @Override
  public void register(IWailaCommonRegistration registration) {
    registration.registerBlockDataProvider(PROVIDER, Ic2TileEntity.class);
  }

  @Override
  public void registerClient(IWailaClientRegistration registration) {
    registration.registerBlockComponent(PROVIDER, Ic2TileEntityBlock.class);
  }

  public static final class Provider
      implements IServerDataProvider<BlockAccessor>, IComponentProvider<BlockAccessor> {
    @Override
    public ResourceLocation getUid() {
      return IC2.getIdentifier("machine_status");
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
      if (accessor.getBlockEntity() instanceof Ic2TileEntity tile)
        data.put("ic2_status", MachineStatusData.collect(tile));
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
      if (!accessor.getServerData().contains("ic2_status")) return;
      var data = accessor.getServerData().getCompound("ic2_status");
      if (data.contains("energy")) {
        bar(
            tooltip,
            data.getDouble("energy") / Math.max(1, data.getDouble("capacity")),
            Component.translatable(
                "jade.ic2.energy",
                number(data.getDouble("energy")),
                number(data.getDouble("capacity"))),
            0xFF318BD1);
        tooltip.add(Component.translatable("jade.ic2.tier", data.getInt("tier")));
      }
      if (data.contains("input"))
        tooltip.add(
            Component.translatable(
                "jade.ic2.flow",
                number(data.getDouble("input")),
                number(data.getDouble("output"))));
      if (data.contains("ratedOutput"))
        tooltip.add(
            Component.translatable("jade.ic2.rated_output", number(data.getDouble("ratedOutput"))));
      if (data.contains("heat"))
        bar(
            tooltip,
            data.getDouble("heat"),
            Component.translatable(
                "jade.ic2.heat", MachineStatusData.percent(data.getDouble("heat") * 100)),
            0xFFE06432);
      if (data.contains("progress"))
        bar(
            tooltip,
            data.getDouble("progress"),
            Component.translatable(
                "jade.ic2.progress", MachineStatusData.percent(data.getDouble("progress") * 100)),
            0xFF53A653);
      if (data.contains("reactorHeat")) {
        bar(
            tooltip,
            data.getDouble("reactorHeat") / Math.max(1, data.getDouble("reactorMaxHeat")),
            Component.translatable(
                "jade.ic2.reactor_heat",
                number(data.getDouble("reactorHeat")),
                number(data.getDouble("reactorMaxHeat"))),
            0xFFE06432);
        tooltip.add(
            Component.translatable(
                "jade.ic2.rated_output", number(data.getDouble("reactorOutput"))));
      }
      if (data.contains("active"))
        tooltip.add(
            Component.translatable(
                "jade.ic2.status",
                Component.translatable(
                    data.getBoolean("active") ? "jade.ic2.running" : "jade.ic2.idle")));
    }

    private static void bar(ITooltip tooltip, double ratio, Component text, int color) {
      var helper = IElementHelper.get();
      tooltip.add(
          helper.progress(
              (float) Math.clamp(ratio, 0, 1),
              text,
              helper.progressStyle().color(color).textColor(0xFFFFFFFF),
              BoxStyle.getNestedBox(),
              true));
    }

    private static String number(double value) {
      return String.format(Locale.ROOT, "%,.2f", value).replace(',', ' ').replaceAll("\\.?0+$", "");
    }
  }
}
