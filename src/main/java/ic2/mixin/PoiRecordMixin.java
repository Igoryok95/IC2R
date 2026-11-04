package ic2.mixin;

import com.mojang.serialization.Codec;
import ic2.core.experimental.VillagePoiCapacity;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PoiRecord.class)
public abstract class PoiRecordMixin implements VillagePoiCapacity.RecordAccess {
  @Shadow private int freeTickets;

  @Override
  public void ic2$setFreeTickets(int freeTickets) {
    this.freeTickets = freeTickets;
  }

  @Inject(method = "codec", at = @At("RETURN"), cancellable = true)
  private static void ic2$migrateJobCapacity(
      Runnable setDirty, CallbackInfoReturnable<Codec<PoiRecord>> result) {
    result.setReturnValue(VillagePoiCapacity.wrap(result.getReturnValue()));
  }
}
