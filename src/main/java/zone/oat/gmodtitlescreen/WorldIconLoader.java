package zone.oat.gmodtitlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static net.fabricmc.loader.impl.FabricLoaderImpl.MOD_ID;

public class WorldIconLoader {
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID); // Use your mod's logger
    private static final Map<String, Identifier> cachedWorldIcons = new HashMap<>(); // Cache to avoid reloading

    /**
     * Loads the icon.png for a given world name and registers it with the texture manager.
     * Caches the loaded texture identifier to prevent redundant loading.
     *
     * @param worldName The name of the world (e.g., "My Super World")
     * @return An Optional containing the Identifier of the loaded texture, or empty if failed.
     */
    public static Optional<Identifier> loadWorldIcon(String worldName) {
        // Check cache first
        if (cachedWorldIcons.containsKey(worldName)) {
            return Optional.of(cachedWorldIcons.get(worldName));
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return Optional.empty(); // Should not happen in client-side context, but for safety
        }

        // Get the path to the saves folder
        Path savesFolderPath = client.runDirectory.toPath().resolve("saves");
        File worldFolder = savesFolderPath.resolve(worldName).toFile(); // Assuming worldName is the folder name

        if (!worldFolder.isDirectory()) {
            LOGGER.warn("World folder not found: {}", worldFolder.getAbsolutePath());
            return Optional.empty();
        }

        File iconFile = new File(worldFolder, "icon.png");

        if (!iconFile.exists()) {
            LOGGER.warn("icon.png not found for world: {}", worldName);
            return Optional.empty();
        }

        try (InputStream is = new FileInputStream(iconFile)) {
            NativeImage image = NativeImage.read(is);
            if (image == null) {
                LOGGER.warn("Failed to read image from icon.png for world: {}", worldName);
                return Optional.empty();
            }

            // Create a new NativeImageBackedTexture from the loaded image
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

            // Generate a unique Identifier for this texture
            // Using a custom namespace and a world-specific path for uniqueness
            Identifier textureId = ModIdentifier.of("world_icons/" + worldName.toLowerCase().replaceAll("[^a-z0-9/._-]", "_"));

            // Register the texture with Minecraft's TextureManager
            client.getTextureManager().registerTexture(textureId, texture);

            // Cache the identifier
            cachedWorldIcons.put(worldName, textureId);
            LOGGER.info("Successfully loaded and registered world icon for: {}", worldName);
            return Optional.of(textureId);

        } catch (IOException e) {
            LOGGER.error("Error loading world icon for world {}: {}", worldName, e.getMessage());
            return Optional.empty();
        }
    }
}