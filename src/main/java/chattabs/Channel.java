package chattabs;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

/** A Hypixel chat channel a tab can show, with the command that sends to it. */
public enum Channel {
    /** Public player chat. */
    ALL_CHAT("All chat", "ac"),
    GUILD("Guild", "gc"),
    OFFICER("Officer", "oc"),
    PARTY("Party", "pc"),
    COOP("Co-op", "cc"),
    /** Direct messages; a tab sending here replies to the last DM ({@code /r}). */
    DM("DMs", "r"),
    /** Everything that isn't one of the channels above: NPCs, server notices. Sends as normal chat. */
    SYSTEM("System", null),
    /** Every message. Sends as normal chat. Was "ALL" in 0.1.0 files. */
    @SerializedName(value = "EVERYTHING", alternate = "ALL")
    EVERYTHING("Everything", null);

    public final String label;
    /** Command (without the slash) that sends its argument to this channel, or null for normal chat. */
    public final @Nullable String command;

    Channel(String label, @Nullable String command) {
        this.label = label;
        this.command = command;
    }
}
