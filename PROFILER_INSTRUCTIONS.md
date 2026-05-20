# Phase 1 Optimization & Profiler Usage

## What the Profiler Now Covers

The profiler instruments **every major server-side system** in the mod:

| System | Metric Keys | Notes |
|--------|-------------|-------|
| Soldier targeting scan | `targetScanTime` / `targetScans` | AABB entity search |
| Mount targeting scan | `mountScanTime` / `mountScans` | AABB entity search |
| AABB query internals | `aabbQueryTime` / `aabbQueries` | Raw query cost |
| Predicate checks | `predicateCheckTime` / `predicateChecks` | isValidTarget() |
| Combat tick (total) | `combatTickTime` / `combatTicks` | Outer combat loop |
| Status effects | `statusEffectTime` / `statusEffectTicks` | slow/combustion ticks |
| Target selection | `targetSelectionTime` / `targetSelections` | updateTargetCache call |
| Separation sampling | `separationSampleTime` / `separationSamples` | pushback force |
| Mount acquire | `mountAcquireTime` / `mountAcquireCalls` | tryAcquireMount() |
| Idle brake | `idleBrakeTime` / `idleBrakeCalls` | applyIdleBraking() |
| Ranged decisions | `rangedDecisionTime` / `rangedDecisions` | tryRangedAttack() |
| Melee engagements | `meleeEngagementTime` / `meleeEngagements` | applyCombatDamage() |
| Chase target | `chaseTargetTime` / `chaseTargetCalls` | movement toward target |
| **Soldier physics** | `soldierPhysicsTime` / `soldierPhysicsTicks` | gravity + drag + move() |
| **Mount server tick** | `mountTickTime` / `mountTicks` | serverMountTick() |
| **Projectile collision** | `projectileTickTime` / `projectileTicks` | performEntityCollisionCheck() |
| **Nexus server tick** | `nexusTickTime` / `nexusTicks` | spawn logic |
| **Nexus summon count** | `nexusSummonCountTime` / `nexusSummonCountCalls` | world-wide AABB scan |

### Key hotspots to watch

- **`nexusSummonCountTime`** — `countActiveSummons()` scans every entity in the entire world AABB; cost scales with total entity count, not soldier count. If multiple nexuses are active this fires frequently.
- **`soldierPhysicsTime`** — `move()` performs block-collision AABB sweeps; with 500 soldiers this can rival targeting cost.
- **`projectileTickTime`** — each in-flight projectile scans nearby entities every tick. High ranged-unit counts multiply this fast.
- **`mountTickTime`** — includes lerp math and velocity blending per mount per tick.

### 1. **Stagger Intervals Increased**
- `TARGET_SCAN_INTERVAL`: 12 → 16 ticks
- `MOUNT_SCAN_INTERVAL`: 8 → 12 ticks
- **Effect**: Reduces concurrent targeting scans from ~84 soldiers/tick to ~73, improving spike distribution
- **Expected**: ~24 ticks → reduced spike frequency, peak should drop from 38ms to 12-18ms

### 2. **Mount Search Range Updated**
- `MOUNT_SEARCH_RANGE`: 8.0 → 16.0 blocks
- **Effect**: Soldiers can detect nearby mounts from further away
- **Rationale**: User requirement for improved mount detection

### 3. **Profiler Integrated**
- All targeting scan methods now instrument timing:
  - `targetScanTime`: Total time for updateTargetCache()
  - `mountScanTime`: Total time for updateMountTargetCache()
  - `aabbQueryTime`: Time for AABB entity search
  - `predicateCheckTime`: Time for validity checks
- **State**: Profiler is **disabled by default** (zero overhead when off)

---

## How to Use the Profiler

### Enable Profiler
```
/claylegion profiler enable
```

### Run Test Scenario
Spawn ~500+ clay soldiers to generate load:
1. Use Creative mode and soldier spawner item
2. Let them fight (or just spawn on teams) for ~30-60 seconds
3. Profiler will collect measurements across all targeting scans

### Get Performance Report
```
/claylegion profiler report
```

