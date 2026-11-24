package dev.containerscan;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Remembers what the player has seen inside containers. 100% client-side:
 * we remember the last block right-clicked, then mirror the open screen's
 * container slots (which the server syncs to us) into a map.
 */
public final class InventoryCache {
    private record Key(Identifier dim, long pos) {}

    private static final Map<Key, Set<Item>> CACHE = new HashMap<>();
    private static Set<Item> enderContents = null; // ender chests share one inventory
    private static BlockPos lastPos;
    private static Identifier lastDim;

    private InventoryCache() {}

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient()) {
                lastPos = hit.getBlockPos();
                lastDim = world.getRegistryKey().getValue();
            }
            return ActionResult.PASS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof HandledScreen<?> hs) {
                // Re-capture every tick so late-arriving slot syncs are included.
                ScreenEvents.afterTick(screen).register(s -> capture(hs));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static void clear() {
        CACHE.clear();
        enderContents = null;
        lastPos = null;
    }

    private static void capture(HandledScreen<?> screen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || lastPos == null) return;

        ScreenHandler handler = screen.getScreenHandler();
        if (handler instanceof PlayerScreenHandler) return;

        BlockEntity be = mc.world.getBlockEntity(lastPos);
        if (be == null) return;
        ContainerKind kind = ContainerKind.of(be.getType());
        if (kind == null) return;

        Set<Item> items = new HashSet<>();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory) continue;
            ItemStack st = slot.getStack();
            if (!st.isEmpty()) items.add(st.getItem());
        }

        if (kind == ContainerKind.ENDER_CHEST) {
            enderContents = items;
            return;
        }

        CACHE.put(new Key(lastDim, lastPos.asLong()), items);

        // Double chests: both halves share the same contents.
        BlockState state = mc.world.getBlockState(lastPos);
        if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
            BlockPos other = lastPos.offset(ChestBlock.getFacing(state));
            CACHE.put(new Key(lastDim, other.asLong()), items);
        }
    }

    /** Known contents of a container, or null if the player has never opened it. */
    public static Set<Item> contentsOf(Identifier dim, BlockPos pos, BlockEntity be, ContainerKind kind) {
        if (kind == ContainerKind.ENDER_CHEST) return enderContents;

        // Chiseled bookshelves and decorated pots sync their contents to the client without a GUI.
        if ((kind == ContainerKind.CHISELED_BOOKSHELF || kind == ContainerKind.DECORATED_POT)
                && be instanceof Inventory inv) {
            Set<Item> items = new HashSet<>();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (!st.isEmpty()) items.add(st.getItem());
            }
            return items;
        }
        return CACHE.get(new Key(dim, pos.asLong()));
    }
}
