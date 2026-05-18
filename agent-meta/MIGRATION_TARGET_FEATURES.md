# Clay Soldiers Migration Target Reference

This document serves as the architectural baseline and practical parity checklist for porting the Clay Soldiers mod to modern Minecraft (26.1.2) + Fabric + JDK 25.

## 1. Scope and Source of Truth
*   **Primary Baseline:** `OLD/src/main/java` and `OLD/src/main/resources` (prioritized for parity).
*   **Secondary Reference:** `OLD/java_old` (defines Worldgen and Clay Nexus mechanics).

## 2. Verified Feature Surface (High Level)
*   Core soldier combat entity with team logic and upgrades.
*   Mount ecosystem (5 core classes: Horse, Pegasus, Turtle, Bunny, Gecko).
*   Ranged projectile subsystem (Gravel, Snow, Fire Charge, Emerald).
*   6-category rich upgrade system (60 entries total).
*   Soldier/mount doll crafting matrixes and utility tools (Disruptors).
*   Chest-based inventory automation via item-frame marking.
*   Dispenser automation support.
*   Lexicon-driven in-game documentation engine.
*   Client rendering pipeline (custom low-poly non-LivingEntity models, layered visuals).
*   Network packet infrastructure for particle/upgrade synchronization.
*   External API hook points (JEI, WAILA/HWYLA, The One Probe).

## 3. Reference Asset Counts
*   **Soldier Doll Variants:** 39
*   **Mount Variants:** Horse (10), Pegasus (10), Turtle (9), Bunny (16), Gecko (36)
*   **Lexicon Upgrades:** 60 entries
*   **Recipe Definitions:** 88 JSON structures

---

## 4. Porting Roadmap & Checkpoint Status

### Phase 1: Core Loop
- [x] Base soldier spawn + team combat + direct melee loop
- [x] Lightweight entity architecture (`Entity` base, optimized spatial data layout)
- [x] 39-Team identity processing and doll serialization

### Phase 2: Mounts and Throwables
- [x] Mount entity scaffolding and single-passenger registration
- [x] Mount damage-delegation and combat state matrix handling
- [x] Target-scale hitbox reduction for mount entities
- [x] Projectile entity vector scaffolding
- [ ] Advanced mount trait behaviors (Wall climbing, 3D pathing, fluid dampening overrides)
- [ ] Projectile payload effects (Slow, Burn, Piercing damage matrix)
- [ ] Placeholder asset swap for production render models/textures

### Phase 3: Upgrade Parity
- [x] Early scaffold: upgrade registry + ingestion + debug inspection pipeline pulled forward to unblock Phase 2 projectile/mount validation
- [ ] All 60 upgrade behaviors implemented via bitfield processing
- [ ] Incompatibility rule processing engines (e.g., Sugar vs. Diamond)
- [ ] Conditional status effects (Blind, Explode, Zombify, Revive)

### Phase 4: Recipes and Data Pack Content
- [ ] 88 recipe data files validated and loaded
- [ ] Creative tab visibility and item group discoverability fixes

### Phase 5: UX, FX, and Integrations
- [ ] Lexicon screen UI rendering and text localization bindings
- [ ] Sync packets for particles and entity-state updates
- [ ] JEI / Waila / TOP data provider plugins implemented

### Phase 6: Legacy Automation & Worldgen
- [ ] Clay Nexus block + automated entity handling
- [ ] Clay Hut structure generation and biome lookup tables

---

## 5. Architectural Invariants (Rendering & System Constraints)
*   **Coordinate Model:** Non-LivingEntity submission pipeline dictates Y-axis model space is positive-up.
*   **UV Layout:** Keep box dimensions and texture UV mapping synchronized (e.g., 2x5x2 leg mapping = height 5 paint region).
*   **LOD Strategy:** Drop accessory rendering layers when distance to camera > 32 blocks.
*   **Allocation Minimization:** Zero object instantiation inside hot loops (`tick()`, `render()`). Use mutable structures (`BlockPos.Mutable`).