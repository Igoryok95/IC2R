package ic2.core.experimental;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/** Experimental 2.8.222 barrel state, with quantities measured in original litre units. */
public final class ExperimentalBrewState {
  public int type, amount, age, hops, wheat, solids, alcohol, quality;
  public boolean opened;

  public void tick(boolean tapped) {
    if (type == 0 || amount <= 0 || tapped) return;
    if (age < Integer.MAX_VALUE) age++;
    if (type == 1 && quality < 5 && age >= 24000D * Math.pow(3, quality == 4 ? 6 : quality)) {
      age = 0;
      quality++;
    }
  }

  public boolean add(int ingredient, int count, RandomSource random) {
    if (opened || count <= 0) return false;
    if (ingredient == 0 && (type == 0 || type == 1) && amount + count <= 32) {
      type = 1;
      amount += count;
      return true;
    }
    if ((ingredient == 1 || ingredient == 2)
        && (type == 0 || type == 1)
        && (ingredient == 1 ? wheat : hops) + count <= 64) {
      type = 1;
      if (ingredient == 1) wheat += count;
      else hops += count;
      if (quality == 0) age = 0;
      else if (quality == 1) {
        if (random.nextBoolean()) quality = 0;
        else if (random.nextBoolean()) quality = 5;
      } else if (quality == 2) {
        if (random.nextBoolean()) quality = 5;
      } else quality = 5;
      return true;
    }
    if (ingredient == 3 && (type == 0 || type == 2) && age <= 600 && amount + count <= 32) {
      type = 2;
      amount += count;
      return true;
    }
    return false;
  }

  public void open() {
    if (opened) return;
    opened = true;
    if (type != 1) return;
    float ratio = hops <= 0 ? 0 : (float) hops / wheat;
    alcohol =
        ratio <= .25F
            ? 0
            : ratio <= 1F / 3
                ? 1
                : ratio <= .5F ? 2 : ratio < 2 ? 3 : (int) Math.min(6, Math.floor(ratio) + 2);
    if (ratio >= 5) quality = 5;
    ratio = amount <= 0 ? Float.POSITIVE_INFINITY : (float) (hops + wheat) / amount;
    solids =
        ratio <= 5F / 12
            ? 0
            : ratio <= .5F
                ? 1
                : ratio < 1 ? 2 : ratio == 1 ? 3 : ratio < 2 ? 4 : ratio < 2.4F ? 5 : 6;
    if (ratio >= 4) quality = 5;
  }

  public int value() {
    if (type == 0 || amount <= 0) return 0;
    open();
    int details = type == 1 ? (quality << 6) | (alcohol << 3) | solids : progress();
    return (details << 7) | ((amount - 1) << 2) | type;
  }

  public int progress() {
    return Math.min(100, (int) ((long) age * 100 / Math.max(1, rumTime(amount))));
  }

  public static int rumTime(int amount) {
    return (int) (1200D * amount * Math.pow(.95, amount - 1));
  }

  public boolean drain() {
    if (type == 0 || amount <= 0) return false;
    open();
    int progress = progress();
    amount--;
    if (type == 2) age = (int) ((long) progress * rumTime(amount) / 100);
    if (amount == 0) load(new CompoundTag());
    return true;
  }

  public CompoundTag save() {
    var nbt = new CompoundTag();
    nbt.putInt("type", type);
    nbt.putInt("amount", amount);
    nbt.putInt("age", age);
    nbt.putInt("hops", hops);
    nbt.putInt("wheat", wheat);
    nbt.putInt("solids", solids);
    nbt.putInt("alcohol", alcohol);
    nbt.putInt("quality", quality);
    nbt.putBoolean("opened", opened);
    return nbt;
  }

  public void load(CompoundTag nbt) {
    type = Math.clamp(nbt.getInt("type"), 0, 2);
    amount = Math.clamp(nbt.getInt("amount"), 0, 32);
    age = Math.max(0, nbt.getInt("age"));
    hops = Math.clamp(nbt.getInt("hops"), 0, 64);
    wheat = Math.clamp(nbt.getInt("wheat"), 0, 64);
    solids = Math.clamp(nbt.getInt("solids"), 0, 6);
    alcohol = Math.clamp(nbt.getInt("alcohol"), 0, 6);
    quality = Math.clamp(nbt.getInt("quality"), 0, 5);
    opened = nbt.getBoolean("opened");
  }
}
