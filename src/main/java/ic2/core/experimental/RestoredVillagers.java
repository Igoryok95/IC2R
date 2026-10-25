package ic2.core.experimental;

import com.google.common.collect.ImmutableSet;
import ic2.api.crops.Crops;
import ic2.core.item.ItemCropSeed;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Original profession identities/artwork with trades adapted to native Experimental items. */
@EventBusSubscriber(modid = "ic2")
public final class RestoredVillagers {
  private static final DeferredRegister<PoiType> POIS =
      DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, "ic2");
  private static final DeferredRegister<VillagerProfession> PROFESSIONS =
      DeferredRegister.create(Registries.VILLAGER_PROFESSION, "ic2");
  public static final Map<String, Supplier<VillagerProfession>> ALL = new LinkedHashMap<>();

  static {
    profession("electric", () -> Ic2Blocks.IRON_FURNACE);
    profession("nuclear", () -> Ic2Blocks.NUCLEAR_REACTOR);
    profession("crop", () -> Ic2Blocks.CROPMATRON);
    profession("demo", () -> Ic2Blocks.ITNT);
    profession("brewing", () -> ExperimentalBrewing.BARREL);
    profession("greg", () -> Ic2Blocks.INDUSTRIAL_WORKBENCH);
  }

  private static void profession(String name, Supplier<Block> block) {
    var poi =
        POIS.register(
            name,
            () ->
                new PoiType(
                    ImmutableSet.copyOf(block.get().getStateDefinition().getPossibleStates()),
                    2,
                    2));
    ALL.put(
        name,
        PROFESSIONS.register(
            name,
            () ->
                new VillagerProfession(
                    "ic2." + name,
                    holder -> holder.is(poi.getKey()),
                    holder -> holder.is(poi.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH)));
  }

  public static void register(IEventBus bus) {
    POIS.register(bus);
    PROFESSIONS.register(bus);
  }

  @SubscribeEvent
  public static void trades(VillagerTradesEvent event) {
    String role = null;
    for (var entry : ALL.entrySet())
      if (entry.getValue().get() == event.getType()) {
        role = entry.getKey();
        break;
      }
    if (role == null) return;
    var trades = event.getTrades();
    switch (role) {
      case "electric" -> {
        trades.get(1).add(sell(3, Ic2Items.CIRCUIT, 1));
        trades.get(1).add(sell(8, Ic2Items.MACHINE, 1));
        trades.get(2).add(buy(Ic2Items.COPPER_CABLE, 12, 1));
        trades.get(3).add(exchange(5, Ic2Items.CIRCUIT, 5, Ic2Items.ADVANCED_CIRCUIT, 2));
        trades.get(4).add(exchange(24, Ic2Items.MACHINE, 1, Ic2Items.SOLAR_GENERATOR, 1));
        trades.get(5).add(exchange(12, Ic2Items.CIRCUIT, 8, Ic2Items.ADVANCED_CIRCUIT, 4));
      }
      case "nuclear" -> {
        trades.get(1).add(sell(3, Ic2Items.REACTOR_COOLANT_CELL, 1));
        trades.get(1).add(sell(3, Ic2Items.HEAT_VENT, 1));
        trades.get(2).add(sell(4, Ic2Items.HEAT_EXCHANGER, 1));
        trades.get(2).add(sell(10, Ic2Items.DENSE_COPPER_PLATE, 1));
        trades.get(3).add(exchange(8, Ic2Items.HEAT_VENT, 1, Ic2Items.ADVANCED_HEAT_VENT, 1));
        trades
            .get(4)
            .add(exchange(12, Ic2Items.HEAT_EXCHANGER, 1, Ic2Items.ADVANCED_HEAT_EXCHANGER, 1));
        trades.get(5).add(sell(16, ExperimentalItems.CONTAINMENT_BOX, 1));
        trades.get(5).add(sell(3, ExperimentalItems.IODINE_TABLET, 16));
      }
      case "crop" -> {
        trades.get(1).add(sell(2, Ic2Items.FERTILIZER, 8));
        trades.get(2).add(sell(3, Ic2Items.GRIN_POWDER, 8));
        trades.get(4).add(sell(5, Ic2Items.HYDRATION_CELL, 1));
        trades.get(5).add(sell(6, Ic2Items.WEED_EX_CELL, 1));
        for (var crop : Crops.instance.getCrops()) {
          if (crop == Crops.weed || crop.getProperties().tier() > 9) continue;
          int tier = Math.clamp(crop.getProperties().tier() / 2 + 1, 1, 5);
          trades
              .get(tier)
              .add(
                  (entity, random) -> {
                    int minimum = tier >= 4 ? 5 : 0;
                    ItemStack seed =
                        ItemCropSeed.generateItemStackFromValues(
                            crop,
                            minimum + random.nextInt(minimum == 5 ? 8 : 9),
                            minimum + random.nextInt(minimum == 5 ? 8 : 9),
                            minimum + random.nextInt(minimum == 5 ? 8 : 9),
                            4);
                    return new MerchantOffer(
                        new ItemCost(Items.EMERALD, tier + 4), seed, 4, tier * 5, .05F);
                  });
        }
      }
      case "demo" -> {
        trades.get(1).add(buy(Items.TNT, 3, 1));
        trades.get(1).add(sell(2, Items.GUNPOWDER, 8));
        trades.get(2).add(buy(Ic2Items.ITNT, 3, 2));
        trades.get(3).add(sell(3, ExperimentalDynamite.DYNAMITE, 8));
        trades
            .get(4)
            .add(exchange(2, ExperimentalDynamite.DYNAMITE, 8, ExperimentalDynamite.STICKY, 8));
        trades.get(5).add(sell(12, ExperimentalDynamite.REMOTE, 1));
      }
      case "brewing" -> {
        trades.get(1).add(sell(1, Ic2Items.EMPTY_MUG, 1));
        trades.get(1).add(sell(4, ExperimentalBrewing.BARREL_ITEM, 1));
        trades.get(2).add(sell(2, Ic2Items.HOPS, 8));
        trades.get(2).add(buy(Ic2Items.COFFEE_BEANS, 16, 1));
        trades.get(4).add(buy(Ic2Items.COFFEE_MUG, 1, 1));
        trades.get(5).add(buyDrink((100 << 7) | 2, 2));
        for (int quality = 1; quality <= 4; quality++)
          for (int solids = 0; solids <= 6; solids++)
            for (int hops = 0; hops <= 6; hops++) {
              int value = (quality << 13) | (hops << 10) | (solids << 7) | 1;
              trades.get(quality + 1).add(buyDrink(value, quality == 4 ? 2 : 1));
            }
      }
      case "greg" -> {
        trades.get(1).add(sell(1, Ic2Items.CF_POWDER, 8));
        trades.get(2).add(buy(Ic2Items.CF_POWDER, 16, 1));
        trades.get(3).add(sell(8, Ic2Items.METER, 1));
        trades.get(3).add(sell(12, Ic2Items.FOAM_SPRAYER, 1));
        trades.get(4).add(exchange(24, Ic2Items.ADVANCED_CIRCUIT, 1, Ic2Items.JETPACK_ELECTRIC, 1));
        trades.get(5).add(exchange(16, Ic2Items.DRILL, 1, Ic2Items.DIAMOND_DRILL, 1));
        trades.get(5).add(sell(24, Ic2Items.CHAINSAW, 1));
        trades.get(5).add(exchange(16, Ic2Items.WRENCH, 1, Ic2Items.ELECTRIC_WRENCH, 1));
      }
      default -> throw new IllegalStateException("Unknown IC2 profession " + role);
    }
  }

  public static VillagerTrades.ItemListing sell(int emeralds, ItemLike output, int count) {
    return (entity, random) ->
        new MerchantOffer(
            new ItemCost(Items.EMERALD, emeralds), new ItemStack(output, count), 12, 5, .05F);
  }

  public static VillagerTrades.ItemListing buy(ItemLike input, int count, int emeralds) {
    return (entity, random) ->
        new MerchantOffer(
            new ItemCost(input, count), new ItemStack(Items.EMERALD, emeralds), 12, 5, .05F);
  }

  public static VillagerTrades.ItemListing exchange(
      int emeralds, ItemLike input, int inputCount, ItemLike output, int count) {
    return (entity, random) ->
        new MerchantOffer(
            new ItemCost(Items.EMERALD, emeralds),
            Optional.of(new ItemCost(input, inputCount)),
            new ItemStack(output, count),
            4,
            15,
            .05F);
  }

  private static VillagerTrades.ItemListing buyDrink(int value, int emeralds) {
    var drink = ExperimentalBrewing.drink(value);
    var predicate =
        DataComponentPredicate.builder()
            .expect(DataComponents.CUSTOM_DATA, drink.get(DataComponents.CUSTOM_DATA))
            .build();
    return (entity, random) ->
        new MerchantOffer(
            new ItemCost(ExperimentalBrewing.BOOZE_MUG.builtInRegistryHolder(), 1, predicate),
            new ItemStack(Items.EMERALD, emeralds),
            8,
            10,
            .05F);
  }

  private RestoredVillagers() {}
}
