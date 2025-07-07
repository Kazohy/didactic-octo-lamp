package zone.oat.gmodtitlescreen;

import net.minecraft.util.Identifier;

public final class ModIdentifier {
  public static Identifier of(String path) {
    return Identifier.of("gmod-title-screen", path);
  }
}