package uk.toakan.tad.chattabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/**
 * Where the open chat is on screen, so the tab bar can stick to it. Read from the pose chat is drawn with (see
 * {@code ChatComponentMixin}), so it follows any mod that moves or scales chat, such as Huddable. Closed chat is
 * drawn with the same pose, and the box always has the open chat's height, so the bar stays put when chat opens.
 */
public final class ChatFrame {
    /** A box in screen (GUI) pixels. */
    public record Box(int left, int top, int right, int bottom) {
        public int width() {
            return right - left;
        }
    }

    private static Box last;

    private ChatFrame() {}

    /** Called with chat's pose right after vanilla scaled it, while chat (open or closed) is drawn or hit-tested. */
    public static void record(Matrix3x2f pose, int screenHeight) {
        Options options = Minecraft.getInstance().options;
        last = box(pose, options.chatScale().get(), ChatComponent.getWidth(options.chatWidth().get()),
            entryHeight(options), openLines(options), screenHeight);
    }

    /** The open chat's box from the last frame, or where vanilla puts it before chat has been drawn. */
    public static Box bounds(int screenHeight) {
        if (last != null) return last;
        Options options = Minecraft.getInstance().options;
        double scale = options.chatScale().get();
        Matrix3x2f pose = new Matrix3x2f().scale((float) scale, (float) scale).translate(4, 0);
        return box(pose, scale, ChatComponent.getWidth(options.chatWidth().get()), entryHeight(options),
            openLines(options), screenHeight);
    }

    /** Lines in the open chat, as {@code ChatComponent.getLinesPerPage} works it out while chat is focused. */
    private static int openLines(Options options) {
        return ChatComponent.getHeight(options.chatHeightFocused().get()) / entryHeight(options);
    }

    private static int entryHeight(Options options) {
        return (int) (9.0 * (options.chatLineSpacing().get() + 1.0));
    }

    /**
     * The chat box as ChatComponent lays it out in its own coordinates (x from -4 to its width + 8, the page of
     * lines above {@code (screenHeight - 40) / scale}), mapped through {@code pose}.
     */
    static Box box(Matrix3x2f pose, double scale, int chatWidth, int entryHeight, int lines, int screenHeight) {
        int maxWidth = (int) Math.ceil(chatWidth / scale);
        int bottom = (int) Math.floor((screenHeight - 40) / scale);
        int top = bottom - lines * entryHeight;
        Vector2f a = pose.transformPosition(-4, top, new Vector2f());
        Vector2f b = pose.transformPosition(maxWidth + 8, bottom, new Vector2f());
        return new Box(Math.round(Math.min(a.x, b.x)), Math.round(Math.min(a.y, b.y)),
            Math.round(Math.max(a.x, b.x)), Math.round(Math.max(a.y, b.y)));
    }
}
