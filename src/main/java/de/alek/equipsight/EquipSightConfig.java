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
    public int positionY = -1; // -1 means auto-calculate relative to bottom/hotbar

    @ConfigEntry.Gui.Tooltip
    public float scale = 1.0f;

    @ConfigEntry.Gui.Tooltip
    public boolean showDurability = true;

    @ConfigEntry.Gui.Tooltip
    public boolean onlyShowDamageable = true;

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Gui.Tooltip
    public DisplayStyle displayStyle = DisplayStyle.PERCENTAGE;

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Gui.Tooltip
    public Orientation orientation = Orientation.VERTICAL;

    public enum DisplayStyle {
        ABSOLUTE, PERCENTAGE
    }

    public enum Orientation {
        VERTICAL, HORIZONTAL
    }

    public static EquipSightConfig get() {
        return AutoConfig.getConfigHolder(EquipSightConfig.class).getConfig();
    }
}
