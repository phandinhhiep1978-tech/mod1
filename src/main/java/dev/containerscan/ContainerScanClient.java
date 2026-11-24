package dev.containerscan;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ContainerScanClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeybindHandler.init();
        InventoryCache.init();
        HighlightRenderer.init();
        ClientTickEvents.END_CLIENT_TICK.register(ContainerScanner::tick);
    }
}
