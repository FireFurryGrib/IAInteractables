package me.FireKillGrib.iAInteractables.managers;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.Furnace;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class FurnaceManager {
    private final Map<String, FurnaceController> controllers = new HashMap<>();

    public FurnaceController getOrCreate(Furnace furnace, Location location) {
        String key = locationToString(location);
        return controllers.computeIfAbsent(key, k -> new FurnaceController(furnace, location));
    }

    public FurnaceController get(Location location) {
        String key = locationToString(location);
        return controllers.get(key);
    }

    public void remove(Location location) {
        String key = locationToString(location);
        FurnaceController controller = controllers.remove(key);
        if (controller != null) {
            controller.shutdown();
        }
    }

    public void shutdown() {
        for (FurnaceController controller : controllers.values()) {
            controller.shutdown();
        }
        controllers.clear();
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "_" + 
                loc.getBlockX() + "_" + 
                loc.getBlockY() + "_" + 
                loc.getBlockZ();
    }

    public void saveAll() {
        for (FurnaceController controller : controllers.values()) {
            Plugin.getInstance().getFurnaceDataManager().saveAsync(
                controller.getLocation(),
                controller.getInventory(), 
                controller.getCookingProgress(),
                controller.isAutomated(),
                controller.getBlockedSlots(),
                controller.getTank() != null ? controller.getTank().getFluid() : null,
                controller.getSideConfig()
            );
        }
    }
}