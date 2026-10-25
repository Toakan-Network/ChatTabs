package uk.toakan.tad.chattabs;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChatTabsClient implements ClientModInitializer {
    public static final String MOD_ID = "chat-tabs";
    public static final String NAME = "ChatTabs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Reads an InputConstants code at runtime. The codes differ between Minecraft versions (26.3 moved
     * from GLFW to SDL), and javac would inline a direct reference, so one source tree works on both this way.
     */
    public static int inputCode(String name) {
        try {
            return InputConstants.class.getField(name).getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Missing InputConstants." + name, e);
        }
    }

    @Override
    public void onInitializeClient() {
        ChatTabs.load();
        TabBar.register();
        // Each server starts with no DM tabs; the filter also turns on or off with the server.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ChatTabs.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ChatTabs::reset));
    }
}
