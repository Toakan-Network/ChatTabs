# ChatTabs

Fabric client mod (mod id `chat-tabs`, Java package `uk.toakan.tad.chattabs`): WoW-style chat tabs for Hypixel. Tabs filter the
vanilla chat by channel, typing in a tab sends to its channel, DMs get their own tabs, inactive tabs count unread
messages. Loom 1.17, Java 25.

Shared rules (commits, versioning, resource light) are in the parent `Games\Minecraft\CLAUDE.md`. This file only
has what differs. Progress, next steps and known issues are in `ROADMAP.md`: read it before continuing, and
update it in the same commit when a step lands or an issue is found or fixed.

## Hypixel rule exceptions (agreed with the user)
- Runs on all of Hypixel (lobbies and every game), not only SkyBlock. Off on other servers and when disabled:
  every hook passes straight through (`ChatTabs.isActive()`).
- Typed chat in a channel tab is sent as one prefixed command (`/gc <msg>`, `/msg <name> <msg>`) per Enter the
  player pressed. Never send anything on a tab click or on its own; no `/chat <channel>` switching.

## Design
- Every message stays in vanilla's `allMessages`; only the drawn lines are filtered. `ChatComponentMixin` wraps the
  two `addMessageToDisplayQueue` calls (in `addMessage` and `refreshTrimmedMessages`); a tab switch calls
  `rescaleChat()`. Don't use vanilla's `setVisibleMessageFilter`: it drops failing messages from history, and
  `LocalPlayer` owns it for chat restrictions.
- Compatible with compacting (wraps NEW GuiMessage/Line and injects at the same refresh INVOKE), morechathistory
  (raises the 100 constants), beautifiedchatclient and NoChatRestrictions (no ChatComponent hooks). Keep to
  `@WrapOperation` so these stack.
- `ChatScreenMixin` wraps `sendChat` in `ChatScreen.handleChatInput` only, so other mods' sends aren't rerouted.
- Classification is `MessageClassifier` on the plain text with `§` codes stripped (Hypixel sends them in the text).
  `From stash:` is a SkyBlock notice, not a DM. All chat needs a SkyBlock level, a player rank from `RANKS`, or
  the gray unranked lobby form; `[NPC]`, `[FEAR]` and the like stay System. Unit tested; add a case for every new
  format.
- Tab bar: Fabric `ScreenEvents.afterExtract` and `ScreenMouseEvents` (click, drag, release) on `ChatScreen`.
  It sticks to the chat box from `ChatFrame`, which `ChatComponentMixin` records from the pose right after vanilla's
  one `updatePose` call in `extractRenderState`. Any chat mover (Huddable) changes the pose before that, so no
  Huddable code or dependency is needed. The bar draws at chat's pose scale (`ChatFrame.Box.scale`), so it keeps
  chat's size. The closed-chat bar is drawn inside vanilla's CHAT element (`replaceElement`), not a layer of its own:
  Huddable would wrap a separate layer and scale it a second time. Top / Bottom / Free (`Config.barPosition`, Free
  is an offset from the chat's top-left, snapping via `PanelSnap` copied from Huddable). Bottom hides while a `/`
  command is typed.
- Look: `TabStyle` (style.json) is Skyblock Shopping List's `HudStyle` cut down to the tab bar, with its palette as
  defaults, and `RoundedBox` is copied from it. Keep option names and clamps in step with Shopping List.
- Dependencies: Fabric API and YACL (Settings). Mod Menu optional; only `compat.ModMenuCompat` touches it.

## Builds: one jar per Minecraft version
As in Huddable: `./gradlew build` builds `:mc26.2` and `:mc26.3` from the one `src/`. Jars land in the root
`build/libs`, named `chat-tabs-<mod_version>+<mc>.jar`. No `org.lwjgl.glfw`; read mouse codes with
`ChatTabsClient.inputCode("MOUSE_BUTTON_LEFT")`.

## Testing in game
The dev client isn't on Hypixel, so tabs stay hidden there. Test in the live instance
(`G:\Games\Minecraft\Instances\Hypixel 26.2\`). Config is `config\chat-tabs\` (`tabs.json`, `style.json`).
