package uk.toakan.tad.chattabs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabTest {
    private static List<Tab> build(List<Config.TabConfig> configs) {
        List<Tab> tabs = new ArrayList<>();
        for (Config.TabConfig c : configs) tabs.add(Tab.of(c));
        return tabs;
    }

    @Test
    void openTabSurvivesDeleteBeforeIt() {
        List<Config.TabConfig> configs = Config.defaultTabs();
        Tab guild = build(configs).get(2);
        configs.remove(1);
        assertEquals(1, Tab.find(build(configs), guild));
    }

    @Test
    void openTabSurvivesMove() {
        List<Config.TabConfig> configs = Config.defaultTabs();
        Tab guild = build(configs).get(2);
        Collections.swap(configs, 2, 4);
        assertEquals(4, Tab.find(build(configs), guild));
    }

    @Test
    void openTabSurvivesRename() {
        List<Config.TabConfig> configs = Config.defaultTabs();
        Tab guild = build(configs).get(2);
        configs.get(2).name = "Guild chat";
        assertEquals(2, Tab.find(build(configs), guild));
    }

    @Test
    void resetLosesOpenTab() {
        Tab guild = build(Config.defaultTabs()).get(2);
        assertEquals(-1, Tab.find(build(Config.defaultTabs()), guild));
    }

    @Test
    void dmTabFoundByIdentity() {
        List<Tab> tabs = build(Config.defaultTabs());
        Tab dm = Tab.whisper("Steve");
        tabs.add(dm);
        assertEquals(tabs.size() - 1, Tab.find(tabs, dm));
        assertEquals(-1, Tab.find(tabs, Tab.whisper("Steve")));
    }
}
