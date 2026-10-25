package chattabs;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sorts a Hypixel chat line into its {@link Channel} by the prefix Hypixel puts in front of it. */
public final class MessageClassifier {
    /** "From [MVP+] Name: hi" (received) or "To [VIP] Name: hi" (sent); the rank is optional. */
    private static final Pattern DM = Pattern.compile("^(?:From|To) (?:\\[[^\\]]+\\] )?([A-Za-z0-9_]{1,16}): ");
    /** Legacy colour codes ("§" plus one character), which Hypixel still sends inside the text. */
    private static final Pattern FORMATTING = Pattern.compile("§.");

    /** The channel of a line, and for a DM the other player's name. */
    public record Result(Channel channel, @Nullable String partner) {
        static final Result SYSTEM = new Result(Channel.SYSTEM, null);
    }

    private MessageClassifier() {}

    /** Classifies a message's plain text; {@code §} colour codes in it are ignored. */
    public static Result classify(String text) {
        String plain = FORMATTING.matcher(text).replaceAll("").stripLeading();
        if (plain.startsWith("Guild > ")) return new Result(Channel.GUILD, null);
        if (plain.startsWith("Officer > ")) return new Result(Channel.OFFICER, null);
        if (plain.startsWith("Party > ")) return new Result(Channel.PARTY, null);
        if (plain.startsWith("Co-op > ")) return new Result(Channel.COOP, null);
        Matcher dm = DM.matcher(plain);
        // SkyBlock's "From stash: ..." item notices share the DM shape.
        if (dm.find() && !dm.group(1).toLowerCase(Locale.ROOT).equals("stash")) {
            return new Result(Channel.DM, dm.group(1));
        }
        return Result.SYSTEM;
    }
}
