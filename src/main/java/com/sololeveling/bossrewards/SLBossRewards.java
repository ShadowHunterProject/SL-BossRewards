package com.sololeveling.bossrewards;

import com.sololeveling.bossrewards.commands.ReloadCommand;
import com.sololeveling.bossrewards.listeners.BossDeathListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SLBossRewards extends JavaPlugin {

    private static SLBossRewards instance;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        createMessagesConfig();

        if (getServer().getPluginManager().getPlugin("MythicMobs") == null) {
            getLogger().severe("Khong tim thay MythicMobs! Plugin bi vo hieu hoa.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new BossDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new com.sololeveling.bossrewards.listeners.BossDamageListener(this), this);
        if (getCommand("slbossrewards") != null) {
            getCommand("slbossrewards").setExecutor(new ReloadCommand(this));
        }
        
        getLogger().info("SL-BossRewards da duoc kich hoat thanh cong!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SL-BossRewards da bi vo hieu hoa.");
    }

    public static SLBossRewards getInstance() {
        return instance;
    }

    public void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public void reloadConfigs() {
        reloadConfig();
        if (messagesFile.exists()) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        } else {
            createMessagesConfig();
        }
    }
}
