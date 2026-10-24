package ic2.core.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import ic2.core.IC2;
import ic2.core.util.LogCategory;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class GlTexture implements Closeable {
  private static final Map<ResourceLocation, GlTexture> textures = new HashMap<>();
  private final ResourceLocation loc;
  protected int textureId;
  protected int width;
  protected int height;
  protected int canvasWidth;
  protected int canvasHeight;

  public GlTexture(ResourceLocation loc) {
    this.loc = loc;
  }

  public static void init() {
    ResourceManager manager = Minecraft.getInstance().getResourceManager();
    if (manager instanceof ReloadableResourceManager) {
      ((ReloadableResourceManager) manager)
          .registerReloadListener(
              (synchronizer,
                  managerx,
                  prepareProfiler,
                  applyProfiler,
                  prepareExecutor,
                  applyExecutor) -> {
                for (GlTexture texture : GlTexture.textures.values()) {
                  if (texture != null) {
                    texture.close();
                  }
                }

                GlTexture.textures.clear();
                return CompletableFuture.completedFuture(null);
              });
    } else {
      IC2.log.warn(LogCategory.General, "The resource manager {} is not reloadable.", manager);
    }
  }

  public static GlTexture get(ResourceLocation identifier) {
    GlTexture ret = textures.get(identifier);
    return ret != null ? ret : add(identifier, new GlTexture(identifier));
  }

  public static GlTexture add(ResourceLocation identifier, GlTexture texture) {
    try {
      texture.load(Minecraft.getInstance().getResourceManager());
    } catch (IOException e) {
      IC2.log.warn(LogCategory.General, "Can't load texture %s", identifier);
      texture.close();
      texture = null;
    }

    textures.put(identifier, texture);
    return texture;
  }

  protected void load(ResourceManager manager) throws IOException {
    Resource resource = manager.getResourceOrThrow(this.loc);

    try (InputStream is = resource.open()) {
      this.load(ImageIO.read(is));
    }
  }

  protected void load(BufferedImage img) {
    this.width = img.getWidth();
    this.height = img.getHeight();
    // Image renders the original resource through Minecraft's texture manager. Its UVs
    // must use that resource's actual dimensions, without a second padded GL allocation.
    this.canvasWidth = this.width;
    this.canvasHeight = this.height;
  }

  @Override
  public void close() {
    // Minecraft owns and releases the texture, including during resource reloads.
    this.textureId = 0;
  }

  public void bind() {
    this.textureId = Minecraft.getInstance().getTextureManager().getTexture(this.loc).getId();
    RenderSystem.bindTexture(this.textureId);
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }

  public int getCanvasWidth() {
    return this.canvasWidth;
  }

  public int getCanvasHeight() {
    return this.canvasHeight;
  }
}
