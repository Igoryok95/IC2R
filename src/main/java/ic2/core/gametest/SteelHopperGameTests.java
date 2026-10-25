package ic2.core.gametest;

import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.transport.SteelHopperBlockEntity;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class SteelHopperGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  @GameTest(template = EMPTY, timeoutTicks = 60)
  public static void steelHopperMovesFourTimesAsFastAsVanilla(GameTestHelper h) {
    var steel = hopper(h, new BlockPos(0, 1, 1), Ic2Blocks.STEEL_HOPPER, Direction.DOWN);
    var vanilla = hopper(h, new BlockPos(2, 1, 1), Blocks.HOPPER, Direction.DOWN);
    var steelOutput = chest(h, new BlockPos(0, 0, 1));
    var vanillaOutput = chest(h, new BlockPos(2, 0, 1));
    steel.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
    vanilla.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
    h.runAfterDelay(
        8,
        () -> {
          int steelStart = count(steelOutput), vanillaStart = count(vanillaOutput);
          h.runAfterDelay(
              16,
              () -> {
                h.assertTrue(
                    count(steelOutput) - steelStart == 8,
                    "steel transfers eight items in sixteen ticks");
                h.assertTrue(
                    count(vanillaOutput) - vanillaStart == 2,
                    "ordinary hoppers retain their eight-tick interval");
                h.assertTrue(
                    count(steel) + count(steelOutput) == 64,
                    "steel transfer cannot duplicate or lose items");
                h.succeed();
              });
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 60)
  public static void steelHopperChainKeepsItsTransferRate(GameTestHelper h) {
    var output = chest(h, new BlockPos(0, 1, 1));
    var lower = hopper(h, new BlockPos(1, 1, 1), Ic2Blocks.STEEL_HOPPER, Direction.WEST);
    var upper = hopper(h, new BlockPos(1, 2, 1), Ic2Blocks.STEEL_HOPPER, Direction.DOWN);
    var source = chest(h, new BlockPos(1, 3, 1));
    source.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
    h.runAfterDelay(
        10,
        () -> {
          int before = count(output);
          h.runAfterDelay(
              16,
              () -> {
                h.assertTrue(
                    count(output) - before == 8, "a steel chain preserves ten items per second");
                h.assertTrue(
                    count(source) + count(upper) + count(lower) + count(output) == 64,
                    "chain transfers conserve inventory contents");
                h.succeed();
              });
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 70)
  public static void steelHopperRetainsVanillaLockingInventoryAndSaving(GameTestHelper h) {
    var relative = new BlockPos(1, 1, 1);
    var hopper =
        (SteelHopperBlockEntity) hopper(h, relative, Ic2Blocks.STEEL_HOPPER, Direction.DOWN);
    var output = chest(h, relative.below());
    h.setBlock(relative.east(), Blocks.REDSTONE_BLOCK);
    hopper.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
    var level = h.getLevel();
    var pos = h.absolutePos(relative);
    var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
    h.assertTrue(
        handler != null && handler.getSlots() == 5, "mod item pipes see all five hopper slots");
    h.assertTrue(
        handler.insertItem(1, new ItemStack(Items.COPPER_INGOT, 5), true).isEmpty()
            && count(hopper) == 32,
        "simulated pipe insertion does not change inventory");
    handler.insertItem(1, new ItemStack(Items.COPPER_INGOT, 5), false);
    h.assertTrue(
        handler.extractItem(1, 3, false).getCount() == 3 && count(hopper) == 34,
        "real capability transfers preserve item counts");
    h.assertTrue(
        AbstractContainerMenu.getRedstoneSignalFromContainer(hopper) > 0,
        "comparator reads the hopper inventory");
    var player = h.makeMockServerPlayerInLevel();
    var menu = hopper.createMenu(1, player.getInventory(), player);
    h.assertTrue(
        menu instanceof HopperMenu && menu.slots.size() == 41,
        "the native five-slot hopper menu is used");
    hopper.setCooldown(2);
    var saved = hopper.saveWithFullMetadata(level.registryAccess());
    saved.putString(
        "CustomName",
        Component.Serializer.toJson(
            Component.translatable("block.ic2.steel_hopper"), level.registryAccess()));
    h.assertTrue(
        saved.getString("id").equals("ic2:steel_hopper") && saved.getInt("TransferCooldown") == 2,
        "the custom type and transfer cooldown are saved");
    var restored =
        (SteelHopperBlockEntity)
            BlockEntity.loadStatic(pos, hopper.getBlockState(), saved, level.registryAccess());
    h.assertTrue(
        restored != null && count(restored) == 34 && restored.getCustomName() != null,
        "all five slots and the custom name survive reloading");
    level.removeBlockEntity(pos);
    level.setBlockEntity(restored);
    h.runAfterDelay(
        10,
        () -> {
          h.assertTrue(
              !level.getBlockState(pos).getValue(HopperBlock.ENABLED)
                  && count(output) == 0
                  && count(restored) == 34,
              "redstone blocks transfers after reloading");
          h.setBlock(relative.east(), Blocks.AIR);
          h.runAfterDelay(
              12,
              () -> {
                h.assertTrue(
                    count(output) > 0 && count(output) + count(restored) == 34,
                    "removing redstone resumes transfer");
                level.destroyBlock(pos, true);
                var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
                int blocks =
                    drops.stream()
                        .filter(e -> e.getItem().is(Ic2Items.STEEL_HOPPER))
                        .mapToInt(e -> e.getItem().getCount())
                        .sum();
                int contents =
                    drops.stream()
                        .filter(e -> !e.getItem().is(Ic2Items.STEEL_HOPPER))
                        .mapToInt(e -> e.getItem().getCount())
                        .sum();
                h.assertTrue(
                    drops.stream()
                        .filter(e -> e.getItem().is(Ic2Items.STEEL_HOPPER))
                        .allMatch(
                            e ->
                                e.getItem()
                                    .has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)),
                    "the dropped hopper retains its custom name like a vanilla hopper");
                h.assertTrue(
                    blocks == 1 && contents + count(output) == 34,
                    "breaking drops one hopper and every remaining inventory item");
                h.succeed();
              });
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 60)
  public static void steelHopperCollectsDroppedItemsAndUsesSteelRecipe(GameTestHelper h) {
    var hopper = hopper(h, new BlockPos(1, 1, 1), Ic2Blocks.STEEL_HOPPER, Direction.DOWN);
    var output = chest(h, new BlockPos(1, 0, 1));
    var pos = h.absolutePos(new BlockPos(1, 1, 1));
    var entity =
        new ItemEntity(
            h.getLevel(),
            pos.getX() + .5,
            pos.getY() + .8,
            pos.getZ() + .5,
            new ItemStack(Items.IRON_INGOT, 4));
    entity.setDeltaMovement(0, 0, 0);
    h.getLevel().addFreshEntity(entity);
    var s = new ItemStack(Ic2Items.STEEL_INGOT);
    var advancement =
        h.getLevel()
            .getServer()
            .getAdvancements()
            .get(ic2.core.IC2.getIdentifier("recipes/shaped/steel_hopper"));
    var unlock =
        (net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance)
            advancement.value().criteria().get("has_steel").triggerInstance();
    h.assertTrue(
        unlock.items().getFirst().test(s)
            && !unlock.items().getFirst().test(new ItemStack(Items.STONE)),
        "the loaded recipe-book criterion requires steel rather than any inventory item");
    var input =
        CraftingInput.of(
            3,
            3,
            List.of(
                s.copy(),
                ItemStack.EMPTY,
                s.copy(),
                s.copy(),
                new ItemStack(Items.HOPPER),
                s.copy(),
                ItemStack.EMPTY,
                s.copy(),
                ItemStack.EMPTY));
    var recipe =
        h.getLevel()
            .getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
            .orElseThrow();
    var result = recipe.value().assemble(input, h.getLevel().registryAccess());
    h.assertTrue(
        result.is(Ic2Items.STEEL_HOPPER)
            && result.getCount() == 1
            && result.getRarity() == Rarity.UNCOMMON,
        "five steel ingots and a vanilla hopper craft one yellow-named steel hopper");
    h.succeedWhen(
        () ->
            h.assertTrue(
                !entity.isAlive() && count(hopper) + count(output) == 4,
                "native item-entity collection does not lose or duplicate the dropped stack"));
  }

  @GameTest(template = EMPTY, timeoutTicks = 120)
  public static void steelHopperSuppliesBothHotInductionInputsAndPreservesXp(GameTestHelper h) {
    var furnacePos = new BlockPos(1, 1, 1);
    h.setBlock(furnacePos, Ic2Blocks.INDUCTION_FURNACE);
    var furnace = (TileEntityInduction) h.getBlockEntity(furnacePos);
    furnace.getComponent(ic2.core.block.comp.Energy.class).addEnergy(10000);
    furnace.heat = 10000;
    furnace.setManualWarmup(true);
    var hopper = hopper(h, furnacePos.above(), Ic2Blocks.STEEL_HOPPER, Direction.DOWN);
    var source = chest(h, furnacePos.above(2));
    source.setItem(0, new ItemStack(Items.RAW_IRON, 64));
    h.runAfterDelay(
        20,
        () ->
            h.assertTrue(
                !furnace.inputSlotA.isEmpty()
                    && !furnace.inputSlotB.isEmpty()
                    && Math.abs(
                            furnace.inputSlotA.get().getCount()
                                - furnace.inputSlotB.get().getCount())
                        <= 1,
                "steel hopper maintains balanced nonempty induction inputs"));
    h.runAfterDelay(
        80,
        () -> {
          h.assertTrue(
              furnace.outputSlotA.get().getCount() + furnace.outputSlotB.get().getCount() >= 10,
              "the fully heated furnace keeps processing both lanes");
          h.assertTrue(
              count(source) + count(hopper) + count(furnace) == 64,
              "smelting automation conserves all items");
          h.assertTrue(
              furnace.saveWithFullMetadata(h.getLevel().registryAccess()).getDouble("xp") > 0,
              "automated smelting retains collectible furnace XP");
          h.succeed();
        });
  }

  private static HopperBlockEntity hopper(
      GameTestHelper h, BlockPos pos, Block block, Direction facing) {
    h.setBlock(pos, block.defaultBlockState().setValue(HopperBlock.FACING, facing));
    return (HopperBlockEntity) h.getBlockEntity(pos);
  }

  private static Container chest(GameTestHelper h, BlockPos pos) {
    h.setBlock(pos, Blocks.CHEST);
    return (Container) h.getBlockEntity(pos);
  }

  private static int count(Container container) {
    int total = 0;
    for (int slot = 0; slot < container.getContainerSize(); slot++)
      total += container.getItem(slot).getCount();
    return total;
  }
}
