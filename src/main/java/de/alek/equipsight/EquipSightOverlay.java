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

import java.awt.Color;
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

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int startX = config.positionX;
        int startY = config.positionY;

        float scale = config.scale;

        // Default positioning logic
        // Calculate defaults in absolute screen coordinates
        if (startY == -1) {
             // Align to bottom, just above hotbar if possible.
             // Hotbar is 22px high.
             // If scaling is applied, we want the BOTTOM of the lowest item to be at screenHeight - 22.
             startY = screenHeight - 22;
        }

        if (config.positionX == 10) {
             // Default: Left of hotbar
             startX = (screenWidth / 2) - 91 - 25;
        }

        List<ItemStack> items = new ArrayList<>();
        items.add(player.getEquippedStack(EquipmentSlot.HEAD));
        items.add(player.getEquippedStack(EquipmentSlot.CHEST));
        items.add(player.getEquippedStack(EquipmentSlot.LEGS));
        items.add(player.getEquippedStack(EquipmentSlot.FEET));
        items.add(player.getMainHandStack());
        items.add(player.getOffHandStack());

        // Spacing in pixels (scaled)
        // Standard item is 16x16. Padding 4. Total 20.
        // Since we scale the item rendering, the spacing on screen should be 20 * scale.
        int itemSpacing = (int) (20 * scale);

        // Current rendering position (Top-Left of the item slot in screen coords)
        int currentX = startX;
        int currentY = startY;

        // Logic:
        // VERTICAL: Stacks UPWARDS from startY. startY is the Top-Left of the BOTTOM-most item (Offhand).
        // So Item 0 (Head) should be highest. Item 5 (Offhand) lowest.
        // Loop Backwards: Offhand -> Head.
        // Render Offhand at currentY. Then currentY -= spacing.

        // HORIZONTAL: Stacks RIGHTWARDS from startX.
        // Head -> Offhand.
        // Loop Forwards.

        if (config.orientation == EquipSightConfig.Orientation.VERTICAL) {
            // StartY is the anchor for the bottom item.
            // If user provided custom Y, assume it's the anchor point.
            // If default Y, it's screenHeight - 22.

            // Loop backwards (Offhand first, at the bottom)
            for (int i = items.size() - 1; i >= 0; i--) {
                ItemStack stack = items.get(i);
                if (shouldSkip(stack, config)) continue;

                // We render at currentX, currentY.
                // Note: RenderItem assumes (x,y) is top-left.
                // If default startY is near bottom of screen, we render UP.
                // BUT we must ensure currentY doesn't start too low if the item height is included.
                // Standard item logic: render at (x,y).

                // If startY = screenHeight - 22. This is the top-left of the bottom slot.
                // The item extends to startY + 16*scale.

                renderHudItem(context, client, stack, currentX, currentY, config);
                currentY -= itemSpacing;
            }
        } else {
            // HORIZONTAL
            // Loop forwards (Head -> Offhand)
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (shouldSkip(stack, config)) continue;

                renderHudItem(context, client, stack, currentX, currentY, config);
                currentX += itemSpacing;
            }
        }
    }

    private boolean shouldSkip(ItemStack stack, EquipSightConfig config) {
        if (stack.isEmpty()) return true;
        if (config.onlyShowDamageable && !stack.isDamageable()) return true;
        return false;
    }

    private void renderHudItem(DrawContext context, MinecraftClient client, ItemStack stack, int x, int y, EquipSightConfig config) {
        context.getMatrices().push();

        // Apply scale at the item position
        // We want (x,y) to be the top-left of the item ON SCREEN.
        // item is drawn at (0,0) in scaled space.
        // Matrix: Translate(x, y, 0) -> Scale(s, s, 1)
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(config.scale, config.scale, 1.0f);

        // Draw Item at (0,0) relative to the translated origin
        context.drawItem(stack, 0, 0);
        // context.drawItemInSlot(client.textRenderer, stack, 0, 0);

        context.getMatrices().pop();

        // Render Durability Text
        // We render this separately to ensure crisp text (maybe different scaling)
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

        // Color
        float hue = Math.max(0.0F, (float) remaining / (float) maxDamage) / 3.0F;
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);

        // Text Rendering Logic
        // We want small text (scale 0.5) centered below the item.
        // Item width on screen = 16 * config.scale.
        // Item height on screen = 16 * config.scale.
        // Item center X = x + (8 * config.scale).
        // Item bottom Y = y + (16 * config.scale).

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200); // Z-index above item

        float textScale = 0.5f * config.scale; // Keep text proportional to item scale?
        // Or fixed scale 0.5? Usually people want text to scale with the HUD.
        // Let's use 0.5 * config.scale.

        // If we scale the matrix, we lose pixel alignment.
        // To get crisp text, we should try to keep scale at 1.0 or integer factors if possible,
        // but user requested scaling.
        // The fuzziness in the screenshot comes from non-integer alignment + shadows.

        // Let's try to align the starting position to integer pixels.

        context.getMatrices().translate(x, y, 0); // Move to item top-left
        context.getMatrices().scale(textScale, textScale, 1.0f);

        // Now we are in scaled space.
        // 1 unit here = (1 / textScale) pixels on screen.

        int textWidth = textRenderer.getWidth(text);

        // We want to center horizontally relative to the item (width 16 in item-space).
        // Item width in text-space:
        // Item width (pixels) = 16 * config.scale.
        // Text scale = 0.5 * config.scale.
        // Ratio = 2.
        // So item is 32 units wide in text-space.
        // Center is 16.

        // X position in text-space:
        float textX = (16.0f / 0.5f) / 2.0f - (textWidth / 2.0f);
        // (32 / 2) - width/2 = 16 - width/2.

        // Y position: Below item.
        // Item height (pixels) = 16 * config.scale.
        // Text scale = 0.5 * config.scale.
        // Height in text-space = 32.
        float textY = 32.0f;

        // Draw text
        // Use main color. Disable built-in shadow to draw custom one if needed,
        // but standard shadow usually works if scale is clean.
        // The screenshot showed garbled text. This happens when text is drawn at non-integer coordinates with a small scale.

        // Let's cast to int to snap to grid in the scaled space.
        int drawX = (int) textX;
        int drawY = (int) textY;

        // Draw simple shadow manually for better contrast at small scales?
        // Or just use drawText with shadow=true.
        // Try shadow=false and manual black outline if it helps?
        // Standard Minecraft HUDs often use shadow=true.
        // But at 0.5 scale, the shadow offset (1px) becomes 0.5px on screen, which looks blurry.

        // Solution: Draw text at scale 1.0 (screen pixels) but calculating position manually?
        // No, font is bitmap. Scaling down looks bad if not carefully done.
        // But user wants "Scale" config.

        // If we render at 0.5 scale, we should ensure the screen coordinates align.
        // (x + drawX * textScale) should be integer.

        context.drawText(textRenderer, text, drawX, drawY, rgb, true);

        context.getMatrices().pop();
    }
}