Output format (example):
```
===== TargetingProfiler Report =====
Target Scans:          45230 (avg 0.123 µs/scan, total 5.563 ms)
Mount Scans:           28150 (avg 0.087 µs/scan, total 2.449 ms)
AABB Queries:          avg 0.045 µs, total 3.298 ms
Predicate Checks:      avg 0.018 µs, total 2.138 ms
Combat Ticks:          500000 (avg 0.312 µs/tick, total 156.0 ms)
Status Effects:        avg 0.011 µs, total 5.500 ms
Target Selection:      avg 0.040 µs, total 20.0 ms
Separation Samples:    avg 0.025 µs, total 3.125 ms
Mount Acquire:         avg 0.018 µs, total 9.0 ms
Idle Brake:            avg 0.008 µs, total 4.0 ms
Ranged Decisions:      avg 0.022 µs, total 2.2 ms
Melee Engagements:     avg 0.015 µs, total 7.5 ms
Chase Target:          avg 0.019 µs, total 9.5 ms
--- Other Systems ---
Soldier Physics (move): 500000 ticks (avg 0.280 µs/tick, total 140.0 ms)
Mount Server Tick:      12500 ticks  (avg 0.450 µs/tick, total 5.625 ms)
Projectile Collision:   4800 ticks   (avg 0.380 µs/tick, total 1.824 ms)
Nexus Server Tick:      2400 ticks   (avg 1.200 µs/tick, total 2.880 ms)
Nexus Summon Count:     120 calls    (avg 85.0 µs/call,  total 10.2 ms)  ← watch this!
Per-Tick Peaks: ...
Other System Peaks/tick: soldierPhysics X.XXX ms, mount X.XXX ms, projectile X.XXX ms, nexus X.XXX ms
```

### Disable Profiler
```
/claylegion profiler disable
```

---

## Expected Results

### **Before Phase 1**:
- Spike every 24 ticks (LCM of 12 & 8)
- Peak: 38-40ms lag frame
- Root cause: ~84 concurrent entity queries

### **After Phase 1**:
- Spike frequency reduced (new LCM of 16 & 12 = 48 ticks, half as frequent)
- Peak expected: 12-18ms lag frame
- Per-tick concurrent scans: ~31 (targets) + ~42 (mounts) = 73 soldiers (was 84)

### **Profiler Breakdown**:
- `aabbQueryTime` dominates (most expensive)
- `predicateCheckTime` should be much lower (cheap checks first)
- If peak still >20ms: proceed to **Phase 2** (work-queue based staggering)

---

## Troubleshooting

### Profiler Showing No Data
- Ensure profiler is enabled: `/claylegion profiler enable`
- Ensure targeting scans are happening (soldiers alive and in combat)
- Check console output for errors

### Still Seeing 38ms Spikes?
- Profiler will show where time is spent
- If `aabbQueryTime` dominates: proceed to Phase 2 (work queue)
- If `predicateCheckTime` high: consider further predicate optimization

### How to Disable Phase 1 (Revert)
Edit [SoldierTargetingHelper.java](src/main/java/io/github/joshiat/claylegion/entity/SoldierTargetingHelper.java):
- `TARGET_SCAN_INTERVAL = 12` (was 16)
- `MOUNT_SCAN_INTERVAL = 8` (was 12)
- `MOUNT_SEARCH_RANGE = 8.0` (was 16.0)
- Remove profiler calls (optional)

---

## Industry Context

Profilers are standard in game modding:
- **Malisis Core** mod includes profiling APIs
- **Spark** profiler (popular for Minecraft) uses similar timing instrumentation
- Zero-overhead design (disabled by default) is industry best practice

**Why measure?**: 
- "If you can't measure it, you can't improve it"
- Profiler proves whether Phase 1 actually fixed the problem
- Data-driven decision for Phase 2 (work queue vs keep Phase 1)

---

## Phase 2 (If Needed)

If profiler shows spike still >20ms after Phase 1:
- Distribute scans across multiple ticks using a work queue
- Each tick processes only 1 soldier scan instead of N
- Eliminates alignment spike entirely (smooth distribution)
- Trades slight targeting delay for consistent frame times
