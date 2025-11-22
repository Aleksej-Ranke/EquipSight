package de.alek.equipsight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Environment(EnvType.CLIENT)
public class EquipSightClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register Config
        AutoConfig.register(EquipSightConfig.class, GsonConfigSerializer::new);

        // Register HUD Overlay
        HudRenderCallback.EVENT.register(new EquipSightOverlay());
    }
}
