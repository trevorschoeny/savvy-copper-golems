# Savvy Copper Golems

Savvy Copper Golems lets copper golems sort into the shulker boxes stored in your chests. A golem treats each shulker box in a chest as a chest of its own, so a box holding cobblestone gets the cobblestone. Golems also sort into barrels and into shulker boxes placed as blocks.

It only needs to be on the server, so players join with an unmodded game. In singleplayer, your game is the server.

### Features

#### Shulker boxes in chests

- A golem looks inside the shulker boxes stored in a chest, and puts its item in a box that already holds that item.
- It goes through the chest slot by slot, so whichever comes first, a loose stack or a box, gets the item.
- A full box passes the item on, and an empty box takes anything, the same as an empty chest.
- A golem never puts a shulker box inside another shulker box.
- This is on by default, and can be turned off. Off, a stored shulker box is just another item, and chests and barrels follow vanilla's own rule.

#### More places to sort

- Golems sort into barrels, including the shulker boxes stored in them.
- Golems sort into shulker boxes placed as blocks.
- Both are on by default, and each can be turned off.

#### How much golems carry

- How many items a golem takes from a copper chest at once can be raised past vanilla's 16, up to a full stack.

#### How far golems look

- How many containers a golem checks, and how far it looks, can be raised or lowered. Vanilla's numbers are the defaults.

#### Crammed golems

- Copper golems packed into one block stay stacked instead of pushing apart when one is nudged. On by default.

### Requirements

- [Fabric API](https://modrinth.com/mod/fabric-api)

### Pairs well with

- [Mod Menu](https://modrinth.com/mod/modmenu) or [Mod Hub](https://modrinth.com/mod/mod-hub), either of which opens the settings screen from its mod list.
- [Headcount](https://modrinth.com/mod/headcount), my mod for picking one entity out of a crowd, such as waxing one copper golem among several.

### Compatibility

It runs on Minecraft 26.3 with Fabric. It's needed on the server only, and players without it can join. It sorts into chests, barrels, and shulker boxes, not ender chests or storage from other mods.

### How to use

#### Shulker boxes in chests

Put shulker boxes in a chest the way you'd fill a chest for a golem. A golem holding an item works through the chest from the first slot to the last:

- A loose stack of the item takes it, the same as in vanilla.
- A shulker box is checked as if it were a chest of its own. If the box holds the item and has room, the item goes in.
- A full box is skipped, and the golem carries on to the next slot.
- An empty shulker box takes anything, the same as an empty chest. Put empty boxes at the end of a chest if you don't want them catching everything.
- A chest holding only shulker boxes won't take a loose item unless one of its boxes or slots already has that item.
- A shulker box is never put into a shulker box, so golems carrying shulker boxes sort them the vanilla way.

A chest full of shulker boxes takes a golem no longer to search than any other chest.

#### Barrels and placed shulker boxes

- Golems sort into a barrel the same way they do into a chest, shulker boxes stored inside included. A barrel opens even with a block on top of it.
- Golems sort into a shulker box placed as a block. It needs room above its lid to open, the same as for a player.

#### Settings

Open the settings from Mod Menu or Mod Hub. Without either, they're in `config/savvycoppergolems/config.json`. On a dedicated server, the server's own file is the one that counts.

**Where golems sort**

| Setting | Default |
| --- | --- |
| Shulker boxes in chests and barrels | On |
| Barrels | On |
| Placed shulker boxes | On |

- Turning off Shulker boxes in chests and barrels stops golems from looking inside stored shulker boxes. A stored box is just another item, and chests and barrels sort by vanilla's own rule.

**How much golems carry**

| Setting | Default |
| --- | --- |
| Items carried | 16 (from 1 to 64) |

- Items carried is how many items a golem takes from a copper chest in one trip. Vanilla's number is 16; raising it lets a golem carry up to a full stack.

**How far golems look**

| Setting | Default |
| --- | --- |
| Containers checked | 10 (from 1 to 64) |
| Sideways range | 32 blocks (from 1 to 128) |
| Up and down range | 8 blocks (from 1 to 64) |

- Containers checked is how many containers a golem tries without finding a place for its item before it rests and starts over.
- Sideways range and Up and down range are how far from itself a golem looks for containers.
- Bigger numbers mean more work for the server.

**Crammed golems**

| Setting | Default |
| --- | --- |
| Stay stacked | On |

- Stay stacked keeps copper golems crammed into one block stacked on one spot instead of pushing apart when one is nudged a little.

### License

MIT.
