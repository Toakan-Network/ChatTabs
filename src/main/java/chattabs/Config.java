package chattabs;

import java.util.ArrayList;
import java.util.List;

/** Settings, saved as {@code config/chat-tabs/tabs.json}. Fields missing from the file keep these defaults. */
public final class Config {
    public boolean enabled = true;
    /** Open a tab per DM conversation. */
    public boolean whisperTabs = true;
    /** Most DM tabs open at once; the oldest closes for a new one. */
    public int maxWhisperTabs = 5;
    public boolean showUnread = true;
    /** Draw the tab bar on the HUD while chat is closed, so unread counts show. */
    public boolean showBarClosed = true;
    public BarPosition barPosition = BarPosition.BOTTOM;
    /** Free bar position: offset of its top-left corner from the chat's top-left corner. */
    public int freeX = 0;
    public int freeY = FREE_Y_DEFAULT;
    public List<TabConfig> tabs = defaultTabs();

    /** Free position's default: just above the chat. */
    static final int FREE_Y_DEFAULT = -13;

    /** Where the tab bar sits: above the chat, under it, or where it was dragged (still moving with the chat). */
    public enum BarPosition { TOP, BOTTOM, FREE }

    /** A tab the player set up. */
    public static final class TabConfig {
        public String name;
        public List<Channel> channels;
        /** Where typed chat goes; {@link Channel#command} null means normal chat. */
        public Channel sendTo;

        /** For Gson. */
        private TabConfig() {}

        public TabConfig(String name, List<Channel> channels, Channel sendTo) {
            this.name = name;
            this.channels = new ArrayList<>(channels);
            this.sendTo = sendTo;
        }
    }

    static List<TabConfig> defaultTabs() {
        List<TabConfig> tabs = new ArrayList<>();
        tabs.add(new TabConfig("General", List.of(Channel.EVERYTHING), Channel.EVERYTHING));
        tabs.add(new TabConfig("All", List.of(Channel.ALL_CHAT), Channel.ALL_CHAT));
        tabs.add(new TabConfig("Guild", List.of(Channel.GUILD, Channel.OFFICER), Channel.GUILD));
        tabs.add(new TabConfig("Party", List.of(Channel.PARTY), Channel.PARTY));
        tabs.add(new TabConfig("DMs", List.of(Channel.DM), Channel.DM));
        tabs.add(new TabConfig("System", List.of(Channel.SYSTEM), Channel.EVERYTHING));
        return tabs;
    }

    /** Fixes what a hand-edited file can break: no tabs, missing names or channels. */
    void sanitize() {
        if (tabs == null) tabs = new ArrayList<>();
        tabs.removeIf(t -> t == null);
        for (TabConfig t : tabs) {
            if (t.name == null || t.name.isBlank()) t.name = "Tab";
            if (t.channels == null) t.channels = new ArrayList<>();
            t.channels.removeIf(c -> c == null);
            if (t.sendTo == null) t.sendTo = Channel.EVERYTHING;
        }
        if (tabs.isEmpty()) tabs = defaultTabs();
        maxWhisperTabs = Math.clamp(maxWhisperTabs, 0, 20);
        if (barPosition == null) barPosition = BarPosition.BOTTOM;
        freeX = Math.clamp(freeX, -10000, 10000);
        freeY = Math.clamp(freeY, -10000, 10000);
    }
}
