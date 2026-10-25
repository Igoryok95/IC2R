package ic2.core.block.machine.tileentity;

import com.mojang.authlib.GameProfile;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.IWarmupControl;
import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import ic2.core.util.StackUtil;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class WarmupAndBufferGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  public static void assertNoSignal(GameTestHelper h, Ic2TileEntity tile) {
    for (var side : Direction.values()) {
      var state = tile.getBlockState();
      h.assertTrue(
          state.getSignal(h.getLevel(), tile.getBlockPos(), side) == 0
              && state.getDirectSignal(h.getLevel(), tile.getBlockPos(), side) == 0,
          "local warmup never emits direct or weak redstone");
    }
  }

  @GameTest(template = EMPTY)
  public static void nativeWarmupPreservesEnergyRulesAndExternalRedstone(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.INDUCTION_FURNACE);
    TileEntityInduction induction = h.getBlockEntity(POS);
    induction.setManualWarmup(true);
    induction.updateEntityServer();
    h.assertTrue(induction.heat == 0, "manual standby cannot heat without EU");
    induction.energy.addEnergy(100);
    induction.updateEntityServer();
    h.assertTrue(
        induction.heat == 1 && induction.energy.getEnergy() == 99,
        "induction standby uses the original one EU");
    induction.setManualWarmup(false);
    induction.updateEntityServer();
    h.assertTrue(induction.heat == 0, "off cools an idle induction furnace");
    h.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
    induction.updateEntityServer();
    h.assertTrue(induction.heat == 1, "external redstone works with manual flag off");
    h.setBlock(POS.east(), Blocks.AIR);

    h.setBlock(POS, Ic2Blocks.CENTRIFUGE);
    TileEntityCentrifuge centrifuge = h.getBlockEntity(POS);
    centrifuge.setManualWarmup(true);
    centrifuge.energy.addEnergy(100);
    centrifuge.updateEntityServer();
    h.assertTrue(
        centrifuge.heat == 1 && centrifuge.energy.getEnergy() == 99,
        "idle thermal centrifuge warms using its original EU demand");
    centrifuge.setManualWarmup(false);
    centrifuge.updateEntityServer();
    h.assertTrue(centrifuge.heat == 0, "off cools an idle centrifuge");

    h.setBlock(POS, Ic2Blocks.BLAST_FURNACE);
    TileEntityBlastFurnace blast = h.getBlockEntity(POS);
    blast.setManualWarmup(true);
    blast.updateEntityServer();
    h.assertTrue(blast.heat == 0, "blast furnace needs a real hU source");
    var heaterPos = POS.relative(blast.getFacing());
    h.setBlock(
        heaterPos,
        Ic2Blocks.ELECTRIC_HEAT_GENERATOR
            .defaultBlockState()
            .setValue(
                ic2.core.block.tileentity.Ic2TileEntityBlock.anyFacingProperty,
                blast.getFacing().getOpposite()));
    ic2.core.block.heatgenerator.tileentity.TileEntityElectricHeatGenerator heater =
        h.getBlockEntity(heaterPos);
    heater.coilSlot.put(0, new ItemStack(Ic2Items.COIL));
    heater.getComponent(Energy.class).addEnergy(1000);
    heater.setHeatBuffer(10);
    blast.updateEntityServer();
    h.assertTrue(blast.heat > 0, "manual standby draws real heat from the facing heater");
    assertNoSignal(h, blast);
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeWarmupToggleRequiresOpenNearbyMenuAndSurvivesSaving(GameTestHelper h) {
    for (var block :
        new net.minecraft.world.level.block.Block[] {
          Ic2Blocks.INDUCTION_FURNACE, Ic2Blocks.CENTRIFUGE, Ic2Blocks.BLAST_FURNACE
        }) {
      h.setBlock(POS, block);
      var tile = (Ic2TileEntity) h.getBlockEntity(POS);
      var control = (IWarmupControl) tile;
      var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "warmup-test"));
      player.setGameMode(GameType.SURVIVAL);
      player.setPos(
          tile.getBlockPos().getX() + .5,
          tile.getBlockPos().getY(),
          tile.getBlockPos().getZ() + .5);
      control.onNetworkEvent(player, IWarmupControl.TOGGLE_WARMUP_EVENT);
      h.assertTrue(!control.isManualWarmup(), "unopened machine rejects toggle");
      player.containerMenu = ((ic2.core.IHasGui) tile).createServerScreenHandler(1, player);
      control.onNetworkEvent(player, IWarmupControl.TOGGLE_WARMUP_EVENT);
      h.assertTrue(control.isManualWarmup(), "open nearby menu toggles on");
      var saved = tile.saveWithFullMetadata(h.getLevel().registryAccess());
      var loaded =
          (IWarmupControl)
              BlockEntity.loadStatic(
                  tile.getBlockPos(), tile.getBlockState(), saved, h.getLevel().registryAccess());
      h.assertTrue(loaded != null && loaded.isManualWarmup(), "local state survives world reload");
      player.setPos(player.getX() + 20, player.getY(), player.getZ());
      control.onNetworkEvent(player, IWarmupControl.TOGGLE_WARMUP_EVENT);
      h.assertTrue(control.isManualWarmup(), "distant toggle rejected");
      assertNoSignal(h, tile);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 160)
  public static void manualWarmupLeavesRealHopperUnlocked(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.INDUCTION_FURNACE);
    h.setBlock(POS.above(), Blocks.HOPPER);
    TileEntityInduction machine = h.getBlockEntity(POS);
    machine.energy.addEnergy(10000);
    machine.setManualWarmup(true);
    HopperBlockEntity hopper = h.getBlockEntity(POS.above());
    hopper.setItem(0, new ItemStack(Ic2Items.IRON_DUST, 10));
    h.succeedWhen(
        () -> {
          h.assertTrue(
              hopper.isEmpty(), "hopper keeps transferring while manual standby is enabled");
          h.assertTrue(machine.heat > 0, "furnace warms while hopper works");
          h.assertTrue(
              hopper.getBlockState().getValue(HopperBlock.ENABLED),
              "hopper is not redstone locked");
          h.assertTrue(
              machine.inputSlotA.get().getCount() == 5 && machine.inputSlotB.get().getCount() == 5,
              "both parallel inputs remain balanced");
          assertNoSignal(h, machine);
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 180)
  public static void compactBufferPullsAndPushesWithoutEu(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.ITEM_BUFFER_2);
    h.setBlock(POS.west(), Blocks.BARREL);
    h.setBlock(POS.east(), Blocks.CHEST);
    TileEntityBetterItemBuffer buffer = h.getBlockEntity(POS);
    var source = (net.minecraft.world.Container) h.getBlockEntity(POS.west());
    ChestBlockEntity target = h.getBlockEntity(POS.east());
    source.setItem(0, new ItemStack(Items.CLAY_BALL, 17));
    for (int i = 0; i < 2; i++) {
      var stack = new ItemStack(i == 0 ? Ic2Items.PULLING_UPGRADE : Ic2Items.EJECTOR_UPGRADE);
      StackUtil.getOrCreateNbtData(stack)
          .putByte("dir", (byte) (1 + (i == 0 ? Direction.WEST : Direction.EAST).ordinal()));
      buffer.upgradeSlot.put(i, stack);
    }
    h.assertTrue(
        buffer.bufferSlot.size() == 9 && buffer.upgradeSlot.size() == 4,
        "original nine storage and four upgrade slots");
    h.assertTrue(buffer.getComponent(Energy.class) == null, "buffer has no EU component");
    h.succeedWhen(
        () -> {
          h.assertTrue(source.isEmpty() && buffer.bufferSlot.isEmpty(), "all items transferred");
          int total = 0;
          for (int i = 0; i < target.getContainerSize(); i++) total += target.getItem(i).getCount();
          h.assertTrue(total == 17, "transfer neither loses nor duplicates items");
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 60)
  public static void compactBufferKeepsItemsWithFullOutputAndReloadsInventory(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.ITEM_BUFFER_2);
    h.setBlock(POS.east(), Blocks.CHEST);
    TileEntityBetterItemBuffer buffer = h.getBlockEntity(POS);
    ChestBlockEntity chest = h.getBlockEntity(POS.east());
    for (int i = 0; i < chest.getContainerSize(); i++)
      chest.setItem(i, new ItemStack(Items.STONE, 64));
    buffer.bufferSlot.put(8, new ItemStack(Items.CLAY_BALL, 17));
    buffer.upgradeSlot.put(3, new ItemStack(Ic2Items.EJECTOR_UPGRADE));
    h.runAfterDelay(
        30,
        () -> {
          h.assertTrue(
              buffer.bufferSlot.get(8).getCount() == 17, "full destination leaves items in buffer");
          var saved = buffer.saveWithFullMetadata(h.getLevel().registryAccess());
          var copy =
              (TileEntityBetterItemBuffer)
                  BlockEntity.loadStatic(
                      buffer.getBlockPos(),
                      buffer.getBlockState(),
                      saved,
                      h.getLevel().registryAccess());
          h.assertTrue(
              copy != null
                  && copy.bufferSlot.get(8).getCount() == 17
                  && copy.upgradeSlot.get(3).is(Ic2Items.EJECTOR_UPGRADE),
              "last storage and upgrade slots survive saving");
          h.succeed();
        });
  }
}
