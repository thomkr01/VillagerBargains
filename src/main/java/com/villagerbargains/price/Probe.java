package com.villagerbargains.price;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

final class Probe {
    static void probe(LootContext c) {
        var k = LootContextParams.THIS_ENTITY;
        Object a = c.getParams();
        Object b = c.getOptional(k);
        Object d = c.get(k);
        Object e = c.getOrThrow(k);
        Object f = c.getParams().getOptionalParameter(k);
        Object g = c.getParams().getParameter(k);
        Object h = c.getParams().getOptional(k);
        Object i = c.getParams().getOrThrow(k);
        Object j = c.getParams().get(k);
    }
}
