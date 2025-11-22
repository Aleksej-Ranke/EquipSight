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

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int startX = config.positionX;
        int startY = config.positionY;

        if (startY == -1) {
             startY = screenHeight - 22;
        }

        if (config.positionX == 10) {
             startX = (screenWidth / 2) - 91 - 25;
        }

        context.getMatrices().push();

        context.getMatrices().scale(config.scale, config.scale, 1.0f);

        float scale = config.scale;
        int x = (int) (startX / scale);
        int y = (int) (startY / scale);

        List<ItemStack> items = new ArrayList<>();
        items.add(player.getEquippedStack(EquipmentSlot.HEAD));
        items.add(player.getEquippedStack(EquipmentSlot.CHEST));
        items.add(player.getEquippedStack(EquipmentSlot.LEGS));
        items.add(player.getEquippedStack(EquipmentSlot.FEET));
        items.add(player.getMainHandStack());
        items.add(player.getOffHandStack());

        int itemSpacing = 20;

        // Loop logic depends on orientation.
        // We can iterate normally and adjust X or Y.

        // If horizontal: grows to the right.
        // If vertical: grows upwards (from bottom) or downwards?
        // Original code was growing UPWARDS (currentY -= itemSpacing) with the loop going backwards (size-1 to 0).
        // Let's keep consistent logic for "Start from startX/startY and grow outward".

        // Wait, original loop:
        /*
        for (int i = items.size() - 1; i >= 0; i--) {
             ...
             currentY -= itemSpacing; // Go up
        }
        */
        // This implies the list order was rendered bottom-to-top.
        // List: Head, Chest, Legs, Feet, Main, Off.
        // i=Off -> render at Y, Y becomes Y-20.
        // i=Main -> render at Y-20...
        // ...
        // i=Head -> render at Top.
        // This places Head at the top, Offhand at the bottom (anchored at startY).
        // This matches "Left of hotbar" where it grows up.

        int currentX = x;
        int currentY = y;

        // Iterate backwards to keep the stack order (Head on top)
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            // Check durability filter
            if (config.onlyShowDamageable && !stack.isDamageable()) {
                continue;
            }

            renderItem(context, client, stack, currentX, currentY, config);

            if (config.orientation == EquipSightConfig.Orientation.HORIZONTAL) {
                // If horizontal, we probably want Head on the Left? or Right?
                // If we iterate backwards (Offhand first), and we add spacing...
                // Offhand at X, Main at X+20... Head at X+100.
                // That puts Head on the Right.
                // Usually Head is Left.
                // So for Horizontal, maybe we should iterate forwards? 0 to size-1.
                // If 0 (Head) at X. Chest at X+20...
                // That puts Head on Left.

                // Let's handle iteration order based on orientation to be safe?
                // Or just adjust the position calculation.
                // If we want consistent iteration (backwards for Vertical-Up), we can just subtract X for horizontal?
                // Or maybe the user wants Head on Left.
                // Let's stick to the current loop but change X/Y update.

                // Vertical: Offhand at Bottom (Y), Head at Top.
                // Horizontal: Offhand at Right? Head at Left?
                // If loop is backwards: Offhand is first rendered.
                // If we want Head Left, we need Head to be at `x`.
                // So Offhand should be at `x + something`.
                // This means we should start at `x + totalWidth` and subtract?
                // Or just iterate forwards for horizontal.

                // Let's iterate FORWARDS for horizontal (Head -> Offhand).
                // But wait, the loop was `items.size() - 1` to `0` specifically to stack UP from the anchor.

                // Actually, let's keep it simple.
                // Vertical: Anchor is Bottom-Left. Grows UP. (Head is Top).
                // Horizontal: Anchor is Bottom-Left. Grows RIGHT. (Head is Left).

                // If we want Head at Top (Vertical), we need Head to have smallest Y.
                // Anchor Y is the "Bottom". So we start at Y and subtract.
                // So we need to render the BOTTOM item first (Offhand) at Y. Then render Main at Y-20.
                // This matches the loop `for (int i = items.size() - 1 ...)`

                // If we want Head at Left (Horizontal), we need Head to have smallest X.
                // Anchor X is "Left".
                // If we use the same loop (Offhand first), we would render Offhand at X. Then Main at X+20?
                // That puts Offhand at Left. Head at Right.
                // That might be weird. Head -> Chest -> ... -> Main -> Off usually reads Left to Right.

                // So for Horizontal, we want Head rendered at X.
                // This means we should process Head first?
                // Or process Offhand last.
            } else {
                 currentY -= itemSpacing;
            }
        }

        // Re-implementing with cleaner loop logic
        // We want:
        // VERTICAL: Anchor is Bottom. Head is Top. Stack grows UP.
        // HORIZONTAL: Anchor is Left. Head is Left. Stack grows RIGHT.

        if (config.orientation == EquipSightConfig.Orientation.VERTICAL) {
            // Render Bottom-to-Top (Offhand/Feet -> Head)
            // Loop backwards
            for (int i = items.size() - 1; i >= 0; i--) {
                ItemStack stack = items.get(i);
                if (shouldSkip(stack, config)) continue;

                renderItem(context, client, stack, currentX, currentY, config);
                currentY -= itemSpacing;
            }
        } else {
            // HORIZONTAL
            // Render Left-to-Right (Head -> Offhand)
            // Loop forwards
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (shouldSkip(stack, config)) continue;

                renderItem(context, client, stack, currentX, currentY, config);
                currentX += itemSpacing;
            }
        }

        context.getMatrices().pop();
    }

    private boolean shouldSkip(ItemStack stack, EquipSightConfig config) {
        if (stack.isEmpty()) return true;
        if (config.onlyShowDamageable && !stack.isDamageable()) return true;
        return false;
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
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200); // Bring text forward

        float scale = 0.5f; // Small text
        context.getMatrices().scale(scale, scale, 1.0f);

        int textWidth = textRenderer.getWidth(text);
        // Center relative to item (which is 16px wide)

        int scaledX = (int) ((x + 8) / scale - textWidth / 2);
        int scaledY = (int) ((y + 16) / scale); // Below the item

        context.drawText(textRenderer, text, scaledX, scaledY, rgb, true);

        context.getMatrices().pop();
    }
}
