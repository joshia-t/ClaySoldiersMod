This is a technical manifesto designed for a `NOTICE.md` or `ARCHITECTURE.md` file. It frames the project not just as a "port," but as a modern re-engineering of the Clay Soldiers concept with a focus on high-performance RTS-scale simulation.

---

# Technical Manifesto: Clay Soldiers Reborn (2026 Port)

## 🎯 Project Intent

The goal of this project is to port the "Clay Soldiers" concept to modern Minecraft (Fabric/JDK 25) with a primary focus on **RTS-scale performance**. We aim to support skirmishes involving **hundreds (500+) of active units** simultaneously without degrading the server tick rate or client FPS.

## 🛠️ Tech Stack & Environment

* **Platform:** Fabric Loader (Minimal overhead, direct bytecode access).
* **Runtime:** JDK 25+ (Leveraging **JEP 502: Stable Values** and **JEP 519: Compact Object Headers**).
* **Build System:** Gradle with Loom.
* **Language:** Java (optimized for the HotSpot JIT).

---

## 🏗️ Architectural Core Principles

### 1. The Entity Bottleneck (The "Anti-LivingEntity" Rule)

To achieve RTS performance, we **explicitly avoid** extending `LivingEntity` or `PathfinderMob`.

* **Decision:** All soldiers extend the base `Entity` class.
* **Reasoning:** `LivingEntity` carries significant legacy bloat (vanilla potion logic, equipment slot polling, and heavy `onLivingUpdate` loops) that scales poorly at $O(n)$ where $n > 100$.
* **Implementation:** We implement a custom, lightweight `Statemachine` for health, combat, and team logic.

### 2. High-Density AI (Pathfinding & Logic)

* **Lazy Ticking:** Logic updates (Target seeking, upgrade checks) should be interleaved or performed on a "Lazy Tick" (e.g., every 5–10 ticks) rather than every world tick.
* **Swarm Pathfinding:** Units should favor a "Follow the Leader" or "Vector-to-Target" movement pattern. A* Pathfinding should be used sparingly and only when a unit's progress is obstructed for $> X$ ticks.
* **Bitmask Upgrades:** With over 60 upgrades, we do not use `List<Upgrade>` or `ArrayList`. All active upgrades are stored as bitfields (`long`) for $O(1)$ constant-time status checks.

### 3. Rendering & Visuals (LOD & Batching)

* **Draw Call Optimization:** 39 teams and 60+ visual upgrades create a combinatorial explosion of textures.
* **Requirement:** Use **Texture Atlasing** or **Instanced Rendering** where possible. Visual layers (capes, goggles, etc.) must be rendered conditionally based on Camera Distance (LOD). If a unit is $> 32$ blocks away, only the base team color should be rendered.

### 4. Data & Automation

* **Data Components:** Use the post-1.20.5 `DataComponent` system for all soldier items.
* **Memory Management:** Minimize object allocation in the main combat loop. Reuse `Vector3d` and `BlockPos.Mutable` instances to reduce GC pressure during large battles.

---

## 🗺️ Feature Roadmap & Technical Constraints

### Soldiers & Teams

* **Bitwise Logic:** Teams are indexed IDs; friendly-fire checks are simple integer comparisons.
* **Brick Variation:** Implemented as a "Recovery State" to prevent entity deletion and subsequent overhead of new object instantiation.

### Mount Ecosystem

* **Rider Logic:** Mounts are separate lightweight entities. The relationship must be handled via `Entity.startRiding()`, but movement logic is "Pulled" by the Mount from the Rider's AI to keep the hierarchy flat.

### The Clay Nexus & Automation

* **Async Processing:** Logic for the Nexus (scanning for soldiers/restocking) should ideally be handled off the main server thread or spread across multiple ticks to prevent "Micro-stutter."

---

## 📜 Credits & Heritage

This project is a technical reimagining of the original **Clay Soldiers Mod** by **SanAndreasP** and **KodaichiZero**. Their original game design is the foundation; this port provides the modern engine to realize that vision at a larger scale.

> **Note to Contributors:** Always profile before you optimize. Use **Spark** or **JProfiler** to identify bottlenecks. If a feature costs more than 0.05ms per tick at 500 units, it needs to be refactored.