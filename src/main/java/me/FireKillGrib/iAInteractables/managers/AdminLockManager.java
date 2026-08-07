package me.FireKillGrib.iAInteractables.managers;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.fluids.IOState;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class AdminLockManager {
    private final File file;
    private YamlConfiguration config;

    public AdminLockManager() {
        file = new File(Plugin.getInstance().getDataFolder(), "fluids_admin_locks.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void setLockedSide(String machineId, BlockFace face, IOState state) {
        if (state == null) {
            config.set(machineId + "." + face.name(), null); // Снимаем блокировку
        } else {
            config.set(machineId + "." + face.name(), state.name());
        }
        try { config.save(file); } catch (IOException ignored) {}
    }

    public IOState getLockedSide(String machineId, BlockFace face) {
        String path = machineId + "." + face.name();
        if (config.contains(path)) {
            return IOState.valueOf(config.getString(path));
        }
        return null;
    }
}