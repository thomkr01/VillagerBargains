package com.villagerbargains.mixin;

import com.villagerbargains.trade.PriceResolver;
import com.villagerbargains.util.ModLogger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts villager trade generation at TAIL of updateTrades.
 * Replaces each MerchantOffer with a fresh one whose costA count
 * is our configured price, locked at construction time.
 *
 * MC 26.1.x (official Mojang mappings): MerchantOffer fields are raw
 * ItemStack — no ItemCost wrapper.
 * Constructor: MerchantOffer(ItemStack costA, ItemStack costB, ItemStack result,
 *                            int uses, int maxUses, int xp, float priceMultiplier)
 */
@Mixin(Villager.class)
public abstract class VillagerTradesMixin extends AbstractVillager {

    public VillagerTradesMixin(EntityType<? extends AbstractVillager> type,
                               net.minecraft.world.level.Level level) {
        super(type, level);
    }

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void villagerbargains$repriceOffers(ServerLevel serverLevel, CallbackInfo ci) {
        MerchantOffers offers = this.getOffers();
        if (offers == null || offers.isEmpty()) {
            ModLogger.get().info("[VillagerBargains UpdateTrades] No offers to reprice.");
            return;
        }
        ModLogger.get().info("[VillagerBargains UpdateTrades] Repricing {} offer(s).", offers.size());
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer original = offers.get(i);
            MerchantOffer replaced = buildRepriced(original);
            if (replaced != null) {
                offers.set(i, replaced);
            }
        }
    }

    /**
     * Returns a new MerchantOffer with our configured price as costA count.
     * Returns null if the trade is not in our registry (leave unchanged).
     */
    private static MerchantOffer buildRepriced(MerchantOffer original) {
        int price = resolvePrice(original);
        if (price < 0) {
            ModLogger.get().info("[VillagerBargains Lookup] '{}' not in registry, leaving unchanged.",
                    describeResult(original.getResult()));
            return null;
        }
        price = Math.max(1, Math.min(64, price));
        int oldPrice = original.getBaseCostA().getCount();

        // Build replacement costA with our price; keep the item the same.
        ItemStack newCostA = original.getBaseCostA().copyWithCount(price);

        // costB: use EMPTY if not present (vanilla uses empty ItemStack for no second cost).
        ItemStack costB = original.getCostB().isEmpty() ? ItemStack.EMPTY : original.getCostB().copy();

        MerchantOffer fresh = new MerchantOffer(
                newCostA,
                costB,
                original.getResult().copy(),
                original.getUses(),
                original.getMaxUses(),
                original.getXp(),
                original.getPriceMultiplier()
        );
        // Prevent reputation/demand from shifting the price after construction.
        fresh.setSpecialPriceDiff(0);

        ModLogger.get().info("[VillagerBargains Repriced] '{}' : {} -> {} emeralds",
                describeResult(original.getResult()), oldPrice, price);
        return fresh;
    }

    /** Resolves configured price for an offer. Returns -1 if not in registry. */
    private static int resolvePrice(MerchantOffer offer) {
        ItemStack result = offer.getResult();

        // Enchanted book path
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var    entry = enchantments.entrySet().iterator().next();
                String id    = entry.getKey().getRegisteredName();
                int    lvl   = entry.getValue();
                int    price = PriceResolver.resolveBook("enchanted_book:" + id, lvl);
                if (price < 0)
                    ModLogger.get().info("[VillagerBargains Lookup] Book '{}' lvl {} not in registry.", id, lvl);
                return price;
            }
            ModLogger.get().info("[VillagerBargains Lookup] Enchanted book with no stored enchantments.");
            return -1;
        }

        // Normal trade — try result item id
        String resultId = itemId(result);
        int price = PriceResolver.resolve(resultId);
        if (price >= 0) return price;

        // Fallback: try costA item id
        String costId = itemId(offer.getBaseCostA());
        price = PriceResolver.resolve(costId);
        if (price < 0)
            ModLogger.get().info("[VillagerBargains Lookup] result='{}' costA='{}' not in registry.",
                    resultId, costId);
        return price;
    }

    private static String itemId(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        String raw = stack.getItem().toString();
        int i = raw.lastIndexOf(':');
        return i >= 0 ? raw.substring(i + 1) : raw;
    }

    private static String describeResult(ItemStack result) {
        if (!result.isEmpty() && result.getItem() == Items.ENCHANTED_BOOK) {
            ItemEnchantments enc = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enc != null && !enc.isEmpty()) {
                var e = enc.entrySet().iterator().next();
                return e.getKey().getRegisteredName() + " lvl " + e.getValue();
            }
            return "enchanted_book(unknown)";
        }
        return itemId(result);
    }
}
