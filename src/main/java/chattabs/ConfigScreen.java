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
import net.minecraft.client.resources.language.I18n;
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
            .name(Component.translatable("chat-tabs.config.general"))
            .option(toggle("chat-tabs.config.general.enabled", d.enabled, () -> c.enabled, v -> c.enabled = v))
            .option(toggle("chat-tabs.config.general.dm_tabs", d.whisperTabs, () -> c.whisperTabs,
                v -> c.whisperTabs = v))
            .option(slider("chat-tabs.config.general.max_dm_tabs", 1, 20, d.maxWhisperTabs, () -> c.maxWhisperTabs,
                v -> c.maxWhisperTabs = v))
            .option(toggle("chat-tabs.config.general.unread", d.showUnread, () -> c.showUnread, v -> c.showUnread = v))
            .option(Option.<Config.BarPosition>createBuilder()
                .name(Component.translatable("chat-tabs.config.general.bar_position"))
                .description(OptionDescription.of(Component.translatable("chat-tabs.config.general.bar_position.desc")))
                .binding(d.barPosition, () -> c.barPosition, v -> c.barPosition = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(Config.BarPosition.class)
                    .formatValue(p -> Component.translatable(switch (p) {
                        case TOP -> "chat-tabs.config.general.bar_position.top";
                        case BOTTOM -> "chat-tabs.config.general.bar_position.bottom";
                        case FREE -> "chat-tabs.config.general.bar_position.free";
                    })))
                .build())
            .option(button("chat-tabs.config.general.reset_free", parent, GENERAL_CATEGORY, () -> {
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
            .name(Component.translatable("chat-tabs.config.tabs"))
            .tooltip(Component.translatable("chat-tabs.config.tabs.desc"));
        for (int i = 0; i < tabs.size(); i++) {
            Config.TabConfig tab = tabs.get(i);
            int index = i;
            OptionGroup.Builder group = OptionGroup.createBuilder()
                .name(Component.literal(tab.name))
                .collapsed(i != open)
                .option(Option.<String>createBuilder()
                    .name(Component.translatable("chat-tabs.config.tabs.name"))
                    .description(OptionDescription.of(Component.translatable("chat-tabs.config.tabs.name.desc")))
                    .binding("Tab", () -> tab.name, v -> tab.name = v)
                    .controller(StringControllerBuilder::create)
                    .build());
            for (Channel channel : Channel.values()) {
                group.option(Option.<Boolean>createBuilder()
                    .name(Component.translatable("chat-tabs.config.tabs.show", channel.label()))
                    .description(OptionDescription.of(Component.translatable(showDescription(channel))))
                    .binding(false, () -> tab.channels.contains(channel), v -> {
                        if (!v) tab.channels.remove(channel);
                        else if (!tab.channels.contains(channel)) tab.channels.add(channel);
                    })
                    .controller(TickBoxControllerBuilder::create)
                    .build());
            }
            group.option(Option.<Channel>createBuilder()
                    .name(Component.translatable("chat-tabs.config.tabs.send_to"))
                    .description(OptionDescription.of(Component.translatable("chat-tabs.config.tabs.send_to.desc")))
                    .binding(Channel.EVERYTHING, () -> SEND_TO.contains(tab.sendTo) ? tab.sendTo : Channel.EVERYTHING,
                        v -> tab.sendTo = v)
                    .controller(opt -> CyclingListControllerBuilder.create(opt).values(SEND_TO)
                        .formatValue(c -> c.command == null
                            ? Component.translatable("chat-tabs.config.tabs.send_to.normal")
                            : Component.translatable("chat-tabs.config.tabs.send_to.channel", c.label(), c.command)))
                    .build())
                .option(button("chat-tabs.config.tabs.move_left", parent, TABS_CATEGORY, () -> {
                    if (index == 0) return index;
                    Collections.swap(tabs, index, index - 1);
                    return index - 1;
                }))
                .option(button("chat-tabs.config.tabs.move_right", parent, TABS_CATEGORY, () -> {
                    if (index == tabs.size() - 1) return index;
                    Collections.swap(tabs, index, index + 1);
                    return index + 1;
                }))
                .option(button("chat-tabs.config.tabs.delete", parent, TABS_CATEGORY, () -> {
                        if (tabs.size() > 1) tabs.remove(index);
                        return -1;
                    }));
            category.group(group.build());
        }
        category.group(OptionGroup.createBuilder()
            .name(Component.translatable("chat-tabs.config.tabs.new"))
            .option(button("chat-tabs.config.tabs.add", parent, TABS_CATEGORY, () -> {
                    tabs.add(new Config.TabConfig("Tab " + (tabs.size() + 1), List.of(Channel.EVERYTHING),
                        Channel.EVERYTHING));
                    return tabs.size() - 1;
                }))
            .option(button("chat-tabs.config.tabs.reset", parent, TABS_CATEGORY, () -> {
                    tabs.clear();
                    tabs.addAll(Config.defaultTabs());
                    return -1;
                }))
            .build());
        return category.build();
    }

    /** The lang key of a channel's Show option description. */
    private static String showDescription(Channel channel) {
        return switch (channel) {
            case ALL_CHAT -> "chat-tabs.config.tabs.show.all_chat.desc";
            case GUILD -> "chat-tabs.config.tabs.show.guild.desc";
            case OFFICER -> "chat-tabs.config.tabs.show.officer.desc";
            case PARTY -> "chat-tabs.config.tabs.show.party.desc";
            case COOP -> "chat-tabs.config.tabs.show.coop.desc";
            case DM -> "chat-tabs.config.tabs.show.dm.desc";
            case SYSTEM -> "chat-tabs.config.tabs.show.system.desc";
            case EVERYTHING -> "chat-tabs.config.tabs.show.everything.desc";
        };
    }

    private static ConfigCategory appearance(Screen parent) {
        TabStyle d = new TabStyle();
        List<String> fonts = TabStyle.availableFonts();
        if (!fonts.contains(TabStyle.get().font)) fonts.add(TabStyle.get().font);
        // The preview draws a copy that follows every unsaved change; the real look only changes on Save.
        Look look = new Look(TabStyle.copy());
        return ConfigCategory.createBuilder()
            .name(Component.translatable("chat-tabs.config.appearance"))
            .group(OptionGroup.createBuilder()
                .name(Component.translatable("chat-tabs.config.appearance.colours"))
                .option(look.colour("chat-tabs.config.appearance.tab_background", d.tabBackground,
                    st -> st.tabBackground, (st, v) -> st.tabBackground = v))
                .option(look.colour("chat-tabs.config.appearance.tab_border", d.tabBorder, st -> st.tabBorder,
                    (st, v) -> st.tabBorder = v))
                .option(look.colour("chat-tabs.config.appearance.tab_text", d.tabText, st -> st.tabText,
                    (st, v) -> st.tabText = v))
                .option(look.colour("chat-tabs.config.appearance.hover_background", d.hoverBackground,
                    st -> st.hoverBackground, (st, v) -> st.hoverBackground = v))
                .option(look.colour("chat-tabs.config.appearance.active_background", d.activeBackground,
                    st -> st.activeBackground, (st, v) -> st.activeBackground = v))
                .option(look.colour("chat-tabs.config.appearance.active_border", d.activeBorder,
                    st -> st.activeBorder, (st, v) -> st.activeBorder = v))
                .option(look.colour("chat-tabs.config.appearance.active_text", d.activeText, st -> st.activeText,
                    (st, v) -> st.activeText = v))
                .option(look.colour("chat-tabs.config.appearance.unread_text", d.unreadText, st -> st.unreadText,
                    (st, v) -> st.unreadText = v))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.translatable("chat-tabs.config.appearance.sizes"))
                .option(look.slider("chat-tabs.config.appearance.tab_height", 10, 20, d.tabHeight,
                    st -> st.tabHeight, (st, v) -> st.tabHeight = v))
                .option(look.slider("chat-tabs.config.appearance.padding", 1, 12, d.padding,
                    st -> st.padding, (st, v) -> st.padding = v))
                .option(look.slider("chat-tabs.config.appearance.gap", 0, 8, d.gap, st -> st.gap,
                    (st, v) -> st.gap = v))
                .option(look.slider("chat-tabs.config.appearance.border_width", 0, 3, d.borderWidth,
                    st -> st.borderWidth, (st, v) -> st.borderWidth = v))
                .option(look.slider("chat-tabs.config.appearance.radius", 0, 6, d.radius,
                    st -> st.radius, (st, v) -> st.radius = v))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.translatable("chat-tabs.config.appearance.text"))
                .option(look.finish(Option.<String>createBuilder()
                        .name(Component.translatable("chat-tabs.config.appearance.font"))
                        .controller(opt -> DropdownStringControllerBuilder.create(opt).values(fonts)),
                    Component.translatable("chat-tabs.config.appearance.font.desc"),
                    d.font, st -> st.font, (st, v) -> st.font = v))
                .option(look.toggle("chat-tabs.config.appearance.text_shadow", d.textShadow,
                    st -> st.textShadow, (st, v) -> st.textShadow = v))
                .option(look.toggle("chat-tabs.config.appearance.bold_active", d.boldActive,
                    st -> st.boldActive, (st, v) -> st.boldActive = v))
                .build())
            .option(button("chat-tabs.config.appearance.reset", parent, APPEARANCE_CATEGORY, () -> {
                    TabStyle.reset();
                    return -1;
                }))
            .build();
    }

    /**
     * A button that changes the tab list or look. It keeps what is on screen (applies every unsaved option first, as
     * Save would), runs {@code action}, saves, and rebuilds the screen on {@code category}. The action returns the
     * tab section to leave open, or -1. {@code key} is the name's lang key; the button text and the description
     * are {@code key.button} and {@code key.desc}.
     */
    private static ButtonOption button(String key, Screen parent, int category, IntSupplier action) {
        return ButtonOption.createBuilder()
            .name(Component.translatable(key))
            .text(Component.translatable(key + ".button"))
            .description(OptionDescription.of(Component.translatable(key + ".desc")))
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

    /** A switch named by lang key {@code key} and described by {@code key.desc}. */
    private static Option<Boolean> toggle(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable(key))
            .description(OptionDescription.of(Component.translatable(key + ".desc")))
            .binding(def, getter, setter)
            .controller(opt -> BooleanControllerBuilder.create(opt).onOffFormatter().coloured(true))
            .build();
    }

    /** A slider named by lang key {@code key} and described by {@code key.desc}. */
    private static Option<Integer> slider(String key, int min, int max, int def, Supplier<Integer> getter,
                                          Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
            .name(Component.translatable(key))
            .description(OptionDescription.of(Component.translatable(key + ".desc")))
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

        <T> Option<T> finish(Option.Builder<T> builder, Component description, T def, Function<TabStyle, T> get,
                             BiConsumer<TabStyle, T> set) {
            TabStyle saved = TabStyle.get();
            return builder
                .description(OptionDescription.createBuilder().text(description).customImage(preview).build())
                .binding(def, () -> get.apply(saved), v -> set.accept(saved, v))
                .addListener((option, event) -> {
                    if (event == OptionEventListener.Event.STATE_CHANGE) set.accept(draft, option.pendingValue());
                })
                .build();
        }

        Option<Color> colour(String key, int def, ToIntFunction<TabStyle> get, ObjIntConsumer<TabStyle> set) {
            return finish(Option.<Color>createBuilder()
                    .name(Component.translatable(key))
                    .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)),
                Component.translatable("chat-tabs.config.appearance.colour.desc",
                    I18n.get(key).toLowerCase(Locale.ROOT)),
                new Color(def, true), st -> new Color(get.applyAsInt(st), true), (st, c) -> set.accept(st, c.getRGB()));
        }

        Option<Integer> slider(String key, int min, int max, int def, Function<TabStyle, Integer> get,
                               BiConsumer<TabStyle, Integer> set) {
            return finish(Option.<Integer>createBuilder()
                    .name(Component.translatable(key))
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(1)),
                Component.translatable(key + ".desc"), def, get, set);
        }

        Option<Boolean> toggle(String key, boolean def, Function<TabStyle, Boolean> get,
                               BiConsumer<TabStyle, Boolean> set) {
            return finish(Option.<Boolean>createBuilder()
                    .name(Component.translatable(key))
                    .controller(opt -> BooleanControllerBuilder.create(opt).onOffFormatter().coloured(true)),
                Component.translatable(key + ".desc"), def, get, set);
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
