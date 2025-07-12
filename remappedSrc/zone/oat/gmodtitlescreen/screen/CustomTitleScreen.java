package net.kazohy.guioverhaul.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.terraformersmc.modmenu.gui.ModsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import net.kazohy.guioverhaul.Mod;
import net.kazohy.guioverhaul.ModIdentifier;
import net.kazohy.guioverhaul.PressableTextWithTooltipWidget;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Environment(EnvType.CLIENT)
public class CustomTitleScreen extends Screen {
  public static final Text VERSION_TEXT = Text.translatable("menu.gmod-title-screen.version", SharedConstants.getGameVersion().getName(), FabricLoader.getInstance().getModContainer("gmod-title-screen").get().getMetadata().getVersion().getFriendlyString());
  private static final Identifier ACCESSIBILITY_ICON_TEXTURE = new Identifier("textures/gui/accessibility.png");
  private final boolean isMinceraft;
  @Nullable
  private String splashText;
  private static final Identifier MINECRAFT_TITLE_TEXTURE = new Identifier("textures/gui/title/minecraft.png");
  private static final Identifier EDITION_TITLE_TEXTURE = new Identifier("textures/gui/title/edition.png");
  private static final Identifier GMOD_TITLE_TEXTURE = new ModIdentifier("textures/gui/title/gmod_title.png");

  private PressableTextWidget version;

  private class SmallPressableTextWidget extends PressableTextWidget {
    public SmallPressableTextWidget(int x, int y, int width, int height, Text text, PressAction onPress, TextRenderer textRenderer) {
      super(x, y, width, height, text, onPress, textRenderer);
    }

    public boolean shouldDownscale() {
      return this.width > CustomTitleScreen.this.width * 0.6f;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      matrices.push();
      if (shouldDownscale()) {
        matrices.translate(CustomTitleScreen.this.width, 0d, 0d);
        matrices.scale(0.5f, 0.5f, 1.0f);
        matrices.translate(-CustomTitleScreen.this.width, 0d, 0d);
      }

      int padding = 3;

      RenderSystem.disableBlend();
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      DrawableHelper.fill(matrices, version.getX() - padding, version.getY() - padding, version.getX() + version.getWidth() + padding * 2, version.getY() + version.getHeight() + padding * 2, 0xFF111111);

      super.renderButton(matrices, mouseX, mouseY, delta);

      matrices.pop();
    }
  }

  private static CompletableFuture<Void> loadBackgroundTexturesAsync(TextureManager textureManager, Executor executor) {
    return SlideshowBackground.loadBackgroundTexturesAsync(textureManager, executor);
  }

  private void openLink(String url) {
    this.client.setScreen(new ConfirmLinkScreen(openInBrowser -> {
      if (openInBrowser) {
        Util.getOperatingSystem().open(url);
      }

      this.client.setScreen(this);
    }, url, true));
  }

  public CustomTitleScreen() {
    super(Text.translatable("narrator.screen.title"));
    this.isMinceraft = (double)new Random().nextFloat() < 1.0E-4;
  }

  public static CompletableFuture<Void> loadTexturesAsync(TextureManager textureManager, Executor executor) {
    return CompletableFuture.allOf(
      textureManager.loadTextureAsync(MINECRAFT_TITLE_TEXTURE, executor),
      textureManager.loadTextureAsync(EDITION_TITLE_TEXTURE, executor),
      textureManager.loadTextureAsync(GMOD_TITLE_TEXTURE, executor),
      loadBackgroundTexturesAsync(textureManager, executor)
    );
  }

