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
    public int unread;

    Tab(String name, List<Channel> channels, Channel sendTo, @Nullable String whisperWith) {
        this.name = name;
        this.channels = channels.isEmpty() ? EnumSet.noneOf(Channel.class) : EnumSet.copyOf(channels);
        this.sendTo = sendTo;
        this.whisperWith = whisperWith;
    }

    static Tab of(Config.TabConfig config) {
        return new Tab(config.name, config.channels, config.sendTo, null);
    }

    static Tab whisper(String player) {
        return new Tab(player, List.of(), Channel.DM, player);
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
