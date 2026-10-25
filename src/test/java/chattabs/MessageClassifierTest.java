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
    void everythingElseIsSystem() {
        assertChannel(Channel.SYSTEM, "From stash: You picked up 64 items");
        assertChannel(Channel.SYSTEM, "[598] ᛃ [MVP+] Kizkey2A: hello all");
        assertChannel(Channel.SYSTEM, "Guildmaster says hi");
        assertChannel(Channel.SYSTEM, "Party Finder > Someone joined the dungeon group!");
        assertNull(MessageClassifier.classify("Welcome to Hypixel SkyBlock!").partner());
    }
}
