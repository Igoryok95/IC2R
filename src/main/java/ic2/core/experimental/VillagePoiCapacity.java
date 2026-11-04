package ic2.core.experimental;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

/** Migrates only restored IC2 job reservations from the former two-worker capacity. */
public final class VillagePoiCapacity {
  public static final String CAPACITY_KEY = "ic2_job_capacity";

  public interface RecordAccess {
    void ic2$setFreeTickets(int freeTickets);
  }

  public static Codec<PoiRecord> wrap(Codec<PoiRecord> original) {
    return original.mapResult(
        new Codec.ResultFunction<PoiRecord>() {
          @Override
          public <T> DataResult<Pair<PoiRecord, T>> apply(
              DynamicOps<T> ops, T input, DataResult<Pair<PoiRecord, T>> result) {
            return result.map(
                pair -> {
                  PoiRecord record = pair.getFirst();
                  if (restored(record)) {
                    int previousCapacity =
                        ops.get(input, CAPACITY_KEY)
                            .flatMap(ops::getNumberValue)
                            .result()
                            .map(Number::intValue)
                            .orElse(2);
                    int capacity = record.getPoiType().value().maxTickets();
                    int occupied = Math.max(0, previousCapacity - record.getFreeTickets());
                    ((RecordAccess) record).ic2$setFreeTickets(Math.max(0, capacity - occupied));
                  }
                  return pair;
                });
          }

          @Override
          public <T> DataResult<T> coApply(
              DynamicOps<T> ops, PoiRecord record, DataResult<T> result) {
            if (!restored(record)) return result;
            return result.map(
                value ->
                    ops.set(
                        value,
                        CAPACITY_KEY,
                        ops.createInt(record.getPoiType().value().maxTickets())));
          }
        });
  }

  private static boolean restored(PoiRecord record) {
    return record
        .getPoiType()
        .unwrapKey()
        .map(
            key ->
                key.location().getNamespace().equals("ic2")
                    && RestoredVillagers.ALL.containsKey(key.location().getPath()))
        .orElse(false);
  }

  private VillagePoiCapacity() {}
}
