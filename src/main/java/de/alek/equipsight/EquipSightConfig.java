package de.alek.equipsight;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "equipsight")
public class EquipSightConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean toggleEnabled = true;

    @ConfigEntry.Gui.Tooltip
    public int positionX = 10;

    @ConfigEntry.Gui.Tooltip
    public int positionY = -1; // -1 means auto-calculate relative to bottom/hotbar if we implement that logic, or just default Y.

    @ConfigEntry.Gui.Tooltip
    public float scale = 1.0f;

    @ConfigEntry.Gui.Tooltip
    public boolean showDurability = true;

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Gui.Tooltip
    public DisplayStyle displayStyle = DisplayStyle.PERCENTAGE;

    public enum DisplayStyle {
        ABSOLUTE, PERCENTAGE
    }

    public static EquipSightConfig get() {
        return AutoConfig.getConfigHolder(EquipSightConfig.class).getConfig();
    }
}
