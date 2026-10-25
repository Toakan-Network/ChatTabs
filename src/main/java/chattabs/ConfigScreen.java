package chattabs;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The Settings screen, built with YetAnotherConfigLib. Options write into {@link ChatTabs#config()} and
 * {@link TabStyle#get()}, and Save writes both files. The tab buttons (add, move, delete) save straight away.
 */
public final class ConfigScreen {
    /** Where a tab can send typed chat; System and Everything both mean normal chat, so only Everything is offered. */
    private static final List<Channel> SEND_TO = List.of(Channel.EVERYTHING, Channel.ALL_CHAT, Channel.GUILD,
        Channel.OFFICER, Channel.PARTY, Channel.COOP, Channel.DM);

    private ConfigScreen() {}

    public static Screen create(Screen parent) {
        Runnable reopen = () -> Minecraft.getInstance().gui.setScreen(create(parent));
        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal(ChatTabsClient.NAME))
            .category(general())
            .category(tabs(reopen))
            .category(appearance(reopen))
            .save(() -> {
                ChatTabs.saveConfig();
                TabStyle.save();
            })
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory general() {
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
            .build();
    }

    private static ConfigCategory tabs(Runnable reopen) {
        List<Config.TabConfig> tabs = ChatTabs.config().tabs;
        String saveFirst = " Press Save first if you have unsaved changes.";
        ConfigCategory.Builder category = ConfigCategory.createBuilder()
            .name(Component.literal("Tabs"))
            .tooltip(Component.literal("The tabs in the bar, left to right. DM tabs open on their own after these."));
        for (int i = 0; i < tabs.size(); i++) {
            Config.TabConfig tab = tabs.get(i);
            int index = i;
            OptionGroup.Builder group = OptionGroup.createBuilder()
                .name(Component.literal(tab.name))
                .collapsed(true)
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
                .option(button("Move left", "Move", "Moves this tab one place to the left." + saveFirst, () -> {
                    if (index > 0) Collections.swap(tabs, index, index - 1);
                }, reopen))
                .option(button("Move right", "Move", "Moves this tab one place to the right." + saveFirst, () -> {
                    if (index < tabs.size() - 1) Collections.swap(tabs, index, index + 1);
                }, reopen))
                .option(button("Delete tab", "Delete", "Removes this tab. The last tab can't be deleted." + saveFirst, () -> {
                    if (tabs.size() > 1) tabs.remove(index);
                }, reopen));
            category.group(group.build());
        }
        category.group(OptionGroup.createBuilder()
            .name(Component.literal("New tab"))
            .option(button("Add tab", "Add", "Adds a tab showing every message, at the right end." + saveFirst,
                () -> tabs.add(new Config.TabConfig("Tab " + (tabs.size() + 1), List.of(Channel.EVERYTHING),
                    Channel.EVERYTHING)), reopen))
            .option(button("Reset tabs", "Reset", "Puts back the default tabs: General, All, Guild, Party, DMs and "
                + "System." + saveFirst, () -> {
                tabs.clear();
                tabs.addAll(Config.defaultTabs());
            }, reopen))
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

    private static ConfigCategory appearance(Runnable reopen) {
        TabStyle s = TabStyle.get();
        TabStyle d = new TabStyle();
        List<String> fonts = TabStyle.availableFonts();
        if (!fonts.contains(s.font)) fonts.add(s.font);
        return ConfigCategory.createBuilder()
            .name(Component.literal("Appearance"))
            .group(OptionGroup.createBuilder()
                .name(Component.literal("Colours"))
                .option(colour("Tab background", d.tabBackground, () -> s.tabBackground, v -> s.tabBackground = v))
                .option(colour("Tab border", d.tabBorder, () -> s.tabBorder, v -> s.tabBorder = v))
                .option(colour("Tab text", d.tabText, () -> s.tabText, v -> s.tabText = v))
                .option(colour("Hovered tab background", d.hoverBackground, () -> s.hoverBackground, v -> s.hoverBackground = v))
                .option(colour("Open tab background", d.activeBackground, () -> s.activeBackground, v -> s.activeBackground = v))
                .option(colour("Open tab border", d.activeBorder, () -> s.activeBorder, v -> s.activeBorder = v))
                .option(colour("Open tab text", d.activeText, () -> s.activeText, v -> s.activeText = v))
                .option(colour("Unread tab text", d.unreadText, () -> s.unreadText, v -> s.unreadText = v))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.literal("Sizes"))
                .option(slider("Tab height", "Height of the tabs in pixels.", 10, 20, d.tabHeight,
                    () -> s.tabHeight, v -> s.tabHeight = v))
                .option(slider("Padding", "Space between a tab's border and its name.", 1, 12, d.padding,
                    () -> s.padding, v -> s.padding = v))
                .option(slider("Gap", "Space between tabs.", 0, 8, d.gap, () -> s.gap, v -> s.gap = v))
                .option(slider("Border width", "0 draws no border.", 0, 3, d.borderWidth,
                    () -> s.borderWidth, v -> s.borderWidth = v))
                .option(slider("Corner radius", "0 draws square corners.", 0, 6, d.radius,
                    () -> s.radius, v -> s.radius = v))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.literal("Text"))
                .option(Option.<String>createBuilder()
                    .name(Component.literal("Font"))
                    .description(OptionDescription.of(Component.literal(
                        "Fonts from your resource packs. Unknown fonts fall back to Minecraft's.")))
                    .binding(d.font, () -> s.font, v -> s.font = v)
                    .controller(opt -> DropdownStringControllerBuilder.create(opt).values(fonts))
                    .build())
                .option(toggle("Text shadow", "Draw tab names with a shadow.", d.textShadow,
                    () -> s.textShadow, v -> s.textShadow = v))
                .option(toggle("Bold open tab", "Draw the open tab's name in bold.", d.boldActive,
                    () -> s.boldActive, v -> s.boldActive = v))
                .build())
            .option(button("Reset appearance", "Reset", "Puts every Appearance setting back to its default.",
                TabStyle::reset, reopen))
            .build();
    }

    /** A button that changes something, saves the tabs and reopens the screen to show the result. */
    private static ButtonOption button(String name, String text, String description, Runnable action, Runnable reopen) {
        return ButtonOption.createBuilder()
            .name(Component.literal(name))
            .text(Component.literal(text))
            .description(OptionDescription.of(Component.literal(description)))
            .action((screen, button) -> {
                action.run();
                ChatTabs.saveConfig();
                reopen.run();
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

    private static Option<Color> colour(String name, int def, IntSupplier getter, IntConsumer setter) {
        return Option.<Color>createBuilder()
            .name(Component.literal(name))
            .description(OptionDescription.of(Component.literal(
                "Colour and transparency of: " + name.toLowerCase(Locale.ROOT) + ".")))
            .binding(new Color(def, true), () -> new Color(getter.getAsInt(), true), c -> setter.accept(c.getRGB()))
            .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
            .build();
    }
}
