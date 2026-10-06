package com.sololeveling.bossrewards.listeners;

import com.sololeveling.bossrewards.SLBossRewards;
import com.sololeveling.bossrewards.managers.BossDamageTracker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class BossDamageListener implements Listener {

    private final SLBossRewards plugin;

    public BossDamageListener(SLBossRewards plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        Entity damager = event.getDamager();
        
        Player playerDamager = null;
        
        if (damager instanceof Player) {
            playerDamager = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                playerDamager = (Player) projectile.getShooter();
            }
        }
        
        if (playerDamager != null) {
            // Track damage cho tat ca entity bi Player danh
            BossDamageTracker.getInstance().addDamage(target.getUniqueId(), playerDamager.getUniqueId(), event.getFinalDamage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        // Xoa du lieu khi entity chet
        BossDamageTracker.getInstance().clearBossData(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // Xoa du lieu neu chunk bi unload (tranh memory leak voi nhung entity khong chet)
        for (Entity entity : event.getChunk().getEntities()) {
            BossDamageTracker.getInstance().clearBossData(entity.getUniqueId());
        }
    }
}
