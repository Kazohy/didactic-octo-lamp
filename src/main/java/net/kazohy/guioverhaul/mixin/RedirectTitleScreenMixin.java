package net.kazohy.guioverhaul.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.kazohy.guioverhaul.screen.CustomTitleScreen;

// this sucks. lmao.
@Mixin(MinecraftClient.class)
public class RedirectTitleScreenMixin {
  @Redirect(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", opcode = Opcodes.PUTFIELD))
  private void injected(MinecraftClient instance, Screen value) {
    if (value instanceof TitleScreen) {
      //instance.currentScreen = new CustomTitleScreen();
      // this causes a fabric api error of all things, so let's do a little redundancy, shall we?
      instance.setScreen(new CustomTitleScreen());
      // yes. Wonderful. let's fucking call it Twice.
      // (i genuinely could not find anything that Isn't this)
    } else {
      instance.currentScreen = value;
    }
  }
}
