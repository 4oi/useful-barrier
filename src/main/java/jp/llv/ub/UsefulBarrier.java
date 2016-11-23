/* 
 * Copyright (C) 2016 toyblocks
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jp.llv.ub;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class UsefulBarrier extends JavaPlugin implements Listener {

    private static final Set<ItemDrop> ITEM_DROPS = new HashSet();

    private static Material BREAKER;
    private static Material VISIBLE;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration conf = getConfig();
        try {
            BREAKER = Material.valueOf(conf.getString("break_tool").toUpperCase());
            VISIBLE = Material.valueOf(conf.getString("visible_tool").toUpperCase());
        } catch (IllegalArgumentException ex) {
            getLogger().warning("No such material. Now using default tool");
            BREAKER = Material.BARRIER;
            VISIBLE = Material.BARRIER;
        }
        EffectingTask.init(this, conf.getInt("check_range"), conf.getLong("check_freq"), VISIBLE);
        ConfigurationSection drops = conf.getConfigurationSection("drops");
        for (String name : drops.getKeys(false)) {
            Material m;
            try {
                m = Material.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ex) {
                getLogger().log(Level.WARNING, "No such material: {0}", name);
                continue;
            }
            ITEM_DROPS.add(new ItemDrop(m, drops.getDouble(name)));
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void checkItemInHand(Player player) {
        if ((player.isOnline())
                && (player.getGameMode() == GameMode.SURVIVAL)
                && (player.getInventory().getItemInMainHand() != null) && (player.getInventory().getItemInMainHand().getType() == Material.BARRIER)) {
            EffectingTask.call(player);
        } else {
            EffectingTask.cancel(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemEvent(PlayerItemHeldEvent event) {
        checkItemInHand(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemEvent(PlayerJoinEvent event) {
        checkItemInHand(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemEvent(PlayerQuitEvent event) {
        checkItemInHand(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemEvent(PlayerDropItemEvent event) {
        checkItemInHand(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemEvent(PlayerGameModeChangeEvent event) {
        checkItemInHand(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemEvent(InventoryClickEvent event) {
        if ((event.getWhoClicked() instanceof Player)) {
            checkItemInHand((Player) event.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemEvent(PlayerPickupItemEvent event) {
        checkItemInHand(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractBarrier(PlayerInteractEvent event) {
        if ((event.getAction() != Action.LEFT_CLICK_BLOCK) || (event.getPlayer().getInventory().getItemInMainHand().getType() != BREAKER) || (event.getPlayer().getGameMode() != GameMode.SURVIVAL) || (event.getClickedBlock().getType() != Material.BARRIER)) {
            return;
        }
        BlockBreakEvent newEvent = new BlockBreakEvent(event.getClickedBlock(), event.getPlayer());
        getServer().getPluginManager().callEvent(newEvent);
        if (newEvent.isCancelled()) {
            return;
        }
        Location l = event.getClickedBlock().getLocation();
        event.getClickedBlock().setType(Material.AIR);
        l.getWorld().playEffect(l, Effect.STEP_SOUND, Material.GLASS);
        ITEM_DROPS.stream().forEach(id -> event.getClickedBlock().getWorld().dropItem(l, id.getDrops()));
    }

    private static class ItemDrop {

        private static final Random RANDOM = new Random();
        private final Material material;
        private final double percentage;

        public ItemDrop(Material material, double percentage) {
            this.material = material;
            this.percentage = percentage;
        }

        public ItemStack getDrops() {
            int amount = (int) (this.percentage / 100.0D);
            double chance = this.percentage / 100.0D - amount;
            if (chance >= RANDOM.nextDouble()) {
                amount++;
            }
            return new ItemStack(this.material, amount);
        }
    }
}
