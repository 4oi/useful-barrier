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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class EffectingTask extends BukkitRunnable {

    private static int RANGE = 10;
    private static long CHECK_PERIOD = 20L;
    private static Material VISIBLE = Material.BARRIER;
    private static final Map<Player, EffectingTask> TASKS = new HashMap();

    private static UsefulBarrier instance;
    private final Player player;

    private EffectingTask(Player target) {
        this.player = target;
    }

    @Override
    public void run() {
        if ((!this.player.isOnline())
                || (this.player.getGameMode() != GameMode.SURVIVAL)
                || (this.player.getInventory().getItemInMainHand() == null)
                || (this.player.getInventory().getItemInMainHand().getType() != VISIBLE)) {
            cancel(this.player);
            return;
        }

        Set<Location> locations = new HashSet();
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    Location l = this.player.getLocation().getBlock().getLocation().clone().add(x, y, z);
                    if (l.getBlock().getType() == Material.BARRIER) {
                        locations.add(l.add(0.5D, 0.5D, 0.5D));
                    }
                }
            }
        }

        locations.stream().forEach((l) -> this.player.spawnParticle(Particle.BARRIER, l, 1));
    }

    public static void init(UsefulBarrier instance, int range, long checkPeriod, Material visible) {
        EffectingTask.instance = instance;
        RANGE = range;
        CHECK_PERIOD = checkPeriod;
        VISIBLE = visible;
    }

    public static void call(Player target) {
        if (instance == null) {
            throw new IllegalStateException("not initialized");
        }
        EffectingTask newTask = new EffectingTask(target);
        TASKS.put(target, newTask);
        newTask.runTaskTimer(instance, 0L, CHECK_PERIOD);
    }

    public static void cancel(Player target) {
        EffectingTask task = (EffectingTask) TASKS.get(target);
        if (task == null) {
            return;
        }
        task.cancel();
        TASKS.remove(target);
    }
}
