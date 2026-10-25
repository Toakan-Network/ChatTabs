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

## Untested in game
- Everything on Hypixel. Formats not seen in the live logs yet: `Officer >`, `Party >`, `Co-op >`, `From`/`To` DMs
  (only `Guild >` and `From stash:` were). Commands `/ac`, `/oc` and `/cc` to confirm.
- All chat: SkyBlock lines (level, ranks VIP to MVP++, unranked) are from the logs; lobby lines (no level) and
  staff ranks are not. A player-chat line that lands in System means the rank list or pattern needs a case.
- With Huddable moving chat, the tab bar stays above the input box rather than following the chat.
- Together with compacting (`(x2)` merging across tab switches), morechathistory (long history) and
  beautifiedchatclient (does it change the prefixes?).
- `ChatScreenMixin` applies when the chat screen first opens; not yet exercised.
- 26.3 in a real client.

## Next
- Appearance presets and share codes, as in Shopping List's `HudPresets`.
- Style the chat background per tab.
- Optional tab bar while chat is closed, so unread counts show.
- Custom regex filters per tab.
- Stretch: detachable, resizable chat windows (each tab drawn as its own panel).
