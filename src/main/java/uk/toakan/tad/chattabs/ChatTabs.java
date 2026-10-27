package uk.toakan.tad.chattabs;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The tabs and which one is open. Every message stays in vanilla's chat history; the open tab only decides which
 * of them are drawn (see {@code ChatComponentMixin}). Off Hypixel, or when disabled, everything passes through.
 * Render thread only.
 */
public final class ChatTabs {
    static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("chat-tabs").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "tabs.json");
    /** Vanilla's chat box length ({@code ChatScreen.init}), for chat and typed commands alike. */
    static final int CHAT_LIMIT = 256;

    private static Config config = new Config();
    /** The tabs from the settings, then the DM tabs in the order they opened. */
    private static final List<Tab> tabs = new ArrayList<>();
    private static int active;
    private static long version;
    /** Channel per chat message, by identity (GuiMessage is a record, so equals would compare whole texts). */
    private static Map<GuiMessage, MessageClassifier.Result> classified = new IdentityHashMap<>();
    /** The map before the rebuild in progress ({@link #refreshChat}), or null. */
    private static Map<GuiMessage, MessageClassifier.Result> previousClassified;
    private static final int MAX_CLASSIFIED = 4096;

    private ChatTabs() {}

    public static Config config() {
        return config;
    }

    public static List<Tab> tabs() {
        return Collections.unmodifiableList(tabs);
    }

    public static int activeIndex() {
        return active;
    }

    /** Changes whenever the tabs, the open tab or an unread count change, so the bar knows to remake its labels. */
    static long version() {
        return version;
    }

    static void load() {
        Config loaded = JsonFiles.read(CONFIG_FILE, Config.class);
        config = loaded == null ? new Config() : loaded;
        config.sanitize();
        rebuildTabs();
    }

    /** Saves the settings and applies them to the tab bar, keeping open DM tabs. */
    public static void saveConfig() {
        writeConfig();
        rebuildTabs();
        refreshChat();
    }

    /** Saves the settings file only, for changes that don't touch the tabs (the dragged bar position). */
    static void writeConfig() {
        config.sanitize();
        JsonFiles.write(CONFIG_FILE, config);
    }

    private static void rebuildTabs() {
        // The open tab stays open by what it is, not where it was: deleting or moving a tab before it in Settings
        // must not switch the player to another tab (and so another channel to send to).
        Tab open = active < tabs.size() ? tabs.get(active) : null;
        List<Tab> whispers = tabs.stream().filter(t -> t.whisperWith != null).toList();
        tabs.clear();
        for (Config.TabConfig t : config.tabs) tabs.add(Tab.of(t));
        if (config.whisperTabs) tabs.addAll(whispers.subList(Math.max(0, whispers.size() - config.maxWhisperTabs), whispers.size()));
        int found = open != null ? Tab.find(tabs, open) : -1;
        active = found >= 0 ? found : 0;
        version++;
    }

    /** True on Hypixel with the mod enabled: only then do tabs filter or redirect anything. */
    public static boolean isActive() {
        if (!config.enabled) return false;
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server != null && server.ip != null && isHypixel(server.ip);
    }

    /** hypixel.net or a subdomain of it, with or without a port; not just any address containing it. */
    static boolean isHypixel(String address) {
        String host = address.trim().toLowerCase(Locale.ROOT);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }

    /** Whether the open tab shows {@code message}. */
    public static boolean visible(GuiMessage message) {
        return !isActive() || tabs.get(active).shows(classify(message));
    }

    /**
     * A new message reached the chat history: opens a DM tab for a new conversation and counts it as unread in
     * the other tabs that show it. Returns whether the open tab shows it.
     */
    public static boolean onAdded(GuiMessage message) {
        if (!isActive()) return true;
        MessageClassifier.Result result = classify(message);
        if (result.channel() == Channel.DM && result.partner() != null) openWhisperTab(result.partner());
        for (int i = 0; i < tabs.size(); i++) {
            if (i != active && tabs.get(i).shows(result)) tabs.get(i).unread++;
        }
        version++;
        return tabs.get(active).shows(result);
    }

    private static void openWhisperTab(String player) {
        if (!config.whisperTabs || config.maxWhisperTabs == 0) return;
        int open = 0;
        for (Tab tab : tabs) {
            if (tab.whisperWith == null) continue;
            if (tab.whisperWith.equalsIgnoreCase(player)) return;
            open++;
        }
        if (open >= config.maxWhisperTabs) {
            boolean closed = false;
            for (int i = 0; i < tabs.size(); i++) {
                if (tabs.get(i).whisperWith != null && i != active) {
                    tabs.remove(i);
                    if (i < active) active--;
                    closed = true;
                    break;
                }
            }
            // Only the open DM tab could close: keep it rather than go over the limit (the DMs tab still shows it).
            if (!closed) return;
        }
        tabs.add(Tab.whisper(player));
    }

    public static void select(int index) {
        if (index < 0 || index >= tabs.size()) return;
        active = index;
        tabs.get(index).unread = 0;
        version++;
        refreshChat();
    }

    /** Closes a DM tab; tabs from the settings stay. */
    public static void close(int index) {
        if (index < 0 || index >= tabs.size() || tabs.get(index).whisperWith == null) return;
        tabs.remove(index);
        if (index < active) active--;
        else if (index == active) active = 0;
        version++;
        refreshChat();
    }

    /** The command (without the slash) that sends typed chat from the open tab, or null to send it as is. */
    public static @Nullable String commandFor(String message) {
        return isActive() ? tabs.get(active).command(message) : null;
    }

    /**
     * Most characters the chat box may hold: vanilla's {@link #CHAT_LIMIT}, less the open tab's prefix ("/gc ",
     * "/msg Name "), so what a tab sends is never longer than typing the command yourself. A typed command keeps
     * the full limit.
     */
    public static int inputLimit(String typed) {
        if (!isActive() || typed.startsWith("/")) return CHAT_LIMIT;
        String command = tabs.get(active).command("");
        return command == null ? CHAT_LIMIT : CHAT_LIMIT - 1 - command.length();
    }

    /** A new connection: DM tabs and unread counts start over. */
    static void reset() {
        if (active < tabs.size() && tabs.get(active).whisperWith != null) active = 0;
        tabs.removeIf(t -> t.whisperWith != null);
        for (Tab tab : tabs) tab.unread = 0;
        active = Math.clamp(active, 0, tabs.size() - 1);
        version++;
        refreshChat();
    }

    /**
     * Redraws the chat for the open tab, from the top. The rebuild passes every message in the history, so it
     * refills a fresh classification map: messages vanilla dropped from the history drop out of it too.
     */
    private static void refreshChat() {
        previousClassified = classified;
        classified = new IdentityHashMap<>();
        try {
            Minecraft.getInstance().gui.hud.getChat().rescaleChat();
        } finally {
            previousClassified = null;
        }
    }

    /** The message's channel, classified once: a tab switch then doesn't run the patterns over the whole history. */
    private static MessageClassifier.Result classify(GuiMessage message) {
        MessageClassifier.Result result = classified.get(message);
        if (result != null) return result;
        if (previousClassified != null) result = previousClassified.get(message);
        if (result == null) result = MessageClassifier.classify(message.content().getString());
        // Between rebuilds only new messages are added; a long session without one is cut back here.
        if (classified.size() >= MAX_CLASSIFIED) classified.clear();
        classified.put(message, result);
        return result;
    }
}
