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

import java.util.List;

/**
 * The row of tabs on the chat screen, in the gap between the chat (bottom at height - 40) and the input box
 * (top at height - 14). Left click opens a tab, right click closes a DM tab.
 */
final class TabBar {
    /** Bottom of the bar, one pixel above the input box's background. */
    private static final int BOTTOM_OFFSET = 15;
    private static final int LEFT = 2;

    private TabBar() {}

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof ChatScreen chat)) return;
            ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> draw(chat, graphics, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !click(chat, event));
        });
    }

    /** Hidden off Hypixel, and while a command is typed so its suggestions aren't covered. */
    private static boolean hidden(ChatScreen screen) {
        return !ChatTabs.isActive() || ((ChatScreenAccessor) screen).chattabs$input().getValue().startsWith("/");
    }

    private static void draw(ChatScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hidden(screen)) return;
        TabStyle style = TabStyle.get();
        Font font = Minecraft.getInstance().font;
        List<Tab> tabs = ChatTabs.tabs();
        int bottom = screen.height - BOTTOM_OFFSET;
        int top = bottom - style.tabHeight;
        int textY = top + (style.tabHeight - font.lineHeight) / 2 + 1;
        int x = LEFT;
        for (int i = 0; i < tabs.size(); i++) {
            boolean open = i == ChatTabs.activeIndex();
            Component label = label(tabs.get(i), open, style);
            int w = font.width(label) + 2 * style.padding;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < bottom;
            int background = open ? style.activeBackground : hover ? style.hoverBackground : style.tabBackground;
            RoundedBox.fill(graphics, x, top, w, style.tabHeight, style.radius, background);
            int border = open ? style.activeBorder : style.tabBorder;
            for (int b = 0; b < style.borderWidth; b++) {
                RoundedBox.outline(graphics, x + b, top + b, w - 2 * b, style.tabHeight - 2 * b,
                    Math.max(0, style.radius - b), border);
            }
            graphics.text(font, label, x + style.padding, textY, 0xFFFFFFFF, style.textShadow);
            x += w + style.gap;
        }
    }

    /** Handles a click on the bar; true when it hit a tab, so the chat screen doesn't also get it. */
    private static boolean click(ChatScreen screen, MouseButtonEvent event) {
        if (hidden(screen)) return false;
        TabStyle style = TabStyle.get();
        int bottom = screen.height - BOTTOM_OFFSET;
        int top = bottom - style.tabHeight;
        if (event.y() < top || event.y() >= bottom) return false;
        Font font = Minecraft.getInstance().font;
        List<Tab> tabs = ChatTabs.tabs();
        int x = LEFT;
        for (int i = 0; i < tabs.size(); i++) {
            int w = font.width(label(tabs.get(i), i == ChatTabs.activeIndex(), style)) + 2 * style.padding;
            if (event.x() >= x && event.x() < x + w) {
                if (event.button() == ChatTabsClient.inputCode("MOUSE_BUTTON_RIGHT")) ChatTabs.close(i);
                else if (event.button() == ChatTabsClient.inputCode("MOUSE_BUTTON_LEFT")) ChatTabs.select(i);
                return true;
            }
            x += w + style.gap;
        }
        return false;
    }

    /** The tab's name, with its unread count when it has one. */
    private static Component label(Tab tab, boolean open, TabStyle style) {
        boolean showUnread = !open && tab.unread > 0 && ChatTabs.config().showUnread;
        String text = showUnread ? tab.name + " (" + Math.min(tab.unread, 99) + (tab.unread > 99 ? "+)" : ")") : tab.name;
        int color = open ? style.activeText : showUnread ? style.unreadText : style.tabText;
        return style.text(text, color, open && style.boldActive);
    }
}
