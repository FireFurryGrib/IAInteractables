package me.FireKillGrib.iAInteractables.multiblock;

import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

public class MultiblockGUI {
    
    // Идеальная сетка инвентаря (5 строк ровно по 9 символов)
    private static final String[] GUI_PATTERN = {
        "XXXXXXXXX",
        "X1234567X",
        "X890abcdX",
        "XefghijkX",
        "XXXXXXXXX"
    };
    
    public static void openGUI(Player player, MultiblockInstance instance) {
        Gui gui = Gui.normal()
            .setStructure(GUI_PATTERN)
            .addIngredient('X', new SimpleItem(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" ")))
            .addIngredient('1', new SimpleItem(new ItemBuilder(Material.PAPER).setDisplayName("§eИнформация о структуре"), click -> handleGUIClick(player, instance, "1")))
            .addIngredient('2', new SimpleItem(new ItemBuilder(Material.REDSTONE).setDisplayName("§cКонтроль"), click -> handleGUIClick(player, instance, "2")))
            .addIngredient('3', new SimpleItem(new ItemBuilder(Material.CHEST).setDisplayName("§6Хранилище"), click -> handleGUIClick(player, instance, "3")))
            .addIngredient('4', new SimpleItem(new ItemBuilder(Material.FURNACE).setDisplayName("§8Работа"), click -> handleGUIClick(player, instance, "4")))
            .addIngredient('5', new SimpleItem(new ItemBuilder(Material.WATER_BUCKET).setDisplayName("§bЖидкости"), click -> handleGUIClick(player, instance, "5")))
            .addIngredient('6', new SimpleItem(new ItemBuilder(Material.LAVA_BUCKET).setDisplayName("§cТепло"), click -> handleGUIClick(player, instance, "6")))
            .addIngredient('7', new SimpleItem(new ItemBuilder(Material.ENCHANTING_TABLE).setDisplayName("§dМагия"), click -> handleGUIClick(player, instance, "7")))
            .addIngredient('8', new SimpleItem(new ItemBuilder(Material.BEACON).setDisplayName("§bЭнергия"), click -> handleGUIClick(player, instance, "8")))
            .addIngredient('9', new SimpleItem(new ItemBuilder(Material.COMPARATOR).setDisplayName("§cСигналы"), click -> handleGUIClick(player, instance, "9")))
            .addIngredient('0', new SimpleItem(new ItemBuilder(Material.NOTE_BLOCK).setDisplayName("§aЗвуки"), click -> handleGUIClick(player, instance, "0")))
            .addIngredient('a', new SimpleItem(new ItemBuilder(Material.MUSIC_DISC_13).setDisplayName("§5Музыка"), click -> handleGUIClick(player, instance, "a")))
            .addIngredient('b', new SimpleItem(new ItemBuilder(Material.SUGAR).setDisplayName("§fРецепты"), click -> handleGUIClick(player, instance, "b")))
            .addIngredient('c', new SimpleItem(new ItemBuilder(Material.CRAFTING_TABLE).setDisplayName("§eСоздание"), click -> handleGUIClick(player, instance, "c")))
            .addIngredient('d', new SimpleItem(new ItemBuilder(Material.BOOKSHELF).setDisplayName("§6Библиотека"), click -> handleGUIClick(player, instance, "d")))
            .addIngredient('e', new SimpleItem(new ItemBuilder(Material.ANVIL).setDisplayName("§7Ремонт"), click -> handleGUIClick(player, instance, "e")))
            // Заглушки для пустых слотов меню
            .addIngredient('f', new SimpleItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ")))
            .addIngredient('g', new SimpleItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ")))
            .addIngredient('h', new SimpleItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ")))
            .addIngredient('i', new SimpleItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ")))
            .addIngredient('j', new SimpleItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ")))
            .addIngredient('k', new SimpleItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName(" ")))
            .build();
        
        Window.single()
            .setTitle(new AdventureComponentWrapper(ChatUtil.color("&8Multiblock: " + instance.getTemplateName())))
            .setGui(gui)
            .build(player)
            .open();
    }
    
    public static void showStructureInfo(Player player, MultiblockInstance instance) {
        player.sendMessage("§e=== Информация о многоблочной структуре ===");
        player.sendMessage("§7Тип: §f" + instance.getTemplateName());
        player.sendMessage("§7Позиция ядра: §f" + instance.getCoreLocation().getBlockX() + ", " + instance.getCoreLocation().getBlockY() + ", " + instance.getCoreLocation().getBlockZ());
        player.sendMessage("§7Ориентация: §f" + instance.getRotation() + " градусов");
        player.sendMessage("§7Состояние: §a" + (instance.isActive() ? "Активна" : "Неактивна"));
        player.sendMessage("§7Элементов: §f" + instance.getElementsCount());
    }
    
    public static void handleGUIClick(Player player, MultiblockInstance instance, String slotName) {
        switch (slotName) {
            case "1": showStructureInfo(player, instance); player.closeInventory(); break;
            case "2": player.sendMessage("§cКонтроль структуры (В ВЕЧНОЙ РАЗРАБОТКЕ НАХУЙ, БУДЕТ ЧЕРЕЗ 8768942879849276 ЛЕТ В АПДЕЙТЕ 1.4.88.42.69 И ВООБЩЕ БЕБЕБЕ)"); player.closeInventory(); break;
            case "3": player.sendMessage("§6Хранилище структуры (В разработке)"); player.closeInventory(); break;
            case "4": player.sendMessage("§8Работа структуры (В разработке)"); player.closeInventory(); break;
            default: player.sendMessage("§7Модуль в разработке."); player.closeInventory(); break;
        }
    }
}