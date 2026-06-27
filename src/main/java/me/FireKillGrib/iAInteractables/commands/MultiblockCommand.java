package me.FireKillGrib.iAInteractables.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.multiblock.MultiblockManager;
import me.FireKillGrib.iAInteractables.multiblock.MultiblockTemplate;
import me.FireKillGrib.iAInteractables.multiblock.TemplateScanner;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@CommandAlias("multiblock|mb")
public class MultiblockCommand extends BaseCommand {
    
    private final MultiblockManager multiblockManager;
    
    public MultiblockCommand() {
        this.multiblockManager = Plugin.getInstance().getMultiblockManager();
    }
    
    @Default
    @CatchUnknown
    public void onHelp(Player player) {
        player.sendMessage("§6--- Управление Многоблочными Структурами ---");
        player.sendMessage("§e/multiblock save <имя> §7- Сканировать 8 маркеров и сохранить шаблон");
        player.sendMessage("§e/multiblock list §7- Список шаблонов");
    }
    
    @Subcommand("save")
    @Description("Сохранить шаблон многоблочной структуры")
    @CommandPermission("multiblock.admin")
    public void onSave(Player player, String templateName) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage("§cВы должны смотреть на центральный блок (Ядро) структуры!");
            return;
        }

        try {
            MultiblockTemplate template = TemplateScanner.scan(templateName, target);
            multiblockManager.saveTemplate(template);
            player.sendMessage("§aШаблон '" + templateName + "' успешно отсканирован и сохранен!");
            player.sendMessage("§7Блоков: §f" + template.getBlocks().size() + " §7| Фурнитуры: §f" + template.getFurniture().size());
        } catch (IllegalStateException e) {
            player.sendMessage("§cОшибка сканирования: " + e.getMessage());
            player.sendMessage("§7Убедитесь, что вы поставили ровно 8 блоков-маркеров (по углам) вокруг структуры.");
        } catch (Exception e) {
            player.sendMessage("§cНепредвиденная ошибка при сканировании.");
            e.printStackTrace();
        }
    }
    
    @Subcommand("list")
    @Description("Список всех сохраненных шаблонов")
    @CommandPermission("multiblock.admin")
    public void onList(Player player) {
        player.sendMessage("§eСписок загруженных шаблонов:");
        for (MultiblockTemplate t : multiblockManager.getAllTemplates()) {
            player.sendMessage("§7- §f" + t.getName());
        }
    }
}