package com.villagerbargains.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Lets one jar run on several Minecraft versions.
 *
 * <p>A few vanilla classes exist in only some versions (see {@code mixin/provider}). A mixin is
 * applied only when the class it targets exists in the running game; otherwise it is skipped.
 * Every applied mixin still has {@code defaultRequire = 1}, so a hook that no longer matches
 * vanilla code fails loudly instead of silently doing nothing.
 */
public final class VillagerBargainsMixinPlugin implements IMixinConfigPlugin {
    // Own logger: this runs before the mod is initialised, so it avoids loading other mod classes early.
    private static final Logger LOGGER = LoggerFactory.getLogger("villagerbargains");

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean targetExists = classExists(targetClassName);
        String mixin = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        if (targetExists) {
            LOGGER.info("Applying {} to {}", mixin, targetClassName);
        } else {
            LOGGER.info("Skipping {}: {} does not exist in this Minecraft version", mixin, targetClassName);
        }
        return targetExists;
    }

    /** Checks for the class file without loading the class (loading it here would break mixins). */
    private static boolean classExists(String className) {
        String resource = className.replace('.', '/') + ".class";
        return VillagerBargainsMixinPlugin.class.getClassLoader().getResource(resource) != null;
    }

    // Unused parts of IMixinConfigPlugin.

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
