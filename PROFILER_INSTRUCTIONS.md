# Phase 1 Optimization & Profiler Usage

## What Changed (Phase 1 Surgical Optimizations)

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
targetScanTime:       Average 0.123 µs (45230 samples)
mountScanTime:        Average 0.087 µs (28150 samples)
aabbQueryTime:        Average 0.045 µs (73380 samples)
predicateCheckTime:   Average 0.018 µs (118560 samples)
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
