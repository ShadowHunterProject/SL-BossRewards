package com.sololeveling.bossrewards.listeners;

import com.sololeveling.bossrewards.SLBossRewards;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;

public class BossDeathListener implements Listener {

    private final SLBossRewards plugin;

    public BossDeathListener(SLBossRewards plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBossDeath(MythicMobDeathEvent event) {
        String internalName = event.getMobType().getInternalName();
        ConfigurationSection bossConfig = plugin.getConfig().getConfigurationSection("bosses." + internalName);
        
        if (bossConfig == null) {
            return;
        }

        // Tap hop nhung nguoi choi da nhan qua de khong bi trung lap
        Set<UUID> rewardedPlayers = new HashSet<>();
        
        double maxHealth = 1.0;
        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.attribute.AttributeInstance attr = ((org.bukkit.entity.LivingEntity) event.getEntity()).getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) {
                maxHealth = attr.getValue();
            }
        }
        
        // 1. Xu ly phan thuong cho nguoi ket lieu (Killer)
        Player killer = null;
        if (event.getKiller() instanceof Player) {
            killer = (Player) event.getKiller();
        }

        if (killer != null && bossConfig.contains("killer.commands")) {
            boolean passDamageCheck = true;
            double playerDamage = com.sololeveling.bossrewards.managers.BossDamageTracker.getInstance().getDamage(event.getEntity().getUniqueId(), killer.getUniqueId());
            
            if (bossConfig.contains("killer.min-damage")) {
                double minDamage = bossConfig.getDouble("killer.min-damage");
                if (playerDamage < minDamage) passDamageCheck = false;
            }
            
            if (bossConfig.contains("killer.min-damage-percent")) {
                double minDamagePercent = bossConfig.getDouble("killer.min-damage-percent");
                double requiredDamage = (minDamagePercent / 100.0) * maxHealth;
                if (playerDamage < requiredDamage) passDamageCheck = false;
            }
            
            if (passDamageCheck) {
                List<String> commands = bossConfig.getStringList("killer.commands");
                executeCommands(commands, killer, internalName);
                rewardedPlayers.add(killer.getUniqueId());
            }
        }

