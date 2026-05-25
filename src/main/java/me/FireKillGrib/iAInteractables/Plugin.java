package me.FireKillGrib.iAInteractables;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import me.FireKillGrib.iAInteractables.commands.MainCommand;
import me.FireKillGrib.iAInteractables.listeners.FurnitureListener;
import me.FireKillGrib.iAInteractables.listeners.ItemsAdderListener;
import me.FireKillGrib.iAInteractables.listeners.RecipeBookListener;
import me.FireKillGrib.iAInteractables.listeners.VanillaRecipeListener;
import me.FireKillGrib.iAInteractables.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Plugin extends JavaPlugin {
    @Getter private static Plugin instance;
    @Getter private RecipeManager recipeManager;
    @Getter private ConfigManager configManager;
    @Getter private InstanceManager instanceManager;
    @Getter private FurnaceDataManager furnaceDataManager;
    @Getter private FurnaceManager furnaceManager;
    @Getter private IntegrationManager integrationManager;
    @Getter private VanillaRecipeManager vanillaRecipeManager;

    @Override
    public void onEnable() {
        instance = this;
        int pluginId = 29019;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(
            new Metrics.SimplePie("chart_id", () -> "My value")
        );
        metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            return valueMap;
        }));
        saveDefaultConfig();
        createDefaultConfigs();
        
        configManager = new ConfigManager();
        recipeManager = new RecipeManager();
        instanceManager = new InstanceManager();
        furnaceDataManager = new FurnaceDataManager(getDataFolder());
        furnaceManager = new FurnaceManager();
        integrationManager = new IntegrationManager();
        vanillaRecipeManager = new VanillaRecipeManager();
        
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new MainCommand());
        
        getServer().getPluginManager().registerEvents(new FurnitureListener(), this);
        getServer().getPluginManager().registerEvents(new VanillaRecipeListener(), this);
        getServer().getPluginManager().registerEvents(new VanillaRecipeListener(), this);
        getServer().getPluginManager().registerEvents(new me.FireKillGrib.iAInteractables.utils.ChatInputManager(), this);
        
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            getServer().getPluginManager().registerEvents(new ItemsAdderListener(), this);
        }
        getServer().getPluginManager().registerEvents(new RecipeBookListener(), this);
        
        reload();
        
        getServer().getScheduler().runTaskLater(this, () -> {
            integrationManager.loadRecipes();
        }, 1200L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            furnaceManager.saveAll();
        }, 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (furnaceManager != null) {
            furnaceManager.shutdown();
        }
    }
    
    public void reload() {
        reloadConfig();
        if (configManager != null) configManager.reload();
        if (recipeManager != null) {
            recipeManager.clearAll();
            recipeManager.loadFurnaces();
            recipeManager.loadWorkbenches();
            recipeManager.loadSmithingTables();
        }
        if (vanillaRecipeManager != null) {
            vanillaRecipeManager.load();
        }
    }
    
    private void createDefaultConfigs() {
        File furnacesFolder = new File(getDataFolder(), "furnaces");
        File workbenchesFolder = new File(getDataFolder(), "workbenches");
        File smithingFolder = new File(getDataFolder(), "smithing_tables");
        if (!furnacesFolder.exists()) {
            furnacesFolder.mkdirs();
            saveResource("furnaces/default.yml", false);
        }
        if (!workbenchesFolder.exists()) {
            workbenchesFolder.mkdirs();
            saveResource("workbenches/default.yml", false);
        }
        if (!smithingFolder.exists()) {
            smithingFolder.mkdirs();
            saveResource("smithing_tables/default.yml", false);
        }
    }
}