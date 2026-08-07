package me.FireKillGrib.iAInteractables;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import me.FireKillGrib.iAInteractables.commands.MainCommand;
import me.FireKillGrib.iAInteractables.commands.MultiblockCommand;
import me.FireKillGrib.iAInteractables.fluids.FluidRegistry;
import me.FireKillGrib.iAInteractables.fluids.WrenchListener;
import me.FireKillGrib.iAInteractables.fluids.network.FluidNetworkManager;
import me.FireKillGrib.iAInteractables.fluids.pipes.PipeListener;
import me.FireKillGrib.iAInteractables.fluids.pipes.PipeManager;
import me.FireKillGrib.iAInteractables.listeners.FurnitureListener;
import me.FireKillGrib.iAInteractables.listeners.ItemsAdderListener;
import me.FireKillGrib.iAInteractables.listeners.RecipeBookListener;
import me.FireKillGrib.iAInteractables.listeners.VanillaRecipeListener;
import me.FireKillGrib.iAInteractables.managers.*;
import me.FireKillGrib.iAInteractables.multiblock.MultiblockListener;
import me.FireKillGrib.iAInteractables.multiblock.MultiblockManager;
import me.FireKillGrib.iAInteractables.utils.InfoGenerator; // <--- НОВЫЙ ИМПОРТ
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
    @Getter private MultiblockManager multiblockManager;
    @Getter private FluidRegistry fluidRegistry; 
    @Getter private FluidNetworkManager fluidNetworkManager;
    @Getter private AdminLockManager adminLockManager;
    @Getter private PipeManager pipeManager; 

    @Override
    public void onEnable() {
        instance = this;
        int pluginId = 29019;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));
        metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            valueMap.put("servers", 1);
            valueMap.put("players", Bukkit.getOnlinePlayers().size());
            return valueMap;
        }));
        
        saveDefaultConfig();
        
        // ГЕНЕРАЦИЯ СПРАВОЧНИКА (Всегда свежий при рестарте)
        InfoGenerator.generate(getDataFolder());
        
        createDefaultConfigs();
        
        // Порядок инициализации ВАЖЕН! 
        configManager = new ConfigManager();
        recipeManager = new RecipeManager();
        instanceManager = new InstanceManager();
        furnaceDataManager = new FurnaceDataManager(getDataFolder());
        integrationManager = new IntegrationManager();
        vanillaRecipeManager = new VanillaRecipeManager();
        multiblockManager = new MultiblockManager();
        adminLockManager = new AdminLockManager();
        
        fluidRegistry = new FluidRegistry(); 
        fluidNetworkManager = new FluidNetworkManager(); 
        pipeManager = new PipeManager(); 
        furnaceManager = new FurnaceManager();
        
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new MainCommand());
        manager.registerCommand(new MultiblockCommand());
        
        getServer().getPluginManager().registerEvents(new FurnitureListener(), this);
        getServer().getPluginManager().registerEvents(new VanillaRecipeListener(), this);
        getServer().getPluginManager().registerEvents(new me.FireKillGrib.iAInteractables.utils.ChatInputManager(), this);
        getServer().getPluginManager().registerEvents(new RecipeBookListener(), this);
        getServer().getPluginManager().registerEvents(new MultiblockListener(multiblockManager), this);
        getServer().getPluginManager().registerEvents(new WrenchListener(), this);
        getServer().getPluginManager().registerEvents(new PipeListener(), this); 
        getServer().getPluginManager().registerEvents(new me.FireKillGrib.iAInteractables.fluids.InspectorListener(), this);
        
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            getServer().getPluginManager().registerEvents(new ItemsAdderListener(), this);
        }
        
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
        if (furnaceManager != null) furnaceManager.shutdown();
    }
    
    public void reload() {
        reloadConfig();
        // Обновляем инфо-файл и при /iai reload тоже
        InfoGenerator.generate(getDataFolder());
        
        if (configManager != null) configManager.reload();
        if (fluidRegistry != null) fluidRegistry.loadFluids(); 
        if (pipeManager != null) pipeManager.reload(); 
        if (recipeManager != null) {
            recipeManager.clearAll();
            recipeManager.loadFurnaces();
            recipeManager.loadWorkbenches();
            recipeManager.loadSmithingTables();
        }
        if (vanillaRecipeManager != null) vanillaRecipeManager.load();
    }
    
    private void createDefaultConfigs() {
        File furnacesFolder = new File(getDataFolder(), "furnaces");
        File workbenchesFolder = new File(getDataFolder(), "workbenches");
        File smithingFolder = new File(getDataFolder(), "smithing_tables");
        if (!furnacesFolder.exists()) { furnacesFolder.mkdirs(); saveResource("furnaces/default.yml", false); }
        if (!workbenchesFolder.exists()) { workbenchesFolder.mkdirs(); saveResource("workbenches/default.yml", false); }
        if (!smithingFolder.exists()) { smithingFolder.mkdirs(); saveResource("smithing_tables/default.yml", false); }
    }
}