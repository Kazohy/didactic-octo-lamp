package zone.oat.gmodtitlescreen.screen;


// Import required Minecraft and Fabric classes
import net.minecraft.server.MinecraftServer;
import java.nio.file.Paths;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import java.nio.file.Path;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.slf4j.LoggerFactory;
import zone.oat.gmodtitlescreen.*;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.texture.PlayerSkinProvider;


import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;

import com.mojang.authlib.GameProfile;
import zone.oat.gmodtitlescreen.listener.WorldCreationListener;

import java.util.Optional;
import java.util.Objects;

import static net.fabricmc.loader.impl.FabricLoaderImpl.MOD_ID;


/**
 * CustomTitleScreen is the main menu/title screen class that replaces or modifies the default Minecraft title screen.
 * It customizes layout, textures, recent worlds/servers displays, player avatars, and supports a "GMod-like" user experience.
 */
@Environment(EnvType.CLIENT)
public class CustomTitleScreen extends Screen {

    private static String lastLoadedWorld = "";
    @Nullable
    private Identifier lastWorldIconTexture;

    /** Set the last loaded world name for recents display */
    public static void setLastLoadedWorld(String name) {
        lastLoadedWorld = name;
    }
    /** Get the last loaded world name */
    /** Set the last loaded server name for recents (note: this is a bug, see below) */
    public static void setLastLoadedServer(String name) {
        lastLoadedWorld = name; // <--- BUG!!! Should set lastLoadedServer, not lastLoadedWorld
    }
    /** Get the last loaded server name */
    public static String getLastLoadedServer() {
        return lastLoadedWorld; // <--- BUG!!! Should return lastLoadedServer
    }

    // The version string that shows at the bottom/top of the menu, supports translations
    public static final Text VERSION_TEXT = Text.translatable(
            "menu.gmod-title-screen.version",
            SharedConstants.getGameVersion().getName(),
            FabricLoader.getInstance().getModContainer("gmod-title-screen").get().getMetadata().getVersion().getFriendlyString()
    );

    // Used to render Minecraft or custom logo, passed to the super class if needed
    private final LogoDrawer logoDrawer;

    // (Optional) "Splash" random text, like the yellow one on stock Minecraft title
    @Nullable
    private SplashTextRenderer splashText;

    // Texture resource locations for UI elements
    private static final Identifier GMOD_TITLE_TEXTURE = ModIdentifier.of("textures/gui/title/gmod_title.png");
    private static final Identifier AESTHETIC_ELEMENT_DOWN = ModIdentifier.of("textures/gui/title/aesthetic_element_down.png");
    private static final Identifier AESTHETIC_ELEMENT_UP = ModIdentifier.of("textures/gui/title/aesthetic_elemnt_up.png");
    private static final Identifier NORMAL_BUTTON_TEXTURE = ModIdentifier.of("textures/gui/title/normal_button.png");
    private static final Identifier REALMS_BUTTON_TEXTURE = ModIdentifier.of("textures/gui/title/realms_button.png");
    private static final Identifier NEWS_BUTTON_TEXTURE = ModIdentifier.of("textures/gui/title/news_button.png");
    private static final Identifier PLAYER_WIDGET_BUTTON = ModIdentifier.of("textures/gui/title/player_widget.png");
    private static final Identifier RECENTS_BUTTONS = ModIdentifier.of("textures/gui/title/recents_buttons.png");

    // Version text widget reference for layout/rendering
    private PressableTextWidget version;

