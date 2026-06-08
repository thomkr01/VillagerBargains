package com.villagerbargains.resource;

import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A read-only in-memory resource pack backed by a Map<path, bytes>.
 * Used to inject trade override JSON files at runtime without writing to disk.
 *
 * To add new file types: put additional entries in the map passed to the constructor.
 */
public final class InMemoryResourcePack extends AbstractPackResources {
    private final Map<String, byte[]> files;

    public InMemoryResourcePack(PackLocationInfo info, Map<String, byte[]> files) {
        super(info);
        this.files = files;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... paths) {
        String path = String.join("/", paths);
        byte[] data = files.get(path);
        return data != null ? () -> new ByteArrayInputStream(data) : null;
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix,
                              ResourceOutput output) {
        // No-op: files are served via getRootResource, not enumerated
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        String pfx = type.getDirectory() + "/";
        return files.keySet().stream()
            .filter(p -> p.startsWith(pfx))
            .map(p -> p.substring(pfx.length()))
            .map(p -> p.contains("/") ? p.substring(0, p.indexOf('/')) : p)
            .collect(Collectors.toSet());
    }

    @Override
    public void close() {}
}
