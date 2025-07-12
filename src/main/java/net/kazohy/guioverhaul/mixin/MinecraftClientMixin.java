package net.kazohy.guioverhaul.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.kazohy.guioverhaul.screen.CustomTitleScreen;

// this sucks. lmao.
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
  @Shadow @Final private TextureManager textureManager;

  @Inject(method = "Lnet/minecraft/client/MinecraftClient;<init>(Lnet/minecraft/client/RunArgs;)V", at = @At("TAIL"))
  private void constructor(RunArgs args, CallbackInfo ci) {
    CustomTitleScreen.registerTextures(textureManager);
  }
}
