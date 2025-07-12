package net.kazohy.guioverhaul;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.MinecraftClient;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.network.ClientPlayerEntity;


public class DraggableSkinWidget extends ClickableWidget {
    private boolean dragging;
    private double dragOffsetX, dragOffsetY;

    public DraggableSkinWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw a translucent background or border (optional)
        // context.fill(getX(), getY(), getX()+width, getY()+height, 0x80000000);

        // Render the local player in 3D at the center of this widget
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            int centerX = this.getX() + this.width / 2;
            int centerY = this.getY() + this.height / 2;
            float size = 30.0f; // scale of the model

            // Offset the model so it is vertically centered
            Vector3f vector = new Vector3f(0.0f, player.getHeight() / 2.0f, 0.0f);
            // Rotate model to face the camera (flip by PI radians)
            Quaternionf rot = new Quaternionf().rotateZ((float)Math.PI);

            // Draw the entity using InventoryScreen.drawEntity
            InventoryScreen.drawEntity(
                    context,
                    centerX, centerY, size,
                    vector, rot, /* no extra pitch */ null,
                    player
            );
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // Begin dragging: record offset between mouse and widget origin
        this.dragging = true;
        this.dragOffsetX = mouseX - this.getX();
        this.dragOffsetY = mouseY - this.getY();
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        // If dragging, update widget position
        if (this.dragging) {
            this.setX((int)(mouseX - this.dragOffsetX));
            this.setY((int)(mouseY - this.dragOffsetY));
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        // Stop dragging on release
        this.dragging = false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // No-op for now (or add a descriptive narration if you want)
    }
}