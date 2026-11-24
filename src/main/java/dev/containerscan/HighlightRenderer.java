package dev.containerscan;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Draws translucent boxes + outlines around hits with the depth test disabled (see-through). */
public final class HighlightRenderer {
    // 1.21.5+ replaced RenderSystem.disableDepthTest() with a pipeline that declares its depth state.
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("containerscan", "pipeline/xray_box"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build());

    private static final RenderLayer LAYER =
            RenderLayer.of("containerscan_xray", RenderSetup.builder(PIPELINE).translucent().build());

    private static final double E = 0.003;   // grow box slightly to avoid z-fighting
    private static final double T = 0.025;   // outline thickness

    private HighlightRenderer() {}

    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(HighlightRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        var hits = ContainerScanner.hits();
        if (!ScanConfig.enabled || hits.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();
        MatrixStack matrices = ctx.matrices();
        Matrix4f m = matrices.peek().getPositionMatrix();

        try (BufferAllocator alloc = new BufferAllocator(1 << 16)) {
            BufferBuilder bb = new BufferBuilder(alloc, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (ContainerScanner.Hit h : hits) {
                int r = h.green() ? 40 : 255, g = h.green() ? 255 : 40, b = h.green() ? 90 : 40;
                double x0 = h.pos().getX() - cam.x - E, y0 = h.pos().getY() - cam.y - E, z0 = h.pos().getZ() - cam.z - E;
                double x1 = x0 + 1 + 2 * E, y1 = y0 + 1 + 2 * E, z1 = z0 + 1 + 2 * E;

                box(bb, m, x0, y0, z0, x1, y1, z1, r, g, b, 70);           // translucent fill
                // 12 edges as thin boxes
                for (int i = 0; i < 4; i++) {
                    double ex = (i & 1) == 0 ? x0 : x1 - T, ez = (i & 2) == 0 ? z0 : z1 - T;
                    box(bb, m, ex, y0, ez, ex + T, y1, ez + T, r, g, b, 230);            // vertical
                    double ey = (i & 1) == 0 ? y0 : y1 - T, ez2 = (i & 2) == 0 ? z0 : z1 - T;
                    box(bb, m, x0, ey, ez2, x1, ey + T, ez2 + T, r, g, b, 230);          // along X
                    double ex2 = (i & 1) == 0 ? x0 : x1 - T, ey2 = (i & 2) == 0 ? y0 : y1 - T;
                    box(bb, m, ex2, ey2, z0, ex2 + T, ey2 + T, z1, r, g, b, 230);        // along Z
                }
            }

            BuiltBuffer built = bb.endNullable();
            if (built != null) LAYER.draw(built);
        }
    }

    private static void v(BufferBuilder bb, Matrix4f m, double x, double y, double z, int r, int g, int b, int a) {
        bb.vertex(m, (float) x, (float) y, (float) z).color(r, g, b, a);
    }

    private static void quad(BufferBuilder bb, Matrix4f m, int r, int g, int b, int a,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz) {
        v(bb, m, ax, ay, az, r, g, b, a); v(bb, m, bx, by, bz, r, g, b, a);
        v(bb, m, cx, cy, cz, r, g, b, a); v(bb, m, dx, dy, dz, r, g, b, a);
    }

    private static void box(BufferBuilder bb, Matrix4f m, double x0, double y0, double z0,
                            double x1, double y1, double z1, int r, int g, int b, int a) {
        quad(bb, m, r, g, b, a, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); // bottom
        quad(bb, m, r, g, b, a, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0); // top
        quad(bb, m, r, g, b, a, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0); // north
        quad(bb, m, r, g, b, a, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); // south
        quad(bb, m, r, g, b, a, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); // west
        quad(bb, m, r, g, b, a, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1); // east
    }
}
