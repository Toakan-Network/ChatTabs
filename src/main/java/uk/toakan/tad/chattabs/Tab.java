package uk.toakan.tad.chattabs;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/** A tab in the bar: which channels it shows and where typed chat goes. */
public final class Tab {
    public final String name;
    private final EnumSet<Channel> channels;
    private final Channel sendTo;
    /** The other player of a DM tab, or null for a tab from the settings. */
    public final @Nullable String whisperWith;
    /** The settings entry a tab from the settings was made from (null for DM tabs), so it can be found again. */
    final Config.@Nullable TabConfig source;
    public int unread;

    Tab(String name, List<Channel> channels, Channel sendTo, @Nullable String whisperWith,
        Config.@Nullable TabConfig source) {
        this.name = name;
        this.channels = channels.isEmpty() ? EnumSet.noneOf(Channel.class) : EnumSet.copyOf(channels);
        this.sendTo = sendTo;
        this.whisperWith = whisperWith;
        this.source = source;
    }

    static Tab of(Config.TabConfig config) {
        return new Tab(config.name, config.channels, config.sendTo, null, config);
    }

    static Tab whisper(String player) {
        return new Tab(player, List.of(), Channel.DM, player, null);
    }

    /**
     * Where {@code previous} is in {@code tabs} after they were rebuilt: the same DM tab, or the tab made from the
     * same settings entry (which moving or editing tabs in Settings keeps). -1 when it is gone.
     */
    static int find(List<Tab> tabs, Tab previous) {
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            if (tab == previous || previous.source != null && tab.source == previous.source) return i;
        }
        return -1;
    }

    public boolean shows(MessageClassifier.Result message) {
        if (whisperWith != null) return message.channel() == Channel.DM && whisperWith.equalsIgnoreCase(message.partner());
        return channels.contains(Channel.EVERYTHING) || channels.contains(message.channel());
    }

    /** The command (without the slash) that sends {@code message} from this tab, or null for normal chat. */
    public @Nullable String command(String message) {
        if (whisperWith != null) return "msg " + whisperWith + " " + message;
        return sendTo.command == null ? null : sendTo.command + " " + message;
    }
}
