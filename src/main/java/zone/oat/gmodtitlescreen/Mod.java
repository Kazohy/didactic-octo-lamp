package zone.oat.gmodtitlescreen;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zone.oat.gmodtitlescreen.screen.CustomTitleScreen;

public class Mod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("");
	public static ModConfig config;

	@Override
	public void onInitializeClient() {
		AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}
}