  @Override
  public boolean shouldPause() {
    return false;
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  private int getXPadding() {
    return this.width / 22;
  }

  private int getLogoY() {
    if (Mod.config.showGModLogo) {
      return this.height / 32;
    } else {
      return this.height / 12;
    }
  }

  private int getLogoHeight() {
    if (Mod.config.showGModLogo) {
      return 75;
    } else {
      return 50;
    }
  }

  private double getSplashTextX() {
    if (Mod.config.showGModLogo) {
      return 220d;
    } else {
      return 260d;
    }
  }

  @Override
  protected void init() {
    if (this.splashText == null) {
      this.splashText = this.client.getSplashTextLoader().get();
    }

    int versionWidth = this.textRenderer.getWidth(VERSION_TEXT) - 1;
    int versionX = this.width - versionWidth;
    int buttonsX = getXPadding();
    int buttonsY = getLogoY() * 2 + getLogoHeight();

    int buttonHeight = 12;
    int buttonSpacing = buttonHeight + 1;
    int buttonSectionSpacing = 6;

    this.addDrawableChild(new PressableTextWidget(
            buttonsX,
            buttonsY,
            130,
            buttonHeight,
            Text.translatable("menu.gmod-title-screen.singleplayer"),
            button -> this.client.setScreen(new SelectWorldScreen(this)),
            this.textRenderer
    ));

    this.addDrawableChild(new PressableTextWidget(
            buttonsX,
            buttonsY + buttonSpacing * 1,
            130,
            buttonHeight,
            Text.translatable("menu.gmod-title-screen.multiplayer"),
            button -> {
              this.client.setScreen(new MultiplayerScreen(this));
            },
            this.textRenderer
    ));

    boolean modsButtonActive = FabricLoader.getInstance().isModLoaded("modmenu");
    if (modsButtonActive) {
      var modsButton = this.addDrawableChild(new PressableTextWidget(
              buttonsX,
              buttonsY + buttonSpacing * 2 + buttonSectionSpacing,
              98,
              buttonHeight,
              Text.translatable("menu.gmod-title-screen.modmenu"),
              button -> this.client.setScreen(new ModsScreen(this)),
              textRenderer
      ));
      modsButton.active = true;
    } else {
      var modsButton = this.addDrawableChild(new PressableTextWithTooltipWidget(
              buttonsX,
              buttonsY + buttonSpacing * 2 + buttonSectionSpacing,
              98,
              buttonHeight,
              Text.translatable("menu.gmod-title-screen.modmenu").formatted(Formatting.GRAY),
              button -> {},
              textRenderer,
              Tooltip.of(Text.translatable("menu.gmod-title-screen.modmenu.not-installed"), Text.translatable("menu.gmod-title-screen.modmenu.not-installed"))
      ));
      modsButton.active = false;
    }

    this.addDrawableChild(new PressableTextWidget(
      buttonsX,
      buttonsY + buttonSpacing * 3 + buttonSectionSpacing,
      98,
      buttonHeight,
      Text.translatable("menu.gmod-title-screen.dupes").formatted(Formatting.GRAY),
      button -> {},
      textRenderer
    )).active = false;
    this.addDrawableChild(new PressableTextWidget(
      buttonsX,
      buttonsY + buttonSpacing * 4 + buttonSectionSpacing,
      98,
      buttonHeight,
      Text.translatable("menu.gmod-title-screen.demos").formatted(Formatting.GRAY),
      button -> {},
      textRenderer
    )).active = false;
    this.addDrawableChild(new PressableTextWidget(
      buttonsX,
      buttonsY + buttonSpacing * 5 + buttonSectionSpacing,
      98,
      buttonHeight,
      Text.translatable("menu.gmod-title-screen.saves").formatted(Formatting.GRAY),
      button -> {},
      textRenderer
    )).active = false;

    this.addDrawableChild(new PressableTextWidget(
      buttonsX,
      buttonsY + buttonSpacing * 6 + buttonSectionSpacing * 2,
      98,
      buttonHeight,
      Text.translatable("menu.gmod-title-screen.options"),
      button -> this.client.setScreen(new OptionsScreen(this, this.client.options)),
      textRenderer
    ));

    this.addDrawableChild(new PressableTextWidget(
      buttonsX,
      buttonsY + buttonSpacing * 7 + buttonSectionSpacing * 3,
      98,
      buttonHeight,
      Text.translatable("menu.gmod-title-screen.quit"),
      button -> this.client.scheduleStop(),
      textRenderer
    ));

    this.addDrawableChild(
      new TexturedButtonWidget(
        this.width - 22,
        this.height - 22,
        20,
        20,
        0,
        106,
        20,
        ButtonWidget.WIDGETS_TEXTURE,
        256,
        256,
        button -> this.client.setScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager())),
        Text.translatable("narrator.button.language")
      )
    );
    this.addDrawableChild(new TexturedButtonWidget(
      this.width - 22 - 22,
      this.height - 22,
      20,
      20,
      0,
      0,
      20,
      ACCESSIBILITY_ICON_TEXTURE,
      32,
      64,
      button -> this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options)),
      Text.translatable("narrator.button.accessibility")
    ));

    version = this.addDrawableChild(
      new SmallPressableTextWidget(
        versionX - 8, 6, versionWidth, 7, VERSION_TEXT, button -> openLink("https://git.oat.zone/oat/gmod-title-screen"), this.textRenderer
      )
    );
    version.setAlpha(0.8f);
  }

  @Override
  public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    int logoX = getXPadding();
    int logoY = getLogoY();

    SlideshowBackground.render(matrices, delta, this.width, this.height);

    RenderSystem.disableBlend();
    RenderSystem.setShader(GameRenderer::getPositionTexProgram);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    int gradientWidth = (int)(this.width * 0.7);

    matrices.push();
    matrices.translate(gradientWidth/2f, this.height/2f, 0);
    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90f));
    DrawableHelper.fillGradient(matrices, -(int)Math.floor(this.height/2f), -(int)Math.floor(gradientWidth/2f) - 1, (int)Math.ceil(this.height/2f), (int)Math.ceil(gradientWidth/2f), 0x7b000000, 0x00000000, 0);
    matrices.pop();

    DrawableHelper.fill(matrices, 0, this.height - 24, this.width, this.height, 0x70111111);

    if (Mod.config.showGModLogo) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      //RenderSystem.defaultBlendFunc();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.setShaderTexture(0, GMOD_TITLE_TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      this.drawTexture(matrices, logoX, logoY, 0, 0, 256, 256);

      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
    } else {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      RenderSystem.setShaderTexture(0, MINECRAFT_TITLE_TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.isMinceraft) {
        this.drawWithOutline(logoX, logoY, (x, y) -> {
          this.drawTexture(matrices, x + 0, y, 0, 0, 99, 44);
          this.drawTexture(matrices, x + 99, y, 129, 0, 27, 44);
          this.drawTexture(matrices, x + 99 + 26, y, 126, 0, 3, 44);
          this.drawTexture(matrices, x + 99 + 26 + 3, y, 99, 0, 26, 44);
          this.drawTexture(matrices, x + 155, y, 0, 45, 155, 44);
        });
      } else {
        this.drawWithOutline(logoX, logoY, (x, y) -> {
          this.drawTexture(matrices, x + 0, y, 0, 0, 155, 44);
          this.drawTexture(matrices, x + 155, y, 0, 45, 155, 44);
        });
      }
    }

    /*
    RenderSystem.setShaderTexture(0, EDITION_TITLE_TEXTURE);
    drawTexture(matrices, logoX + 88, logoY + 37, 0.0F, 0.0F, 98, 14, 128, 16);
    */

    if (this.splashText != null && Mod.config.showSplashText) {
      matrices.push();
      matrices.translate(getSplashTextX(), logoY + getLogoHeight() - 10, 0.0);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F));
      float h = 1.8F - MathHelper.abs(MathHelper.sin((float)(Util.getMeasuringTimeMs() % 1000L) / 1000.0F * (float) (Math.PI * 2)) * 0.1F);
      h = h * 100.0F / (float)(this.textRenderer.getWidth(this.splashText) + 32);
      matrices.scale(h, h, h);
      drawCenteredTextWithShadow(matrices, this.textRenderer, this.splashText, 0, -8, 16776960);
      matrices.pop();
    }

    super.render(matrices, mouseX, mouseY, delta);
  }
}
