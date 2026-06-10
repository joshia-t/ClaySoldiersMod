package io.github.joshiat.claylegion.entity;

/**
 * Standing orders a Clay Nexus issues to its summons (issue #32).
 *
 * <ul>
 *   <li>{@link #MARCH} — free roam: chase and fight without restriction (legacy behavior).</li>
 *   <li>{@link #GUARD} — leashed to the nexus: return home when idle and abandon
 *       chases that stray too far from it.</li>
 *   <li>{@link #HOLD} — stand ground: never chase; fight only what comes into
 *       reach (ranged attacks still fire from the spot).</li>
 * </ul>
 */
public enum NexusOrder {
    MARCH((byte) 0),
    GUARD((byte) 1),
    HOLD((byte) 2);

    public final byte id;

    NexusOrder(byte id) {
        this.id = id;
    }

    public static NexusOrder fromId(byte id) {
        for (NexusOrder order : values()) {
            if (order.id == id) {
                return order;
            }
        }
        return MARCH;
    }

    public NexusOrder next() {
        NexusOrder[] orders = values();
        return orders[(ordinal() + 1) % orders.length];
    }
}
