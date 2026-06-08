package com.villagerbargains.resource;

import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
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
    public @Nullable IoSupplier<InputStream> getResource(PackType type, net.minecraft.resources.ResourceLocation loc) {
        String path = type.getDirectory() + "/" + loc.getNamespace() + "/" + loc.getPath();
        byte[] data = files.get(path);
        return data != null ? () -> new ByteArrayInputStream(data) : null;
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix,
                              ResourceOutput output) {
        String base = type.getDirectory() + "/" + namespace + "/";
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String path = entry.getKey();
            if (path.startsWith(base)) {
                String relative = path.substring(base.length());
                if (relative.startsWith(prefix)) {
                    output.accept(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, relative),
                        () -> new ByteArrayInputStream(entry.getValue())
                    );
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        String prefix = type.getDirectory() + "/";
        return files.keySet().stream()
            .filter(p -> p.startsWith(prefix))
            .map(p -> p.substring(prefix.length()))
            .map(p -> p.contains("/") ? p.substring(0, p.indexOf('/')) : p)
            .collect(Collectors.toSet());
    }

    @Override
    public @Nullable <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        return null;
    }

    @Override
    public void close() {}
}
