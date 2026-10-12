package com.villagerbargains.price;

import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Reads the trading entity from a loot context, whose accessor was renamed between 26.2 and 26.3. */
public final class TradeEntities {
    private static final MethodHandle GETTER = findGetter();

    private TradeEntities() {}

    private static MethodHandle findGetter() {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType type = MethodType.methodType(Object.class, ContextKey.class);
        for (String name : new String[] {"getOptional", "getOptionalParameter"}) {
            try {
                return lookup.findVirtual(LootContext.class, name, type);
            } catch (ReflectiveOperationException ignored) {
                // try the other name
            }
        }
        throw new IllegalStateException("LootContext has neither getOptional nor getOptionalParameter");
    }

    public static Entity of(LootContext context) {
        try {
            return (Entity) GETTER.invoke(context, LootContextParams.THIS_ENTITY);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