        // 2. Xu ly phan thuong ban kinh (Radius)
        if (bossConfig.contains("radius")) {
            org.bukkit.Location deathLoc = event.getEntity().getLocation();
            
            // Lay danh sach cac moc ban kinh
            List<Map<?, ?>> radiusList = bossConfig.getMapList("radius");
            
            // Loai bo cac config loi (thieu distance hoac commands)
            radiusList.removeIf(m -> m.get("distance") == null || m.get("commands") == null);

            // Tim ban kinh lon nhat de quet nguoi choi (Fix loi so 1)
            double maxSearchRadius = 50.0; // Mac dinh fallback
            for (Map<?, ?> radiusConfig : radiusList) {
                try {
                    Object distObj = radiusConfig.get("distance");
                    if (distObj != null) {
                        double dist = Double.parseDouble(distObj.toString());
                        if (dist > maxSearchRadius) {
                            maxSearchRadius = dist;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
            
            // Danh sach tat ca nguoi choi xung quanh boss dua tren maxSearchRadius
            Collection<Player> nearbyPlayers = new ArrayList<>();
            for (Entity entity : event.getEntity().getNearbyEntities(maxSearchRadius, maxSearchRadius, maxSearchRadius)) {
                if (entity instanceof Player) {
                    nearbyPlayers.add((Player) entity);
                }
            }
            
            // Sap xep mốc khoảng cách từ thấp đến cao (vi du: mốc 8 block trước, 16 block sau)
            radiusList.sort((m1, m2) -> {
                try {
                    double d1 = Double.parseDouble(m1.get("distance").toString());
                    double d2 = Double.parseDouble(m2.get("distance").toString());
                    return Double.compare(d1, d2);
                } catch (Exception e) {
                    return 0; // Fix loi NumberFormatException khi sort (Loi so 3)
                }
            });

            for (Map<?, ?> radiusConfig : radiusList) {
                double maxDistance;
                try {
                    Object distObj = radiusConfig.get("distance");
                    if (distObj == null) continue;
                    maxDistance = Double.parseDouble(distObj.toString());
                } catch (NumberFormatException e) {
                    continue; // Bo qua neu distance sai kieu (Fix loi so 3)
                }
                
                double maxDistanceSquared = maxDistance * maxDistance;
                
                int maxPlayers = -1; // -1 la khong gioi han
                Object maxPlayersObj = radiusConfig.get("max-players");
                if (maxPlayersObj != null) {
                    try {
                        maxPlayers = Integer.parseInt(maxPlayersObj.toString());
                    } catch (NumberFormatException ignored) {}
                }
                
                // Fix loi ClassCastException (Loi so 2)
                List<String> commands = new ArrayList<>();
                Object cmdsObj = radiusConfig.get("commands");
                if (cmdsObj instanceof List) {
                    for (Object cmdObj : (List<?>) cmdsObj) {
                        commands.add(cmdObj.toString());
                    }
                } else if (cmdsObj instanceof String) {
                    commands.add(cmdsObj.toString());
                }

                // Tim cac nguoi choi hop le trong moc ban kinh nay (chua nhan qua + nam trong ban kinh)
                List<Player> validPlayersInRadius = new ArrayList<>();
                for (Player p : nearbyPlayers) {
                    if (!rewardedPlayers.contains(p.getUniqueId())) {
                        if (p.getLocation().getWorld().equals(deathLoc.getWorld()) && p.getLocation().distanceSquared(deathLoc) <= maxDistanceSquared) {
                            
                            // Check Damage
                            double playerDamage = com.sololeveling.bossrewards.managers.BossDamageTracker.getInstance().getDamage(event.getEntity().getUniqueId(), p.getUniqueId());
                            boolean passDamageCheck = true;
                            
                            if (radiusConfig.get("min-damage") != null) {
                                try {
                                    double minDamage = Double.parseDouble(radiusConfig.get("min-damage").toString());
                                    if (playerDamage < minDamage) passDamageCheck = false;
                                } catch (NumberFormatException ignored) {}
                            }
                            
                            if (radiusConfig.get("min-damage-percent") != null) {
                                try {
                                    double minDamagePercent = Double.parseDouble(radiusConfig.get("min-damage-percent").toString());
                                    double requiredDamage = (minDamagePercent / 100.0) * maxHealth;
                                    if (playerDamage < requiredDamage) passDamageCheck = false;
                                } catch (NumberFormatException ignored) {}
                            }
                            
                            if (passDamageCheck) {
                                validPlayersInRadius.add(p);
                            }
                        }
                    }
                }

                // Sap xep uu tien nguoi o gan nhat bang distanceSquared (Hieu nang cao)
                validPlayersInRadius.sort(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(deathLoc)));

                // Phat qua cho tung nguoi den khi dat gioi han
                int count = 0;
                for (Player p : validPlayersInRadius) {
                    if (maxPlayers != -1 && count >= maxPlayers) {
                        // Vượt quá giới hạn người nhận, bỏ qua người này ở mốc hiện tại
                        // Họ vẫn có thể nhận quà ở mốc xa hơn (vì chưa được thêm vào rewardedPlayers)
                        continue;
                    }
                    
                    // Đưa người chơi vào danh sách đã nhận thưởng
                    rewardedPlayers.add(p.getUniqueId());
                    executeCommands(commands, p, internalName);
                    count++;
                }
            }
        }
    }

    private void executeCommands(List<String> commands, Player player, String bossName) {
        if (commands == null || commands.isEmpty()) return;
        for (String cmd : commands) {
            String finalCmd = cmd.replace("%player%", player.getName()).replace("%boss%", bossName);
            // Dung lenh chay dong bo tren main thread (vi su kien nay la sync)
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }
        
        // Gui tin nhan chuc mung cho player tu messages.yml
        String prefix = plugin.getMessagesConfig().getString("prefix", "&8[&eBossRewards&8] &r");
        String msg = plugin.getMessagesConfig().getString("reward-received", "");
        if (msg != null && !msg.isEmpty()) {
            msg = msg.replace("%player%", player.getName()).replace("%boss%", bossName);
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + msg));
        }
    }
}
