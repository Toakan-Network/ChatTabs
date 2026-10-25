package chattabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTest {
    @Test
    void oldAllChannelReadsAsEverything() {
        String json = "{\"tabs\":[{\"name\":\"All\",\"channels\":[\"ALL\"],\"sendTo\":\"ALL\"}]}";
        Config config = JsonFiles.GSON.fromJson(json, Config.class);
        assertEquals(List.of(Channel.EVERYTHING), config.tabs.get(0).channels);
        assertEquals(Channel.EVERYTHING, config.tabs.get(0).sendTo);
    }
}
