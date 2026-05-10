package me.treebreak;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TreeBreakPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, BukkitTask> tasks = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {

        Player p = e.getPlayer();
        Block b = e.getBlock();

        if (!isLog(b.getType())) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;
        if (tasks.containsKey(p.getUniqueId())) return;

        // ❗ полностью убираем vanilla mining
        e.setCancelled(true);
        e.setInstaBreak(false);

        // сбрасываем клиентский прогресс (ВАЖНО для Bedrock)
        p.sendBlockDamage(b.getLocation(), 0f);

        start(p, b);
    }

    private void start(Player p, Block b) {

        UUID id = p.getUniqueId();

        BukkitTask task = new BukkitRunnable() {

            int stage = 0;

            @Override
            public void run() {

                if (!p.isOnline()) {
                    stop();
                    return;
                }

                if (b.getType() == Material.AIR) {
                    stop();
                    return;
                }

                if (p.getLocation().distanceSquared(b.getLocation()) > 36) {
                    stop();
                    return;
                }

                // ❗ фикс прогресса (НИКОГДА не 1.0)
                float progress = Math.min(stage / 12f, 0.98f);

                p.sendBlockDamage(b.getLocation(), progress);

                stage++;

                // финальный слом ТОЛЬКО тут
                if (stage >= 12) {

                    ItemStack tool = p.getInventory().getItemInMainHand();

                    b.breakNaturally(tool);

                    p.sendBlockDamage(b.getLocation(), 0f);

                    stop();
                }
            }

            void stop() {
                BukkitTask t = tasks.remove(id);
                if (t != null) t.cancel();
                cancel();
            }

        }.runTaskTimer(this, 0L, 4L); // скорость ломания

        tasks.put(id, task);
    }

    @EventHandler
    public void onAbort(BlockDamageAbortEvent e) {
        BukkitTask t = tasks.remove(e.getPlayer().getUniqueId());
        if (t != null) t.cancel();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        BukkitTask t = tasks.remove(e.getPlayer().getUniqueId());
        if (t != null) t.cancel();
    }

    private boolean isLog(Material m) {
        return m.name().endsWith("_LOG")
                || m.name().endsWith("_WOOD")
                || m.name().equals("CRIMSON_STEM")
                || m.name().equals("WARPED_STEM");
    }
}
