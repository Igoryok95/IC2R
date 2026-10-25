package ic2.core.experimental;

import ic2.api.item.ElectricItem;
import ic2.core.block.wiring.tileentity.TileEntityElectricBlock;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class ExperimentalGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  @GameTest(template = EMPTY)
  public static void stickyDynamiteStaysAtImpactAndFallsWhenSupportBreaks(GameTestHelper h) {
    var block = new BlockPos(1, 1, 1);
    h.setBlock(block, net.minecraft.world.level.block.Blocks.STONE);
    var dynamite = new DynamiteProjectile(ExperimentalDynamite.ENTITY, h.getLevel());
    dynamite.setItem(new ItemStack(ExperimentalDynamite.STICKY));
    var anchor = h.absoluteVec(new Vec3(1.5, 1.5, 1));
    dynamite.setPos(anchor.add(0, 0, -.2));
    dynamite.setDeltaMovement(0, 0, 1);
    dynamite.onHitBlock(
        new BlockHitResult(
            anchor, net.minecraft.core.Direction.NORTH, h.absolutePos(block), false));
    var stuck = dynamite.position();
    for (int i = 0; i < 5; i++) dynamite.tick();
    h.assertTrue(
        dynamite.position().distanceToSqr(stuck) < 1e-10 && dynamite.isNoGravity(),
        "sticky impact stays anchored across ticks");
    h.setBlock(block, net.minecraft.world.level.block.Blocks.AIR);
    dynamite.tick();
    h.assertTrue(!dynamite.isNoGravity(), "broken support restores gravity");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void staticBootsChargeOnlyWhenWornAndWalking(GameTestHelper h) {
    var player = h.makeMockServerPlayerInLevel();
    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    var boots = new ItemStack(ExperimentalItems.STATIC_BOOTS);
    var battery = new ItemStack(Ic2Items.BATPACK);
    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, battery);
    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, boots);
    player.setPos(100, 100, 100);
    boots.getItem().inventoryTick(boots, h.getLevel(), player, 0, false);
    player.setPos(110, 100, 100);
    boots.getItem().inventoryTick(boots, h.getLevel(), player, 0, false);
    h.assertTrue(
        ElectricItem.manager.getCharge(battery) == 2,
        "ten blocks produce two EU as in Experimental");
    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, ItemStack.EMPTY);
    player.setPos(120, 100, 100);
    boots.getItem().inventoryTick(boots, h.getLevel(), player, 0, false);
    h.assertTrue(
        ElectricItem.manager.getCharge(battery) == 2, "unequipped boots do not generate energy");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void iodineConsumesOnlyUsedTabletsAndReducesRadiation(GameTestHelper h) {
    var player = h.makeMockServerPlayerInLevel();
    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    ic2.core.Ic2Potion.radiation.applyTo(player, 2000, 0);
    player.setItemInHand(
        InteractionHand.MAIN_HAND, new ItemStack(ExperimentalItems.IODINE_TABLET, 5));
    ExperimentalItems.IODINE_TABLET.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
    h.assertTrue(
        player.getMainHandItem().isEmpty()
            && player.getEffect(ic2.core.Ic2Potion.radiationHolder()).getDuration() == 1900,
        "five tablets remove five seconds of radiation");
    player.removeEffect(ic2.core.Ic2Potion.radiationHolder());
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ExperimentalItems.IODINE_TABLET));
    ExperimentalItems.IODINE_TABLET.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
    h.assertTrue(
        player.getMainHandItem().getCount() == 1, "healthy player does not consume a tablet");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void remotePrimesExactlyOneLinkedCharge(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    h.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE);
    h.setBlock(pos, ExperimentalDynamite.BLOCK);
    var player = h.makeMockServerPlayerInLevel();
    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ExperimentalDynamite.REMOTE));
    var hit =
        new BlockHitResult(
            Vec3.atCenterOf(h.absolutePos(pos)),
            net.minecraft.core.Direction.UP,
            h.absolutePos(pos),
            false);
    ExperimentalDynamite.REMOTE.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    h.assertTrue(
        h.getBlockState(pos).getValue(PlantedDynamiteBlock.LINKED),
        "charge links to the selected remote");
    ExperimentalDynamite.REMOTE.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
    var entities =
        h.getLevel()
            .getEntitiesOfClass(
                DynamiteProjectile.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(1));
    h.assertTrue(
        h.getBlockState(pos).isAir() && entities.size() == 1 && entities.getFirst().fuse == 40,
        "one linked block becomes one forty-tick fuse");
    entities.forEach(net.minecraft.world.entity.Entity::discard);
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void dynamitePointBlastRespectsExperimentalResistanceLimit(GameTestHelper h) {
    h.setBlock(new BlockPos(2, 1, 1), net.minecraft.world.level.block.Blocks.STONE);
    h.setBlock(new BlockPos(0, 1, 1), net.minecraft.world.level.block.Blocks.OBSIDIAN);
    var entity = new DynamiteProjectile(ExperimentalDynamite.ENTITY, h.getLevel());
    entity.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1, 1, 1))));
    ExperimentalPointExplosion.explode(entity);
    h.assertTrue(
        h.getBlockState(new BlockPos(2, 1, 1)).isAir()
            && h.getBlockState(new BlockPos(0, 1, 1))
                .is(net.minecraft.world.level.block.Blocks.OBSIDIAN),
        "point blast breaks resistance below ten and preserves obsidian");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void mfsuUpgradeKeepsChargeAndInventory(GameTestHelper h) {
    var pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.MFE);
    TileEntityElectricBlock storage = h.getBlockEntity(pos);
    storage.energy.addEnergy(123456);
    ItemStack battery = new ItemStack(Ic2Items.RE_BATTERY);
    ElectricItem.manager.charge(battery, 3210, Integer.MAX_VALUE, true, false);
    storage.chargeSlot.put(battery);
    var player = h.makeMockServerPlayerInLevel();
    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    player.setItemInHand(
        InteractionHand.MAIN_HAND, new ItemStack(ExperimentalItems.MFSU_UPGRADE_KIT));
    var hit =
        new BlockHitResult(
            Vec3.atCenterOf(h.absolutePos(pos)),
            net.minecraft.core.Direction.UP,
            h.absolutePos(pos),
            false);
    ExperimentalItems.MFSU_UPGRADE_KIT.useOn(
        new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    TileEntityElectricBlock upgraded = h.getBlockEntity(pos);
    h.assertTrue(
        h.getBlockState(pos).is(Ic2Blocks.MFSU) && upgraded.getStoredLong() == 123456,
        "upgrade keeps all stored EU");
    h.assertTrue(
        ElectricItem.manager.getCharge(upgraded.chargeSlot.get()) == 3210
            && player.getMainHandItem().isEmpty(),
        "one kit consumed and battery retained");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void universalCellKeepsPartialFluidAcrossBothApisAndReload(GameTestHelper h) {
    var stack = new ItemStack(ExperimentalItems.FLUID_CELL);
    var item = (UniversalFluidCell) stack.getItem();
    h.assertTrue(
        item.fillMb(stack, Ic2FluidStack.create(Fluids.WATER, 750), false, null) == 750,
        "native tank fills cell");
    var forge = stack.getCapability(Capabilities.FluidHandler.ITEM);
    h.assertTrue(
        forge != null && forge.drain(250, IFluidHandler.FluidAction.EXECUTE).getAmount() == 250,
        "NeoForge drains only requested amount");
    var copy =
        ItemStack.parseOptional(
            h.getLevel().registryAccess(),
            (net.minecraft.nbt.CompoundTag) stack.save(h.getLevel().registryAccess()));
    h.assertTrue(
        item.getFluidStack(copy).getAmountMb() == 500, "partial remainder survives saving");
    h.assertTrue(
        item.fillMb(copy, Ic2FluidStack.create(Fluids.LAVA, 100), false, null) == 0,
        "different fluids cannot mix");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void universalCellCollectsAndPlacesWorldWater(GameTestHelper h) {
    var player = h.makeMockServerPlayerInLevel();
    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    var waterPos = h.absolutePos(new BlockPos(1, 1, 1));
    player.setPos(waterPos.getX() + .5, waterPos.getY() - 1, waterPos.getZ() - .5);
    player.setYRot(0);
    player.setXRot(0);
    var source = new BlockPos(1, 1, 1);
    h.setBlock(source, net.minecraft.world.level.block.Blocks.WATER);
    var stack = new ItemStack(ExperimentalItems.FLUID_CELL);
    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    var cell = (UniversalFluidCell) stack.getItem();
    var hit =
        Item.getPlayerPOVHitResult(
            h.getLevel(), player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
    h.assertTrue(
        hit.getBlockPos().equals(waterPos)
            && h.getLevel().mayInteract(player, waterPos)
            && player.mayUseItemAt(waterPos, hit.getDirection(), stack),
        "source reachable: hit="
            + hit.getBlockPos()
            + " type="
            + hit.getType()
            + " expected="
            + waterPos
            + " range="
            + player.blockInteractionRange()
            + " interact="
            + h.getLevel().mayInteract(player, waterPos)
            + " mayUse="
            + player.mayUseItemAt(waterPos, hit.getDirection(), stack));
    cell.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
    h.assertTrue(
        cell.getFluidStack(stack).getAmountMb() == 1000 && h.getBlockState(source).isAir(),
        "cell collects one source block; stored="
            + cell.getFluidStack(stack).getAmountMb()
            + " state="
            + h.getBlockState(source)
            + " eye="
            + player.getEyePosition()
            + " view="
            + player.getViewVector(1));
    h.setBlock(new BlockPos(1, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
    cell.use(h.getLevel(), player, InteractionHand.MAIN_HAND);
    h.assertTrue(
        cell.getFluidStack(stack).isEmpty() && h.getBlockState(source).getFluidState().isSource(),
        "same cell places water and remains reusable");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void containmentBoxPersistsRadioactiveItemsAndRejectsOthers(GameTestHelper h) {
    var player = h.makeMockServerPlayerInLevel();
    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    var stack = new ItemStack(ExperimentalItems.CONTAINMENT_BOX);
    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    var box = new ContainmentBox.Storage(player, InteractionHand.MAIN_HAND, stack);
    h.assertTrue(
        box.getContainerSize() == 12
            && box.canPlaceItem(0, new ItemStack(Ic2Items.URANIUM))
            && !box.canPlaceItem(0, new ItemStack(Items.STONE)),
        "original radioactive-only twelve slots");
    box.setItem(0, new ItemStack(Ic2Items.URANIUM, 3));
    var reopened =
        new ContainmentBox.Storage(player, InteractionHand.MAIN_HAND, player.getMainHandItem());
    h.assertTrue(
        reopened.getItem(0).is(Ic2Items.URANIUM) && reopened.getItem(0).getCount() == 3,
        "container contents survive reopening");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void pipesMoveFluidWithOriginalPumpRatesWithoutDuplication(GameTestHelper h) {
    var source = new BlockPos(0, 1, 1);
    var middle = new BlockPos(1, 1, 1);
    var output = new BlockPos(2, 1, 1);
    h.setBlock(source, Ic2Blocks.TANK);
    h.setBlock(middle, ExperimentalPipes.BLOCKS.get("bronze_pipe_tiny"));
    h.setBlock(output, Ic2Blocks.TANK);
    var input =
        h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(source), null);
    var result =
        h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(output), null);
    ExperimentalFluidPipe pipe = h.getBlockEntity(middle);
    h.assertTrue(
        input != null && result != null && pipe.tank.getCapacity() == 400,
        "original tiny bronze profile and native tank ports");
    input.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
    pipe.pumps[net.minecraft.core.Direction.WEST.ordinal()] = 1;
    pipe.tick();
    h.assertTrue(
        input.getFluidInTank(0).getAmount() == 968, "LV cover extracts exactly 32 mB per tick");
    for (int i = 1; i < 60; i++) pipe.tick();
    h.assertTrue(
        input.getFluidInTank(0).isEmpty()
            && pipe.tank.isEmpty()
            && result.getFluidInTank(0).getAmount() == 1000,
        "all fluid reaches output exactly once");
    var nbt = pipe.saveWithFullMetadata(h.getLevel().registryAccess());
    pipe.loadWithComponents(nbt, h.getLevel().registryAccess());
    h.assertTrue(
        pipe.pumps[net.minecraft.core.Direction.WEST.ordinal()] == 1,
        "attached cover survives save/load");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void professionsHaveWorkstationsAndValidTradesAtEveryLevel(GameTestHelper h) {
    for (var entry : RestoredVillagers.ALL.entrySet()) {
      var profession = entry.getValue().get();
      for (int level = 1; level <= 5; level++) {
        var villager = EntityType.VILLAGER.create(h.getLevel());
        villager.setVillagerData(
            villager.getVillagerData().setProfession(profession).setLevel(level));
        var offers = villager.getOffers();
        h.assertTrue(!offers.isEmpty(), "trade pool for " + entry.getKey() + " level " + level);
        for (var offer : offers)
          h.assertTrue(
              !offer.getResult().isEmpty() && !offer.getCostA().isEmpty(),
              "offers contain registered goods");
      }
    }
    for (var block :
        List.of(
            Ic2Blocks.IRON_FURNACE,
            Ic2Blocks.NUCLEAR_REACTOR,
            Ic2Blocks.CROPMATRON,
            Ic2Blocks.ITNT,
            ExperimentalBrewing.BARREL,
            Ic2Blocks.INDUSTRIAL_WORKBENCH))
      h.assertTrue(
          PoiTypes.forState(block.defaultBlockState()).isPresent(),
          "workstation is recognized by village AI");
    h.succeed();
  }
}
