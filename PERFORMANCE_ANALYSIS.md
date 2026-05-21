# Performance Spike Analysis: 4ms → 38ms

## Current Bottleneck Diagnosis

### Current Staggering Setup
- **TARGET_SCAN_INTERVAL = 12** → ~1/12 of soldiers scan targets each tick
- **MOUNT_SCAN_INTERVAL = 8** → ~1/8 of soldiers scan mounts each tick  
- **SEPARATION_UPDATE_INTERVAL = 4** → ~1/4 of soldiers compute separation each tick

With ~500 soldiers active:
- **Target scans/tick**: 500/12 ≈ **42 soldiers**
- **Mount scans/tick**: 500/8 ≈ **63 soldiers**
- **Separation comps/tick**: 500/4 ≈ **125 soldiers**

### The Real Problem: Query Cost Per Soldier

Each scan does:
```java
AABB scanBox = soldier.getBoundingBox().inflate(TARGET_RANGE_XZ, TARGET_RANGE_Y, TARGET_RANGE_XZ);
List<ClaySoldierEntity> candidates = soldier.level().getEntitiesOfClass(
    ClaySoldierEntity.class,
    scanBox,
    e -> isValidTarget(e, soldier)
);
```

**Cost breakdown per scan:**
1. `getEntitiesOfClass()` - **O(n_in_world)** walk of all entities
2. AABB collision check - **O(1)** per candidate
3. Predicate `isValidTarget()` - **6 boolean checks** per candidate
   - `candidate != null`
   - `candidate != searcher`
   - `candidate.isAlive()`
   - `!candidate.isRemoved()`
   - `!candidate.isSoldierDead()`
   - `candidate.getTeamId() != searcher.getTeamId()`
   - **`searcher.distanceToSqr(candidate) <= TARGET_RANGE_SQ`** ← **EXPENSIVE**: math operations

With range 4×4 blocks and 500 soldiers density, each scan might check **20-30 candidates**.

### Spike Scenario (4ms → 38ms)

Spike occurs when multiple operations hit same tick:
- Tick T: `(T + ID) % 12 == 0` AND `(T + ID) % 8 == 0` (LCM = 24)
- Every 24 ticks, **target AND mount scans align**
- **84 soldiers scanning targets + 84 scanning mounts = 168 queries in one tick**
- Each query × 25 candidates × 6 checks = ~25,000 operations per tick spike

**Math Check:**
- 168 soldiers × 25 avg candidates × 6 checks = 25,200 ops
- Plus `distanceToSqr()` which involves 3 subtractions + 3 multiplications + 2 additions = 8 ops each
- Total: 25,200 + (168 × 25 × 8) ≈ **58,400 scalar operations per spike tick**

---

## Optimization Strategies

### Strategy 1: Algorithm Optimization (RECOMMENDED FIRST)

**Quick Wins (Low Risk):**

#### A. Predicate Short-Circuit Ordering
Move cheap checks first, expensive last:
```java
// BEFORE: expensive distance calc happens for every candidate
if (searcher.distanceToSqr(candidate) <= TARGET_RANGE_SQ) { ... }

// AFTER: do cheap checks first, distance only if pass other filters
if (candidate != null
    && candidate.isAlive()
    && !candidate.isRemoved()
    && candidate.getTeamId() != searcher.getTeamId()
    && searcher.distanceToSqr(candidate) <= TARGET_RANGE_SQ) { ... }
```
**Benefit:** ~40% reduction per predicate evaluation (halt early on dead/removed).

#### B. Reduce Search Radius
Currently: `TARGET_RANGE_XZ = 4.0` (8×8 block area, 64 block-space)
Reduce to `3.5` or `3.0`:
```java
// Current: inflate(4.0, 1.8, 4.0)
// Proposed: inflate(3.5, 1.8, 3.5)
```
**Benefit:** Fewer candidates per scan (~30% fewer entities in AABB).

#### C. Increase Stagger Intervals (Mild)
```java
// Current
private static final int TARGET_SCAN_INTERVAL = 12;
private static final int MOUNT_SCAN_INTERVAL = 8;

// Proposed
private static final int TARGET_SCAN_INTERVAL = 16;  // Every 16 ticks instead of 12
private static final int MOUNT_SCAN_INTERVAL = 12;   // Every 12 ticks instead of 8
```
**Benefit:** 
- Target scans: 500/16 = 31 soldiers/tick (vs 42)
- Mount scans: 500/12 = 42 soldiers/tick (vs 63)
- Reduces spike overlap frequency (LCM 16, 12 = 48, up from 24)

**Trade-off:** Soldiers are slightly slower to find new targets (imperceptible at this granularity).

---

### Strategy 2: Queue/Stagger Redesign (ALTERNATIVE/FOLLOW-UP)

If Algorithm Opt isn't enough, implement a true work queue:

```java
public class SoldierScanWorkQueue {
    private Queue<ClaySoldierEntity> targetScanQueue;
    private Queue<ClaySoldierEntity> mountScanQueue;
    private int scanBudgetPerTick = 20;  // Only 20 scans/tick max
    
    public void enqueueScan(ClaySoldierEntity soldier, ScanType type) {
        if (type == ScanType.TARGET) {
            targetScanQueue.offer(soldier);
        } else {
            mountScanQueue.offer(soldier);
        }
    }
    
    public void processPendingScans() {
        int completed = 0;
        while (!targetScanQueue.isEmpty() && completed < scanBudgetPerTick) {
            ClaySoldierEntity soldier = targetScanQueue.poll();
            soldier.performTargetScan();
            completed++;
        }
        // Same for mounts...
    }
}
```

**Benefit:** Capped per-tick cost; no spike alignment.  
**Cost:** Added complexity, queue memory overhead.

---

## Recommendation

### Implement in this order:

1. **Phase 1 (5 min, 60% win)**: Algorithm optimization
   - Reorder predicate checks (cheap first)
   - Reduce `TARGET_RANGE_XZ` from 4.0 → 3.5
   - Increase intervals: `TARGET_SCAN_INTERVAL` 12→16, `MOUNT_SCAN_INTERVAL` 8→12

2. **Phase 2 (Conditional, if spike persists)**: Work queue
   - If spike still >20ms after Phase 1, implement queue budgeting

---

## Expected Outcome

**Phase 1 alone should achieve:**
- Base case (non-spike): 4ms → 3ms (predicate + smaller radius)
- Spike case: 38ms → 12-18ms (fewer soldiers aligning + reduced candidates)

**Phase 1 + Phase 2:**
- Guaranteed: 15ms cap per tick (queue limits throughput)

---

## Validation Plan

1. Instrument `SoldierTargetingHelper` with timings:
   ```java
   long start = System.nanoTime();
   List<ClaySoldierEntity> candidates = soldier.level().getEntitiesOfClass(...);
   long queryTime = System.nanoTime() - start;
   ```

2. Log spike frames and identify which operation (target, mount, separation) triggered it.

3. Profile before/after each phase.
