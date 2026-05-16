# Clay Soldiers Migration Target Reference

This document is the feature baseline for porting this mod to a modern Minecraft + Java stack.
It is compiled from the legacy source in this repository and is intended as a practical parity checklist.

## 1. Scope and Source of Truth

Primary baseline (newer legacy code):
- OLD/src/main/java
- OLD/src/main/resources

Secondary historical reference (older branch/codepath):
- OLD/java_old

Notes:
- If behavior differs between OLD/src and OLD/java_old, prioritize OLD/src as the first parity target.
- Worldgen and Clay Nexus behavior are primarily defined in OLD/java_old.

## 2. Verified Feature Surface (High Level)

- Core soldier combat entity with team logic and upgrades
- Mount ecosystem (horse, pegasus, turtle, bunny, gecko)
- Ranged projectile subsystem (gravel, snow, fire charge, emerald)
- Rich upgrade system (6 upgrade categories)
- Soldier and mount doll crafting ecosystem
- Utility/disruption tools
- Chest-based soldier inventory access workflow
- Dispenser automation for doll spawning and disruptor usage
- Lexicon-driven in-game documentation and searchable help
- Client rendering layers, particles, visual effects
- Networking for particles/upgrades/effects and item mode switching
- External integration with JEI, WAILA, The One Probe
- Legacy worldgen structure(s), including clay huts
- Clay Nexus block and tile entity automation system (legacy branch)

## 3. Concrete Counts to Use for Migration Planning

From OLD/src/main/resources/assets/claysoldiers/lang/en_US.lang and recipe assets:

- Soldier doll variants: 39
- Horse variants: 10
- Pegasus variants: 10
- Turtle variants: 9
- Bunny variants: 16
- Gecko variants: 36
- Lexicon-defined upgrade entries: 60
- Recipe JSON files under assets/claysoldiers/recipes: 88

## 4. Core Gameplay Targets

### 4.1 Soldiers and Teaming

Target parity:
- [ ] Spawnable soldier dolls and team identity persisted on dolls
- [ ] Team-based ally/enemy behavior
- [ ] Team coloring/variants and conversion workflows
- [ ] Brick soldier lifecycle and recovery loop

Evidence:
- OLD/src/main/resources/assets/claysoldiers/lang/en_US.lang
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/entity/soldier/EntityClaySoldier.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/item/ItemSoldier.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/item/ItemBrickSoldier.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/registry/team/TeamRegistry.java

### 4.2 Mount System

Target parity:
- [ ] Horse mounts (10 variants)
- [ ] Pegasus mounts (10 variants, flight behavior)
- [ ] Turtle mounts (9 variants, tankier/water behavior)
- [ ] Bunny mounts (16 variants)
- [ ] Gecko mounts (36 variants, climbing identity)

Evidence:
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/entity/mount
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/item/ItemMountHorse.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/item/ItemMountPegasus.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/item/ItemMountTurtle.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/item/ItemMountBunny.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/item/ItemMountGecko.java

### 4.3 Projectile Combat

Target parity:
- [ ] Gravel projectile behavior
- [ ] Snow projectile behavior
- [ ] Fire charge projectile behavior
- [ ] Emerald projectile behavior (special damage/effects)

Evidence:
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/entity/projectile
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/entity/EntityRegistry.java

## 5. Upgrade System Targets

Upgrade categories to preserve:
- [ ] Main hand
- [ ] Off hand
- [ ] Core
- [ ] Behavior
- [ ] Misc
- [ ] Enhancement

Minimum target:
- [ ] Preserve all 60 upgrade entries documented in lexicon text
- [ ] Preserve key incompatibility rules (for example, sugar vs diamond, gunpowder vs magma cream/firework star)
- [ ] Preserve trigger effects (blind, slow, burn, revive, zombify, explode, etc.)

Evidence:
- OLD/src/main/resources/assets/claysoldiers/lang/en_US.lang
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/registry/upgrade
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/entity/soldier/SoldierUpgrade.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/entity/soldier/SoldierEffect.java

## 6. Crafting and Progression Targets

Target parity:
- [ ] Soldier doll base crafting and conversions (dye, glass, resource, washing)
- [ ] Mount doll crafting matrixes
- [ ] Disruptor recipe tiers
- [ ] Brick soldier reverse conversion flow
- [ ] Lexicon and utility item recipes

Evidence:
- OLD/src/main/resources/assets/claysoldiers/recipes
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/crafting
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/compat/jei

## 7. Utility and Automation Targets

### 7.1 Disruptor

Target parity:
- [ ] Three disruptor tiers (clay/hardened/obsidian)
- [ ] Mode switching between target classes (all, dolls, soldiers, mounts, companions, clay blocks)
- [ ] Radius-based disruption behavior

Evidence:
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/item/ItemDisruptor.java
- OLD/src/main/resources/assets/claysoldiers/lang/en_US.lang

### 7.2 Dispenser Integration

