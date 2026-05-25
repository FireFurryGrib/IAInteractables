package me.FireKillGrib.iAInteractables.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.FireKillGrib.iAInteractables.Plugin;
import me.FireKillGrib.iAInteractables.menu.admin.AdminMainMenuGUI;
import me.FireKillGrib.iAInteractables.utils.ChatUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("interactables|iai")
public class MainCommand extends BaseCommand {
    
    @Default
    public void onDefault(CommandSender sender){
        ChatUtil.sendConfigMessageList(sender,"usages");
    }

    @Subcommand("reload")
    @CommandPermission("iainteractables.reload")
    public void onReload(CommandSender sender){
        Plugin.getInstance().reload();
        ChatUtil.sendConfigMessage(sender,"reload");
    }

    @Subcommand("editor|admin")
    @CommandPermission("iainteractables.admin")
    public void onEditor(Player player){
        new AdminMainMenuGUI().open(player);
    }
}