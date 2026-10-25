package uk.toakan.tad.chattabs;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sorts a Hypixel chat line into its {@link Channel} by the prefix Hypixel puts in front of it. */
public final class MessageClassifier {
    /** "From [MVP+] Name: hi" (received) or "To [VIP] Name: hi" (sent); the rank is optional. */
    private static final Pattern DM = Pattern.compile("^(?:From|To) (?:\\[[^\\]]+\\] )?([A-Za-z0-9_]{1,16}): ");
    /**
     * Public chat: "[598] ᛃ [MVP+] Name: hi" on SkyBlock (level, optional emblem, optional rank), "[VIP] Name: hi" in
     * lobbies. Group 1 is the level, 2 the bracket before the name, 3 the name.
     */
    private static final Pattern PUBLIC = Pattern.compile(
        "^(\\[\\d+[^\\]]*\\] )?(?:\\S )?(?:\\[([^\\]]+)\\] )?([A-Za-z0-9_]{1,16})(?: \\[[^\\]]+\\])?: ");
    /** Brackets before a name that are player ranks; others ([NPC], [BOSS], [FEAR], [GUARD]) aren't players. */
    private static final Set<String> RANKS = Set.of("VIP", "VIP+", "MVP", "MVP+", "MVP++", "YOUTUBE", "ADMIN", "GM",
        "MOD", "HELPER", "OWNER", "MOJANG", "EVENTS", "PIG+++", "INNIT", "MCP", "STAFF");
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
        if (isPublic(text, plain)) return new Result(Channel.ALL_CHAT, null);
        return Result.SYSTEM;
    }

    private static boolean isPublic(String raw, String plain) {
        Matcher m = PUBLIC.matcher(plain);
        if (!m.find()) return false;
        String bracket = m.group(2);
        if (bracket != null) return RANKS.contains(bracket);
        // No rank: a SkyBlock level marks a player, otherwise only a lobby player's gray "§7Name§7: " does.
        return m.group(1) != null || raw.contains("§7" + m.group(3) + "§7: ");
    }
}
