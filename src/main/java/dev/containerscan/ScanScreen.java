package dev.containerscan;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScanScreen extends Screen {
    private static final int MAX_RESULTS = 6;

    private TextFieldWidget search;
    private final List<ButtonWidget> resultButtons = new ArrayList<>();
    private final List<Item> resultItems = new ArrayList<>();
    private final List<ButtonWidget> kindButtons = new ArrayList<>();
    private int rightX, listY;
    private boolean backslashWasDown = true; // key is still held from the keybind that opened us

    public ScanScreen() { super(Text.literal("Container Scan")); }

    @Override
    protected void init() {
        kindButtons.clear();
        resultButtons.clear();

        int leftX = width / 2 - 245;
        rightX = width / 2 + 5;
        int top = 30;

        // ---- container toggles (2 columns) ----
        ContainerKind[] kinds = ContainerKind.values();
        for (int i = 0; i < kinds.length; i++) {
            ContainerKind k = kinds[i];
            int x = leftX + (i % 2) * 120, y = top + (i / 2) * 22;
            ButtonWidget b = ButtonWidget.builder(kindLabel(k), btn -> {
                if (!ScanConfig.kinds.remove(k)) ScanConfig.kinds.add(k);
                btn.setMessage(kindLabel(k));
            }).dimensions(x, y, 115, 20).build();
            kindButtons.add(b);
            addDrawableChild(b);
        }
        int rows = (kinds.length + 1) / 2;
        int allY = top + rows * 22 + 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("Select All"), b -> {
            ScanConfig.kinds.addAll(List.of(kinds));
            refreshKindButtons();
        }).dimensions(leftX, allY, 115, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Deselect All"), b -> {
            ScanConfig.kinds.clear();
            refreshKindButtons();
        }).dimensions(leftX + 120, allY, 115, 20).build());

        // ---- master toggle ----
        addDrawableChild(ButtonWidget.builder(masterLabel(), b -> {
            ScanConfig.enabled = !ScanConfig.enabled;
            b.setMessage(masterLabel());
        }).dimensions(rightX, top, 240, 20).build());

        // ---- radius slider ----
        addDrawableChild(new RadiusSlider(rightX, top + 24, 240, 20));

        // ---- item search ----
        TextRenderer tr = textRenderer;
        search = new TextFieldWidget(tr, rightX + 22, top + 54, 218, 20, Text.literal("Search item"));
        search.setPlaceholder(Text.literal("Search item (e.g. diamond, coal)..."));
        search.setMaxLength(64);
        search.setChangedListener(s -> rebuildResults());
        addDrawableChild(search);

        listY = top + 78;
        rebuildResults();
    }

    private Text kindLabel(ContainerKind k) {
        boolean on = ScanConfig.kinds.contains(k);
        return Text.literal((on ? "\u00a7a[x] " : "\u00a7c[ ] ") + "\u00a7f" + k.label);
    }

    private Text masterLabel() {
        return Text.literal("Highlighting: " + (ScanConfig.enabled ? "\u00a7aON" : "\u00a7cOFF"));
    }

    private void refreshKindButtons() {
        ContainerKind[] kinds = ContainerKind.values();
        for (int i = 0; i < kinds.length; i++) kindButtons.get(i).setMessage(kindLabel(kinds[i]));
    }

    private void rebuildResults() {
        for (ButtonWidget b : resultButtons) remove(b);
        resultButtons.clear();
        resultItems.clear();

        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            for (Item item : Registries.ITEM) {
                if (item == Items.AIR) continue;
                String name = item.getName().getString().toLowerCase(Locale.ROOT);
                String id = Registries.ITEM.getId(item).getPath();
                if (name.contains(q) || id.contains(q)) {
                    resultItems.add(item);
                    if (resultItems.size() >= MAX_RESULTS) break;
                }
            }
        }
        for (int i = 0; i < resultItems.size(); i++) {
            Item item = resultItems.get(i);
            ButtonWidget b = ButtonWidget.builder(item.getName(), btn -> ScanConfig.target = item)
                    .dimensions(rightX + 22, listY + i * 20, 218, 20).build();
            resultButtons.add(b);
            addDrawableChild(b);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);

        // icon preview for the selected target, next to the search box
        int top = 30;
        if (ScanConfig.target != null) ctx.drawItem(new ItemStack(ScanConfig.target), rightX + 2, top + 56);

        // icons for each search result
        for (int i = 0; i < resultItems.size(); i++) {
            ctx.drawItem(new ItemStack(resultItems.get(i)), rightX + 2, listY + i * 20 + 2);
        }

        String label = ScanConfig.target == null ? "Target: (none)"
                : "Target: " + ScanConfig.target.getName().getString();
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), rightX, listY + MAX_RESULTS * 20 + 6, 0xFFFFFF55);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Green = has / accepts item, Red = doesn't"),
                rightX, listY + MAX_RESULTS * 20 + 20, 0xFFAAAAAA);
    }

    @Override
    public void tick() {
        // Press the same key again to close (skipped while typing in the search box).
        long handle = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_BACKSLASH) == GLFW.GLFW_PRESS;
        if (down && !backslashWasDown && !search.isFocused()) close();
        backslashWasDown = down;
    }

    @Override
    public boolean shouldPause() { return false; }

    private static final class RadiusSlider extends SliderWidget {
        RadiusSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Text.empty(), (ScanConfig.radius - 5) / 59.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Scan radius: " + (5 + (int) Math.round(value * 59)) + " blocks"));
        }

        @Override
        protected void applyValue() {
            ScanConfig.radius = 5 + (int) Math.round(value * 59);
        }
    }
}
