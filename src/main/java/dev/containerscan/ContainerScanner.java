package dev.containerscan;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ContainerScanner {
    public record Hit(BlockPos pos, boolean green) {}

    private static volatile List<Hit> hits = List.of();
    private static int ticks = 0;

    private ContainerScanner() {}

    public static List<Hit> hits() { return hits; }

    public static void tick(MinecraftClient mc) {
        ClientWorld world = mc.world;
        Item target = ScanConfig.target;
        if (!ScanConfig.enabled || target == null || world == null || mc.player == null) {
            hits = List.of();
            return;
        }
        if (++ticks % 10 != 0) return;

        int r = ScanConfig.radius;
        double r2 = (double) r * r;
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        int cx0 = (int) Math.floor(px - r) >> 4, cx1 = (int) Math.floor(px + r) >> 4;
        int cz0 = (int) Math.floor(pz - r) >> 4, cz1 = (int) Math.floor(pz + r) >> 4;
        Identifier dim = world.getRegistryKey().getValue();

        List<Hit> out = new ArrayList<>();
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = e.getKey();
                    if (pos.getSquaredDistance(px, py, pz) > r2) continue;
                    BlockEntity be = e.getValue();
                    ContainerKind kind = ContainerKind.of(be.getType());
                    if (kind == null || !ScanConfig.kinds.contains(kind)) continue;

                    Set<Item> contents = InventoryCache.contentsOf(dim, pos, be, kind);
                    boolean green = (contents != null && contents.contains(target))
                            || ItemValidator.accepts(kind, target, world);
                    out.add(new Hit(pos.toImmutable(), green));
                }
            }
        }
        hits = out;
    }
}
