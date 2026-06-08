package com.villagerbargains.resource;

import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * A read-only PackResources backed entirely by an in-memory byte map.
 * Avoids disk I/O and keeps startup fast.
 *
 * Keys in the [files] map are in the form: "data/<namespace>/<path>"
 * e.g. "data/minecraft/villager_trade/librarian/level_1/enchanted_book.json"
 *
 * NOTE for MC version updates: if AbstractPackResources gains/loses
 * abstract methods, only this file needs updating.
 */
public final class InMemoryPack extends AbstractPackResources {

    private final Map<String, byte[]> files;

    public InMemoryPack(PackLocationInfo locationInfo, Map<String, byte[]> files) {
        super(locationInfo);
        this.files = Map.copyOf(files);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, net.minecraft.resources.ResourceLocation location) {
        if (type != PackType.SERVER_DATA) return null;
        String key  = "data/" + location.getNamespace() + "/" + location.getPath();
        byte[] data = files.get(key);
        if (data == null) return null;
        byte[] copy = data.clone();
        return () -> new ByteArrayInputStream(copy);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix,
                              ResourceOutput output) {
        if (type != PackType.SERVER_DATA) return;
        String keyPrefix = "data/" + namespace + "/" + prefix;
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(keyPrefix)) continue;
            String path = key.substring(("data/" + namespace + "/").length());
            net.minecraft.resources.ResourceLocation loc =
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, path);
            byte[] copy = entry.getValue().clone();
            output.accept(loc, () -> new ByteArrayInputStream(copy));
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != PackType.SERVER_DATA) return Set.of();
        Set<String> ns = new LinkedHashSet<>();
        for (String key : files.keySet()) {
            String[] parts = key.split("/", 3);
            if (parts.length >= 2) ns.add(parts[1]);
        }
        return Collections.unmodifiableSet(ns);
    }

    @Override
    public void close() {}
}
