package ic2.core.experimental;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ExperimentalClient {
  @SubscribeEvent
  public static void menus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
    event.register(ic2.core.ref.Ic2ScreenHandlers.CONTAINMENT_BOX, ContainmentScreen::new);
  }

  @SubscribeEvent
  public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
    event.enqueueWork(
        () ->
            net.minecraft.client.renderer.item.ItemProperties.register(
                ExperimentalItems.FLUID_CELL,
                ic2.core.IC2.getIdentifier("filled"),
                (stack, level, entity, seed) ->
                    ((UniversalFluidCell) stack.getItem()).getFluidStack(stack).isEmpty() ? 0 : 1));
  }

  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(ExperimentalDynamite.ENTITY, ThrownItemRenderer::new);
    event.registerBlockEntityRenderer(ExperimentalPipes.TYPE, PipeCoverRenderer::new);
  }

  @SubscribeEvent
  public static void blocks(RegisterColorHandlersEvent.Block event) {
    event.register(
        (state, level, pos, tint) -> ((ExperimentalPipeBlock) state.getBlock()).color,
        ExperimentalPipes.BLOCKS.values().toArray(net.minecraft.world.level.block.Block[]::new));
  }

  @SubscribeEvent
  public static void items(RegisterColorHandlersEvent.Item event) {
    event.register(
        (stack, tint) -> {
          if (tint == 0) return -1;
          if (stack.is(ExperimentalPipes.PUMP_LV)) return 0xFF00FF00;
          if (stack.is(ExperimentalPipes.PUMP_MV)) return 0xFFFFFF00;
          var fluid = ((UniversalFluidCell) stack.getItem()).getFluidStack(stack);
          return fluid.isEmpty()
              ? -1
              : net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(
                      fluid.getFluid())
                  .getTintColor();
        },
        ExperimentalItems.FLUID_CELL,
        ExperimentalPipes.PUMP_LV,
        ExperimentalPipes.PUMP_MV);
    event.register(
        (stack, tint) ->
            0xFF000000 | ((ExperimentalPipeBlock) ((BlockItem) stack.getItem()).getBlock()).color,
        ExperimentalPipes.BLOCKS.values().stream()
            .map(net.minecraft.world.level.block.Block::asItem)
            .toArray(net.minecraft.world.item.Item[]::new));
  }

  private ExperimentalClient() {}
}
