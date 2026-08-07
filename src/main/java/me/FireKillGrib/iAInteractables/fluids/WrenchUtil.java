package me.FireKillGrib.iAInteractables.fluids;

import dev.lone.itemsadder.api.CustomStack;
import me.FireKillGrib.iAInteractables.Plugin;
import org.bukkit.inventory.ItemStack;

public class WrenchUtil {
    
    public static boolean isAnyWrench(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String norm = Plugin.getInstance().getConfig().getString("wrenches.normal", "ia_interactables:wrench");
        String adm = Plugin.getInstance().getConfig().getString("wrenches.admin", "ia_interactables:admin_wrench");
        
        return isMatch(item, norm) || isMatch(item, adm);
    }

    public static boolean isAdminWrench(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String adm = Plugin.getInstance().getConfig().getString("wrenches.admin", "ia_interactables:admin_wrench");
        return isMatch(item, adm);
    }

    public static boolean isInspector(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String insp = Plugin.getInstance().getConfig().getString("tools.inspector", "ia_interactables:fluid_inspector");
        return isMatch(item, insp);
    }

    private static boolean isMatch(ItemStack item, String configString) {
        if (configString.startsWith("ia-") || configString.contains(":")) {
            CustomStack cs = CustomStack.byItemStack(item);
            if (cs == null) return false;
            
            String targetId = configString.startsWith("ia-") ? configString.substring(3) : configString;
            if (targetId.contains(":")) targetId = targetId.split(":")[1];
            return cs.getNamespacedID().equals(targetId) || cs.getId().equals(targetId);
        }
        return item.getType().name().equalsIgnoreCase(configString);
    }
}