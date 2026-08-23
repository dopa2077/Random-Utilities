dopas_random_utilities config pack
=================================

This folder holds all tunable settings for dOPas' Random Utilities.


Root files
----------
features.toml
  Enable or disable registration of individual blocks and items.
  false = content does not exist in the game (recipes/items missing).

upgrade_bonuses.toml
  Global strength of EACH upgrade of a given type (percent, or flat range).
  Examples: productivity output bonus, overclock speed, Fortune Mesh treasure chance,
  efficiency FE savings, range blocks per upgrade.
  These apply wherever that upgrade is accepted; they are NOT per-machine caps.
  Machine/item caps (how many you can install) live under upgrades/.


Folders
-------
upgrades/
  max_* caps only: how many of each upgrade a machine or item may hold.
  Never put base_ticks, base_mb, energy drain, recipes, or loot here.
  Files: resource_generator.json, solar_furnace.json, fishnet.json,
         transfer_node.json, powered_machines.json, magnet.json

blocks/
  How blocks behave without upgrades: speeds, amounts, energy math, loot flags,
  generator recipes (blocks/resource_generator/), etc.
  Never put max_* upgrade caps here.

  powered_machines.json
    base_ticks: default cycle length for FE-powered machines before overclock.
    overclock_cost_exponent: FE cost scales as speedFactor ^ exponent when
      overclocks raise cycle speed (see MachineEnergy). Higher values punish
      overclocking harder. Default 1.09.

items/
  Item-only behavior (magnet pull speed/range bases, lasso lists, /dev/null slots).
  Magnet upgrade caps are in upgrades/magnet.json, not items/magnet.json.

treasure/
  Weighted loot tables (treasure_loot.json for Treasure Mesh on the fishnet).


Rules of thumb
--------------
1. max_*            -> upgrades/
2. base_* / rates   -> blocks/ or items/
3. upgrade % power  -> upgrade_bonuses.toml
4. enable content   -> features.toml
