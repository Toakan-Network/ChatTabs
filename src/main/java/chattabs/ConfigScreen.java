package chattabs;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.image.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * The Settings screen, built with YetAnotherConfigLib. Options write into {@link ChatTabs#config()} and
 * {@link TabStyle#get()}, and Save writes both files. The tab buttons (add, move, delete) save straight away.
 */
public final class ConfigScreen {
    /** Where a tab can send typed chat; System and Everything both mean normal chat, so only Everything is offered. */
    private static final List<Channel> SEND_TO = List.of(Channel.EVERYTHING, Channel.ALL_CHAT, Channel.GUILD,
        Channel.OFFICER, Channel.PARTY, Channel.COOP, Channel.DM);

    /** Category positions, for reopening the screen where a button was pressed. */
    private static final int GENERAL_CATEGORY = 0;
    private static final int TABS_CATEGORY = 1;
    private static final int APPEARANCE_CATEGORY = 2;
    /** The tab section left open when a Tabs button rebuilt the screen, or -1. Used once. */
    private static int expandedTab = -1;

    private ConfigScreen() {}

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal(ChatTabsClient.NAME))
            .category(general(parent))
            .category(tabs(parent))
            .category(appearance(parent))
            .save(() -> {
                ChatTabs.saveConfig();
                TabStyle.save();
            })
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory general(Screen parent) {
        Config c = ChatTabs.config();
        Config d = new Config();
        return ConfigCategory.createBuilder()
            .name(Component.literal("General"))
            .option(toggle("Chat tabs", "Show the tab bar and filter chat by tab. Only on Hypixel; other servers "
                + "always get normal chat.", d.enabled, () -> c.enabled, v -> c.enabled = v))
            .option(toggle("DM tabs", "Open a tab for each player you message or who messages you. Right click a "
                + "DM tab to close it.", d.whisperTabs, () -> c.whisperTabs, v -> c.whisperTabs = v))
            .option(slider("Most DM tabs", "How many DM tabs can be open; a new conversation closes the oldest.",
                1, 20, d.maxWhisperTabs, () -> c.maxWhisperTabs, v -> c.maxWhisperTabs = v))
            .option(toggle("Unread counts", "Show how many new messages a tab has, e.g. Guild (3).",
                d.showUnread, () -> c.showUnread, v -> c.showUnread = v))
            .option(Option.<Config.BarPosition>createBuilder()
                .name(Component.literal("Tab bar"))
                .description(OptionDescription.of(Component.literal(
                    "Top: above the chat. Bottom: under the chat. Free: drag the tabs in chat to place them; they "
                        + "snap to the chat's edges and keep their place next to it when the chat moves (e.g. with "
                        + "Huddable).")))
                .binding(d.barPosition, () -> c.barPosition, v -> c.barPosition = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(Config.BarPosition.class)
                    .formatValue(p -> Component.literal(switch (p) {
                        case TOP -> "Top";
                        case BOTTOM -> "Bottom";
                        case FREE -> "Free";
                    })))
                .build())
            .option(button("Reset free position", "Reset", "Puts a Free tab bar back just above the chat.",
                parent, GENERAL_CATEGORY, () -> {
                    c.freeX = 0;
                    c.freeY = Config.FREE_Y_DEFAULT;
                    return -1;
                }))
            .build();
    }

    private static ConfigCategory tabs(Screen parent) {
        List<Config.TabConfig> tabs = ChatTabs.config().tabs;
        int open = expandedTab;
        expandedTab = -1;
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
            .name(Component.literal("Tabs"))
            .tooltip(Component.literal("The tabs in the bar, left to right. DM tabs open on their own after these."));
        for (int i = 0; i < tabs.size(); i++) {
            Config.TabConfig tab = tabs.get(i);
            int index = i;
            OptionGroup.Builder group = OptionGroup.createBuilder()
                .name(Component.literal(tab.name))
                .collapsed(i != open)
                .option(Option.<String>createBuilder()
                    .name(Component.literal("Name"))
                    .description(OptionDescription.of(Component.literal("Shown on the tab.")))
                    .binding("Tab", () -> tab.name, v -> tab.name = v)
                    .controller(StringControllerBuilder::create)
                    .build());
            for (Channel channel : Channel.values()) {
                group.option(Option.<Boolean>createBuilder()
                    .name(Component.literal("Show " + channel.label))
                    .description(OptionDescription.of(Component.literal(showDescription(channel))))
                    .binding(false, () -> tab.channels.contains(channel), v -> {
                        if (!v) tab.channels.remove(channel);
                        else if (!tab.channels.contains(channel)) tab.channels.add(channel);
                    })
                    .controller(TickBoxControllerBuilder::create)
                    .build());
            }
            group.option(Option.<Channel>createBuilder()
                    .name(Component.literal("Send to"))
                    .description(OptionDescription.of(Component.literal(
                        "Where chat typed in this tab goes. Commands you type (starting with /) are sent as they are.")))
                    .binding(Channel.EVERYTHING, () -> SEND_TO.contains(tab.sendTo) ? tab.sendTo : Channel.EVERYTHING,
                        v -> tab.sendTo = v)
                    .controller(opt -> CyclingListControllerBuilder.create(opt).values(SEND_TO)
                        .formatValue(c -> Component.literal(c.command == null ? "Normal chat" : c.label + " (/" + c.command + ")")))
                    .build())
                .option(button("Move left", "Move", "Moves this tab one place to the left.", parent, TABS_CATEGORY, () -> {
                    if (index == 0) return index;
                    Collections.swap(tabs, index, index - 1);
                    return index - 1;
                }))
                .option(button("Move right", "Move", "Moves this tab one place to the right.", parent, TABS_CATEGORY, () -> {
                    if (index == tabs.size() - 1) return index;
                    Collections.swap(tabs, index, index + 1);
                    return index + 1;
                }))
                .option(button("Delete tab", "Delete", "Removes this tab. The last tab can't be deleted.", parent,
                    TABS_CATEGORY, () -> {
                        if (tabs.size() > 1) tabs.remove(index);
                        return -1;
                    }));
            category.group(group.build());
        }
        category.group(OptionGroup.createBuilder()
            .name(Component.literal("New tab"))
            .option(button("Add tab", "Add", "Adds a tab showing every message, at the right end.", parent,
                TABS_CATEGORY, () -> {
                    tabs.add(new Config.TabConfig("Tab " + (tabs.size() + 1), List.of(Channel.EVERYTHING),
                        Channel.EVERYTHING));
                    return tabs.size() - 1;
                }))
            .option(button("Reset tabs", "Reset", "Puts back the default tabs: General, All, Guild, Party, DMs and "
                + "System.", parent, TABS_CATEGORY, () -> {
                    tabs.clear();
                    tabs.addAll(Config.defaultTabs());
                    return -1;
                }))
            .build());
        return category.build();
    }

    private static String showDescription(Channel channel) {
        return switch (channel) {
            case ALL_CHAT -> "Public chat from players (\"[598] [MVP+] Name: ...\").";
            case GUILD -> "Lines starting with \"Guild >\".";
            case OFFICER -> "Lines starting with \"Officer >\".";
            case PARTY -> "Lines starting with \"Party >\".";
            case COOP -> "Lines starting with \"Co-op >\" (SkyBlock co-op chat).";
            case DM -> "Messages from and to players (\"From ...\" and \"To ...\").";
            case SYSTEM -> "Everything that isn't one of the channels above: NPCs, server and game messages.";
            case EVERYTHING -> "Every message.";
        };
    }

    private static ConfigCategory appearance(Screen parent) {
        TabStyle d = new TabStyle();
        List<String> fonts = TabStyle.availableFonts();
        if (!fonts.contains(TabStyle.get().font)) fonts.add(TabStyle.get().font);
        // The preview draws a copy that follows every unsaved change; the real look only changes on Save.
        Look look = new Look(TabStyle.copy());
        return ConfigCategory.createBuilder()
            .name(Component.literal("Appearance"))
            .group(OptionGroup.createBuilder()
                .name(Component.literal("Colours"))
                .option(look.colour("Tab background", d.tabBackground, st -> st.tabBackground, (st, v) -> st.tabBackground = v))
                .option(look.colour("Tab border", d.tabBorder, st -> st.tabBorder, (st, v) -> st.tabBorder = v))
                .option(look.colour("Tab text", d.tabText, st -> st.tabText, (st, v) -> st.tabText = v))
                .option(look.colour("Hovered tab background", d.hoverBackground, st -> st.hoverBackground,
                    (st, v) -> st.hoverBackground = v))
                .option(look.colour("Open tab background", d.activeBackground, st -> st.activeBackground,
                    (st, v) -> st.activeBackground = v))
                .option(look.colour("Open tab border", d.activeBorder, st -> st.activeBorder, (st, v) -> st.activeBorder = v))
                .option(look.colour("Open tab text", d.activeText, st -> st.activeText, (st, v) -> st.activeText = v))
                .option(look.colour("Unread tab text", d.unreadText, st -> st.unreadText, (st, v) -> st.unreadText = v))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.literal("Sizes"))
                .option(look.slider("Tab height", "Height of the tabs in pixels.", 10, 20, d.tabHeight,
                    st -> st.tabHeight, (st, v) -> st.tabHeight = v))
                .option(look.slider("Padding", "Space between a tab's border and its name.", 1, 12, d.padding,
                    st -> st.padding, (st, v) -> st.padding = v))
                .option(look.slider("Gap", "Space between tabs.", 0, 8, d.gap, st -> st.gap, (st, v) -> st.gap = v))
                .option(look.slider("Border width", "0 draws no border.", 0, 3, d.borderWidth,
                    st -> st.borderWidth, (st, v) -> st.borderWidth = v))
                .option(look.slider("Corner radius", "0 draws square corners.", 0, 6, d.radius,
                    st -> st.radius, (st, v) -> st.radius = v))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.literal("Text"))
                .option(look.finish(Option.<String>createBuilder()
                        .name(Component.literal("Font"))
                        .controller(opt -> DropdownStringControllerBuilder.create(opt).values(fonts)),
                    "Fonts from your resource packs. Unknown fonts fall back to Minecraft's.",
                    d.font, st -> st.font, (st, v) -> st.font = v))
                .option(look.toggle("Text shadow", "Draw tab names with a shadow.", d.textShadow,
                    st -> st.textShadow, (st, v) -> st.textShadow = v))
                .option(look.toggle("Bold open tab", "Draw the open tab's name in bold.", d.boldActive,
                    st -> st.boldActive, (st, v) -> st.boldActive = v))
                .build())
            .option(button("Reset appearance", "Reset", "Puts every Appearance setting back to its default.",
                parent, APPEARANCE_CATEGORY, () -> {
                    TabStyle.reset();
                    return -1;
                }))
            .build();
    }

    /**
     * A button that changes the tab list or look. It keeps what is on screen (applies every unsaved option first, as
     * Save would), runs {@code action}, saves, and rebuilds the screen on {@code category}. The action returns the
     * tab section to leave open, or -1.
     */
    private static ButtonOption button(String name, String text, String description, Screen parent, int category,
                                       IntSupplier action) {
        return ButtonOption.createBuilder()
            .name(Component.literal(name))
            .text(Component.literal(text))
            .description(OptionDescription.of(Component.literal(description)))
            .action((screen, button) -> {
                for (ConfigCategory c : screen.config.categories()) {
                    for (OptionGroup g : c.groups()) {
                        for (Option<?> o : g.options()) o.applyValue();
                    }
                }
                expandedTab = action.getAsInt();
                ChatTabs.saveConfig();
                TabStyle.save();
                Screen rebuilt = create(parent);
                Minecraft.getInstance().gui.setScreen(rebuilt);
                // setScreen ran init(), which made the category bar.
                if (rebuilt instanceof YACLScreen yacl) yacl.tabNavigationBar.selectTab(category, false);
            })
            .build();
    }

    private static Option<Boolean> toggle(String name, String description, boolean def, Supplier<Boolean> getter,
                                          Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
            .name(Component.literal(name))
            .description(OptionDescription.of(Component.literal(description)))
            .binding(def, getter, setter)
            .controller(opt -> BooleanControllerBuilder.create(opt).onOffFormatter().coloured(true))
            .build();
    }

    private static Option<Integer> slider(String name, String description, int min, int max, int def,
                                          Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
            .name(Component.literal(name))
            .description(OptionDescription.of(Component.literal(description)))
            .binding(def, getter, setter)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(1))
            .build();
    }

    /**
     * Builds the Appearance options. Each is bound to the saved look (written on Save), copies every unsaved change
     * to {@code draft}, and shows a preview of tabs drawn with the draft above its description.
     */
    private static final class Look {
        private final TabStyle draft;
        private final ImageRenderer preview;

        Look(TabStyle draft) {
            this.draft = draft;
            this.preview = new ImageRenderer() {
                @Override
                public int render(GuiGraphicsExtractor graphics, int x, int y, int width, float delta) {
                    return drawPreview(graphics, x, y, width, draft);
                }

                @Override
                public void close() {
                }
            };
        }

        <T> Option<T> finish(Option.Builder<T> builder, String description, T def, Function<TabStyle, T> get,
                             BiConsumer<TabStyle, T> set) {
            TabStyle saved = TabStyle.get();
            return builder
                .description(OptionDescription.createBuilder().text(Component.literal(description)).customImage(preview).build())
                .binding(def, () -> get.apply(saved), v -> set.accept(saved, v))
                .addListener((option, event) -> {
                    if (event == OptionEventListener.Event.STATE_CHANGE) set.accept(draft, option.pendingValue());
                })
                .build();
        }

        Option<Color> colour(String name, int def, ToIntFunction<TabStyle> get, ObjIntConsumer<TabStyle> set) {
            return finish(Option.<Color>createBuilder()
                    .name(Component.literal(name))
                    .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)),
                "Colour and transparency of: " + name.toLowerCase(Locale.ROOT) + ".",
                new Color(def, true), st -> new Color(get.applyAsInt(st), true), (st, c) -> set.accept(st, c.getRGB()));
        }

        Option<Integer> slider(String name, String description, int min, int max, int def,
                               Function<TabStyle, Integer> get, BiConsumer<TabStyle, Integer> set) {
            return finish(Option.<Integer>createBuilder()
                    .name(Component.literal(name))
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(1)),
                description, def, get, set);
        }

        Option<Boolean> toggle(String name, String description, boolean def, Function<TabStyle, Boolean> get,
                               BiConsumer<TabStyle, Boolean> set) {
            return finish(Option.<Boolean>createBuilder()
                    .name(Component.literal(name))
                    .controller(opt -> BooleanControllerBuilder.create(opt).onOffFormatter().coloured(true)),
                description, def, get, set);
        }
    }

    /** Example tabs, one in each state (normal, open, unread, hovered), on a chat-like background. */
    private static int drawPreview(GuiGraphicsExtractor graphics, int x, int y, int width, TabStyle style) {
        Font font = Minecraft.getInstance().font;
        int height = style.tabHeight + 8;
        graphics.fill(x, y, x + width, y + height, 0x80000000);
        Component[] labels = {
            style.text("General", style.tabText, false),
            style.text("All", style.activeText, style.boldActive),
            style.text("Guild (3)", style.unreadText, false),
            style.text("Party", style.tabText, false),
        };
        int tabX = x + 4;
        for (int i = 0; i < labels.length; i++) {
            int w = font.width(labels[i]) + 2 * style.padding;
            if (tabX + w > x + width) break;
            TabBar.drawTab(graphics, font, style, tabX, y + 4, w, labels[i], i == 1, i == 3);
            tabX += w + style.gap;
        }
        return height;
    }
}
