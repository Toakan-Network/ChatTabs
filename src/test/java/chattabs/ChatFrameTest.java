package chattabs;

import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatFrameTest {
    /** The pose vanilla sets up for chat: scale, then 4 px in. */
    private static Matrix3x2f vanilla(float scale) {
        return new Matrix3x2f().scale(scale, scale).translate(4, 0);
    }

    @Test
    void vanillaPositionAtFullScale() {
        // 320 wide, 10 lines of 9 px, screen 240 high: chat bottom at 200.
        ChatFrame.Box box = ChatFrame.box(vanilla(1), 1.0, 320, 9, 10, 240);
        assertEquals(new ChatFrame.Box(0, 110, 332, 200), box);
    }

    @Test
    void halfScaleKeepsBottomLeft() {
        ChatFrame.Box box = ChatFrame.box(vanilla(0.5f), 0.5, 320, 9, 10, 240);
        assertEquals(0, box.left());
        assertEquals(200, box.bottom());
        assertEquals(155, box.top());
        assertEquals(326, box.right());
    }

    @Test
    void followsAMovedChat() {
        // A mod moving chat 100 right and 50 up before vanilla's scaling.
        Matrix3x2f pose = new Matrix3x2f().translate(100, -50).scale(1, 1).translate(4, 0);
        ChatFrame.Box box = ChatFrame.box(pose, 1.0, 320, 9, 10, 240);
        assertEquals(new ChatFrame.Box(100, 60, 432, 150), box);
    }
}
