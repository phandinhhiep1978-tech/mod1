package dev.containerscan;

import net.minecraft.block.entity.BlockEntityType;

import java.util.HashMap;
import java.util.Map;

public enum ContainerKind {
    HOPPER("Hopper", BlockEntityType.HOPPER),
    FURNACE("Furnace", BlockEntityType.FURNACE),
    SMOKER("Smoker", BlockEntityType.SMOKER),
    BLAST_FURNACE("Blast Furnace", BlockEntityType.BLAST_FURNACE),
    CHEST("Chest", BlockEntityType.CHEST),
    TRAPPED_CHEST("Trapped Chest", BlockEntityType.TRAPPED_CHEST),
    BARREL("Barrel", BlockEntityType.BARREL),
    ENDER_CHEST("Ender Chest", BlockEntityType.ENDER_CHEST),
    SHULKER("Shulker Boxes (all)", BlockEntityType.SHULKER_BOX), // one BE type covers every dye colour
    DROPPER("Dropper", BlockEntityType.DROPPER),
    DISPENSER("Dispenser", BlockEntityType.DISPENSER),
    CRAFTER("Crafter", BlockEntityType.CRAFTER),
    BREWING_STAND("Brewing Stand", BlockEntityType.BREWING_STAND),
    CHISELED_BOOKSHELF("Chiseled Bookshelf", BlockEntityType.CHISELED_BOOKSHELF),
    DECORATED_POT("Decorated Pot", BlockEntityType.DECORATED_POT);

    public final String label;
    public final BlockEntityType<?> type;

    ContainerKind(String label, BlockEntityType<?> type) {
        this.label = label;
        this.type = type;
    }

    private static final Map<BlockEntityType<?>, ContainerKind> LOOKUP = new HashMap<>();
    static { for (ContainerKind k : values()) LOOKUP.put(k.type, k); }

    public static ContainerKind of(BlockEntityType<?> t) { return LOOKUP.get(t); }
}
