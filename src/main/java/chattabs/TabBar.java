package chattabs;

import chattabs.mixin.ChatScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The row of tabs on the chat screen, stuck to the chat wherever it is ({@link ChatFrame}): above it, under it, or
 * at a dragged offset from it (Free). Left click opens a tab, right click closes a DM tab, and in Free a left drag
 * moves the bar, snapping to the chat's and the screen's edges.
 */
final class TabBar {
    /** The bar's bottom never goes below this distance from the screen bottom (the input box is under it). */
    private static final int INPUT_CLEARANCE = 15;
    /** How far (GUI pixels) a press must move before it drags the bar rather than clicking a tab. */
    private static final int DRAG_THRESHOLD = 3;

    /** Where the bar is this frame: its top-left corner and each tab's label and width. */
    private record Layout(int x, int y, int height, List<Component> labels, int[] widths) {
        int width(int gap) {
            int total = 0;
            for (int w : widths) total += w;
            return total + gap * Math.max(0, widths.length - 1);
        }
    }

    /** A left press on a Free bar: the tab under it, the mouse and bar position then, and whether it has moved. */
    private static boolean pressed;
    private static boolean dragging;
    private static int pressedTab;
    private static double pressX;
    private static double pressY;
    private static int startX;
    private static int startY;

    private TabBar() {}

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof ChatScreen chat)) return;
            pressed = false;
            dragging = false;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> draw(chat, graphics, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !click(chat, event));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> !drag(chat, event));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> !release());
        });
    }

    /** Hidden off Hypixel, and at the bottom while a command is typed so its suggestions aren't covered. */
    private static boolean hidden(ChatScreen screen) {
        if (!ChatTabs.isActive()) return true;
        return ChatTabs.config().barPosition == Config.BarPosition.BOTTOM
            && ((ChatScreenAccessor) screen).chattabs$input().getValue().startsWith("/");
    }

    private static Layout layout(ChatScreen screen) {
        TabStyle style = TabStyle.get();
        Font font = Minecraft.getInstance().font;
        List<Tab> tabs = ChatTabs.tabs();
        List<Component> labels = new ArrayList<>(tabs.size());
        int[] widths = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            Component label = label(tabs.get(i), i == ChatTabs.activeIndex(), style);
            labels.add(label);
            widths[i] = font.width(label) + 2 * style.padding;
        }
        Layout sized = new Layout(0, 0, style.tabHeight, labels, widths);
        int barWidth = sized.width(style.gap);
        Config config = ChatTabs.config();
        ChatFrame.Box chat = ChatFrame.bounds(screen.height);
        int x = chat.left();
        int y = switch (config.barPosition) {
            case TOP -> chat.top() - 1 - style.tabHeight;
            case BOTTOM -> Math.min(chat.bottom() + 1, screen.height - INPUT_CLEARANCE - style.tabHeight);
            case FREE -> {
                x = chat.left() + config.freeX;
                yield chat.top() + config.freeY;
            }
        };
        x = Math.clamp(x, 0, Math.max(0, screen.width - barWidth));
        y = Math.clamp(y, 0, Math.max(0, screen.height - style.tabHeight));
        return new Layout(x, y, style.tabHeight, labels, widths);
    }

    private static void draw(ChatScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hidden(screen)) return;
        TabStyle style = TabStyle.get();
        Font font = Minecraft.getInstance().font;
        Layout bar = layout(screen);
        int x = bar.x();
        for (int i = 0; i < bar.widths().length; i++) {
            int w = bar.widths()[i];
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= bar.y() && mouseY < bar.y() + bar.height();
            drawTab(graphics, font, style, x, bar.y(), w, bar.labels().get(i), i == ChatTabs.activeIndex(), hover);
            x += w + style.gap;
        }
    }

    /** One tab: background, border and label. Also used by the Appearance preview. */
    static void drawTab(GuiGraphicsExtractor graphics, Font font, TabStyle style, int x, int y, int w,
                        Component label, boolean open, boolean hover) {
        int background = open ? style.activeBackground : hover ? style.hoverBackground : style.tabBackground;
        RoundedBox.fill(graphics, x, y, w, style.tabHeight, style.radius, background);
        int border = open ? style.activeBorder : style.tabBorder;
        for (int b = 0; b < style.borderWidth; b++) {
            RoundedBox.outline(graphics, x + b, y + b, w - 2 * b, style.tabHeight - 2 * b,
                Math.max(0, style.radius - b), border);
        }
        int textY = y + (style.tabHeight - font.lineHeight) / 2 + 1;
        graphics.text(font, label, x + style.padding, textY, 0xFFFFFFFF, style.textShadow);
    }

    /** The tab at a point, or -1. */
    private static int tabAt(Layout bar, double mouseX, double mouseY) {
        if (mouseY < bar.y() || mouseY >= bar.y() + bar.height()) return -1;
        int gap = TabStyle.get().gap;
        int x = bar.x();
        for (int i = 0; i < bar.widths().length; i++) {
            if (mouseX >= x && mouseX < x + bar.widths()[i]) return i;
            x += bar.widths()[i] + gap;
        }
        return -1;
    }

    /** Handles a press on the bar; true when it hit a tab, so the chat screen doesn't also get it. */
    private static boolean click(ChatScreen screen, MouseButtonEvent event) {
        if (hidden(screen)) return false;
        Layout bar = layout(screen);
        int tab = tabAt(bar, event.x(), event.y());
        if (tab < 0) return false;
        if (event.button() == ChatTabsClient.inputCode("MOUSE_BUTTON_RIGHT")) {
            ChatTabs.close(tab);
        } else if (event.button() == ChatTabsClient.inputCode("MOUSE_BUTTON_LEFT")) {
            if (ChatTabs.config().barPosition != Config.BarPosition.FREE) {
                ChatTabs.select(tab);
            } else {
                // Free: a drag moves the bar, a press without one selects on release.
                pressed = true;
                dragging = false;
                pressedTab = tab;
                pressX = event.x();
                pressY = event.y();
                startX = bar.x();
                startY = bar.y();
            }
        }
        return true;
    }

    /** Moves a pressed Free bar with the mouse; true while it is being dragged. */
    private static boolean drag(ChatScreen screen, MouseButtonEvent event) {
        if (!pressed) return false;
        double moveX = event.x() - pressX;
        double moveY = event.y() - pressY;
        if (!dragging && Math.abs(moveX) + Math.abs(moveY) <= DRAG_THRESHOLD) return true;
        dragging = true;
        TabStyle style = TabStyle.get();
        int barWidth = layout(screen).width(style.gap);
        ChatFrame.Box chat = ChatFrame.bounds(screen.height);
        int[] linesX = {0, screen.width, chat.left(), chat.right(), (chat.left() + chat.right()) / 2};
        int[] linesY = {0, screen.height, chat.top(), chat.bottom()};
        int x = PanelSnap.snap((int) Math.round(startX + moveX), barWidth, linesX).pos();
        int y = PanelSnap.snap((int) Math.round(startY + moveY), style.tabHeight, linesY).pos();
        Config config = ChatTabs.config();
        config.freeX = x - chat.left();
        config.freeY = y - chat.top();
        return true;
    }

    /** Ends a press on a Free bar: saves a drag, or selects the tab that was pressed. */
    private static boolean release() {
        if (!pressed) return false;
        pressed = false;
        if (dragging) {
            dragging = false;
            ChatTabs.writeConfig();
        } else {
            ChatTabs.select(pressedTab);
        }
        return true;
    }

    /** The tab's name, with its unread count when it has one. */
    private static Component label(Tab tab, boolean open, TabStyle style) {
        boolean showUnread = !open && tab.unread > 0 && ChatTabs.config().showUnread;
        String text = showUnread ? tab.name + " (" + Math.min(tab.unread, 99) + (tab.unread > 99 ? "+)" : ")") : tab.name;
        int color = open ? style.activeText : showUnread ? style.unreadText : style.tabText;
        return style.text(text, color, open && style.boldActive);
    }
}
