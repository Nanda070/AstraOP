package com.astraor.state;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum AstraClass {
    ARCHITECT,
    VANGUARD,
    JUGGERNAUT,
    SHADOW,
    RANGER,
    BERSERKER,
    WINDWALKER,
    PHANTOM,
    PHOENIX,
    DRUID,
    VAMPIRE,
    GOLIATH;

    // O(1) lookup — строим Map один раз при загрузке класса
    private static final Map<String, AstraClass> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(AstraClass::getId, Function.identity()));

    public String getId() {
        return this.name().toLowerCase();
    }

    public static AstraClass fromId(String id) {
        if (id == null) return null;
        return BY_ID.get(id.toLowerCase());
    }
}
