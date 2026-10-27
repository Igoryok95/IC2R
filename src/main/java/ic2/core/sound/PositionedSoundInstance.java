package ic2.core.sound;

import ic2.core.IC2;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class PositionedSoundInstance extends SimpleSoundInstance
    implements ListenableSoundInstance, TickableSoundInstance {
  private Runnable onFinish = null;
  private boolean cancelled;

  public PositionedSoundInstance(
      SoundEvent sound, SoundSource category, float volume, float pitch, BlockPos pos) {
    super(sound, category, volume, pitch, IC2.random, pos);
  }

  @Override
  public float getVolume() {
    return Ic2AudioSettings.scale(super.getVolume());
  }

  @Override
  public void addOnFinishListener(Runnable then) {
    Runnable previous = this.onFinish;
    this.onFinish =
        previous == null
            ? then
            : () -> {
              previous.run();
              then.run();
            };
  }

  @Override
  public void onFinish(Runnable then) {
    this.onFinish = then;
  }

  @Override
  public void clearOnFinishListener() {
    this.onFinish = null;
  }

  @Override
  public void finish() {
    Runnable callback = this.onFinish;
    this.onFinish = null;
    if (!this.cancelled && callback != null) callback.run();
  }

  public void cancel() {
    this.cancelled = true;
    this.clearOnFinishListener();
  }

  @Override
  public boolean canPlaySound() {
    return !this.cancelled;
  }

  @Override
  public boolean isStopped() {
    return this.cancelled;
  }

  @Override
  public void tick() {}
}