Target parity:
- [ ] Dispensers spawn dolls/entities automatically
- [ ] Dispensers can activate disruptor behavior

Evidence:
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/dispenser
- OLD/src/main/resources/assets/claysoldiers/lang/en_US.lang

### 7.3 Inventory Access Workflow

Target parity:
- [ ] Soldiers can fetch upgrades from inventories using item-frame marker logic

Evidence:
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/entity/ai/UpgradesChestHelper.java
- OLD/src/main/resources/assets/claysoldiers/lang/en_US.lang

## 8. Client/UX Targets

Target parity:
- [ ] Lexicon UI and content groups (upgrades, soldiers, mounts, misc, info, search)
- [ ] Soldier/mount rendering and layered visuals (capes, crowns, goggles, armor, etc.)
- [ ] Projectile and special effect rendering
- [ ] Particle effects and HUD/stat style overlays where applicable

### 8.1 Rendering Conventions and Parity Guardrails

For modern MC 26.1.2 rendering code, preserve these implementation invariants:

- [ ] When using base EntityRenderer submit pipeline (non-LivingEntityRenderer), treat model Y as positive-up; do not assume LivingEntity flip conventions are applied
- [ ] Keep model box dimensions and texture UV paint dimensions synchronized (for example, a 2x5x2 leg must use h=5 in generated texture regions)
- [ ] Keep team-color tint readable by preserving per-face brightness contrast in texture generation
- [ ] Validate soldier silhouette orientation in-game with an asymmetric visual cue before finalizing model pivots/poses

Current status (completed in this migration pass):

- [x] Clay soldier base renderer verified to use positive-Y-up model authoring in EntityRenderer submit pipeline
- [x] Clay soldier model box dimensions and generated texture UV paint dimensions synchronized (legs fixed from 3-high paint to 5-high paint)
- [x] Clay soldier orientation validated in-game with asymmetric arm pose cue, then restored to neutral pivoting
- [x] Clay soldier depth/occlusion path corrected via proper model RenderType and submitModel argument ordering
- [x] Clay soldier shadow grounding preserved by using submit-pose Y translation tuning instead of render offset overrides

Evidence:
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/client
- OLD/src/main/resources/assets/claysoldiers/lang/en_US.lang
- OLD/src/main/resources/assets/claysoldiers/textures
- OLD/src/main/resources/assets/claysoldiers/models

## 9. Integrations and Data Sync Targets

### 9.1 Integrations

Target parity:
- [ ] JEI recipe visualization support
- [ ] WAILA/HWYLA entity tooltip support
- [ ] The One Probe entity display support

Evidence:
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/compat/jei/JeiPlugin.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/compat/waila/WailaEntityProvider.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/compat/top/TOPProvider.java

### 9.2 Networking

Target parity:
- [ ] Particle sync packet(s)
- [ ] Soldier upgrade sync packet(s)
- [ ] Soldier effect sync packet(s)
- [ ] Server-side disruptor mode switch packet

Evidence:
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/network/PacketManager.java
- OLD/src/main/java/de/sanandrew/mods/claysoldiers/network/packet

## 10. Legacy-Branch-Only Features to Decide Explicitly

These appear in OLD/java_old and should be consciously accepted or dropped in the new implementation:

- [ ] Clay Nexus block/tile entity automation gameplay
- [ ] Clay hut world generation and loot table behavior
- [ ] Companion-specific systems and overlays

Evidence:
- OLD/java_old/de/sanandrew/mods/claysoldiers/block/BlockClayNexus.java
- OLD/java_old/de/sanandrew/mods/claysoldiers/tileentity/TileEntityClayNexus.java
- OLD/java_old/de/sanandrew/mods/claysoldiers/world/gen/WorldGenerator.java
- OLD/java_old/de/sanandrew/mods/claysoldiers/world/gen/feature/WorldGenClayHut.java
- OLD/java_old/de/sanandrew/mods/claysoldiers/entity/companion

## 11. Suggested Porting Phases (Use This Checklist)

Phase 1: Core loop
- [ ] Base soldier spawn + team combat + simple damage loop

Phase 2: Mounts and throwables
- [ ] All mount classes and all projectile classes working end to end

Phase 3: Upgrade parity
- [ ] All 6 categories restored with incompatibility rules and effects

Phase 4: Recipes and data content
- [ ] Recipe/data packs restored and validated

Phase 5: UX and integrations
- [ ] Lexicon, particles/render polish, JEI/TOP/WAILA equivalents

Phase 6: Optional legacy extras
- [ ] Clay Nexus and clay hut worldgen (if still desired)

## 12. Definition of Done for Migration

Use this as acceptance criteria:
- [ ] All major systems in sections 4-9 function in-game on the new target version
- [ ] Variant counts match section 3
- [ ] Upgrade interactions and incompatibilities match legacy behavior
- [ ] Recipe/content coverage is complete
- [ ] Multiplayer sync behavior is stable
- [ ] Performance is acceptable with large soldier skirmishes