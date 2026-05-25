package me.FireKillGrib.iAInteractables.menu.admin;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.utils.ChatInputManager;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.io.File;
import java.io.IOException;

public class AdminEffectEditorGUI {
    private final String category;
    private final String stationName;

    public AdminEffectEditorGUI(String category, String stationName) {
        this.category = category;
        this.stationName = stationName;
    }

    public void open(Player player) {
        String effectPath = category.equals("furnaces") ? "effects.on-complete" : "effects.on-craft";
        
        Gui gui = Gui.normal()
                .setStructure(
                        "X X X X X X X X X",
                        "X X S X X X P X X",
                        "X X X X B X X X X"
                )
                .addIngredient('X', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")))
                .addIngredient('B', new SimpleItem(new ItemBuilder(Material.BARRIER).setDisplayName("&cBack"), 
                        click -> new AdminStationSelectGUI(category).open(player)))
                .addIngredient('S', new SimpleItem(new ItemBuilder(Material.NOTE_BLOCK)
                        .setDisplayName("&eEdit Sound")
                        .addLoreLines("&7Click to type new sound in chat"), click -> {
                    ChatInputManager.requestInput(player, "&eType Bukkit Sound Name (e.g. BLOCK_ANVIL_USE):", input -> {
                        modifyConfig(effectPath + ".sound.sound", input.toUpperCase());
                        player.sendMessage(ChatUtil.color("&aSound updated to " + input.toUpperCase()));
                    });
                }))
                .addIngredient('P', new SimpleItem(new ItemBuilder(Material.FIRE_CHARGE)
                        .setDisplayName("&dEdit Particle")
                        .addLoreLines("&7Click to type new particle in chat"), click -> {
                    ChatInputManager.requestInput(player, "&dType Bukkit Particle Name (e.g. FLAME):", input -> {
                        modifyConfig(effectPath + ".particle.particle", input.toUpperCase());
                        player.sendMessage(ChatUtil.color("&aParticle updated to " + input.toUpperCase()));
                    });
                }))
                .build();

        Window.single()
                .setTitle(new AdventureComponentWrapper(ChatUtil.color("&8Edit Effects: " + stationName)))
                .setGui(gui)
                .build(player)
                .open();
    }

    private void modifyConfig(String path, Object value) {
        File file = new File(Plugin.getInstance().getDataFolder(), category + "/" + stationName + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(path, value);
        try {
            config.save(file);
            Plugin.getInstance().reload();
        } catch (IOException ignored) {}
    }
}