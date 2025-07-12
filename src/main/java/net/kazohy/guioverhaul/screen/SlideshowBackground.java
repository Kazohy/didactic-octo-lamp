package net.kazohy.guioverhaul.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.kazohy.guioverhaul.ModIdentifier;

@Environment(EnvType.CLIENT)
public class SlideshowBackground {
  private static final Identifier[] BACKGROUNDS = {
    ModIdentifier.of("textures/gui/title/background/background_0.png"),
    ModIdentifier.of("textures/gui/title/background/background_1.png"),
    ModIdentifier.of("textures/gui/title/background/background_2.png"),
    ModIdentifier.of("textures/gui/title/background/background_3.png"),
    ModIdentifier.of("textures/gui/title/background/background_4.png"),
    ModIdentifier.of("textures/gui/title/background/background_5.png"),
    ModIdentifier.of("textures/gui/title/background/background_6.png"),
  };
  private static final Integer[][] BACKGROUND_SIZES = {
    {960, 528},
    {960, 528},
    {960, 528},
    {960, 528},
    {960, 528},
    {960, 528},
    {960, 528},
  };
  private static final float SLIDESHOW_IMAGE_LENGTH = 400f;
  private static final float TRANSITION_LENGTH = 30f;
  private static float t = TRANSITION_LENGTH;

  public static void registerBackgroundTextures(TextureManager textureManager) {
    for (Identifier background : BACKGROUNDS) {
      textureManager.registerTexture(background, new ResourceTexture(background));
    }
  }

  private static void renderBackground(DrawContext ctx, float a, int backgroundIndex, float alpha, int width, int height) {
    MatrixStack matrices = ctx.getMatrices();

    matrices.push();

    Identifier bg = BACKGROUNDS[backgroundIndex];
    Integer[] backgroundSize = BACKGROUND_SIZES[backgroundIndex];
    float baseScale = Math.max(width / (float) backgroundSize[0], height / (float) backgroundSize[1]);
    Vector2f overscan = new Vector2f(backgroundSize[0] * baseScale - width, backgroundSize[1] * baseScale - height);

    RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
    RenderSystem.setShaderTexture(0, bg);
    RenderSystem.enableBlend();
    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

    float scale = MathHelper.lerp(a, 1.1f, 1.2f);
    float angle = MathHelper.lerp(a, -1f, 3f);

    matrices.translate(width / 2f, height / 2f, 0f);
    matrices.scale(scale, scale, 1f);
    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
    ctx.drawTexture(
      RenderLayer::getGuiTextured,
      bg,
      (int) Math.floor(-overscan.getX()/2f - width/2f),
      (int) Math.floor(-overscan.getY()/2f - height/2f),
      0,
      0,
      width + (int) Math.ceil(overscan.getX()),
      height + (int) Math.ceil(overscan.getY()),
      1, 1, 1, 1
    );

    matrices.translate(width / 2f, height / 2f, 0f);

    matrices.pop();
  }

  public static void render(DrawContext ctx, float delta, int width, int height) {
    t += delta;

    int index = (int) Math.floor(t / SLIDESHOW_IMAGE_LENGTH) % BACKGROUNDS.length;
    int previousIndex = (index - 1 + BACKGROUNDS.length) % BACKGROUNDS.length;
    float a = (t % SLIDESHOW_IMAGE_LENGTH) / SLIDESHOW_IMAGE_LENGTH;
    float transition = MathHelper.clamp(a / (TRANSITION_LENGTH / SLIDESHOW_IMAGE_LENGTH), 0f, 1f);

    if (transition < 1f) {
      renderBackground(ctx, a + 1f, previousIndex, 1f, width, height);
    }
    renderBackground(ctx, a, index, transition, width, height);
  }
}
