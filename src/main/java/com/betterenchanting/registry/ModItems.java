package com.betterenchanting.registry;

import com.betterenchanting.BetterEnchanting;
import com.betterenchanting.world.item.EssenceItem;
import com.betterenchanting.world.item.AttunementFocusItem;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BetterEnchanting.MOD_ID);

    public static final Supplier<EssenceItem> FIRE_ESSENCE = essence("fire_essence", Rarity.COMMON);
    public static final Supplier<EssenceItem> FROST_ESSENCE = essence("frost_essence", Rarity.COMMON);
    public static final Supplier<EssenceItem> LIGHTNING_ESSENCE = essence("lightning_essence", Rarity.COMMON);
    public static final Supplier<EssenceItem> PHYSICAL_ESSENCE = essence("physical_essence", Rarity.COMMON);
    public static final Supplier<EssenceItem> MINING_ESSENCE = essence("mining_essence", Rarity.COMMON);
    public static final Supplier<EssenceItem> DEFENSIVE_ESSENCE = essence("defensive_essence", Rarity.COMMON);
    public static final Supplier<EssenceItem> VITALITY_ESSENCE = essence("vitality_essence", Rarity.COMMON);
    public static final Supplier<EssenceItem> MOBILITY_ESSENCE = essence("mobility_essence", Rarity.COMMON);
    public static final Supplier<EssenceItem> VOID_ESSENCE = essence("void_essence", Rarity.UNCOMMON);
    public static final Supplier<EssenceItem> PURIFICATION_ESSENCE = essence("purification_essence", Rarity.UNCOMMON);
    public static final DeferredItem<AttunementFocusItem> ATTUNEMENT_FOCUS = ITEMS.register(
            "attunement_focus",
            () -> new AttunementFocusItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
    );
    public static final DeferredItem<BlockItem> ARCANE_CRUCIBLE = ITEMS.registerSimpleBlockItem(
            "arcane_crucible",
            ModBlocks.ARCANE_CRUCIBLE
    );
    public static final DeferredItem<BlockItem> ATTUNEMENT_PEDESTAL = ITEMS.registerSimpleBlockItem(
            "attunement_pedestal",
            ModBlocks.ATTUNEMENT_PEDESTAL
    );

    public static final List<Supplier<? extends Item>> ESSENCES = List.of(
            FIRE_ESSENCE,
            FROST_ESSENCE,
            LIGHTNING_ESSENCE,
            PHYSICAL_ESSENCE,
            MINING_ESSENCE,
            DEFENSIVE_ESSENCE,
            VITALITY_ESSENCE,
            MOBILITY_ESSENCE,
            VOID_ESSENCE,
            PURIFICATION_ESSENCE
    );

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private static Supplier<EssenceItem> essence(String id, Rarity rarity) {
        return ITEMS.register(id, () -> new EssenceItem(new Item.Properties().stacksTo(64).rarity(rarity)));
    }
}
