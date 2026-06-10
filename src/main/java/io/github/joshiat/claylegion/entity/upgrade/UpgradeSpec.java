package io.github.joshiat.claylegion.entity.upgrade;

/**
 * Static definition of a single soldier upgrade.
 *
 * @param flag             the {@link UpgradeFlags} bit for this upgrade
 * @param slot             equipment slot category
 * @param maxUses          finite durability, or 0 for unlimited
 * @param requiresAll      flags that must ALL be present before equipping
 * @param requiresAny      flags of which at least ONE must be present (0 = none required)
 * @param incompatibleWith flags that block equipping if ANY is present
 */
public record UpgradeSpec(
    long flag,
    UpgradeSlot slot,
    int maxUses,
    long requiresAll,
    long requiresAny,
    long incompatibleWith
) {

    public boolean hasFiniteUses() {
        return maxUses > 0;
    }

    public int bitIndex() {
        return Long.numberOfTrailingZeros(flag);
    }

    /** Validates prerequisites and incompatibilities against a soldier's active upgrade bits. */
    public boolean canEquipOnto(long activeUpgrades) {
        if ((activeUpgrades & flag) == flag) {
            return false;
        }
        if ((activeUpgrades & requiresAll) != requiresAll) {
            return false;
        }
        if (requiresAny != 0L && (activeUpgrades & requiresAny) == 0L) {
            return false;
        }
        return (activeUpgrades & incompatibleWith) == 0L;
    }
}
