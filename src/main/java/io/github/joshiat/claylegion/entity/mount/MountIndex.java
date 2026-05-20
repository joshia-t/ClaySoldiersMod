package io.github.joshiat.claylegion.entity.mount;

import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * Per-level mount registry that keeps a fast set of currently boardable mounts.
 */
public final class MountIndex {

    private static final WeakHashMap<Level, MountIndex> INSTANCES = new WeakHashMap<>();

    private final HashSet<BaseMountEntity> allMounts = new HashSet<>();
    private final HashSet<BaseMountEntity> unmounted = new HashSet<>();

    private MountIndex() {
    }

    public static MountIndex get(Level level) {
        return INSTANCES.computeIfAbsent(level, ignored -> new MountIndex());
    }

    public void register(BaseMountEntity mount) {
        allMounts.add(mount);
        refreshAvailability(mount);
    }

    public void unregister(BaseMountEntity mount) {
        allMounts.remove(mount);
        unmounted.remove(mount);
    }

    public void refreshAvailability(BaseMountEntity mount) {
        if (isAvailable(mount)) {
            unmounted.add(mount);
        } else {
            unmounted.remove(mount);
        }
    }

    public BaseMountEntity findNearestAvailable(double x, double y, double z, double maxDistanceSq) {
        BaseMountEntity best = null;
        double bestDistSq = maxDistanceSq;

        Iterator<BaseMountEntity> iterator = unmounted.iterator();
        while (iterator.hasNext()) {
            BaseMountEntity candidate = iterator.next();
            if (!isAvailable(candidate)) {
                iterator.remove();
                continue;
            }

            double dx = candidate.getX() - x;
            double dy = candidate.getY() - y;
            double dz = candidate.getZ() - z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }

        return best;
    }

    private static boolean isAvailable(BaseMountEntity mount) {
        return mount != null
            && mount.isAlive()
            && !mount.isRemoved()
            && mount.getMaxPassengers() > mount.getPassengers().size();
    }
}