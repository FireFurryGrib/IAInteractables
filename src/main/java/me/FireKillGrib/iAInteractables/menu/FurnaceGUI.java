package me.FireKillGrib.iAInteractables.menu;

import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.data.Furnace;
import me.FireKillGrib.iAInteractables.data.FurnaceRecipe;
import me.FireKillGrib.iAInteractables.managers.FurnaceController;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import me.FireKillGrib.iAInteractables.utils.StructureHelper;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.SlotElement;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.*;

public class FurnaceGUI {
    private final Furnace furnace;
    private final FurnaceController controller;
    private Window window;
    private Gui gui;
    private BukkitTask updateTask;
    private int progressGuiSlotIndex = -1;
    
    private final List<Integer> fluidGuiSlots = new ArrayList<>();
    private final Map<Integer, Integer> fluidGuiSlotTypes = new HashMap<>();

    public FurnaceGUI(Furnace furnace, org.bukkit.Location location, FurnaceController controller) {
        this.furnace = furnace;
        this.controller = controller;
    }

    public void open(Player player) {
        gui = createGui(player);
        window = Window.single()
                .setTitle(new AdventureComponentWrapper(ChatUtil.color(furnace.getTitle())))
                .setGui(gui)
                .addCloseHandler(this::onClose)
                .build(player);
        window.open();
        startProgressUpdater();
    }

    private Gui createGui(Player player) {
        String[] mappedStruct = StructureHelper.translate(furnace.getStructure(), furnace.getSpecialFunctions());
        Gui.Builder.Normal guiBuilder = Gui.normal().setStructure(mappedStruct);
                
        Map<Character, Integer> controllerStructure = controller.getStructure();
        Set<Character> processedChars = new HashSet<>();
        
        int guiSlotCounter = 0;
        for (String row : mappedStruct) {
            for (char c : row.toCharArray()) {
                if (c == '1') progressGuiSlotIndex = guiSlotCounter;
                if (c >= '5' && c <= '8') {
                    fluidGuiSlots.add(guiSlotCounter);
                    fluidGuiSlotTypes.put(guiSlotCounter, c - '5');
                }

                if (!processedChars.contains(c)) {
                    processedChars.add(c);
                    if (c == 'X') {
                        guiBuilder.addIngredient('X', furnace.getFiller());
                    } else if (c == '1') {
                        ItemStack pItem = addSettingsLore(furnace.getProgressBar().getItemForProgress(0));
                        guiBuilder.addIngredient('1', new SimpleItem(pItem, click -> {
                            if (click.getEvent().isRightClick()) new FurnaceSettingsGUI(furnace, controller).open(player);
                        }));
                    } else if (c == '2') {
                        guiBuilder.addIngredient('2', new SlotElement.InventorySlotElement(controller.getInventory(), controller.getResultSlot()));
                    } else if (c == '3') {
                        guiBuilder.addIngredient('3', new SimpleItem(new ItemBuilder(Material.BARRIER).setDisplayName("§cBack"), click -> player.closeInventory()));
                    } else if (c == '4') {
                        guiBuilder.addIngredient('4', new SlotElement.InventorySlotElement(controller.getInventory(), controller.getBucketSlot()));
                    } else if (c >= '5' && c <= '8') {
                        guiBuilder.addIngredient(c, new DynamicFluidItem(c - '5'));
                    } else {
                        Integer inventorySlot = controllerStructure.get(c);
                        if (inventorySlot != null) {
                            guiBuilder.addIngredient(c, new SlotElement.InventorySlotElement(controller.getInventory(), inventorySlot));
                        } else {
                            guiBuilder.addIngredient(c, new SimpleItem(new ItemBuilder(Material.AIR)));
                        }
                    }
                }
                guiSlotCounter++;
            }
        }
        return guiBuilder.build();
    }

    private ItemStack addSettingsLore(ItemStack item) {
        ItemStack modified = item.clone();
        org.bukkit.inventory.meta.ItemMeta meta = modified.getItemMeta();
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            lore.add(ChatUtil.color("&8 "));
            lore.add(ChatUtil.color("&e\u25B6 Right-Click to open Settings"));
            meta.lore(lore);
            modified.setItemMeta(meta);
        }
        return modified;
    }

    private void startProgressUpdater() {
        updateTask = Plugin.getInstance().getServer().getScheduler()
                .runTaskTimer(Plugin.getInstance(), this::updateProgressBar, 0L, 5L);
    }

    private void updateProgressBar() {
        if (gui == null) return;
        
        if (progressGuiSlotIndex != -1) {
            ItemStack progressItem;
            if (!controller.isCooking()) {
                progressItem = furnace.getProgressBar().getItemForProgress(0);
            } else {
                FurnaceRecipe recipe = controller.getCurrentRecipe();
                if (recipe != null) {
                    progressItem = furnace.getProgressBar().getItemForProgress(
                            controller.getProgressPercentage(),
                            controller.getCookingProgress(),
                            recipe.getCookTimeTicks()
                    );
                } else {
                    progressItem = furnace.getProgressBar().getItemForProgress(0);
                }
            }
            
            ItemStack finalItem = addSettingsLore(progressItem);
            gui.setItem(progressGuiSlotIndex, new SimpleItem(finalItem, click -> {
                if (click.getEvent().isRightClick()) {
                    new FurnaceSettingsGUI(furnace, controller).open(click.getPlayer());
                }
            }));
        }

        // Обновляем жидкости
        for (int slot : fluidGuiSlots) {
            int type = fluidGuiSlotTypes.get(slot);
            gui.setItem(slot, new DynamicFluidItem(type));
        }
    }

    private void onClose() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void forceClose() {
        onClose();
        if (window != null) window.close();
    }

    private class DynamicFluidItem extends AbstractItem {
        private final int tankIndex; 
        private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

        public DynamicFluidItem(int tankIndex) { this.tankIndex = tankIndex; }

        @Override
        public ItemProvider getItemProvider() {
            Map<String, Integer> contents = controller.getTank().getContents();
            if (contents.isEmpty()) return new ItemBuilder(Material.GLASS_PANE).setDisplayName("§7Пусто");

            String displayId = null;
            int displayAmt = 0;

            if (tankIndex == 3) { // LQU
                for (Map.Entry<String, Integer> entry : contents.entrySet()) {
                    if (entry.getValue() > displayAmt) { displayId = entry.getKey(); displayAmt = entry.getValue(); }
                }
            } else { // LQ1, LQ2, LQ3
                int i = 0;
                for (Map.Entry<String, Integer> entry : contents.entrySet()) {
                    if (i == tankIndex) { displayId = entry.getKey(); displayAmt = entry.getValue(); break; }
                    i++;
                }
            }

            if (displayId == null) return new ItemBuilder(Material.GLASS_PANE).setDisplayName("§7Пусто");

            me.FireKillGrib.iAInteractables.fluids.FluidType type = Plugin.getInstance().getFluidRegistry().getFluid(displayId);
            if (type == null) return new ItemBuilder(Material.GLASS_PANE).setDisplayName("§7Неизвестно");

            return new ItemBuilder(type.getGlassMaterial())
                    .setDisplayName(serializer.serialize(ChatUtil.color(type.getChatColor() + type.getDisplayName())))
                    .addLoreLines(serializer.serialize(ChatUtil.color("&7Количество: &f" + me.FireKillGrib.iAInteractables.fluids.FluidMath.lnToMb(displayAmt) + " mB")));
        }

        @Override
        public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {}
    }
}