    /**
     * SmallPressableTextWidget represents a reduced (smaller) version of a clickable text widget
     * with built-in dynamic scaling if it doesn't fit the screen.
     */
    private class SmallPressableTextWidget extends PressableTextWidget {
        public SmallPressableTextWidget(int x, int y, int width, int height, Text text, PressAction onPress, TextRenderer textRenderer) {
            super(x, y, width, height, text, onPress, textRenderer);
        }
        // Returns true if widget draws too wide for the screen and should scale down
        public boolean shouldDownscale() {
            return this.width > CustomTitleScreen.this.width * 0.6f;
        }
        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            MatrixStack matrices = ctx.getMatrices();
            matrices.push();
            if (shouldDownscale()) {
                // Downscale widget if it's too wide
                matrices.translate(CustomTitleScreen.this.width, 0d, 0d);
                matrices.scale(0.5f, 0.5f, 1.0f);
                matrices.translate(-CustomTitleScreen.this.width, 0d, 0d);
            }
            int padding = 3;
            RenderSystem.disableBlend();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            ctx.fill(version.getX() - padding, version.getY() - padding, version.getX() + version.getWidth() + padding * 2, version.getY() + version.getHeight() + padding * 2, 0xFF111111);
            super.renderWidget(ctx, mouseX, mouseY, delta);
            matrices.pop();
        }
    }

    /**
     * Utility function to pop a Minecraft confirmation screen for a web link (safety).
     */
    private void openLink(String url) {
        this.client.setScreen(new ConfirmLinkScreen(openInBrowser -> {
            if (openInBrowser) {
                Util.getOperatingSystem().open(url);
            }
            this.client.setScreen(this);
        }, url, true));
    }

    /** Constructor to optionally take a custom LogoDrawer */
    public CustomTitleScreen() {
        this(null);
    }

    /**
     * Registers required textures on the TextureManager for the custom title screen and its widgets.
     */
    public static void registerTextures(TextureManager textureManager) {
        textureManager.registerTexture(GMOD_TITLE_TEXTURE, new ResourceTexture(GMOD_TITLE_TEXTURE));
        textureManager.registerTexture(AESTHETIC_ELEMENT_DOWN, new ResourceTexture(AESTHETIC_ELEMENT_DOWN));
        textureManager.registerTexture(AESTHETIC_ELEMENT_UP, new ResourceTexture(AESTHETIC_ELEMENT_UP));
        textureManager.registerTexture(NORMAL_BUTTON_TEXTURE, new ResourceTexture(NORMAL_BUTTON_TEXTURE));
        textureManager.registerTexture(REALMS_BUTTON_TEXTURE, new ResourceTexture(REALMS_BUTTON_TEXTURE));
        textureManager.registerTexture(NEWS_BUTTON_TEXTURE, new ResourceTexture(NEWS_BUTTON_TEXTURE));
        textureManager.registerTexture(PLAYER_WIDGET_BUTTON, new ResourceTexture(PLAYER_WIDGET_BUTTON));
        textureManager.registerTexture(RECENTS_BUTTONS, new ResourceTexture(RECENTS_BUTTONS));
        SlideshowBackground.registerBackgroundTextures(textureManager);
    }

    /** Constructor that optionally injects a custom logo renderer, else uses Minecraft's default style */
    public CustomTitleScreen(@Nullable LogoDrawer logoDrawer) {
        super(Text.translatable("narrator.screen.title"));
        this.logoDrawer = Objects.requireNonNullElseGet(logoDrawer, () -> new LogoDrawer(false));
    }

    /** Suppresses pause when this screen is open (not the game menu, but a main menu) */
    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Suppresses closing with Escape (keeps menu open for interaction) */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    // --- Utility/Helper Methods ---

    /** Computes horizontal padding for the layout depending on screen size */
    private int getXPadding() {
        return this.width / 22;
    }
    /** Computes Y coordinate for main logo depending on config options */
    private int getLogoY() {
        if (Mod.config.showGModLogo) {
            return this.height / 32;
        } else {
            return this.height / 12;
        }
    }
    /** Determines logo height based on config (larger for the GMod logo style) */
    private int getLogoHeight() {
        if (Mod.config.showGModLogo) {
            return 75;
        } else {
            return 50;
        }
    }
    /** Computes X coordinate for splash text position based on the logo */
    private double getSplashTextX() {
        if (Mod.config.showGModLogo) {
            return 220d;
        } else {
            return 260d;
        }
    }

    /**
     * Initialization logic for the title screen (populate buttons, widgets, and banners).
     * This is only run *once* when the screen is first displayed.
     */
    @Override
    protected void init() {
        if (this.splashText == null) {
            this.splashText = this.client.getSplashTextLoader().get();
        }


        // Calculate geometry/placement for the player avatar widget on the right
        int playerWidgetX = this.width / 2 + this.width * (59 / 2) / 1800;
        int playerWidgetY = this.height / 2 - this.height * 298 / 1080 + 279 * this.height / 1080;
        int playerWidgetWidth = 409 * this.width / 1800;
        int playerWidgetHeight = playerWidgetWidth * 280 / 409;

        // Build a fake or real GameProfile for the avatar (for the main menu, player is often null so fallback is necessary)
        GameProfile profile;
        if (client.player != null) {
            profile = client.player.getGameProfile();
        } else { // Main menu fallback for skin display
            String username = client.getSession().getUsername();
            profile = new GameProfile(
                    UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)),
                    username
            );
        }

        // Set up skin supplier and model for skin display
        PlayerSkinProvider skinProvider = MinecraftClient.getInstance().getSkinProvider();
        Supplier<SkinTextures> skinSupplier = skinProvider.getSkinTexturesSupplier(profile);
        LoadedEntityModels models = MinecraftClient.getInstance().getLoadedEntityModels();
        PlayerSkinWidget playerSkin = new PlayerSkinWidget(64, 88, models, skinSupplier);

        // Use reflection to control the skin rotation on the widget (for extra flavor/style)
        try {
            Field xRotField = PlayerSkinWidget.class.getDeclaredField("xRotation");
            Field yRotField = PlayerSkinWidget.class.getDeclaredField("yRotation");
            xRotField.setAccessible(true);
            yRotField.setAccessible(true);
            xRotField.setFloat(playerSkin, -15.0f);
            yRotField.setFloat(playerSkin, -30.0f);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.addDrawableChild(playerSkin);
        playerSkin.setPosition(playerWidgetX + playerWidgetWidth / 2 - 32, playerWidgetY + playerWidgetHeight / 2 - 32);

        // Set up default button textures in the render pipeline and filtering
        RenderSystem.setShaderTexture(0, NORMAL_BUTTON_TEXTURE);
        RenderSystem.setShaderTexture(1, NORMAL_BUTTON_TEXTURE);
        RenderSystem.setShaderTexture(2, NORMAL_BUTTON_TEXTURE);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        int versionWidth = this.textRenderer.getWidth(VERSION_TEXT) - 1;

        // Setup button texture sets (hovered, disabled variants can be swapped if assets change in the future)
        ButtonTextures normalButtonTextures = new ButtonTextures(
                NORMAL_BUTTON_TEXTURE, // normal
                NORMAL_BUTTON_TEXTURE, // disabled
                NORMAL_BUTTON_TEXTURE  // hovered
        );
        ButtonTextures realmsButtonHitbox = new ButtonTextures(null, null, null);

        // Geometry calculations for buttons
        int buttonWidth = 408 * this.width / 1800;
        int buttonheight = buttonWidth * 68 / 408;
        int buttonX = this.width / 2 - buttonWidth - this.width * (59 / 2) / 1800;
        int buttonY = this.height / 2 - this.height * 298 / 1080;

        // Multirow button layout

        // "Worlds" button - opens singleplayer world select
        this.addDrawableChild(new TexturedButtonWidget(
                buttonX,
                buttonY,
                buttonWidth,
                buttonheight,
                normalButtonTextures,
                button -> this.client.setScreen(new SelectWorldScreen(this)),
                Text.literal("Options")
        ));

        // "Servers" button - opens multiplayer server list
        this.addDrawableChild(new TexturedButtonWidget(
                buttonX,
                buttonY + this.height * 17 / 1080 + buttonheight,
                buttonWidth,
                buttonheight,
                normalButtonTextures,
                button -> this.client.setScreen(new MultiplayerScreen(this)),
                Text.literal("Options")
        ));

        // Only show "Mods" button if 'modmenu' present
        boolean modsButtonActive = FabricLoader.getInstance().isModLoaded("modmenu");

        // "Settings" button - opens Options screen
        this.addDrawableChild(new TexturedButtonWidget(
                buttonX,
                buttonY + 3 * (this.height * 17 / 1080 + buttonheight),
                buttonWidth,
                buttonheight,
                normalButtonTextures,
                button -> this.client.setScreen(new OptionsScreen(this, this.client.options)),
                Text.literal("Options")
        ));

        // "Realms" button - opens Minecraft Realms (third party servers)
        this.addDrawableChild(new PressableTextWidget(
                buttonX,
                buttonY + 2 * (this.height * 17 / 1080 + buttonheight),
                buttonWidth,
                buttonheight,
                Text.literal(""),
                button -> this.client.setScreen(new RealmsMainScreen(this)),
                textRenderer
        ));

        // "Accessibility Options" button
        this.addDrawableChild(new TexturedButtonWidget(
                buttonX,
                buttonY + 4 * (this.height * 17 / 1080 + buttonheight),
                buttonWidth,
                buttonheight,
                normalButtonTextures,
                button -> this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options)),
                Text.translatable("menu.gmod-title-screen.accessibility")
        ));

        // "Quit Game" button - exits the application
        this.addDrawableChild(new PressableTextWidget(
                buttonX,
                buttonY + 6 * (this.height * 17 / 1080 + buttonheight),
                buttonWidth,
                buttonheight,
                Text.literal(""),
                button -> this.client.scheduleStop(),
                textRenderer
        ));

    } // end init()

    /**
     * Renders the whole custom title screen including backgrounds, gradients, buttons, icons, and splash/version text.
     */
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = ctx.getMatrices();

        // --- Draw shaded angled black gradient background ---
        int logoX = this.width / 2 + 500;
        int buttonWidth = 408 * this.width / 1800;
        int logoXMiddle = this.width / 2 + 400;
        int logoY = getLogoY();
        int gradientWidth = (int)(this.width * 0.7);

        matrices.push();
        matrices.translate(gradientWidth / 2f, this.height / 2f, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90f));
        ctx.fillGradient(
                -(int) Math.floor(this.height / 2f),
                -(int) Math.floor(gradientWidth / 2f) - 1,
                (int) Math.ceil(this.height / 2f),
                (int) Math.ceil(gradientWidth / 2f),
                0x7b000000, 0x00000000, 0
        );
        matrices.pop();

        int aestheticElementWidth = this.width;

        // --- Set up all used textures in the render pipeline now ---
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, AESTHETIC_ELEMENT_DOWN);
        RenderSystem.setShaderTexture(1, AESTHETIC_ELEMENT_UP);
        RenderSystem.setShaderTexture(2, NORMAL_BUTTON_TEXTURE);
        RenderSystem.setShaderTexture(3, REALMS_BUTTON_TEXTURE);
        RenderSystem.setShaderTexture(4, NEWS_BUTTON_TEXTURE);
        RenderSystem.setShaderTexture(5, PLAYER_WIDGET_BUTTON);
        RenderSystem.setShaderTexture(5, RECENTS_BUTTONS);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        // Render default clickable children (buttons/widgets)
        super.render(ctx, mouseX, mouseY, delta);

        int buttonHeight = buttonWidth * 68 / 408;
        int buttonX = this.width / 2 - buttonWidth - this.width * (59 / 2) / 1800;
        int buttonY = this.height / 2 - this.height * 298 / 1080;
        int recntsButtonY = buttonY;
        int playerWidgetX = this.width / 2 + this.width * (59 / 2) / 1800;
        int playerWidgetY;
        int playerWidgetWidth = 409 * this.width / 1800;
        int playerWidgetHeight = playerWidgetWidth * 280 / 409;
        int spacing = this.height * 17 / 1080 + buttonHeight;
        int actualFontHeight = Math.round(buttonHeight / 4.25f);
        float vanillaFontHeight = 9f;
        int buttonRenderWidth = 408 * this.width / 1800; // Actual button draw width (possibly differs from hitboxes)
        int buttonRenderHeight = buttonRenderWidth * 68 / 408;
        int buttonBaseX = this.width / 2 - buttonRenderWidth - this.width * (59 / 2) / 1800;
        int buttonCurrentY = this.height / 2 - this.height * 298 / 1080; // Y for first button, increases as buttons are drawn

        // --- Draw each button background ---
        for (int i = 0; i < 7; i++) {
            if ((i + 1) % 3 != 0) {
                ctx.drawTexture(RenderLayer::getGuiTextured, NORMAL_BUTTON_TEXTURE, buttonX, buttonY, 0f, 0f, buttonWidth, buttonHeight, buttonWidth, buttonHeight);
            } else if (i == 2) {
                ctx.drawTexture(RenderLayer::getGuiTextured, REALMS_BUTTON_TEXTURE, buttonX, buttonY, 0f, 0f, buttonWidth, buttonHeight, buttonWidth, buttonHeight);
            } else {
                ctx.drawTexture(RenderLayer::getGuiTextured, NEWS_BUTTON_TEXTURE, buttonX, buttonY, 0f, 0f, buttonWidth, buttonHeight, buttonWidth, buttonHeight);
            }

            // Button labels; These should use language keys for actual localization
            Text[] labels = new Text[] {
                    Text.translatable("menu.gmod-title-screen.singleplayer"),
                    Text.translatable("menu.gmod-title-screen.multiplayer"),
                    Text.translatable("menu.gmod-title-screen.realms"),
                    Text.translatable("menu.gmod-title-screen.options"),
                    Text.translatable("menu.gmod-title-screen.modmenu"),
                    Text.translatable("menu.gmod-title-screen.accessibility"),
                    Text.translatable("menu.gmod-title-screen.quit")
            };

            Text label = labels[i];
            int labelWidth = textRenderer.getWidth(label);
            int centeredX = buttonBaseX + (buttonRenderWidth - labelWidth) / 2;
            int centeredY = buttonCurrentY + (buttonRenderHeight - textRenderer.fontHeight) / 2;
            ctx.drawText(textRenderer, label, centeredX, centeredY, 0xFFFFFF, true);

            // Step to next Y
            buttonCurrentY += spacing;
            buttonY += this.height * 17 / 1080 + buttonHeight;
        } // end button loop

        // --- Draw player panel + recents banner panel ---
        playerWidgetY = buttonY - playerWidgetHeight;
        ctx.drawTexture(RenderLayer::getGuiTextured, PLAYER_WIDGET_BUTTON, playerWidgetX, playerWidgetY, 0f, 0f, playerWidgetWidth, playerWidgetHeight, playerWidgetWidth, playerWidgetHeight);

        // Draw recents button area just above player widget
        ctx.drawTexture(
                RenderLayer::getGuiTextured,
                RECENTS_BUTTONS,
                playerWidgetX, recntsButtonY,
                0f, 0f, playerWidgetWidth, playerWidgetWidth * 262 / 409,
                playerWidgetWidth, playerWidgetWidth * 262 / 409
        );

        // --- Draw bottom aesthetic strips (for flourish/decoration) ---
        int textureWidth = this.width;
        int textureHeight = textureWidth * 330 / 1800;
        int y = this.height - textureHeight;

        for (int x = 0; x < this.width; x += textureWidth) {
            ctx.drawTexture(
                    RenderLayer::getGuiTextured,
                    AESTHETIC_ELEMENT_DOWN,
                    x, y,
                    0f, 0f, textureWidth, textureHeight,
                    textureWidth, textureHeight
            );
        }
        ctx.drawTexture(
                RenderLayer::getGuiTextured,
                AESTHETIC_ELEMENT_UP,
                0, 0,
                0f, 0f, textureWidth, textureHeight,
                textureWidth, textureHeight
        );

        // --- Display player name under player widget ---
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String username = MinecraftClient.getInstance().getSession().getUsername();
        int textWidth = tr.getWidth(username);
        ctx.drawText(
                client.textRenderer,
                Text.literal(username),
                playerWidgetX + playerWidgetWidth / 2 - textWidth / 2,
                playerWidgetY + 16 - 4,
                0xFFFFFF,
                true
        );

        // --- Listen for world/server join events and update recent data ---
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String lastLoadedWorld;
            String lastLoadedServer;

            if (client.isInSingleplayer() && client.getServer() != null) {
                lastLoadedWorld = client.getServer().getSaveProperties().getLevelName();
            } else {
                lastLoadedWorld = handler.getConnection().getAddress().toString();
            }
            CustomTitleScreen.setLastLoadedWorld(lastLoadedWorld);
        });

        // Show last loaded server/world for recents
        // Stores last loaded server/world names for showing easily accessible recent information in the UI
        String lastLoadedServer1 = WorldUtils.getLastServerAddress().orElse("No server loaded");
        lastLoadedWorld = WorldUtils.getMostRecentSingleplayerWorldName();

        ctx.drawText(
                client.textRenderer,
                Text.literal(lastLoadedWorld),
                300, // X
                300, // Y
                0xFFFFFF,
                true
        );

        ctx.drawText(
                client.textRenderer,
                Text.literal(lastLoadedServer1),
                400, // X
                300, // Y
                0xFFFFFF,
                true
        );

        String currentLastLoadedWorld = WorldUtils.getMostRecentSingleplayerWorldName();

        // Load and cache the world icon texture if available
        if (!currentLastLoadedWorld.isEmpty() && lastWorldIconTexture == null) {
            Optional<Identifier> iconId = WorldIconLoader.loadWorldIcon(currentLastLoadedWorld);
            iconId.ifPresent(id -> lastWorldIconTexture = id);
        }


        int iconX = playerWidgetX + 4 * this.width / 1800;
        int iconY = recntsButtonY + 39 * this.height / 1080;
        int iconSize = 92 * this.width / 1800;

        // Draw the world icon if it's loaded
        if (lastWorldIconTexture != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Ensure white tint
            ctx.drawTexture(RenderLayer::getGuiTextured, lastWorldIconTexture, iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
        } else {
            // Draws a fallback texture or text if no icon is found
            ctx.drawText(
                    client.textRenderer,
                    Text.literal("No World Icon"), // Fallback text
                    iconX,
                    iconY,
                    0xFFFFFFFF,
                    true
            );
        }

        // --- Draw default Minecraft logo ---
        logoDrawer.draw(ctx, logoXMiddle, 1.0f, logoY);

        // --- Draw splash text ---
        this.splashText.render(ctx, logoXMiddle, this.textRenderer, 0xFF000000);
        WorldCreationListener.init();
    } // end render

    public static void takeScreenshot(MinecraftClient client) {
        Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
        // Hide HUD
        boolean oldHud = client.options.hudHidden;
        client.options.hudHidden = true;

        Framebuffer framebuffer = client.getFramebuffer();

        // Create screenshot (synchronously, without HUD)
        NativeImage image = ScreenshotRecorder.takeScreenshot(framebuffer);

        // Scale to 64x64
        NativeImage scaled = new NativeImage(128, 128, false);
        image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), scaled);
        image.close();


        java.nio.file.Path worldIconPath = Paths.get("saves/world_icon.png");

        System.out.println(worldIconPath.toString() + "nigga");

        LOGGER.info("Saved world icon to {}", worldIconPath);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, joinedClient) -> {
            MinecraftServer server = joinedClient.getServer();
            if (server != null) {
                Path iconPath = server.getSavePath(WorldSavePath.ROOT).resolve("icon.png");
                try {
                    scaled.writeTo(worldIconPath);
                } catch (IOException e) {
                    LOGGER.error("Failed to save world icon", e);
                }
                scaled.close();
            }
        });
        /* java.nio.file.Path iconPathn = client.getServer().getSavePath(WorldSavePath.ROOT).resolve("icon.png");
        String absolutePath = iconPathn.toAbsolutePath().toString();
        System.out.println("Absolute path to icon.png: " + absolutePath);

        // Save to world's folder
        java.nio.file.Path iconPath = client.getServer().getSavePath(WorldSavePath.ROOT).resolve("icon.png"); */


        // Restore HUD
        client.options.hudHidden = oldHud;
    }

}// end class CustomTitleScreen