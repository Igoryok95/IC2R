package ic2.core.classic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Barrel composition and aging rules from Classic, independent of rendering and menus. */
public final class ClassicBrewState {
  public static final int EMPTY = 0;
  public static final int BEER = 1;
  public static final int RUM = 2;
  public static final int WHISKY = 5;
  public static final int POTION = 10;
  public static final int CAPACITY = 32000;
  public static final int POTION_CAPACITY = 3000;
  public static final int WHISKY_YEAR_TICKS = 1209600;
  public static final int[] POTION_DURATIONS = {
    1800, 1500, 1200, 960, 720, 600, 300, 150, 90, 30, 15
  };

  public int type;
  public int amount;
  public int wheat;
  public int hops;
  public int age;
  public int quality;
  public int alcohol;
  public int solids;
  public boolean opened;
  public boolean tapped;
  public String potion = "";

  public boolean isEmpty() {
    return type == EMPTY || amount <= 0;
  }

  public int capacity() {
    return type == POTION ? POTION_CAPACITY : CAPACITY;
  }

  public void tick() {
    if (isEmpty() || opened || tapped) return;
    if (age < Integer.MAX_VALUE) age++;
    if (type == BEER && quality < 5 && age >= beerStageTicks()) {
      age = 0;
      quality++;
    } else if (type == POTION && quality < 6 && age >= potionStageTicks()) {
      age = 0;
      quality++;
    } else if (type == WHISKY && quality < 50 && age >= WHISKY_YEAR_TICKS) {
      age = 0;
      quality++;
    }
  }

  public int beerStageTicks() {
    return (int) (24000 * Math.pow(3, quality == 4 ? 6 : quality));
  }

  public int potionStageTicks() {
    return (int) (5000 * Math.pow(3, quality));
  }

  public int rumTicks() {
    int buckets = amount / 1000;
    return (int) (1200D * Math.max(1, buckets) * Math.pow(0.95, Math.max(0, buckets - 1)));
  }

  public int addWater(int millibuckets) {
    if (opened
        || tapped
        || age > 600
        || millibuckets <= 0
        || !(canApply(BEER) || canApply(WHISKY) || (canApply(POTION) && !potion.isEmpty())))
      return 0;
    int accepted = Math.min(millibuckets, capacity() - amount);
    if (accepted <= 0) return 0;
    if (type == EMPTY) type = BEER;
    amount += accepted;
    return accepted;
  }

  public int addWheat(int requested, RandomSource random) {
    if (opened || tapped || !canApply(BEER)) return 0;
    int accepted = Math.clamp(requested, 0, 64 - wheat);
    if (accepted > 0) {
      type = BEER;
      wheat += accepted;
      alterComposition(random);
    }
    return accepted;
  }

  public int addHops(int requested, RandomSource random) {
    if (opened || tapped || !canApply(BEER)) return 0;
    int accepted = Math.clamp(requested, 0, 64 - hops);
    if (accepted > 0) {
      type = BEER;
      hops += accepted;
      alterComposition(random);
    }
    return accepted;
  }

  public int addSugarCane(int requested) {
    if (opened || tapped || age > 600 || type != EMPTY) return 0;
    int accepted = Math.clamp(requested, 0, 32 - amount / 1000);
    if (accepted > 0) {
      type = RUM;
      amount += accepted * 1000;
    }
    return accepted;
  }

  public int addWhiskyGrist(int requested) {
    if (opened || tapped || age > 600 || quality > 0 || !canApply(WHISKY)) return 0;
    int accepted = Math.clamp(requested, 0, 16 - hops);
    if (accepted > 0) {
      type = WHISKY;
      hops += accepted;
    }
    return accepted;
  }

  public boolean beginPotion() {
    if (opened || tapped || type != EMPTY) return false;
    type = POTION;
    return true;
  }

  public boolean selectPotion(String effect) {
    if (opened || tapped || type != POTION || age > 0 || effect == null || effect.isEmpty())
      return false;
    potion = effect;
    return true;
  }

  public int addPotionModifier(int requested, boolean glowstone) {
    if (opened || tapped || !canApply(POTION) || potion.isEmpty() || age > 0) return 0;
    int accepted = Math.clamp(requested, 0, 20 - (glowstone ? hops : wheat));
    if (glowstone) hops += accepted;
    else wheat += accepted;
    return accepted;
  }

  private boolean canApply(int desired) {
    return type == EMPTY || type == desired;
  }

  private void alterComposition(RandomSource random) {
    switch (quality) {
      case 0 -> age = 0;
      case 1 -> {
        if (random.nextBoolean()) quality = 0;
        else if (random.nextBoolean()) quality = 5;
      }
      case 2 -> {
        if (random.nextBoolean()) quality = 5;
      }
      default -> quality = 5;
    }
  }

