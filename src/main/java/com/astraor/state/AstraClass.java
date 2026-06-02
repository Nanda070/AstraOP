package com.astraor.state;

public enum AstraClass {
    ARCHITECT,
    VANGUARD,
    JUGGERNAUT,
    SHADOW,
    RANGER,
    BERSERKER,
    WINDWALKER;

    public String getId() {
        return this.name().toLowerCase();
    }

    public static AstraClass fromId(String id) {
        if (id == null) return null;
        for (AstraClass c : values()) {
            if (c.getId().equalsIgnoreCase(id)) {
                return c;
            }
        }
        return null;
    }
}
