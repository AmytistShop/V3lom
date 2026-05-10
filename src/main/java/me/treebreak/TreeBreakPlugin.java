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

public final class TreeBreakPlugin extends JavaPlugin implements Listener {

    // Игрок -> задача ломания
    private final Map<UUID, BukkitTask> breakingTasks = new HashMap<>();

    @Override
    public void onEnable() {

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("TreeBreak enabled");
    }

    @Override
    public void onDisable() {

        for (BukkitTask task : breakingTasks.values()) {
            task.cancel();
        }

        breakingTasks.clear();
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Только дерево
        if (!isLog(block.getType())) {
            return;
        }

        // Креатив не трогаем
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Уже ломает
        if (breakingTasks.containsKey(player.getUniqueId())) {
            return;
        }

        // Отменяем обычное ломание
        event.setCancelled(true);

        startBreaking(player, block);
    }

    private void startBreaking(Player player, Block block) {

        UUID uuid = player.getUniqueId();

        BukkitTask task = new BukkitRunnable() {

            int stage = 0;

            @Override
            public void run() {

                // Игрок вышел
                if (!player.isOnline()) {
                    stop();
                    return;
                }

                // Блок уже сломан
                if (block.getType() == Material.AIR) {
                    stop();
                    return;
                }

                // Далеко отошел
                if (player.getLocation().distance(block.getLocation()) > 6) {
                    stop();
                    return;
                }

                // Перестал смотреть
                Block target = player.getTargetBlockExact(6);

                if (target == null || !target.getLocation().equals(block.getLocation())) {
                    stop();
                    return;
                }

                // Анимация ломания
                player.sendBlockDamage(block.getLocation(), stage / 10f);

                stage++;

                // Закончили ломать
                if (stage >= 10) {

                    ItemStack tool = player.getInventory().getItemInMainHand();

                    block.breakNaturally(tool);

                    player.sendBlockDamage(block.getLocation(), 0f);

                    stop();
                }
            }

            private void stop() {

                player.sendBlockDamage(block.getLocation(), 0f);

                BukkitTask current = breakingTasks.remove(uuid);

                if (current != null) {
                    current.cancel();
                }
            }

        // СКОРОСТЬ:
        // 2L = быстро
        // 4L = x2 медленнее
        // 6L = x3 медленнее
        // 8L = очень медленно

        }.runTaskTimer(this, 0L, 4L);

        breakingTasks.put(uuid, task);
    }

    @EventHandler
    public void onAbort(BlockDamageAbortEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        BukkitTask task = breakingTasks.remove(uuid);

        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();

        BukkitTask task = breakingTasks.remove(uuid);

        if (task != null) {
            task.cancel();
        }
    }

    private boolean isLog(Material material) {

        String name = material.name();

        return name.endsWith("_LOG")
                || name.endsWith("_WOOD")
                || name.equals("CRIMSON_STEM")
                || name.equals("WARPED_STEM");
    }
          }
