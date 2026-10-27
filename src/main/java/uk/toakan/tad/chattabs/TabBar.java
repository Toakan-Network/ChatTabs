package uk.toakan.tad.chattabs;

import uk.toakan.tad.chattabs.mixin.ChatScreenAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The row of tabs on the chat screen, stuck to the chat wherever it is ({@link ChatFrame}): above it, under it, or
 * at a dragged offset from it (Free). Left click opens a tab, right click closes a DM tab, and in Free a left drag
 * moves the bar, snapping to the chat's and the screen's edges. The bar is drawn at chat's scale, so it keeps the
 * chat's size through vanilla Chat Scale and any mod scaling chat (Huddable). While chat is closed the same bar is
 * drawn with vanilla's chat HUD element (Settings > General), without clicks, so unread counts show. It is part of
 * that element rather than a layer of its own, so a HUD mover doesn't move or scale it a second time.
 */
final class TabBar {
    /** The bar's bottom never goes below this distance from the screen bottom (the input box is under it). */
    private static final int INPUT_CLEARANCE = 15;
    /** How far (GUI pixels) a press must move before it drags the bar rather than clicking a tab. */
    private static final int DRAG_THRESHOLD = 3;
    /**
     * Where the bar is this frame: its top-left corner on screen, the scale it is drawn at, and each tab's label and
     * width. Widths and height are in the bar's own pixels, before the scale.
     */
    private record Layout(int x, int y, float scale, int height, List<Component> labels, int[] widths) {
        int width(int gap) {
            int total = 0;
            for (int w : widths) total += w;
            return total + gap * Math.max(0, widths.length - 1);
        }

        /** A screen x in the bar's own pixels, from its left edge. */
        double localX(double screenX) {
            return (screenX - x) / scale;
        }

