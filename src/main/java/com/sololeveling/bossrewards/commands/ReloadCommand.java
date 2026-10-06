package com.sololeveling.bossrewards.commands;

import com.sololeveling.bossrewards.SLBossRewards;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final SLBossRewards plugin;

    public ReloadCommand(SLBossRewards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("slbossrewards.admin")) {
            String prefix = plugin.getMessagesConfig().getString("prefix", "&8[&eBossRewards&8] &r");
            String msg = plugin.getMessagesConfig().getString("no-permission", "&cBạn không có quyền sử dụng lệnh này!");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfigs();
            String prefix = plugin.getMessagesConfig().getString("prefix", "&8[&eBossRewards&8] &r");
            String msg = plugin.getMessagesConfig().getString("reload-success", "&aĐã tải lại cấu hình (config.yml) và tin nhắn (messages.yml) thành công!");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
            return true;
        }

        // Default help message
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/slbossrewards reload &7- Tải lại cấu hình"));
        return true;
    }
}
