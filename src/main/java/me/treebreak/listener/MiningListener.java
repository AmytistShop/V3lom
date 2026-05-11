package me.logslow.listener;

import me.logslow.LogSlowPlugin;
import me.logslow.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MiningListener implements Listener {

    private final LogSlowPlugin plugin;

    private final Set<UUID> slowedPlayers = new HashSet<>();

    public MiningListener(LogSlowPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {

        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        boolean onlyLogs = plugin.getConfig().getBoolean("only_logs", true);

        if (onlyLogs && !LogUtil.isLog(material)) {
            removeFatigue(player);
            return;
        }

        double multiplier = plugin.getConfig().getDouble("slow_multiplier", 2.0);

        int amplifier = calculateAmplifier(multiplier);

        PotionEffect effect = new PotionEffect(
                PotionEffectType.MINING_FATIGUE,
                40,
                amplifier,
                false,
                false,
                false
        );

        player.addPotionEffect(effect, true);

        slowedPlayers.add(player.getUniqueId());

        if (plugin.getConfig().getBoolean("verbose", false)) {
            plugin.getLogger().info(player.getName() + " mining " + material.name());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAbort(BlockDamageAbortEvent event) {
        removeFatigue(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeFatigue(event.getPlayer());
        }, 1L);
    }

    private void removeFatigue(Player player) {

        if (!slowedPlayers.contains(player.getUniqueId())) {
            return;
        }

        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        slowedPlayers.remove(player.getUniqueId());
    }

    private int calculateAmplifier(double multiplier) {

        if (multiplier <= 1.2) {
            return 0;
        }

        if (multiplier <= 1.8) {
            return 1;
        }

        if (multiplier <= 2.8) {
            return 2;
        }

        if (multiplier <= 4.0) {
            return 3;
        }

        return 4;
    }
}
