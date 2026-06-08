package com.bargainvillage.resource;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

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
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.SERVER_DATA) return null;
        String key = "data/" + location.getNamespace() + "/" + location.getPath();
        byte[] data = files.get(key);
        if (data == null) return null;
        byte[] copy = data.clone();
        return () -> new ByteArrayInputStream(copy);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.SERVER_DATA) return;
        String keyPrefix = "data/" + namespace + "/" + prefix;
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(keyPrefix)) continue;
            String rest = key.substring("data/".length() + namespace.length() + 1);
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, rest);
            byte[] copy = entry.getValue().clone();
            output.accept(loc, () -> new ByteArrayInputStream(copy));
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != PackType.SERVER_DATA) return Set.of();
        Set<String> namespaces = new LinkedHashSet<>();
        for (String key : files.keySet()) {
            String[] parts = key.split("/", 3);
            if (parts.length >= 2) {
                namespaces.add(parts[1]);
            }
        }
        return Collections.unmodifiableSet(namespaces);
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        return null;
    }

    @Override
    public void close() {
    }
}