  /** Returns true when opening spoiled potion mash must create the original poison cloud. */
  public boolean open() {
    if (opened || isEmpty()) return false;
    int calculatedAlcohol = alcoholLevel();
    int calculatedSolids = solidRatio();
    opened = true;
    if (type == BEER) {
      alcohol = calculatedAlcohol;
      if (wheat == 0 ? hops > 0 : (float) hops / wheat >= 5) {
        quality = 5;
        alcohol = 0;
      }
      solids = calculatedSolids;
      if ((float) (hops + wheat) / (amount / 1000F) >= 4) {
        quality = 5;
        solids = 0;
      }
    } else if (type == POTION) {
      if (quality == 6) {
        drain(amount);
        return true;
      }
      solids =
          quality > 0 && wheat > 0
              ? (int) Mth.clamp(quality * (wheat / 10F), 0, POTION_DURATIONS.length - 1)
              : 0;
      alcohol = quality > 0 && hops > 0 ? (int) Mth.clamp(quality * 0.5F * (hops / 10F), 0, 2) : 0;
      if (alcohol == 1) solids = (int) (solids * 0.5F);
      else if (alcohol == 2) solids = (int) (solids * 0.25F);
    } else if (type == WHISKY) {
      amount = (int) (amount * (1 - quality / 100D));
      amount = (int) (amount * (hops / 16D));
      alcohol = whiskyYear();
    }
    return false;
  }

  public int whiskyYear() {
    return quality >= 50
        ? 5
        : quality >= 25 ? 4 : quality >= 15 ? 3 : quality >= 12 ? 2 : quality >= 10 ? 1 : 0;
  }

  public int alcoholLevel() {
    if (opened) return alcohol;
    float ratio = wheat > 0 ? (float) hops / wheat : hops > 0 ? 10 : 0;
    return ratio >= 4
        ? 6
        : ratio >= 3
            ? 5
            : ratio >= 2 ? 4 : ratio > 0.5F ? 3 : ratio > 0.33333334F ? 2 : ratio > 0.25F ? 1 : 0;
  }

  public int solidRatio() {
    if (opened) return solids;
    float ratio = amount > 0 ? (hops + wheat) / (amount / 1000F) : 10;
    return ratio >= 2.4F
        ? 6
        : ratio >= 2
            ? 5
            : ratio > 1 ? 4 : ratio == 1 ? 3 : ratio > 0.5F ? 2 : ratio > 0.4166667F ? 1 : 0;
  }

  public record Serving(
      int type,
      int count,
      int quality,
      int solids,
      int alcohol,
      int rumProgress,
      int whiskyYear,
      String potion) {}

  public Serving serve(int requested, int volume) {
    if (requested <= 0 || volume <= 0 || !tapped || isEmpty()) return null;
    if (!opened && open()) return null;
    if (isEmpty()) return null;
    int count =
        Math.min(requested, type == WHISKY ? Math.max(1, amount / volume) : amount / volume);
    if (count <= 0) return null;
    int rum = type == RUM ? (int) Math.min(100, (long) age * 100 / rumTicks()) : 0;
    var serving =
        new Serving(
            type, count, quality, solids, alcohol, rum, type == WHISKY ? alcohol : 0, potion);
    int rumAge = type == RUM ? (int) ((long) age * 100 / rumTicks()) : 0;
    boolean wasRum = type == RUM;
    drain(count * volume);
    if (wasRum && !isEmpty()) age = (int) (rumAge / 100F * rumTicks());
    return serving;
  }

  public void drain(int millibuckets) {
    amount = Math.max(0, amount - Math.max(0, millibuckets));
    if (amount > 0) return;
    type = EMPTY;
    age = quality = wheat = hops = alcohol = solids = 0;
    opened = false;
    potion = "";
  }

  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putInt("type", type);
    tag.putInt("fluid", amount);
    tag.putInt("age", age);
    tag.putInt("quality", quality);
    tag.putInt("wheat", wheat);
    tag.putInt("hops", hops);
    tag.putInt("level", alcohol);
    tag.putInt("solid", solids);
    tag.putBoolean("opened", opened);
    tag.putBoolean("tap", tapped);
    tag.putString("potion", potion);
    return tag;
  }

  public void load(CompoundTag tag) {
    int incoming = tag.getInt("type");
    type =
        incoming == BEER || incoming == RUM || incoming == WHISKY || incoming == POTION
            ? incoming
            : EMPTY;
    amount = Math.clamp(tag.getInt("fluid"), 0, capacity());
    age = Math.max(0, tag.getInt("age"));
    quality = Math.clamp(tag.getInt("quality"), 0, type == WHISKY ? 50 : type == POTION ? 6 : 5);
    wheat = Math.clamp(tag.getInt("wheat"), 0, type == POTION ? 20 : 64);
    hops = Math.clamp(tag.getInt("hops"), 0, type == POTION ? 20 : type == WHISKY ? 16 : 64);
    alcohol = Math.clamp(tag.getInt("level"), 0, 6);
    solids = Math.clamp(tag.getInt("solid"), 0, type == POTION ? 10 : 6);
    opened = tag.getBoolean("opened");
    tapped = tag.getBoolean("tap");
    potion = tag.getString("potion");
  }
}
