package net.kazohy.guioverhaul;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.kazohy.guioverhaul.listener.WorldCreationListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static net.kazohy.guioverhaul.screen.CustomTitleScreen.takeScreenshot;

public class Mod implements ClientModInitializer {
	private static final Path RECENT_SERVER_PATH = MinecraftClient.getInstance()
			.runDirectory.toPath()
			.resolve("config/recentServer.json");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final Logger LOGGER = LoggerFactory.getLogger("");
	public static net.kazohy.guioverhaul.ModConfig config;

	@Override
	public void onInitializeClient() {
		AutoConfig.register(net.kazohy.guioverhaul.ModConfig.class, Toml4jConfigSerializer::new);
		config = AutoConfig.getConfigHolder(net.kazohy.guioverhaul.ModConfig.class).getConfig();
        // call your method every tick
        ClientTickEvents.END_CLIENT_TICK.register(this::tickEveryClientTick);
		File gameDir = MinecraftClient.getInstance().runDirectory;
		File myModFolder = new File(gameDir, "higherResScreenshots");

		if (!myModFolder.exists()) {
			boolean created = myModFolder.mkdirs();
			if (created) {
				System.out.println("✔ Folder created: " + myModFolder.getAbsolutePath());
			} else {
				System.err.println("✖ Failed to create folder!");
			}
		}

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ServerInfo serverInfo = client.getCurrentServerEntry();
			if (serverInfo != null) {
				saveRecentServer(serverInfo.name, serverInfo.address);
			}
		});
	}

	private void tickEveryClientTick(MinecraftClient client) {
		if (client.world != null && client.player != null) {
			WorldCreationListener.shouldScreenshotBeTaken = true;
			WorldCreationListener.startCountdown(5000);
			if (WorldCreationListener.shouldScreenshotBeTaken) {
				if (WorldCreationListener.isCountdownFinished()) {
					WorldCreationListener.shouldScreenshotBeTaken = false;
					WorldCreationListener.resetCountdown();
					takeScreenshot(client);
				}
			}
		} else {WorldCreationListener.shouldScreenshotBeTaken = false;}
	}

	private void saveRecentServer(String name, String address) {
		RecentServer server = new RecentServer(name, address);

		try {
			Files.createDirectories(RECENT_SERVER_PATH.getParent());

			String json = GSON.toJson(server);
			Files.writeString(RECENT_SERVER_PATH, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			System.out.println("[RecentServerTracker] Saved server: " + name + " (" + address + ")");
		} catch (IOException e) {
			System.err.println("[RecentServerTracker] Failed to save recent server: " + e.getMessage());
		}
	}

	public static RecentServer loadRecentServer() {
		Path path = MinecraftClient.getInstance()
				.runDirectory.toPath()
				.resolve("config/recentServer.json");

		if (!Files.exists(path)) {
			System.out.println("[RecentServerTracker] recent_server.json does not exist.");
			return null;
		}

		try {
			String json = Files.readString(path);
			return new Gson().fromJson(json, RecentServer.class);
		} catch (IOException e) {
			System.err.println("[RecentServerTracker] Failed to read recent_server.json: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("[RecentServerTracker] JSON is malformed: " + e.getMessage());
		}

		return null;
	}

	public static class RecentServer {
		public String name;
		public String address;

		public RecentServer(String name, String address) {
			this.name = name;
			this.address = address;
		}
	}
}
