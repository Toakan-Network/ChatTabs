package chattabs;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * How the tab bar looks: colours, sizes and text, edited in Settings > Appearance and saved to style.json. Ported
 * from Skyblock Shopping List's HudStyle; the defaults are its palette.
 */
public final class TabStyle {
    public static final String DEFAULT_FONT = "minecraft:default";
    static final File FILE = new File(ChatTabs.CONFIG_DIR, "style.json");

    // Colours (ARGB).
    public int tabBackground = 0x99271910;
    public int tabBorder = 0xFF8A6A1E;
    public int tabText = 0xFFBFAF90;
    public int hoverBackground = 0xCC3A2818;
    public int activeBackground = 0xCC2C4A1B;
    public int activeBorder = 0xFFDAA520;
    public int activeText = 0xFFFFB728;
    /** Label colour of an inactive tab with unread messages. */
    public int unreadText = 0xFFFFFF55;

    // Sizes (GUI pixels).
    public int tabHeight = 12;
    public int padding = 4;
    public int gap = 1;
    public int borderWidth = 1;
    /** Corner rounding in pixels (0 = square). */
    public int radius = 0;

    // Text.
    public String font = DEFAULT_FONT;
    public boolean textShadow = false;
    public boolean boldActive = true;

    private static TabStyle current;
    /** Font id -> description, cached so drawing doesn't parse the id every frame. */
    private transient FontDescription fontDescription;
    private transient String fontDescriptionId;

    /** The active style, loaded from disk on first use. */
    public static TabStyle get() {
        TabStyle style = current;
        if (style == null) {
            style = JsonFiles.read(FILE, TabStyle.class);
            if (style == null) style = new TabStyle();
            style.clamp();
            current = style;
        }
        return style;
    }

    /** Writes the active style to disk. */
    public static void save() {
        TabStyle style = get();
        style.clamp();
        JsonFiles.write(FILE, style);
    }

    /** Puts every look setting back to its default and saves. */
    public static void reset() {
        current = new TabStyle();
        save();
    }

    private void clamp() {
        tabHeight = Math.clamp(tabHeight, 10, 20);
        padding = Math.clamp(padding, 1, 12);
        gap = Math.clamp(gap, 0, 8);
        borderWidth = Math.clamp(borderWidth, 0, 3);
        radius = Math.clamp(radius, 0, 6);
        if (font == null || Identifier.tryParse(font) == null) font = DEFAULT_FONT;
    }

    /** Text in the chosen font and colour. Unknown font ids fall back to Minecraft's default font. */
    public Component text(String text, int color, boolean bold) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(color & 0xFFFFFF).withBold(bold).withFont(fontDescription()));
    }

    private FontDescription fontDescription() {
        FontDescription description = fontDescription;
        if (description == null || !font.equals(fontDescriptionId)) {
            Identifier id = Identifier.tryParse(font);
            description = id == null ? FontDescription.DEFAULT : new FontDescription.Resource(id);
            fontDescription = description;
            fontDescriptionId = font;
        }
        return description;
    }

    /** Every font the loaded resource packs provide (assets/<ns>/font/<name>.json), default first. */
    public static List<String> availableFonts() {
        List<String> fonts = new ArrayList<>();
        FileToIdConverter converter = FileToIdConverter.json("font");
        for (Identifier file : converter.listMatchingResources(Minecraft.getInstance().getResourceManager()).keySet()) {
            Identifier id = converter.fileToId(file);
            // "include/..." fonts are building blocks other fonts pull in, not fonts to pick.
            if (!id.getPath().startsWith("include/")) fonts.add(id.toString());
        }
        fonts.sort(null);
        fonts.remove(DEFAULT_FONT);
        fonts.add(0, DEFAULT_FONT);
        return fonts;
    }
}
