# ChatTabs

WoW-style chat tabs for Hypixel. A row of tabs sits above the chat input; each tab shows only the channels you
pick, typing in a tab sends to its channel, DMs get their own tabs and the other tabs count unread messages.
Client-side. Only on Hypixel: other servers get normal chat.

Requires Fabric API and [YetAnotherConfigLib](https://modrinth.com/mod/yacl). Settings open from
[Mod Menu](https://modrinth.com/mod/modmenu)'s Configure button.

Minecraft 26.2 and 26.3 (one jar per version).

## Tabs
Open chat (T) and click a tab. The chat shows that tab's messages, history included. Default tabs:

| Tab | Shows | Typing sends |
|---|---|---|
| General | every message | normal chat |
| All | public player chat | `/ac` |
| Guild | Guild and Officer | `/gc` |
| Party | Party | `/pc` |
| DMs | every DM | `/r` (reply) |
| System | NPCs, server and game messages | normal chat |

A message shows in every tab that matches it. Channels are read from Hypixel's prefixes:

- **All chat** players talking in public: `[598] [MVP+] Name: ...` on SkyBlock, `[VIP] Name: ...` in lobbies.
  `[NPC]`, `[BOSS]` and similar speakers aren't players, so they go to System.
- **Guild** `Guild > ...`, **Officer** `Officer > ...`, **Party** `Party > ...`, **Co-op** `Co-op > ...`
- **DMs** `From <player>: ...` and `To <player>: ...`
- **System** everything else (NPCs, server and game messages)
- **Everything** every message

Commands you type (starting with `/`) are always sent as they are, and the tab bar steps aside while you type one
so the suggestions stay readable.

## DM tabs
When you message a player or one messages you, a tab with their name opens. Typing there sends `/msg <name>`.
Right click a DM tab to close it. The oldest closes when the limit is reached (5 by default). DM tabs close when
you leave the server.

## Unread counts
A tab you aren't looking at shows how many new messages it has, e.g. `Guild (3)`, in the unread colour.

## Settings
- **General:** turn tabs on or off, DM tabs and their limit, unread counts.
- **Tabs:** one section per tab: name, which channels it shows, where typing sends, move left or right, delete.
  Add tab and Reset tabs at the bottom. These buttons save at once, along with anything else you changed.
- **Appearance:** colours (background, border and text for normal, hovered, open and unread tabs), tab height,
  padding, gap, border width, corner radius, font (any font from your resource packs), text shadow and bold open
  tab. The defaults are Skyblock Shopping List's look.

Files: `config/chat-tabs/tabs.json` and `style.json`.

## Compatibility
Every message stays in the normal chat history; tabs only choose what is drawn, so mods that change chat history
(Compacting, More Chat History) keep working. Beautified Chat Client and No Chat Restrictions don't touch the same
code.

## Hypixel rules
Read-only apart from what you type: a message typed in a channel tab is sent as one command (`/gc hi`), only when
you press Enter. Nothing is sent when you click a tab.
