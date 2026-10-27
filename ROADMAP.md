# ChatTabs roadmap

## Done (0.1.0)
- Tabs filtering the vanilla chat by channel: Guild, Officer, Party, Co-op, DMs, System, All; a message shows in
  every tab that matches.
- Send channel per tab as a prefixed command (`/gc`, `/oc`, `/pc`, `/cc`, `/r`; DM tabs `/msg <name>`).
- Auto DM tabs per conversation (capped, oldest closes), right click to close; unread counts.
- Settings (YACL): General, Tabs (add, delete, move, channels, send to), Appearance (Shopping List style system:
  colours, sizes, border, radius, font, shadow).
- Builds for 26.2 and 26.3; unit tests for the classifier; the dev client starts with `ChatComponentMixin` applied.
- 0.1.1: All chat channel (public player chat, `/ac`) split from System; old "All" is now Everything (0.1.0 files
  read `ALL` as `EVERYTHING`). Default tabs General, All, Guild, Party, DMs, System.
- 0.1.2: the Tabs settings buttons keep unsaved edits, the category and the edited tab's section open.
- 0.2.0: the tab bar follows the chat (`ChatFrame` reads chat's pose), positions Top / Bottom / Free with drag
  and snapping in Free.
- 0.2.1: live preview in Settings > Appearance (draft style copy, as in CraftingTracker).
- 0.2.2: Settings text in a lang file (`assets/chat-tabs/lang/en_us.json`).
- 0.2.3: the tab bar shows on the HUD while chat is closed (Settings > General > Tabs while chat is closed, on by
  default): a Fabric HUD layer after vanilla's chat. `ChatFrame` now records closed chat too and always uses the
  open chat's height, so the bar doesn't move when chat opens.
- 0.2.6: the tab bar draws at chat's scale (vanilla Chat Scale, Huddable chat scale and sizing), open or closed.
  The closed bar is drawn with vanilla's CHAT HUD element instead of its own `chat-tabs:tab_bar` layer, which
  Huddable scaled a second time (bar changed size when chat opened with Huddable sizing Window).
- 0.2.5: Lock tab bar (Settings > General, off by default): a locked Free bar only opens tabs, never drags.
- 0.2.7: the open tab stays open when tabs are deleted, moved or edited in Settings (it was kept by position, so
  typed chat could go to another tab's channel). Found by its settings entry (`Tab.find`, `TabTest`).
- 0.2.8: the chat box length is vanilla's 256 less the open tab's prefix (`/gc `, `/msg <name> `), so a sent
  command is never longer than a typed one (`ChatTabs.inputLimit`, set from `TabBar` when it changes).
- 0.2.9: a new DM no longer opens a tab past the limit when the only DM tab that could close is the open one.

## Untested in game
- Everything on Hypixel. Formats not seen in the live logs yet: `Officer >`, `Party >`, `Co-op >`, `From`/`To` DMs
  (only `Guild >` and `From stash:` were). Commands `/ac`, `/oc` and `/cc` to confirm.
- All chat: SkyBlock lines (level, ranks VIP to MVP++, unranked) are from the logs; lobby lines (no level) and
  staff ranks are not. A player-chat line that lands in System means the rank list or pattern needs a case.
- Tab bar following chat moved or scaled by Huddable; Top / Bottom / Free placement and the Free drag and snap.
- The Tabs settings buttons (category kept, section left open, unsaved edits kept).
- Known: when the open tab has no messages, vanilla skips drawing chat, so the bar uses the last known chat box
  (vanilla's spot until chat has been drawn once).
- Together with compacting (`(x2)` merging across tab switches), morechathistory (long history) and
  beautifiedchatclient (does it change the prefixes?).
- `ChatScreenMixin` applies when the chat screen first opens; not yet exercised.
- 0.2.6 bar at chat's scale: same size open and closed with Huddable sizing Window; clicks and the Free drag when
  scaled. Top placement with a short unfocused chat height.
- 26.3 in a real client.

## Next
- Appearance presets and share codes, as in Shopping List's `HudPresets`.
- Style the chat background per tab.
- Custom regex filters per tab.
- Stretch: detachable, resizable chat windows (each tab drawn as its own panel).
