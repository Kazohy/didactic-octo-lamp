package zone.oat.gmodtitlescreen.mixin;

import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zone.oat.gmodtitlescreen.screen.CustomTitleScreen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(TextureManager.class)
public class LoadCustomTitleScreenMixin {
  @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;loadTexturesAsync(Lnet/minecraft/client/texture/TextureManager;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
  private CompletableFuture<Void> injected(TextureManager textureManager, Executor executor) {
    return CustomTitleScreen.loadTexturesAsync(textureManager, executor);
  }
}
