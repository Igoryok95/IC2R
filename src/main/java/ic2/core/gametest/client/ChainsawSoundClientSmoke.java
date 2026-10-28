package ic2.core.gametest.client;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.item.tool.ItemElectricTool;
import ic2.core.sound.EntityTrackingSoundInstance;
import ic2.core.sound.Ic2AudioSettings;
import ic2.core.sound.SoundClient;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import org.slf4j.LoggerFactory;

/** One native audio-engine pass for both chainsaws; no production inventory is touched. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class ChainsawSoundClientSmoke {
  private static final ResourceLocation IDLE = ResourceLocation.parse("ic2:item.chainsaw.idle");
  private static final ResourceLocation STOP = ResourceLocation.parse("ic2:item.chainsaw.stop");
  private static final List<SoundInstance> REQUESTS = new CopyOnWriteArrayList<>();
  private static final List<SoundInstance> STARTED = new CopyOnWriteArrayList<>();
  private static final List<ResourceLocation> TOOLS = new ArrayList<>();
  private static int ticks, test;

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void requested(PlaySoundEvent event) {
    if (Boolean.getBoolean("ic2.chainsawSoundSmoke")
        && event.getSound() != null
        && isChainsaw(event.getSound())) REQUESTS.add(event.getSound());
  }

  @SubscribeEvent
  public static void started(PlaySoundSourceEvent event) {
    if (Boolean.getBoolean("ic2.chainsawSoundSmoke") && isChainsaw(event.getSound()))
      STARTED.add(event.getSound());
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) throws ReflectiveOperationException {
    if (!Boolean.getBoolean("ic2.chainsawSoundSmoke")) return;
    var mc = Minecraft.getInstance();
    var server = mc.getSingleplayerServer();
    if (mc.player == null || mc.level == null || server == null || mc.getOverlay() != null) return;
    mc.options.pauseOnLostFocus = false;
    int phase = ++ticks;
    if (TOOLS.isEmpty()) {
      TOOLS.add(ResourceLocation.parse("ic2:chainsaw"));
      var advanced = ResourceLocation.parse("ic2r_addons:advanced_chainsaw");
      if (BuiltInRegistries.ITEM.containsKey(advanced)) TOOLS.add(advanced);
      mc.setScreen(null);
      mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
      mc.options.getSoundSourceOptionInstance(SoundSource.PLAYERS).set(1.0);
      Ic2AudioSettings.setVolume(1);
    }
    var toolId = TOOLS.get(test / 4);
    int action = test % 4;
    var playerId = mc.player.getUUID();
    if (phase == 1) {
      REQUESTS.clear();
      STARTED.clear();
      select(mc, 1);
      server.execute(
          () -> {
            var player = server.getPlayerList().getPlayer(playerId);
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(.5, 128, 100.5);
            player.getInventory().clearContent();
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(toolId));
            ElectricItem.manager.charge(
                stack, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, false);
            player.getInventory().setItem(0, stack);
            player.getInventory().setItem(2, stack.copy());
            player.getInventory().setItem(1, new ItemStack(Items.STONE));
          });
    }
    if (phase == 12) select(mc, 0);
    if (phase == 22) select(mc, 2);
    if (phase == 24) select(mc, 0);
    if (phase == 35) {
      require(count(STARTED, IDLE) == 1, "idle must start once, even with two stacks");
      require(count(STARTED, STOP) == 0, "switching between identical saws must not shut down");
      require(activeIdle(mc) == 1, "expected one actual idle channel");
      switch (action) {
        case 0 -> select(mc, 1);
        case 1 -> mc.player.drop(false);
        case 2 ->
            server.execute(
                () ->
                    server
                        .getPlayerList()
                        .getPlayer(playerId)
                        .getInventory()
                        .setItem(0, ItemStack.EMPTY));
        case 3 ->
            server.execute(
                () -> {
                  var stack = server.getPlayerList().getPlayer(playerId).getMainHandItem();
                  ElectricItem.manager.discharge(
                      stack, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, false, false);
                });
      }
    }
    if (phase == 60) {
      require(count(REQUESTS, STOP) == 1, "shutdown must be requested exactly once");
      require(count(STARTED, STOP) == 1, "shutdown must reach the actual decoded audio channel");
      require(activeIdle(mc) == 0, "idle channel survived unequip or empty charge");
      LoggerFactory.getLogger("IC2R-Chainsaw-Sound")
          .info(
              "CHAINSAW_SOUND_CHECK: {} action={} -> one audible shutdown, no idle loop",
              toolId,
              action);
      if (action == 0) select(mc, 0);
      else if (action == 3)
        server.execute(
            () ->
                ElectricItem.manager.charge(
                    server.getPlayerList().getPlayer(playerId).getMainHandItem(),
                    Double.POSITIVE_INFINITY,
                    Integer.MAX_VALUE,
                    true,
                    false));
    }
    if (phase == 80) {
      if (action == 0 || action == 3) {
        require(activeIdle(mc) == 1, "idle did not restart after re-equip or recharge");
        require(count(STARTED, IDLE) == 2, "idle restarted more than once");
      }
      require(count(STARTED, STOP) == 1, "duplicate shutdown while remaining unequipped/empty");
      require(
          shutdownControllers() == (action == 0 || action == 3 ? 1 : 0),
          "completed shutdown controller was not released");
      ((ItemElectricTool) BuiltInRegistries.ITEM.get(toolId)).stopHeldSounds(mc.player, false);
      select(mc, 1);
      server.execute(
          () -> server.getPlayerList().getPlayer(playerId).getInventory().clearContent());
    }
    if (phase == 95) {
      require(activeIdle(mc) == 0, "cleanup left a running idle channel");
      if (++test < TOOLS.size() * 4) ticks = 0;
      else {
        LoggerFactory.getLogger("IC2R-Chainsaw-Sound")
            .info(
                "IC2R_CHAINSAW_SOUND_CLIENT_PASS: {} saws; hotbar, drop, inventory removal, discharge/recharge, two stacks, completed-tail cleanup",
                TOOLS.size());
        mc.stop();
      }
    }
  }

  private static void select(Minecraft mc, int slot) {
    mc.player.getInventory().selected = slot;
    mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
  }

  private static boolean isChainsaw(SoundInstance sound) {
    return sound.getLocation().equals(IDLE) || sound.getLocation().equals(STOP);
  }

  private static long count(List<SoundInstance> sounds, ResourceLocation id) {
    return sounds.stream().filter(sound -> sound.getLocation().equals(id)).count();
  }

  @SuppressWarnings("unchecked")
  private static long activeIdle(Minecraft mc) throws ReflectiveOperationException {
    Field engineField =
        net.minecraft.client.sounds.SoundManager.class.getDeclaredField("soundEngine");
    engineField.setAccessible(true);
    var engine = engineField.get(mc.getSoundManager());
    var channels = engine.getClass().getDeclaredField("instanceToChannel");
    channels.setAccessible(true);
    return ((Map<SoundInstance, ?>) channels.get(engine))
        .keySet().stream()
            .filter(sound -> sound.getLocation().equals(IDLE))
            .filter(
                sound ->
                    !(sound instanceof EntityTrackingSoundInstance tracking)
                        || !tracking.isStopped())
            .filter(mc.getSoundManager()::isActive)
            .count();
  }

  private static int shutdownControllers() throws ReflectiveOperationException {
    Field owners = IC2.soundManager.getClass().getDeclaredField("objectToSoundMap");
    owners.setAccessible(true);
    Field soundEvent = SoundClient.class.getDeclaredField("soundEvent");
    soundEvent.setAccessible(true);
    int count = 0;
    for (Object value : ((Map<?, ?>) owners.get(IC2.soundManager)).values())
      for (Object controller : (List<?>) value)
        if (((net.minecraft.sounds.SoundEvent) soundEvent.get(controller))
            .getLocation()
            .equals(STOP)) count++;
    return count;
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