        double localY(double screenY) {
            return (screenY - y) / scale;
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
    /** The chat box length last set on the open chat screen. */
    private static int inputLimit = ChatTabs.CHAT_LIMIT;

    private TabBar() {}

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof ChatScreen chat)) return;
            pressed = false;
            dragging = false;
            // init() just set vanilla's length.
            inputLimit = ChatTabs.CHAT_LIMIT;
            limitInput(chat);
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> draw(chat, graphics, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !click(chat, event));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> !drag(chat, event));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> !release());
        });
        HudElementRegistry.replaceElement(VanillaHudElements.CHAT, original -> (graphics, delta) -> {
            original.extractRenderState(graphics, delta);
            Minecraft mc = Minecraft.getInstance();
            if (!ChatTabs.isActive() || !ChatTabs.config().showBarClosed || mc.gui.screen() instanceof ChatScreen) return;
            drawBar(graphics, layout(graphics.guiWidth(), graphics.guiHeight()), -1, -1);
        });
    }

    /** Keeps the chat box's length to what the open tab can send ({@link ChatTabs#inputLimit}); set only on change. */
    private static void limitInput(ChatScreen screen) {
        EditBox input = ((ChatScreenAccessor) screen).chattabs$input();
        int limit = ChatTabs.inputLimit(input.getValue());
        if (limit != inputLimit) {
            inputLimit = limit;
            input.setMaxLength(limit);
        }
    }

    /** Hidden off Hypixel, and at the bottom while a command is typed so its suggestions aren't covered. */
    private static boolean hidden(ChatScreen screen) {
        if (!ChatTabs.isActive()) return true;
        return ChatTabs.config().barPosition == Config.BarPosition.BOTTOM
            && ((ChatScreenAccessor) screen).chattabs$input().getValue().startsWith("/");
    }

    private static Layout layout(ChatScreen screen) {
        return layout(screen.width, screen.height);
    }

    /** Each tab's label and width, remade only when {@link LabelsKey} changes (the closed bar draws every frame). */
    private record Labels(List<Component> labels, int[] widths) {}

    private record LabelsKey(long tabs, TabStyle style, long styleVersion, int fonts) {}

    private static Labels labels;
    private static LabelsKey labelsKey;
    /** Changes on every font reload: widths may differ. */
    private static int fontGeneration;

    static void fontsReloaded() {
        fontGeneration++;
    }

    private static Labels labels(TabStyle style) {
        LabelsKey key = new LabelsKey(ChatTabs.version(), style, TabStyle.version(), fontGeneration);
        if (labels != null && key.equals(labelsKey)) return labels;
        Font font = Minecraft.getInstance().font;
        List<Tab> tabs = ChatTabs.tabs();
        List<Component> made = new ArrayList<>(tabs.size());
        int[] widths = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            Component label = label(tabs.get(i), i == ChatTabs.activeIndex(), style);
            made.add(label);
            widths[i] = font.width(label) + 2 * style.padding;
        }
        labels = new Labels(List.copyOf(made), widths);
        labelsKey = key;
        return labels;
    }

    private static Layout layout(int screenWidth, int screenHeight) {
        TabStyle style = TabStyle.get();
        Labels made = labels(style);
        List<Component> labels = made.labels();
        int[] widths = made.widths();
        ChatFrame.Box chat = ChatFrame.bounds(screenHeight);
        float scale = chat.scale();
        Layout sized = new Layout(0, 0, scale, style.tabHeight, labels, widths);
        int barWidth = Math.round(sized.width(style.gap) * scale);
        int barHeight = Math.round(style.tabHeight * scale);
        Config config = ChatTabs.config();
        int x = chat.left();
        int y = switch (config.barPosition) {
            case TOP -> chat.top() - 1 - barHeight;
            case BOTTOM -> Math.min(chat.bottom() + 1, screenHeight - INPUT_CLEARANCE - barHeight);
            case FREE -> {
                x = chat.left() + config.freeX;
                yield chat.top() + config.freeY;
            }
        };
        x = Math.clamp(x, 0, Math.max(0, screenWidth - barWidth));
        y = Math.clamp(y, 0, Math.max(0, screenHeight - barHeight));
        return new Layout(x, y, scale, style.tabHeight, labels, widths);
    }

    private static void draw(ChatScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        limitInput(screen);
        if (hidden(screen)) return;
        drawBar(graphics, layout(screen), mouseX, mouseY);
    }

    /** Every tab of {@code bar}, at its scale; the one under the mouse is drawn hovered. */
    private static void drawBar(GuiGraphicsExtractor graphics, Layout bar, int mouseX, int mouseY) {
        TabStyle style = TabStyle.get();
        Font font = Minecraft.getInstance().font;
        int hovered = tabAt(bar, mouseX, mouseY);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(bar.x(), bar.y());
        pose.scale(bar.scale(), bar.scale());
        int x = 0;
        for (int i = 0; i < bar.widths().length; i++) {
            int w = bar.widths()[i];
            drawTab(graphics, font, style, x, 0, w, bar.labels().get(i), i == ChatTabs.activeIndex(), i == hovered);
            x += w + style.gap;
        }
        pose.popMatrix();
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
        double localX = bar.localX(mouseX);
        double localY = bar.localY(mouseY);
        if (localY < 0 || localY >= bar.height()) return -1;
        int gap = TabStyle.get().gap;
        int x = 0;
        for (int i = 0; i < bar.widths().length; i++) {
            if (localX >= x && localX < x + bar.widths()[i]) return i;
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
            if (ChatTabs.config().barPosition != Config.BarPosition.FREE || ChatTabs.config().lockBar) {
                ChatTabs.select(tab);
            } else {
                // Free and unlocked: a drag moves the bar, a press without one selects on release.
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
        Layout bar = layout(screen);
        int barWidth = Math.round(bar.width(style.gap) * bar.scale());
        int barHeight = Math.round(bar.height() * bar.scale());
        ChatFrame.Box chat = ChatFrame.bounds(screen.height);
        int[] linesX = {0, screen.width, chat.left(), chat.right(), (chat.left() + chat.right()) / 2};
        int[] linesY = {0, screen.height, chat.top(), chat.bottom()};
        int x = PanelSnap.snap((int) Math.round(startX + moveX), barWidth, linesX).pos();
        int y = PanelSnap.snap((int) Math.round(startY + moveY), barHeight, linesY).pos();
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
