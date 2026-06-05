package com.astraor.state;

import com.astraor.AstraORPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class KeybindManager {

    private final NamespacedKey keybindKey;

    public enum AbilityKeybind {
        SHIFT_RIGHT_CLICK("Shift + ПКМ"),
        SHIFT_LEFT_CLICK("Shift + ЛКМ"),
        SWAP_HANDS("Клавиша F (Смена предмета)"),
        DROP_ITEM("Клавиша Q (Выбросить предмет)");

        private final String displayName;

        AbilityKeybind(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public KeybindManager(AstraORPlugin plugin) {
        this.keybindKey = new NamespacedKey(plugin, "ability_keybind");
    }

    public AbilityKeybind getPlayerKeybind(Player player) {
        String bind = player.getPersistentDataContainer().get(keybindKey, PersistentDataType.STRING);
        if (bind != null) {
            try {
                return AbilityKeybind.valueOf(bind);
            } catch (IllegalArgumentException ignored) {}
        }
        return AbilityKeybind.SHIFT_RIGHT_CLICK; // Default
    }

    public void setPlayerKeybind(Player player, AbilityKeybind keybind) {
        player.getPersistentDataContainer().set(keybindKey, PersistentDataType.STRING, keybind.name());
    }
}
