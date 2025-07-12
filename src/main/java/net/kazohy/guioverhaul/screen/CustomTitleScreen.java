package net.kazohy.guioverhaul.screen;

import net.minecraft.client.gui.hud.InGameHud;

import java.nio.file.Files;
import java.nio.file.Paths;

import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
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
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.slf4j.LoggerFactory;
import net.kazohy.guioverhaul.*;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.texture.PlayerSkinProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;

import com.mojang.authlib.GameProfile;
import net.kazohy.guioverhaul.listener.WorldCreationListener;

import java.util.Optional;
import java.util.Objects;

import static net.fabricmc.loader.impl.FabricLoaderImpl.MOD_ID;
import static net.kazohy.guioverhaul.WorldUtils.getWorldGameMode;
import static net.kazohy.guioverhaul.listener.WorldCreationListener.getMostRecentlyCreatedWorld;

@Environment(EnvType.CLIENT)
public class CustomTitleScreen extends Screen {
    public static final Logger LOGGER = LoggerFactory.getLogger("");
    private long startTime = System.currentTimeMillis();
    private long lastPauseTime = 0;
    private boolean isPaused = false;
    private boolean hasPausedThisLoop = false;

    private static String lastLoadedWorld = "";
    @Nullable
    private Identifier lastWorldIconTexture;
    public static int screenWidth;
    public static int screenHeight;

    private final LogoDrawer logoDrawer;

    @Nullable
    private SplashTextRenderer splashText;

    private static final Identifier GMOD_TITLE_TEXTURE = ModIdentifier.of("textures/gui/title/gmod_title.png");
    private static final Identifier AESTHETIC_ELEMENT_DOWN = ModIdentifier.of("textures/gui/title/aesthetic_element_down.png");
    private static final Identifier AESTHETIC_ELEMENT_UP = ModIdentifier.of("textures/gui/title/aesthetic_elemnt_up.png");
    private static final Identifier NORMAL_BUTTON_TEXTURE = ModIdentifier.of("textures/gui/title/normal_button.png");
    private static final Identifier REALMS_BUTTON_TEXTURE = ModIdentifier.of("textures/gui/title/realms_button.png");
    private static final Identifier NEWS_BUTTON_TEXTURE = ModIdentifier.of("textures/gui/title/news_button.png");
    private static final Identifier PLAYER_WIDGET_BUTTON = ModIdentifier.of("textures/gui/title/player_widget.png");
    private static final Identifier RECENTS_BUTTONS = ModIdentifier.of("textures/gui/title/recents_buttons.png");

    public CustomTitleScreen() {
        this(null);
    }

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

    public CustomTitleScreen(@Nullable LogoDrawer logoDrawer) {
        super(Text.translatable("narrator.screen.title"));
        this.logoDrawer = Objects.requireNonNullElseGet(logoDrawer, () -> new LogoDrawer(false));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }


    private int getLogoY() {
        if (Mod.config.showGModLogo) {
            return this.height / 32;
        } else {
            return this.height / 12;
        }
    }

