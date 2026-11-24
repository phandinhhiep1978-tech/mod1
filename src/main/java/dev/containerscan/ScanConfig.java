package dev.containerscan;

import net.minecraft.item.Item;
import java.util.EnumSet;

/** Shared, in-memory settings edited by the GUI. */
public final class ScanConfig {
    public static boolean enabled = true;
    public static int radius = 32;               // 5..64
    public static Item target = null;
    public static final EnumSet<ContainerKind> kinds = EnumSet.allOf(ContainerKind.class);
    private ScanConfig() {}
}
