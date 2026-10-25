package ic2.core.gametest;

import ic2.api.energy.prefab.BasicSink;
import ic2.api.energy.prefab.BasicSinkSource;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.info.Info;
import ic2.api.item.ElectricItem;
import ic2.core.block.comp.Energy;
import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.block.wiring.tileentity.TileEntityElectricBlock;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class EnergyStorageBoundsGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final List<Block> STORES =
      List.of(
          Ic2Blocks.BATBOX,
          Ic2Blocks.CESU,
          Ic2Blocks.MFE,
          Ic2Blocks.MFSU,
          Ic2Blocks.BATBOX_CHARGEPAD,
          Ic2Blocks.CESU_CHARGEPAD,
          Ic2Blocks.MFE_CHARGEPAD,
          Ic2Blocks.MFSU_CHARGEPAD);

  private static BlockPos pos(int index) {
    return new BlockPos(index % 3, 1, index / 3);
  }

  private static void placeStores(GameTestHelper h) {
    for (int i = 0; i < STORES.size(); i++) {
      h.setBlock(pos(i), STORES.get(i));
      TileEntityElectricBlock store = h.getBlockEntity(pos(i));
      store.energy.setSendingEnabled(false);
    }
  }

  @GameTest(template = EMPTY)
  public static void allEightStoresRejectExcessPacketsAndClampLegacyState(GameTestHelper h) {
    placeStores(h);
    h.runAtTickTime(
        5,
        () -> {
          for (int i = 0; i < STORES.size(); i++) {
            TileEntityElectricBlock store = h.getBlockEntity(pos(i));
            double capacity = store.energy.getCapacity();
            var sink = (IEnergySink) store.energy.getDelegate();
            store.energy.addEnergy(capacity - 10.25);
            h.assertTrue(
                sink.injectEnergy(Direction.UP, 64, 32) == 53.75,
                "packet reports its unaccepted energy");
            h.assertTrue(store.energy.getEnergy() == capacity, "packet never exceeds capacity");
            h.assertTrue(
                sink.injectEnergy(Direction.UP, 32, 32) == 32,
                "a full store rejects the entire packet");
            store.setStored(-1);
            h.assertTrue(store.getStored() == 0, "legacy setter cannot create negative storage");
            store.setStored(1);
            h.assertTrue(
                store.addEnergy(-100) == -1 && store.getStored() == 0,
                "signed legacy transfers still drain energy without making the buffer negative");
            store.setStored(Integer.MAX_VALUE);
            h.assertTrue(store.getStored() == capacity, "legacy setter is capacity bounded");
            store.energy.useEnergy(5);
            h.assertTrue(
                store.addEnergy(100) == 5 && store.energy.getEnergy() == capacity,
                "legacy addEnergy reports only the amount actually accepted");

            store.energy.forceAddEnergy(50);
            var saved = store.saveWithFullMetadata(h.getLevel().registryAccess());
            store.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(
                store.energy.getEnergy() == capacity,
                "old overfilled saves are repaired on load, not hidden in the GUI");
            h.assertTrue(
                store.energy.getFillRatio() == 1 && store.energy.getComparatorValue() == 15,
                "gauge and comparator read the real bounded amount");
          }
          h.succeed();
        });
  }

  @GameTest(template = EMPTY)
  public static void allEightStoresWaitForWholeRedstoneFuelPortions(GameTestHelper h) {
    placeStores(h);
    double fuel = Info.getItemInfo().getEnergyValue(new ItemStack(Items.REDSTONE));
    h.assertTrue(fuel > 0, "redstone has its native EU value");
    for (int i = 0; i < STORES.size(); i++) {
      TileEntityElectricBlock store = h.getBlockEntity(pos(i));
      double capacity = store.energy.getCapacity();
      store.energy.setEnergy(capacity - fuel + 1);
      store.dischargeSlot.put(new ItemStack(Items.REDSTONE));
      store.energy.onWorldTick();
      h.assertTrue(
          store.energy.getEnergy() == capacity - fuel + 1
              && store.dischargeSlot.get().getCount() == 1,
          "an oversized indivisible fuel portion is neither consumed nor wasted");
      store.energy.useEnergy(1);
      store.energy.onWorldTick();
      h.assertTrue(
          store.energy.getEnergy() == capacity && store.dischargeSlot.isEmpty(),
          "redstone fills the store exactly once there is room for the whole portion");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void sinkOnlyMachinesAndApiPrefabsReturnRejectedEnergy(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.MACERATOR);
    Ic2TileEntity machine = h.getBlockEntity(pos);
    Energy energy = machine.getComponent(Energy.class);
    h.runAtTickTime(
        5,
        () -> {
          energy.setEnergy(energy.getCapacity() - .5);
          IEnergySink sink = (IEnergySink) energy.getDelegate();
          h.assertTrue(
              sink.injectEnergy(Direction.UP, 32, 32) == 31.5,
              "sink-only components preserve rejected fractions too");
          h.assertTrue(energy.getEnergy() == energy.getCapacity(), "machine buffer is bounded");
          energy.setCapacity(100);
          h.assertTrue(energy.getEnergy() == 100, "capacity reduction clamps the actual buffer");

          var basic = new BasicSink(h.getLevel(), h.absolutePos(pos), 1000, 1);
          var dual =
              new BasicSinkSource(h.getLevel(), h.absolutePos(pos), 1000, 1, 1) {
                @Override
                public boolean acceptsEnergyFrom(
                    ic2.api.energy.tile.IEnergyEmitter emitter, Direction side) {
                  return true;
                }

                @Override
                public boolean emitsEnergyTo(
                    ic2.api.energy.tile.IEnergyAcceptor receiver, Direction side) {
                  return true;
                }
              };
          basic.setEnergyStored(999.5);
          dual.setEnergyStored(999.5);
          h.assertTrue(
              basic.injectEnergy(Direction.UP, 32, 32) == 31.5
                  && dual.injectEnergy(Direction.UP, 32, 32) == 31.5,
              "public sink prefabs obey the same rejection contract");
          basic.setEnergyStored(1050);
          dual.setEnergyStored(1050);
          h.assertTrue(
              basic.getEnergyStored() == 1000 && dual.getEnergyStored() == 1000,
              "prefab setters cannot overfill");
          basic.setCapacity(500);
          h.assertTrue(basic.getEnergyStored() == 500, "prefab capacity reduction is bounded");
          var nbt = new CompoundTag();
          var data = new CompoundTag();
          data.putDouble("energy", 1050);
          nbt.put("IC2BasicSinkSource", data);
          dual.readFromNBT(nbt);
          h.assertTrue(dual.getEnergyStored() == 1000, "prefab legacy saves are bounded");
          h.succeed();
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 80)
  public static void multipleSourcesFillOnlyFreeSpaceAndKeepRejectedEu(GameTestHelper h) {
    var target = new BlockPos(1, 1, 1);
    var a = target.above();
    var b = target.west();
    h.setBlock(target, Ic2Blocks.BATBOX);
    h.setBlock(a, Ic2Blocks.GENERATOR);
    h.setBlock(b, Ic2Blocks.GENERATOR);
    TileEntityElectricBlock store = h.getBlockEntity(target);
    store.energy.setSendingEnabled(false);
    Energy sourceA = ((Ic2TileEntity) h.getBlockEntity(a)).getComponent(Energy.class);
    Energy sourceB = ((Ic2TileEntity) h.getBlockEntity(b)).getComponent(Energy.class);
    store.energy.addEnergy(39950);
    sourceA.addEnergy(1000);
    sourceB.addEnergy(1000);
    h.runAtTickTime(
        30,
        () -> {
          h.assertTrue(
              store.energy.getEnergy() == 40000, "parallel sources cannot overfill BatBox");
          Ic2GameTestAssertions.assertNear(
              h,
              sourceA.getEnergy() + sourceB.getEnergy(),
              1950,
              "unaccepted EU stays in its sources; no energy is created or discarded");
          h.succeed();
        });
  }

  @GameTest(template = EMPTY)
  public static void batpackFinalChargeUpdatesMenuWithoutMovingItem(GameTestHelper h) {
    BlockPos pos = new BlockPos(1, 1, 1);
    h.setBlock(pos, Ic2Blocks.BATBOX);
    TileEntityElectricBlock store = h.getBlockEntity(pos);
    var pack = new ItemStack(Ic2Items.BATPACK);
    ElectricItem.manager.charge(pack, 59900, 1, true, false);
    store.chargeSlot.put(pack);
    var snapshot = pack.copy();
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var menu = store.createServerScreenHandler(1, player);
    var notifiedCharge = new java.util.concurrent.atomic.AtomicReference<>(0.0);
    menu.addSlotListener(
        new ContainerListener() {
          @Override
          public void slotChanged(AbstractContainerMenu container, int slot, ItemStack stack) {
            if (stack.is(Ic2Items.BATPACK))
              notifiedCharge.set(ElectricItem.manager.getCharge(stack));
          }

          @Override
          public void dataChanged(AbstractContainerMenu container, int id, int value) {}
        });
    menu.broadcastChanges();
    store.energy.addEnergy(100);
    store.energy.onWorldTick();
    h.assertTrue(
        ElectricItem.manager.getCharge(pack) == 60000 && store.energy.getEnergy() == 0,
        "upper charging slot supplies the exact last 100 EU");
    h.assertTrue(
        ElectricItem.manager.getCharge(snapshot) == 59900,
        "charging must not mutate the menu's previous ItemStack copy");
    menu.broadcastChanges();
    h.assertTrue(
        notifiedCharge.get() == 60000,
        "menu sends the final 60000 EU value without moving the pack to armor");
    var full = pack.copy();
    ElectricItem.manager.discharge(pack, 25, 1, true, false, false);
    h.assertTrue(
        ElectricItem.manager.getCharge(full) == 60000,
        "discharging also leaves previous snapshots unchanged");
    var empty = new ItemStack(Ic2Items.BATPACK);
    var untouched = empty.copy();
    ElectricItem.manager.charge(empty, 100, 1, false, true);
    ElectricItem.manager.discharge(empty, 100, 1, false, false, true);
    h.assertTrue(ItemStack.matches(empty, untouched), "simulation does not create item data");
    h.succeed();
  }
}