    @Override
    protected void init() {
        if (this.splashText == null) {
            assert this.client != null;
            this.splashText = this.client.getSplashTextLoader().get();
        }

        screenWidth = this.width;
        screenHeight = this.height;

        int playerWidgetX = this.width / 2 + this.width * (59 / 2) / 1800;
        int playerWidgetY = this.height / 2 - this.height * 298 / 1080 + 279 * this.height / 1080;
        int playerWidgetWidth = 409 * this.width / 1800;
        int playerWidgetHeight = playerWidgetWidth * 280 / 409;

        GameProfile profile;
        assert client != null;
        if (client.player != null) {
            profile = client.player.getGameProfile();
        } else {
            String username = client.getSession().getUsername();
            profile = new GameProfile(
                    UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)),
                    username
            );
        }

        PlayerSkinProvider skinProvider = MinecraftClient.getInstance().getSkinProvider();
        Supplier<SkinTextures> skinSupplier = skinProvider.getSkinTexturesSupplier(profile);
        LoadedEntityModels models = MinecraftClient.getInstance().getLoadedEntityModels();
        PlayerSkinWidget playerSkin = new PlayerSkinWidget(64, 88, models, skinSupplier);

        this.addDrawableChild(playerSkin);
        playerSkin.setPosition(playerWidgetX + playerWidgetWidth / 2 - 32, playerWidgetY + playerWidgetHeight / 2 - 16);

        RenderSystem.setShaderTexture(0, NORMAL_BUTTON_TEXTURE);
        RenderSystem.setShaderTexture(1, NORMAL_BUTTON_TEXTURE);
        RenderSystem.setShaderTexture(2, NORMAL_BUTTON_TEXTURE);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        ButtonTextures normalButtonTextures = new ButtonTextures(
                NORMAL_BUTTON_TEXTURE,
                NORMAL_BUTTON_TEXTURE,
                NORMAL_BUTTON_TEXTURE
        );

        int buttonWidth = 408 * this.width / 1800;
        int buttonheight = buttonWidth * 68 / 408;
        int buttonX = this.width / 2 - buttonWidth - this.width * (59 / 2) / 1800;
        int buttonY = this.height / 2 - this.height * 298 / 1080;

        this.addDrawableChild(new TexturedButtonWidget(
                buttonX,
                buttonY,
                buttonWidth,
                buttonheight,
                normalButtonTextures,
                button -> this.client.setScreen(new SelectWorldScreen(this)),
                Text.literal("Options")
        ));

        this.addDrawableChild(new TexturedButtonWidget(
                buttonX,
                buttonY + this.height * 17 / 1080 + buttonheight,
                buttonWidth,
                buttonheight,
                normalButtonTextures,
                button -> this.client.setScreen(new MultiplayerScreen(this)),
                Text.literal("Options")
        ));

        boolean modsButtonActive = FabricLoader.getInstance().isModLoaded("modmenu");

        this.addDrawableChild(new TexturedButtonWidget(
                buttonX,
                buttonY + 3 * (this.height * 17 / 1080 + buttonheight),
                buttonWidth,
                buttonheight,
                normalButtonTextures,
                button -> this.client.setScreen(new OptionsScreen(this, this.client.options)),
                Text.literal("Options")
        ));

        this.addDrawableChild(new TexturedButtonWidget(
                playerWidgetX,
                buttonY + 43 * this.height / 1080,
                buttonWidth,
                108 * this.height / 1080,
                normalButtonTextures,
                button -> playWorld(WorldUtils.getMostRecentSingleplayerWorldName()),
                Text.literal("Options")
        ));

        Mod.RecentServer last = Mod.loadRecentServer();
        String lastJoinedServerAddress;
        if (last != null) {
            lastJoinedServerAddress = last.address;
            System.out.println(lastJoinedServerAddress);
            this.addDrawableChild(new PressableTextWidget(
                    playerWidgetX,
                    buttonY + 43 * this.height / 1080 + 122 * this.height / 1080,
                    buttonWidth,
                    108 * this.height / 1080,
                    Text.literal(""),
                    button -> joinServer(lastJoinedServerAddress),
                    textRenderer
            ));
        } else {
            this.addDrawableChild(new PressableTextWidget(
                    playerWidgetX,
                    buttonY + 43 * this.height / 1080 + 122 * this.height / 1080,
                    buttonWidth,
                    115 * this.height / 1080,
                    Text.literal(""),
                    button -> this.client.setScreen(new MultiplayerScreen(this)),
                    textRenderer
            ));
        }


        this.addDrawableChild(new PressableTextWidget(
                buttonX,
                buttonY + 2 * (this.height * 17 / 1080 + buttonheight),
                buttonWidth,
                buttonheight,
                Text.literal(""),
                button -> this.client.setScreen(new RealmsMainScreen(this)),
                textRenderer
        ));

        this.addDrawableChild(new TexturedButtonWidget(
                buttonX,
                buttonY + 4 * (this.height * 17 / 1080 + buttonheight),
                buttonWidth,
                buttonheight,
                normalButtonTextures,
                button -> this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options)),
                Text.translatable("menu.gmod-title-screen.accessibility")
        ));

        this.addDrawableChild(new PressableTextWidget(
                buttonX,
                buttonY + 6 * (this.height * 17 / 1080 + buttonheight),
                buttonWidth,
                buttonheight,
                Text.literal(""),
                button -> this.client.scheduleStop(),
                textRenderer
        ));

    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = ctx.getMatrices();

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
        int buttonRenderWidth = 408 * this.width / 1800;
        int buttonRenderHeight = buttonRenderWidth * 68 / 408;
        int buttonBaseX = this.width / 2 - buttonRenderWidth - this.width * (59 / 2) / 1800;
        int buttonCurrentY = this.height / 2 - this.height * 298 / 1080;

        for (int i = 0; i < 7; i++) {
            if ((i + 1) % 3 != 0) {
                ctx.drawTexture(RenderLayer::getGuiTextured, NORMAL_BUTTON_TEXTURE, buttonX, buttonY, 0f, 0f, buttonWidth, buttonHeight, buttonWidth, buttonHeight);
            } else if (i == 2) {
                ctx.drawTexture(RenderLayer::getGuiTextured, REALMS_BUTTON_TEXTURE, buttonX, buttonY, 0f, 0f, buttonWidth, buttonHeight, buttonWidth, buttonHeight);
            } else {
                ctx.drawTexture(RenderLayer::getGuiTextured, NEWS_BUTTON_TEXTURE, buttonX, buttonY, 0f, 0f, buttonWidth, buttonHeight, buttonWidth, buttonHeight);
            }

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

            buttonCurrentY += spacing;
            buttonY += this.height * 17 / 1080 + buttonHeight;
        }

        playerWidgetY = buttonY - playerWidgetHeight;
        ctx.drawTexture(RenderLayer::getGuiTextured, PLAYER_WIDGET_BUTTON, playerWidgetX, playerWidgetY, 0f, 0f, playerWidgetWidth, playerWidgetHeight, playerWidgetWidth, playerWidgetHeight);

        ctx.drawTexture(
                RenderLayer::getGuiTextured,
                RECENTS_BUTTONS,
                playerWidgetX, recntsButtonY,
                0f, 0f, playerWidgetWidth, playerWidgetWidth * 262 / 409,
                playerWidgetWidth, playerWidgetWidth * 262 / 409
        );

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

        lastLoadedWorld = WorldUtils.getMostRecentSingleplayerWorldName();

        int lastesTextX = playerWidgetX + 189 * this.width / 1800;
        int lastestWorldTextY = Math.round(recntsButtonY + getFromFigmaHeight(39 + 100 / 2 - 24 / 2) - textRenderer.fontHeight / 2);
        int lastestServerTextY = Math.round(lastestWorldTextY + getFromFigmaHeight(115) + textRenderer.fontHeight / 2);
        int lastestWorldGamemodeTextY  = Math.round(recntsButtonY + getFromFigmaHeight(39 + 100 / 2 + 24 / 2) + textRenderer.fontHeight / 2);
        int lastestServerIPAdress  = Math.round(lastestWorldGamemodeTextY + getFromFigmaHeight(115) + textRenderer.fontHeight / 2);
        Mod.RecentServer last = Mod.loadRecentServer();
        String lastJoinedServerName;
        String lastJoinedServerAddress;
        if (last != null) {
            lastJoinedServerName = last.name;
            lastJoinedServerAddress = last.address;
        } else {
            lastJoinedServerAddress = "Null";
            lastJoinedServerName = "No servers/realms joined";
        }

        lastLoadedWorld = ScrollingTextUtil.getScrollingText(lastLoadedWorld, 17);

        ctx.drawText(
                client.textRenderer,
                Text.literal(lastLoadedWorld),
                lastesTextX,
                lastestWorldTextY,
                0xFFFFFF,
                true
        );

        Text gameModeText;

        try {
            gameModeText = Text.literal(getWorldGameMode(lastLoadedWorld).toString().substring(0, 1).toUpperCase() + getWorldGameMode(lastLoadedWorld).toString().substring(1).toLowerCase());
        } catch (IOException e) {
            gameModeText = Text.literal("Gamemode: Error");
            e.printStackTrace(); // Or log properly
        }

        ctx.drawText(
                client.textRenderer,
                gameModeText,
                lastesTextX,
                lastestWorldGamemodeTextY,
                0xBEFFFFFF,
                true
        );

        lastJoinedServerAddress = ScrollingTextUtil.getScrollingText(lastJoinedServerAddress, 17);

        ctx.drawText(
                client.textRenderer,
                Text.literal(lastJoinedServerAddress),
                lastesTextX,
                lastestServerIPAdress,
                0xBEFFFFFF,
                true
        );

        if (!lastLoadedWorld.isEmpty() && lastWorldIconTexture == null) {
            Optional<Identifier> iconId = WorldIconLoader.loadWorldIcon(lastLoadedWorld);
            iconId.ifPresent(id -> lastWorldIconTexture = id);
        }

        int iconX = Math.round(getFromFigmaWidth(6) + playerWidgetX);
        int iconY = Math.round(getFromFigmaHeight(39 + 8) + recntsButtonY);
        int iconHeight = 99 * screenHeight / 1080;
        int iconWidth = (iconHeight * 16) / 9;

        lastJoinedServerName = ScrollingTextUtil.getScrollingText(lastJoinedServerName, 17);

        ctx.drawText(client.textRenderer, lastJoinedServerName, lastesTextX, lastestServerTextY, 0xFFFFFF, true);

        if (lastWorldIconTexture != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            ctx.drawTexture(RenderLayer::getGuiTextured, lastWorldIconTexture, iconX, iconY, 0f, 0f, iconWidth, iconHeight, iconWidth, iconHeight);
        } else {
            ctx.drawText(
                    client.textRenderer,
                    Text.literal("No World Icon"),
                    iconX,
                    iconY,
                    0xFFFFFFFF,
                    true
            );
        }

        logoDrawer.draw(ctx, logoXMiddle, 1.0f, logoY);

        this.splashText.render(ctx, logoXMiddle, this.textRenderer, 0xFF000000);
        WorldCreationListener.init();
    }

    public static void takeScreenshot(MinecraftClient client) {
        InGameHud hud = MinecraftClient.getInstance().inGameHud;
        hud.vignetteDarkness = 0f;
        Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        Framebuffer framebuffer = client.getFramebuffer();

        NativeImage image = ScreenshotRecorder.takeScreenshot(framebuffer);


        Path latestWorld = Paths.get(getMostRecentlyCreatedWorld().toString() + "/world_icon.png");

        LOGGER.info("Saved world icon to {}", latestWorld);

        try {
            image.writeTo(latestWorld);
        } catch (IOException e) {
            LOGGER.error("Failed to save world icon", e);
        }
        image.close();
    }

    public static void playWorld(String worldName) {
        MinecraftClient client = MinecraftClient.getInstance();

        Path savesDir = client.runDirectory.toPath().resolve("saves");
        Path worldDir = savesDir.resolve(worldName);

        if (!Files.exists(worldDir)) {
            System.out.println("World folder doesn't exist: " + worldDir);
            return;
        }

        client.execute(() -> {
            client.disconnect();
            client.setScreen(null); // clear any lingering screen
            client.createIntegratedServerLoader().start(worldName, () -> {
                System.out.println("World loaded.");
            });
        });
    }

    private void joinServer(String ipAddress) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null) {
            System.err.println("MinecraftClient instance is null. Cannot connect.");
            return;
        }

        ServerAddress address = ServerAddress.parse(ipAddress);

        // For direct IP connections, you typically don't have existing server info
        // or specific cookies to pass. So, null is usually appropriate here.
        ServerInfo serverInfo = new ServerInfo(ipAddress, ipAddress, ServerInfo.ServerType.OTHER); // name, address, local
        @Nullable CookieStorage cookieStorage = null; // No cookies needed for a fresh connection

        client.execute(() -> {
            ConnectScreen.connect(
                    new TitleScreen(), // Parent screen
                    client,            // MinecraftClient instance
                    address,           // ServerAddress (parsed IP/domain)
                    serverInfo,        // ServerInfo (null for fresh connect by IP)
                    false,             // quickPlay: false for a manual connection
                    cookieStorage      // CookieStorage: null unless you have specific cookie data
            );
            client.inGameHud.getChatHud().addMessage(Text.literal("Attempting to connect to: " + ipAddress));
        });
    }

    public static int getFromFigmaHeight(int measure) {
        return (measure * screenHeight / 1080);
    }

    public static int getFromFigmaWidth(int measure) {
        return (measure * screenWidth / 1800);
    }

    public class ScrollingTextUtil {
        private static int scrollIndex = 0;
        private static int tickCounter = 0;
        private static int pauseTicks = 0;
        private static boolean isPaused = false;

        // Settings
        private static final int SCROLL_SPEED_TICKS = 10;   // Slower scroll
        private static final int PAUSE_DURATION_TICKS = 80;

        /**
         * Get the cropped, scrolling version of fullText with optional trailing spaces.
         *
         * @param baseText The main message to scroll.
         * @param visibleLength The number of characters to show.
         * @return A cropped, scrolling segment of the text.
         */
        public static String getScrollingText(String baseText, int visibleLength) {
            if (!(baseText.length() <= visibleLength)) {
                if (visibleLength <= 0 || baseText.isEmpty()) return "";

                // Add padding spaces at the end of the string
                String fullText = baseText + " ".repeat(Math.max(0, 7));

                tickCounter++;

                if (isPaused) {
                    pauseTicks++;
                    if (pauseTicks >= PAUSE_DURATION_TICKS) {
                        isPaused = false;
                        pauseTicks = 0;
                    }
                } else if (tickCounter % SCROLL_SPEED_TICKS == 0) {
                    scrollIndex = (scrollIndex + 1) % fullText.length();
                    if (scrollIndex == 0) {
                        isPaused = true;
                    }
                }

                // Scroll the text
                String scrolled = fullText.substring(scrollIndex) + fullText.substring(0, scrollIndex);
                if (visibleLength > scrolled.length()) {
                    visibleLength = scrolled.length();
                }

                return scrolled.substring(0, visibleLength);
            } else {return baseText;}
        }
    }
}