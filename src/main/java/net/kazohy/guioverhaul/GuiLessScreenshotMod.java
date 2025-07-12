package net.kazohy.guioverhaul;



import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

public class GuiLessScreenshotMod implements ClientModInitializer {

    // Declare your KeyBinding instance
    private static KeyBinding guiLessScreenshotKey;

    // This method is called when the client-side of your mod is initialized.
    @Override
    public void onInitializeClient() {
        // 1. Define the KeyBinding
        // Parameters:
        //   - Identifier: A unique translation key for the keybind name (e.g., "key.guilessscreenshot.capture")
        //   - InputType: Specifies if it's a keyboard key (InputUtil.Type.KEYSYM) or mouse button
        //   - Default Key Code: The default key (e.g., GLFW.GLFW_KEY_F12 for F12)
        //   - Category: A translation key for the category name in Minecraft's controls settings
        guiLessScreenshotKey = new KeyBinding(
                "key.guilessscreenshot.capture",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F12, // Default key is F12
                Text.translatable("key.categories.guilessscreenshot").getString() // Category name in settings
        );

        // 2. Register the KeyBinding with Fabric
        KeyBindingHelper.registerKeyBinding(guiLessScreenshotKey);

        // 3. Register a client tick event listener
        // This event fires at the end of every client tick.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check if the custom keybinding is pressed
            if (guiLessScreenshotKey.isPressed()) {
                // If pressed, call our method to take the GUI-less screenshot
                takeGuiLessScreenshot(client);
            }
        });
    }

    /**
     * Handles the logic for taking a screenshot without the GUI.
     * This method is called when the custom keybinding is pressed.
     *
     * @param client The current MinecraftClient instance.
     */
    private void takeGuiLessScreenshot(MinecraftClient client) {
        // 1. Ensure the player is in a world (not on the main menu, etc.)
        if (client.world == null) {
            // Send a chat message to the player if they are not in a world
            client.inGameHud.getChatHud().addMessage(Text.literal("Cannot take screenshot: Not in a world."));
            return; // Exit the method early
        }

        // 2. Store the current GUI visibility state
        // This allows us to restore the GUI to its original state after the screenshot.
        boolean wasHudHidden = client.options.hudHidden;

        // 3. Temporarily hide the GUI
        // This is the core step to get a GUI-less view.
        client.options.hudHidden = true;

        // 4. Schedule the actual screenshot capture on the main client thread.
        // This is crucial for thread safety, as GUI updates and rendering calls
        // must happen on the main thread. The 'execute' method ensures this.
        client.execute(() -> {
            // Determine the directory where the screenshot will be saved.
            // Option A: Use Minecraft's default screenshots directory (usually .minecraft/screenshots)
            File screenshotsDir = new File(client.runDirectory, "screenshots");

            // Option B: Save to the current world's save directory (uncomment and use this if preferred)
            File worldDirectory = null;
            ClientWorld currentWorld = client.world;
            if (currentWorld != null && client.getLevelStorage() != null) {
                try {
                    // Get the path to the current world's save folder
                    // This method might vary slightly with Minecraft versions, but WorldSavePath.ROOT is reliable.
                    Path worldSavePath = client.getLevelStorage().getSavesDirectory().resolve(currentWorld.getRegistryKey().getValue().getPath());
                    worldDirectory = worldSavePath.toFile();
                    // Ensure the directory exists
                    if (!worldDirectory.exists()) {
                        worldDirectory.mkdirs();
                    }
                } catch (Exception e) {
                    // Log any errors if unable to get world directory
                    System.err.println("Error getting world directory: " + e.getMessage());
                    client.inGameHud.getChatHud().addMessage(Text.literal("Error getting world directory for screenshot."));
                    // Fallback to default screenshots directory if world directory cannot be found
                    worldDirectory = screenshotsDir;
                }
            } else {
                // Fallback to default screenshots directory if world or level storage is null
                worldDirectory = screenshotsDir;
            }

            // Ensure the chosen directory exists
            if (!worldDirectory.exists()) {
                worldDirectory.mkdirs();
            }

            // Define the callback for when the screenshot saving process is complete.
            // This consumer receives a Text object with a success or failure message.
            Consumer<Text> screenshotCallback = (message) -> {
                // Restore GUI visibility to its original state after the screenshot is saved.
                client.options.hudHidden = wasHudHidden;
                // Display the success/failure message in the player's chat.
                client.inGameHud.getChatHud().addMessage(message);
            };

            // Call Minecraft's built-in ScreenshotRecorder to capture and save the screenshot.
            // This method handles capturing the framebuffer, encoding to PNG, and writing to file.
            String filename = "player_view_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + ".png";

            ScreenshotRecorder.saveScreenshot(
                    worldDirectory,             // The directory to save the screenshot to
                    filename,                   // The generated unique filename
                    client.getFramebuffer(),        // The current framebuffer to capture
                    screenshotCallback              // The callback to execute after saving
            );
        });
    }
}
