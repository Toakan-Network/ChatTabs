package uk.toakan.tad.chattabs;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** A Hypixel chat channel a tab can show, with the command that sends to it. */
public enum Channel {
    /** Public player chat. */
    ALL_CHAT("chat-tabs.channel.all_chat", "ac"),
    GUILD("chat-tabs.channel.guild", "gc"),
    OFFICER("chat-tabs.channel.officer", "oc"),
    PARTY("chat-tabs.channel.party", "pc"),
    COOP("chat-tabs.channel.coop", "cc"),
    /** Direct messages; a tab sending here replies to the last DM ({@code /r}). */
    DM("chat-tabs.channel.dm", "r"),
    /** Everything that isn't one of the channels above: NPCs, server notices. Sends as normal chat. */
    SYSTEM("chat-tabs.channel.system", null),
    /** Every message. Sends as normal chat. Was "ALL" in 0.1.0 files. */
    @SerializedName(value = "EVERYTHING", alternate = "ALL")
    EVERYTHING("chat-tabs.channel.everything", null);

    /** Lang key of the channel's name. */
    private final String labelKey;
    /** Command (without the slash) that sends its argument to this channel, or null for normal chat. */
    public final @Nullable String command;

    Channel(String labelKey, @Nullable String command) {
        this.labelKey = labelKey;
        this.command = command;
    }

    /** The channel's name, as shown in Settings. */
    public Component label() {
        return Component.translatable(labelKey);
    }
}
