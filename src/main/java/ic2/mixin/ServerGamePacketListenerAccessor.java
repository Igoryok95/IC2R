package ic2.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerAccessor {
  @Accessor("aboveGroundTickCount")
  void ic2$setFloatingTicks(int ticks);

  @Accessor("aboveGroundTickCount")
  int ic2$getFloatingTicks();

  @Accessor("clientIsFloating")
  void ic2$setClientFloating(boolean floating);

  @Accessor("clientIsFloating")
  boolean ic2$isClientFloating();
}
