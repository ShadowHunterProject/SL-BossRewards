package com.sololeveling.bossrewards.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossDamageTracker {

    private static BossDamageTracker instance;
    
    // Boss UUID -> (Player UUID -> Total Damage)
    private final Map<UUID, Map<UUID, Double>> damageMap;

    private BossDamageTracker() {
        this.damageMap = new ConcurrentHashMap<>();
    }

    public static BossDamageTracker getInstance() {
        if (instance == null) {
            instance = new BossDamageTracker();
        }
        return instance;
    }

    public void addDamage(UUID bossUuid, UUID playerUuid, double damage) {
        damageMap.computeIfAbsent(bossUuid, k -> new ConcurrentHashMap<>())
                 .merge(playerUuid, damage, Double::sum);
    }

    public double getDamage(UUID bossUuid, UUID playerUuid) {
        Map<UUID, Double> players = damageMap.get(bossUuid);
        if (players != null) {
            return players.getOrDefault(playerUuid, 0.0);
        }
        return 0.0;
    }

    public void clearBossData(UUID bossUuid) {
        damageMap.remove(bossUuid);
    }
}
