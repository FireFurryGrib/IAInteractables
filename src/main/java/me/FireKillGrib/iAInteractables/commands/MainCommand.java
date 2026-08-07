package me.FireKillGrib.iAInteractables.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.menu.admin.AdminMainMenuGUI;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

@CommandAlias("interactables|iai")
public class MainCommand extends BaseCommand {
    private final Map<UUID, String> pendingFixes = new HashMap<>();

    @Default
    public void onDefault(CommandSender sender) {
        ChatUtil.sendConfigMessageList(sender, "usages");
    }

    @Subcommand("reload")
    @CommandPermission("iainteractables.reload")
    public void onReload(CommandSender sender) {
        Plugin.getInstance().reload();
        ChatUtil.sendConfigMessage(sender, "reload");
    }

    @Subcommand("editor|admin")
    @CommandPermission("iainteractables.admin")
    public void onEditor(Player player) {
        new AdminMainMenuGUI().open(player);
    }

    @Subcommand("fix")
    @CommandPermission("iainteractables.admin")
    public void onFix(Player player, String stationName) {
        pendingFixes.put(player.getUniqueId(), stationName);
        player.sendMessage(ChatUtil.color("&eВы уверены, что хотите конвертировать конфиг &c" + stationName + "&e? Это заменит R, P, Z на систему тегов $. Введите &c/iai confirm &eдля подтверждения."));
    }

    @Subcommand("confirm")
    @CommandPermission("iainteractables.admin")
    public void onConfirm(Player player) {
        if (!pendingFixes.containsKey(player.getUniqueId())) return;
        String stationName = pendingFixes.remove(player.getUniqueId());
        
        File dataFolder = Plugin.getInstance().getDataFolder();
        File[] folders = {new File(dataFolder, "furnaces"), new File(dataFolder, "workbenches"), new File(dataFolder, "smithing_tables")};
        boolean found = false;
        
        for (File folder : folders) {
            File file = new File(folder, stationName + ".yml");
            if (file.exists()) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                List<String> struct = cfg.getStringList("structure");
                List<String> newStruct = new ArrayList<>();
                List<String> funcs = new ArrayList<>();
                
                for (String row : struct) {
                    StringBuilder sb = new StringBuilder();
                    for (char c : row.toCharArray()) {
                        if (c == 'P') { sb.append('$'); funcs.add("PRO"); }
                        else if (c == 'R') { sb.append('$'); funcs.add("RES"); }
                        else if (c == 'Z') { sb.append('$'); funcs.add("BAC"); }
                        // Убрана конвертация 'B' (ведро), теперь это обычный слот ингредиента, если не указан $ и тег BUC
                        else { sb.append(c); }
                    }
                    newStruct.add(sb.toString());
                }
                
                cfg.set("structure", newStruct);
                cfg.set("special_functions", funcs);
                try { cfg.save(file); found = true; } catch (Exception ignored) {}
                break;
            }
        }
        
        if (found) {
            Plugin.getInstance().reload();
            player.sendMessage(ChatUtil.color("&aКонфиг успешно обновлен и плагин перезагружен!"));
        } else {
            player.sendMessage(ChatUtil.color("&cСтанция не найдена. Проверьте имя файла (без .yml)."));
        }
    }
}