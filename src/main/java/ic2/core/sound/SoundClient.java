package ic2.core.sound;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public class SoundClient extends Sound {
  /** Ticks to wait for streamed/looping sounds to become active before treating them as dead. */
  private static final int PLAY_GRACE_TICKS = 40;

  private final List<SoundInstance> startedSoundList = new CopyOnWriteArrayList<>();
  public net.minecraft.client.sounds.SoundManager vanillaManager =
      Minecraft.getInstance().getSoundManager();
  private RepeatablePositionedSoundInstance repeatInstance = null;
  private PositionedSoundInstance onceInstance = null;
  private EntityTrackingSoundInstance entityTrackingInstance = null;
  private SoundEvent soundEvent;
  private SoundSource soundCategory;
  private BlockPos pos;
  private LivingEntity entity;
  private float volume;
  private boolean loopRequested;
  private float pitch;
  private Item sourceItem;
  private boolean isStarted = false;
  private int playGraceTicks = 0;
  private long playbackGeneration;
  private boolean playbackPending;
  private Runnable finishListener;
  private SoundEvent nextIntro;

  protected SoundClient() {}

  public SoundClient(
      SoundEvent soundEvent, SoundSource soundCategory, BlockPos pos, float volume, float pitch) {
    this();
    this.soundEvent = soundEvent;
    this.soundCategory = soundCategory;
    this.pos = pos;
    this.volume = volume;
    this.pitch = pitch;
    this.repeatInstance =
        new RepeatablePositionedSoundInstance(soundEvent, soundCategory, volume, pitch, pos);
    this.onceInstance = new PositionedSoundInstance(soundEvent, soundCategory, volume, pitch, pos);
  }

  public SoundClient(
      SoundEvent soundEvent,
      SoundSource soundCategory,
      LivingEntity entity,
      float volume,
      float pitch) {
    this();
    this.soundEvent = soundEvent;
    this.soundCategory = soundCategory;
    this.entity = entity;
    this.volume = volume;
    this.pitch = pitch;
    this.entityTrackingInstance =
        new EntityTrackingSoundInstance(soundEvent, soundCategory, volume, pitch, entity);
  }

  public void setSourceItem(Item item) {
    this.sourceItem = item;
    if (this.entityTrackingInstance != null) {
      this.entityTrackingInstance.setSourceItem(item);
    }
  }

  @Override
  public void playWithIntro(SoundEvent intro) {
    this.stop();
    this.nextIntro = intro;
    this.play();
  }

  @Override
  public void play() {
    SoundEvent intro = this.nextIntro;
    this.nextIntro = null;
    loopRequested = true;
    super.play();
    this.isStarted = true;
    long generation = ++this.playbackGeneration;
    this.playbackPending = true;
    DeferredSoundOps.run(
        () -> {
          if (generation != this.playbackGeneration || !this.isStarted) return;
          this.playbackPending = false;
          if (this.repeatInstance != null) {
            if (this.shouldKeepCurrentInstance(this.repeatInstance)) {
              return;
            }

            this.stopAllStarted();
            this.repeatInstance =
                intro == null
                    ? new RepeatablePositionedSoundInstance(
                        this.soundEvent, this.soundCategory, this.volume, this.pitch, this.pos)
                    : new IntroLoopSoundInstance(
                        intro,
                        this.soundEvent,
                        this.soundCategory,
                        this.volume,
                        this.pitch,
                        this.pos);
            this.vanillaManager.play(this.repeatInstance);
            this.startedSoundList.add(this.repeatInstance);
            this.playGraceTicks = PLAY_GRACE_TICKS;
          }

          if (this.entityTrackingInstance != null) {
            if (this.shouldKeepCurrentInstance(this.entityTrackingInstance)) {
              return;
            }

            this.stopAllStarted();
            this.entityTrackingInstance =
                new EntityTrackingSoundInstance(
                    this.soundEvent, this.soundCategory, this.volume, this.pitch, this.entity);
            if (this.sourceItem != null) {
              this.entityTrackingInstance.setSourceItem(this.sourceItem);
            }

            this.vanillaManager.play(this.entityTrackingInstance);
            this.startedSoundList.add(this.entityTrackingInstance);
            this.playGraceTicks = PLAY_GRACE_TICKS;
          }
        });
  }

  /**
   * Keep the current instance when it is already active, or still within the post-play grace window
   * (streamed sounds may report inactive while buffering). Never leave untracked orphans.
   */
  private boolean shouldKeepCurrentInstance(SoundInstance instance) {
    if (instance == null
        || this.isInstanceCancelled(instance)
        || !this.startedSoundList.contains(instance)) {
      return false;
    }

    if (this.vanillaManager.isActive(instance)) {
      return true;
    }

    return this.playGraceTicks > 0 && this.startedSoundList.contains(instance);
  }

  @Override
  public void playOnce() {
    this.nextIntro = null;
    loopRequested = false;
    super.playOnce();
    this.isStarted = true;
    long generation = ++this.playbackGeneration;
    this.playbackPending = true;
    DeferredSoundOps.run(
        () -> {
          if (generation != this.playbackGeneration || !this.isStarted) return;
          this.playbackPending = false;
          this.stopAllStarted();
          this.playGraceTicks = 0;
          if (this.onceInstance != null) {
            this.onceInstance =
                new PositionedSoundInstance(
                    this.soundEvent, this.soundCategory, this.volume, this.pitch, this.pos);
            this.vanillaManager.play(this.onceInstance);
            this.startedSoundList.add(this.onceInstance);
          }

          if (this.entityTrackingInstance != null) {
            this.entityTrackingInstance =
                new EntityTrackingSoundInstance(
                    this.soundEvent, this.soundCategory, this.volume, this.pitch, this.entity);
            this.entityTrackingInstance.playOnce();
            this.startedSoundList.add(this.entityTrackingInstance);
          }
        });
  }

  @Override
  public void stop() {
    this.nextIntro = null;
    this.finishListener = null;
    if (!this.isStarted && !this.playbackPending && this.startedSoundList.isEmpty()) return;
    loopRequested = false;
    super.stop();
    this.isStarted = false;
    this.playGraceTicks = 0;
    this.playbackGeneration++;
    this.playbackPending = false;
    // A decoded buffer may finish loading after SoundEngine.stop(). Tickable cancellation
    // keeps that late source stopped until vanilla releases its channel.
    for (SoundInstance instance : this.startedSoundList) this.cancelInstance(instance);
    DeferredSoundOps.run(this::stopAllStarted);
  }

  private void stopAllStarted() {
    for (SoundInstance instance : this.startedSoundList) {
      if (instance != null) {
        this.cancelInstance(instance);
        this.vanillaManager.stop(instance);
      }
    }
    this.startedSoundList.clear();
    this.stopInstance(this.repeatInstance);
    this.stopInstance(this.onceInstance);
    this.stopInstance(this.entityTrackingInstance);
  }

  private void stopInstance(SoundInstance instance) {
    if (instance != null) {
      this.cancelInstance(instance);
      this.vanillaManager.stop(instance);
      this.startedSoundList.remove(instance);
    }
  }

  private void cancelInstance(SoundInstance instance) {
    if (instance instanceof PositionedSoundInstance positioned) positioned.cancel();
    else if (instance instanceof EntityTrackingSoundInstance tracking) tracking.cancel();
  }

  private boolean isPlayingSound(SoundInstance instance) {
    return instance != null
        && !this.isInstanceCancelled(instance)
        && this.vanillaManager.isActive(instance);
  }

  private boolean isInstanceCancelled(SoundInstance instance) {
    return instance instanceof net.minecraft.client.resources.sounds.TickableSoundInstance tickable
        && tickable.isStopped();
  }

  @Override
  public boolean isPlaying() {
    if (!this.isStarted) return false;
    if (this.isPlayingSound(this.onceInstance)
        || this.isPlayingSound(this.repeatInstance)
        || this.isPlayingSound(this.entityTrackingInstance)) {
      return true;
    }

    // Treat grace-window instances as playing so callers do not spam play() and spawn orphans.
    return this.playbackPending || this.playGraceTicks > 0 && !this.startedSoundList.isEmpty();
  }

  private void checkSoundFinished(SoundInstance instance) {
    if (!(instance instanceof ListenableSoundInstance listenableSoundInstance)) {
      return;
    }

    if (!this.startedSoundList.contains(instance) || this.vanillaManager.isActive(instance)) {
      return;
    }

    // Streamed / looping sounds may not report active immediately after play().
    if (this.playGraceTicks > 0) {
      return;
    }

    boolean looping =
        this.loopRequested
            && (instance instanceof RepeatablePositionedSoundInstance
                || instance instanceof EntityTrackingSoundInstance);
    if (looping && this.isStarted) {
      // Engine dropped the loop while we still want it — clear tracking so play() can restart.
      // Do not run finish listeners (those are for intentional start→loop transitions).
      this.startedSoundList.remove(instance);
      return;
    }

    this.startedSoundList.remove(instance);
    listenableSoundInstance.finish();
    Runnable callback = this.finishListener;
    this.finishListener = null;
    if (callback != null) callback.run();
  }

  @Override
  public void addOnFinishListener(Runnable then) {
    Runnable previous = this.finishListener;
    this.finishListener =
        previous == null
            ? then
            : () -> {
              previous.run();
              then.run();
            };
  }

  @Override
  public void onFinish(Runnable then) {
    this.finishListener = then;
  }

  public void resumeAfterMute() {
    if (loopRequested && isStarted) {
      stopAllStarted();
      play();
    }
  }

  @Override
  public void tick() {
    super.tick();
    if (this.playGraceTicks > 0) {
      this.playGraceTicks--;
    }

    if (loopRequested && Ic2AudioSettings.scale(1) == 0) return;
    if (this.isStarted) {
      this.checkSoundFinished(this.onceInstance);
      this.checkSoundFinished(this.repeatInstance);
      this.checkSoundFinished(this.entityTrackingInstance);
      if (this.startedSoundList.isEmpty()
          && !this.isPlayingSound(this.onceInstance)
          && !this.isPlayingSound(this.repeatInstance)
          && !this.isPlayingSound(this.entityTrackingInstance)
          && this.playGraceTicks <= 0) {
        if (!this.playbackPending) this.isStarted = false;
      }
    }
  }
}
