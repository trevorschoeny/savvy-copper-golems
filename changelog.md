*First release, not yet through Trev's walk or the smoke.*

### 1.0.1

#### Fixed

- A placed shulker box's lid stayed open after a golem's visit; it now closes, the same as vanilla's own containers.
- Filling a stack of several shulker boxes put the item into every box in the stack instead of treating the stack as a single item.
- A container stayed open if a golem's visit was interrupted (hurt, leashed, or removed); every end to a visit, including removal, now closes it.
- In a 3x3 wall of chests, a golem could miss the top corners at random even with room to spare; it now prefers a container that already holds its item and has room for the stack, so corners get found as reliably as the rest of the wall.

### 1.0.0

First release.

#### Added

- Copper golems sort into the shulker boxes stored in a chest, slot by slot, as if each box were a chest of its own.
- Copper golems sort into barrels and into placed shulker boxes. Both are on by default and can be turned off.
- A setting to turn off sorting into stored shulker boxes, so chests and barrels follow vanilla's own rule instead.
- A setting for how many items a golem carries from a copper chest at once, past vanilla's 16, up to a full stack.
- Settings for how many containers a golem checks and how far it looks, with vanilla's numbers as defaults.
- A setting to keep copper golems crammed into one block stacked instead of pushing apart when nudged, on by default.
