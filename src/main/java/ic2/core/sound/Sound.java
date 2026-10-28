package ic2.core.sound;

public class Sound {
  public void play() {}

  public void playWithIntro(net.minecraft.sounds.SoundEvent intro) {
    play();
  }

  public void stop() {}

  public void playOnce() {}

  public boolean isPlaying() {
    return false;
  }

  public void addOnFinishListener(Runnable then) {}

  public void onFinish(Runnable then) {}

  public void setSourceItem(net.minecraft.world.item.Item item) {}

  public void tick() {}
}
