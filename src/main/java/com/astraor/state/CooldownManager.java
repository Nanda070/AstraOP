package com.astraor.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Централизованный менеджер кулдаунов способностей.
 * Потокобезопасен, хранит кулдауны по имени способности.
 */
public class CooldownManager {

    // ability -> (UUID -> timestamp)
    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * Возвращает true, если кулдаун ещё не прошёл.
     */
    public boolean isOnCooldown(UUID uuid, String ability, long cooldownMs) {
        long last = cooldowns
                .getOrDefault(ability, Map.of())
                .getOrDefault(uuid, 0L);
        return System.currentTimeMillis() - last < cooldownMs;
    }

    /**
     * Возвращает оставшееся время в мс, или 0 если кулдаун прошёл.
     */
    public long getRemaining(UUID uuid, String ability, long cooldownMs) {
        long last = cooldowns
                .getOrDefault(ability, Map.of())
                .getOrDefault(uuid, 0L);
        long remaining = cooldownMs - (System.currentTimeMillis() - last);
        return Math.max(0L, remaining);
    }

    /**
     * Проставляет кулдаун «прямо сейчас».
     */
    public void setCooldown(UUID uuid, String ability) {
        cooldowns
                .computeIfAbsent(ability, k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis());
    }

    /**
     * Сбрасывает все кулдауны игрока (например, при выходе с сервера).
     */
    public void clearPlayer(UUID uuid) {
        cooldowns.values().forEach(map -> map.remove(uuid));
    }
}
