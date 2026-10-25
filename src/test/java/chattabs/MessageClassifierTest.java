package chattabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageClassifierTest {
    private static void assertChannel(Channel expected, String text) {
        assertEquals(expected, MessageClassifier.classify(text).channel(), text);
    }

    @Test
    void channelPrefixes() {
        assertChannel(Channel.GUILD, "Guild > [MVP+] Tad_Dekker [GM]: hello");
        assertChannel(Channel.GUILD, "Guild > SeigeMonster joined.");
        assertChannel(Channel.OFFICER, "Officer > [VIP] Someone: hi");
        assertChannel(Channel.PARTY, "Party > [MVP++] Someone: hi");
        assertChannel(Channel.COOP, "Co-op > [VIP+] Someone: hi");
    }

    @Test
    void formattingCodesAreIgnored() {
        assertChannel(Channel.GUILD, "§2Guild > §b[MVP§c+§b] Tad_Dekker §f: hi");
        MessageClassifier.Result dm = MessageClassifier.classify("§dFrom §r§b[MVP§r§c+§r§b] Kizkey2A§r§7: §r§7hey");
        assertEquals(Channel.DM, dm.channel());
        assertEquals("Kizkey2A", dm.partner());
    }

    @Test
    void directMessages() {
        MessageClassifier.Result from = MessageClassifier.classify("From [MVP+] Tad_Dekker: hi");
        assertEquals(Channel.DM, from.channel());
        assertEquals("Tad_Dekker", from.partner());
        MessageClassifier.Result to = MessageClassifier.classify("To minicookiebites: hi");
        assertEquals(Channel.DM, to.channel());
        assertEquals("minicookiebites", to.partner());
    }

    @Test
    void publicChat() {
        assertChannel(Channel.ALL_CHAT, "[598] ᛃ §b[MVP§5+§b] Kizkey2A§f: hello all");
        assertChannel(Channel.ALL_CHAT, "[273] ⸎ [MVP+] Tad_Dekker: hi");
        assertChannel(Channel.ALL_CHAT, "[569] 🐸 §6[MVP§1++§6] Thicc_lad§f: hi");
        assertChannel(Channel.ALL_CHAT, "[114] §a[VIP] 5mi1§f: hi");
        assertChannel(Channel.ALL_CHAT, "[48] §7itzDapL§7: unranked on SkyBlock");
        assertChannel(Channel.ALL_CHAT, "[62] 90Zhz: no colour codes");
        assertChannel(Channel.ALL_CHAT, "§b[MVP§c+§b] Someone§f: in a lobby");
        assertChannel(Channel.ALL_CHAT, "§7Someone§7: unranked in a lobby");
    }

    @Test
    void everythingElseIsSystem() {
        assertChannel(Channel.SYSTEM, "From stash: You picked up 64 items");
        assertChannel(Channel.SYSTEM, "[NPC] Maddox: Hello there");
        assertChannel(Channel.SYSTEM, "[FEAR] Deadline: boo");
        assertChannel(Channel.SYSTEM, "[GUARD] Khulak: halt");
        assertChannel(Channel.SYSTEM, "API: something happened");
        assertChannel(Channel.SYSTEM, "Guildmaster says hi");
        assertChannel(Channel.SYSTEM, "Party Finder > Someone joined the dungeon group!");
        assertNull(MessageClassifier.classify("Welcome to Hypixel SkyBlock!").partner());
    }
}
