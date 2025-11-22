package de.alek.equipsight;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ColorHelper;

import java.util.ArrayList;
import java.util.List;

public class EquipSightOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        EquipSightConfig config = EquipSightConfig.get();

        if (!config.toggleEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.options.hudHidden) {
            return;
        }

        // Calculate position
        // User asked for "Left of Hotbar" as default.
        // The hotbar is centered. Screen width / 2.
        // Hotbar width is roughly 182.
        // So left of hotbar starts around (Width / 2) - 91 - (some padding).

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int startX = config.positionX;
        int startY = config.positionY;

        // If using default/unconfigured values (simple heuristic), try to place it nicely if not customized.
        // But the user can configure it. Let's interpret X and Y as absolute offsets if positive/custom.
        // But to satisfy "Left of Hotbar" default behavior if the config is fresh/defaults:
        // We might want to dynamically calculate the default spot if X/Y are at their "default" values.
        // However, the config has '10' and '-1'. Let's treat these as "Use default calculation".

        if (startY == -1) {
             startY = screenHeight - 22; // Just above bottom, aligned with hotbar items somewhat.
        }

        if (config.positionX == 10) {
            // Default default: Left of hotbar
             startX = (screenWidth / 2) - 91 - 25; // 91 is half hotbar width. 25 is padding/slot width.
             // Actually we are rendering multiple items. We should stack them vertically or horizontally?
             // Usually armor HUDs are vertical or horizontal.
             // "Left of Hotbar" usually implies a vertical column or a horizontal row.
             // Let's assume a vertical column going up from the bottom left of the hotbar.
        }

        context.getMatrices().push();

        // Apply scale
        context.getMatrices().scale(config.scale, config.scale, 1.0f);

        // We need to adjust coordinates because of scaling
        // logicalX = x / scale
        float scale = config.scale;
        int x = (int) (startX / scale);
        int y = (int) (startY / scale);

        // Items to render: Head, Chest, Legs, Boots, MainHand, OffHand.
        // Order: Usually Head -> Boots, then Hands.
        // Or Boots -> Head.
        // Let's do: Helmet, Chestplate, Leggings, Boots, Main Hand, Off Hand.

        List<ItemStack> items = new ArrayList<>();
        items.add(player.getEquippedStack(EquipmentSlot.HEAD));
        items.add(player.getEquippedStack(EquipmentSlot.CHEST));
        items.add(player.getEquippedStack(EquipmentSlot.LEGS));
        items.add(player.getEquippedStack(EquipmentSlot.FEET));
        items.add(player.getMainHandStack());
        items.add(player.getOffHandStack());

        // We render them vertically growing UPWARDS from the startY?
        // Or downwards?
        // If "Left of Hotbar", usually it's bottom-aligned.

        int itemSpacing = 20; // 16 px icon + 4 px padding

        // Render loop
        int currentY = y;

        for (int i = items.size() - 1; i >= 0; i--) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            renderItem(context, client, stack, x, currentY, config);
            currentY -= itemSpacing; // Go up
        }

        context.getMatrices().pop();
    }

    private void renderItem(DrawContext context, MinecraftClient client, ItemStack stack, int x, int y, EquipSightConfig config) {
        // Render Item
        context.drawItem(stack, x, y);
        // context.drawItemInSlot(client.textRenderer, stack, x, y); // This draws count/overlay too usually

        // Render Durability if applicable
        if (config.showDurability && stack.isDamageable()) {
            renderDurabilityText(context, client.textRenderer, stack, x, y, config);
        }
    }

    private void renderDurabilityText(DrawContext context, TextRenderer textRenderer, ItemStack stack, int x, int y, EquipSightConfig config) {
        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamage();
        int remaining = maxDamage - currentDamage;

        String text;
        if (config.displayStyle == EquipSightConfig.DisplayStyle.PERCENTAGE) {
            int percent = (int) Math.round(((double) remaining / maxDamage) * 100);
            text = percent + "%";
        } else {
            text = String.valueOf(remaining);
        }

        // Calculate Color: Green -> Yellow -> Red
        // HSB: Green is roughly 0.33 (120 deg), Red is 0.0 (0 deg).
        float hue = Math.max(0.0F, (float) remaining / (float) maxDamage) / 3.0F;
        int color = ColorHelper.Argb.fromFloats(1.0f, hue, 1.0f, 1.0f);
        // Actually ColorHelper.fromFloats might expect RGB.
        // We want HSB to RGB.
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);

        // Center text under the item or over it?
        // "Unter jedem Item soll die verbleibende Haltbarkeit stehen" -> Under each item.
        // Item is 16x16.

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200); // Bring text forward

        float scale = 0.5f; // Small text
        context.getMatrices().scale(scale, scale, 1.0f);

        int textWidth = textRenderer.getWidth(text);
        // Center relative to item (which is 16px wide)
        // scaledX = (x + 8) * (1/scale) - (textWidth / 2)

        int scaledX = (int) ((x + 8) / scale - textWidth / 2);
        int scaledY = (int) ((y + 16) / scale); // Below the item

        context.drawText(textRenderer, text, scaledX, scaledY, rgb, true);

        context.getMatrices().pop();
    }
}
