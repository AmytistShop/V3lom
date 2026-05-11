package me.logslow;

import me.logslow.listener.MiningListener;
import org.bukkit.plugin.java.JavaPlugin;

public class LogSlowPlugin extends JavaPlugin {

    private static LogSlowPlugin instance;

    public static LogSlowPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new MiningListener(this), this);

        getLogger().info("LogSlow enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LogSlow disabled.");
    }
}
