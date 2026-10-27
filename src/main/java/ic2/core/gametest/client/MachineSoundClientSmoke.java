package ic2.core.gametest.client;

import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.block.comp.Energy;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotProcessable;
import ic2.core.block.machine.IWarmupControl;
import ic2.core.block.machine.tileentity.TileEntityCanner;
import ic2.core.block.machine.tileentity.TileEntityElectricFurnace;
import ic2.core.block.machine.tileentity.TileEntityInduction;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.block.tileentity.TileEntityBase;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.ref.Ic2SoundEvents;
import ic2.core.sound.DeferredSoundOps;
import ic2.core.sound.Ic2AudioSettings;
import ic2.core.sound.PositionedSoundInstance;
import ic2.core.sound.RepeatablePositionedSoundInstance;
import ic2.core.sound.Sound;
import ic2.core.sound.SoundClient;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

/** One opt-in real audio-engine pass, with Addons included when supplied on the runtime path. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class MachineSoundClientSmoke {
  private static final List<BlockPos> MACHINES = new ArrayList<>();
  private static final List<ResourceLocation> BLOCK_IDS = new ArrayList<>();
  private static final List<SoundClient> DIRECT = new ArrayList<>();
  private static final BlockPos PROBE = new BlockPos(0, 128, 60);
  private static int ticks;
  private static int callbacks;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) throws ReflectiveOperationException {
    if (!Boolean.getBoolean("ic2.machineSoundSmoke")) return;
    var mc = Minecraft.getInstance();
    var server = mc.getSingleplayerServer();
    if (mc.player == null || mc.level == null || server == null || mc.getOverlay() != null) return;
    mc.options.pauseOnLostFocus = false;
    int phase = ++ticks;
    if (phase == 1) {
      mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
      mc.options.getSoundSourceOptionInstance(SoundSource.BLOCKS).set(1.0);
      Ic2AudioSettings.setVolume(1);
      var playerId = mc.player.getUUID();
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(playerId);
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(.5, 128, 58.5);
            String[] names = {
              "ic2:canner",
              "ic2:macerator",
              "ic2:compressor",
              "ic2:extractor",
              "ic2:electric_furnace",
              "ic2:induction_furnace",
              "ic2r_addons:rotary_macerator",
              "ic2r_addons:singularity_compressor",
              "ic2r_addons:centrifuge_extractor",
              "ic2r_addons:compacting_recycler",
              "ic2r_addons:liquescent_extruder",
              "ic2r_addons:impellerized_roller",
              "ic2r_addons:water_jet_cutter",
              "ic2r_addons:thermal_washer"
            };
            for (String name : names) {
              var id = ResourceLocation.parse(name);
              if (!BuiltInRegistries.BLOCK.containsKey(id)) continue;
              var pos = PROBE.offset(MACHINES.size() % 5 * 2 - 4, 0, MACHINES.size() / 5 * 2 + 2);
              MACHINES.add(pos);
              BLOCK_IDS.add(id);
              var level = player.serverLevel();
              level.removeBlock(pos, false);
            }
          });
    }
    if (phase == 10) {
      var dimension = mc.level.dimension();
      server.execute(
          () -> {
            var level = server.getLevel(dimension);
            for (int i = 0; i < MACHINES.size(); i++) {
              var pos = MACHINES.get(i);
              var id = BLOCK_IDS.get(i);
              level.setBlockAndUpdate(
                  pos,
                  BuiltInRegistries.BLOCK
                      .get(id)
                      .defaultBlockState()
                      .setValue(Ic2TileEntityBlock.ACTIVE, false));
              var machine = (TileEntityBase) level.getBlockEntity(pos);
              if (machine instanceof TileEntityCanner canner) {
                canner.setMode(TileEntityCanner.Mode.BottleLiquid);
                canner.inputTank.fillMbUnchecked(
                    Ic2FluidStack.create(net.minecraft.world.level.material.Fluids.WATER, 8000),
                    false);
              }
              var fluids = machine.getComponent(Fluids.class);
              if (fluids != null && !(machine instanceof TileEntityCanner))
                for (var tank : fluids.getAllTanks())
                  tank.fillMbUnchecked(
                      Ic2FluidStack.create(net.minecraft.world.level.material.Fluids.WATER, 8000),
                      false);
              if (machine instanceof IWarmupControl warmup) warmup.setManualWarmup(true);
            }
          });
    }
    if (phase == 20) {
      // Queued play cancelled before the queue is flushed, then cancelled after buffer loading
      // starts.
      for (var soundEvent :
          List.of(
              Ic2SoundEvents.MACHINE_CANNER_OPERATE,
              Ic2SoundEvents.MACHINE_CANNER_REVERSE,
              Ic2SoundEvents.MACHINE_MACERATOR_OPERATE,
              Ic2SoundEvents.MACHINE_COMPRESSOR_OPERATE)) {
        var queued = direct(soundEvent);
        queued.play();
        queued.stop();
        var buffering = direct(soundEvent);
        buffering.play();
        DeferredSoundOps.flush();
        buffering.stop();
      }
      var once =
          new PositionedSoundInstance(
              Ic2SoundEvents.MACHINE_INTERRUPT1, SoundSource.BLOCKS, 1, 1, PROBE);
      once.addOnFinishListener(() -> callbacks++);
      once.addOnFinishListener(() -> callbacks += 2);
      once.finish();
      once.finish();
      require(callbacks == 3, "completion callbacks must run once, without recursion");
      once.onFinish(() -> callbacks++);
      once.cancel();
      once.finish();
      require(
          callbacks == 3 && once.isStopped() && !once.canPlaySound(),
          "cancelled source must stay cancelled");
    }
    if (phase == 70) {
      for (var sound : DIRECT)
        require(!sound.isPlaying(), "cancelled direct sound still requested");
      assertNoLoops(mc, List.of(PROBE));
      check(mc, false, "warmup only / cancelled queued and buffering sources");
      change(mc, MachineSoundClientSmoke::addInput);
    }
    if (phase == 75) change(mc, MachineSoundClientSmoke::clearInput);
    if (phase == 120) {
      check(mc, false, "input removed immediately after startup");
      change(mc, MachineSoundClientSmoke::addInput);
    }
    if (phase == 200) {
      check(mc, true, "normal processing after quick interruption");
      change(mc, machine -> outputs(machine, true));
    }
    if (phase == 245) {
      check(mc, false, "output blocked while warmup remains enabled");
      change(
          mc,
          machine -> {
            outputs(machine, false);
            addInput(machine);
          });
    }
    if (phase == 310) {
      check(mc, true, "processing resumed after output cleared");
      change(
          mc,
          machine -> {
            var energy = machine.getComponent(Energy.class);
            energy.useEnergy(energy.getEnergy());
          });
    }
    if (phase == 355) {
      check(mc, false, "energy lost while still hot");
      change(mc, MachineSoundClientSmoke::addInput);
    }
    if (phase == 420) {
      check(mc, true, "energy restored");
      var dimension = mc.level.dimension();
      server.execute(
          () -> MACHINES.forEach(pos -> server.getLevel(dimension).removeBlock(pos, false)));
    }
    if (phase == 465) {
      assertNoLoops(mc, MACHINES);
      LoggerFactory.getLogger("IC2R-Machine-Sound")
          .info(
              "IC2R_MACHINE_SOUND_CLIENT_PASS: {} machines; queued/buffering cancellation, startup interruption, processing, blocked output, power loss, restart and block removal",
              MACHINES.size());
      mc.stop();
    }
  }

  private static SoundClient direct(net.minecraft.sounds.SoundEvent event) {
    var sound =
        (SoundClient) IC2.soundManager.createSound(DIRECT, event, SoundSource.BLOCKS, PROBE, 1, 1);
    DIRECT.add(sound);
    return sound;
  }

  private static void change(Minecraft mc, Consumer<TileEntityBase> action) {
    var server = mc.getSingleplayerServer();
    var dimension = mc.level.dimension();
    server.execute(
        () ->
            MACHINES.forEach(
                pos ->
                    action.accept(
                        (TileEntityBase) server.getLevel(dimension).getBlockEntity(pos))));
  }

  @SuppressWarnings("unchecked")
  private static void addInput(TileEntityBase machine) {
    machine.getComponent(Energy.class).addEnergy(100000);
    if (machine instanceof TileEntityCanner canner) {
      canner.canInputSlot.put(new ItemStack(Items.BUCKET, 16));
      return;
    }
    if (machine instanceof TileEntityInduction induction) {
      induction.inputSlotA.put(new ItemStack(Items.RAW_IRON, 64));
      induction.inputSlotB.put(new ItemStack(Items.RAW_IRON, 64));
      return;
    }
    if (machine instanceof TileEntityElectricFurnace furnace) {
      furnace.inputSlot.put(new ItemStack(Items.RAW_IRON, 64));
      return;
    }
    try {
      var input =
          (InvSlotProcessable<?, ?, ?>) machine.getClass().getField("inputSlot").get(machine);
      if (BuiltInRegistries.BLOCK
          .getKey(machine.getBlockState().getBlock())
          .getPath()
          .equals("compacting_recycler")) {
        input.put(new ItemStack(Items.COBBLESTONE, 64));
        return;
      }
      var field = InvSlotProcessable.class.getDeclaredField("recipeManager");
      field.setAccessible(true);
      var getter =
          (Recipes.IGetter<
                  ? extends IMachineRecipeManager<IRecipeInput, Collection<ItemStack>, ItemStack>>)
              field.get(input);
      for (var recipe : getter.get(machine.getLevel()).getRecipes()) {
        if (recipe.getOutput().isEmpty() || recipe.getInput().getInputs().isEmpty()) continue;
        var stack = recipe.getInput().getInputs().getFirst();
        input.put(stack.copyWithCount(stack.getMaxStackSize()));
        return;
      }
      throw new IllegalStateException("No recipe for " + machine.getClass().getSimpleName());
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void clearInput(TileEntityBase machine) {
    if (machine instanceof TileEntityCanner canner) canner.canInputSlot.put(ItemStack.EMPTY);
    else if (machine instanceof TileEntityInduction induction) {
      induction.inputSlotA.put(ItemStack.EMPTY);
      induction.inputSlotB.put(ItemStack.EMPTY);
    } else {
      try {
        ((InvSlot) machine.getClass().getField("inputSlot").get(machine)).put(ItemStack.EMPTY);
      } catch (ReflectiveOperationException ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  private static void outputs(TileEntityBase machine, boolean blocked) {
    try {
      for (String name :
          machine instanceof TileEntityInduction
              ? List.of("outputSlotA", "outputSlotB")
              : List.of("outputSlot")) {
        var slot = (InvSlot) machine.getClass().getField(name).get(machine);
        for (int i = 0; i < slot.size(); i++)
          slot.put(i, blocked ? new ItemStack(Items.BEDROCK, 64) : ItemStack.EMPTY);
      }
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void check(Minecraft mc, boolean expected, String phase)
      throws ReflectiveOperationException {
    Field loop = TileEntityBase.class.getDeclaredField("loopingSound");
    loop.setAccessible(true);
    for (var pos : MACHINES) {
      var machine = (TileEntityBase) mc.level.getBlockEntity(pos);
      var sound = (Sound) loop.get(machine);
      require(
          machine.getActive() == expected
              && (machine.getLoopingSoundEvent() == null
                  ? sound == null
                  : sound != null && sound.isPlaying() == expected),
          machine.getClass().getSimpleName()
              + " during "
              + phase
              + ": active="
              + machine.getActive()
              + ", sound="
              + (sound != null && sound.isPlaying()));
    }
    if (!expected) assertNoLoops(mc, MACHINES);
    LoggerFactory.getLogger("IC2R-Machine-Sound")
        .info(
            "MACHINE_SOUND_CHECK: {} -> playing={} ({} machines)",
            phase,
            expected,
            MACHINES.size());
  }

  private static void assertNoLoops(Minecraft mc, List<BlockPos> positions)
      throws ReflectiveOperationException {
    var engineField =
        net.minecraft.client.sounds.SoundManager.class.getDeclaredField("soundEngine");
    engineField.setAccessible(true);
    var engine = engineField.get(mc.getSoundManager());
    var channels = engine.getClass().getDeclaredField("instanceToChannel");
    channels.setAccessible(true);
    for (var sound : ((Map<SoundInstance, ?>) channels.get(engine)).keySet()) {
      if (!(sound instanceof RepeatablePositionedSoundInstance)) continue;
      var pos = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
      require(
          !positions.contains(pos),
          "Orphan sound still registered in the real SoundEngine at "
              + pos
              + ": "
              + sound.getLocation());
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
