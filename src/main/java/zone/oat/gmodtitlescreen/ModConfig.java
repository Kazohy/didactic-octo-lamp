package zone.oat.gmodtitlescreen;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "gmod-title-screen")
@Config.Gui.Background("textures/block/blue_concrete_powder.png")
public
class ModConfig implements ConfigData {
  @ConfigEntry.Gui.Tooltip(count = 2)
  public boolean showGModLogo = false;
  public boolean showSplashText = true;
}