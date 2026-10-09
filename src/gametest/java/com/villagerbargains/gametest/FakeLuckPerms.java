package com.villagerbargains.gametest;

import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stands in for LuckPerms: answers permission checks through the same fabric-permissions-api
 * event LuckPerms listens to. Only touched when that API is installed, so this class is never
 * loaded on servers without it.
 */
final class FakeLuckPerms {
    /** Nodes explicitly granted (true) or denied (false); anything else falls back to operator. */
    static final Map<String, Boolean> NODES = new ConcurrentHashMap<>();
    /** Every Villager Bargains node checked since the last {@link #reset()}. */
    static final Set<String> CHECKED = ConcurrentHashMap.newKeySet();
    private static boolean registered;

    private FakeLuckPerms() {}

    static boolean installed() {
        return FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
    }

    static void reset() {
        if (!registered) {
            registered = true;
            PermissionCheckEvent.EVENT.register((source, permission) -> {
                if (!permission.startsWith("villagerbargains.")) {
                    return TriState.DEFAULT;
                }
                CHECKED.add(permission);
                Boolean value = NODES.get(permission);
                return value == null ? TriState.DEFAULT : TriState.of(value);
            });
        }
        NODES.clear();
        CHECKED.clear();
    }
}
