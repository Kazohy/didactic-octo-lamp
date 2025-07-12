package net.kazohy.guioverhaul;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class PressableTextWithTooltipWidget extends PressableTextWidget {
  protected final Tooltip tooltip;

  public PressableTextWithTooltipWidget(int x, int y, int width, int height, Text text, PressAction onPress, TextRenderer textRenderer, Tooltip tooltip) {
    super(x, y, width, height, text, onPress, textRenderer);
    this.tooltip = tooltip;
    setTooltip(tooltip);
  }
}
