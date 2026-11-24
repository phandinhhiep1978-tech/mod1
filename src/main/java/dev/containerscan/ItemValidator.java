package dev.containerscan;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipePropertySet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.ItemTags;

import java.util.Set;

/** "Could this container accept this item?" - the container-type validity check. */
public final class ItemValidator {
    private static final Set<Item> BREWING_ITEMS = Set.of(
            // ingredients
            Items.NETHER_WART, Items.REDSTONE, Items.GLOWSTONE_DUST, Items.GUNPOWDER, Items.DRAGON_BREATH,
            Items.FERMENTED_SPIDER_EYE, Items.SPIDER_EYE, Items.SUGAR, Items.RABBIT_FOOT,
            Items.GLISTERING_MELON_SLICE, Items.GOLDEN_CARROT, Items.MAGMA_CREAM, Items.GHAST_TEAR,
            Items.TURTLE_HELMET, Items.PHANTOM_MEMBRANE, Items.PUFFERFISH, Items.COBWEB, Items.SLIME_BLOCK,
            Items.STONE, Items.BREEZE_ROD,
            // fuel
            Items.BLAZE_POWDER,
            // bottle slots
            Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.GLASS_BOTTLE);

    private ItemValidator() {}

    public static boolean accepts(ContainerKind kind, Item item, ClientWorld world) {
        ItemStack stack = new ItemStack(item);
        return switch (kind) {
            case HOPPER, CHEST, TRAPPED_CHEST, BARREL, DROPPER, DISPENSER, CRAFTER, DECORATED_POT -> true;
            case ENDER_CHEST -> true;
            case SHULKER -> !(item instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock)
                    && item.canBeNested();
            case CHISELED_BOOKSHELF -> stack.isIn(ItemTags.BOOKSHELF_BOOKS);
            case BREWING_STAND -> BREWING_ITEMS.contains(item);
            case FURNACE -> isFuel(world, stack) || smeltable(RecipePropertySet.FURNACE_INPUT, stack);
            case SMOKER -> isFuel(world, stack) || smeltable(RecipePropertySet.SMOKER_INPUT, stack);
            case BLAST_FURNACE -> isFuel(world, stack) || smeltable(RecipePropertySet.BLAST_FURNACE_INPUT, stack);
        };
    }

    private static boolean isFuel(ClientWorld world, ItemStack stack) {
        return world.getFuelRegistry().isFuel(stack);
    }

    /** The server syncs "valid smelting inputs" per furnace type to the client as recipe property sets. */
    private static boolean smeltable(RegistryKey<RecipePropertySet> key, ItemStack stack) {
        ClientPlayNetworkHandler h = MinecraftClient.getInstance().getNetworkHandler();
        return h != null && h.getRecipeManager().getPropertySet(key).canUse(stack);
    }
}
