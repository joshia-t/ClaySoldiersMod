package io.github.joshiat.claylegion.entity.drop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Stores transient drop-state metadata on ItemStack custom_data.
 *
 * These fields are meant for in-world handling only and are stripped when
 * the player picks an item up so inventory stacks stay compact.
 */
public final class DropStackMetadata {

    public static final String UPGRADE_FLAG_TAG = "cl_drop_upgrade_flag";
    public static final String UPGRADE_USES_TAG = "cl_drop_upgrade_uses";
    public static final String SOLDIER_USES_TAG = "cl_drop_soldier_uses";
    public static final String NO_ZOMBIFY_TAG = "cl_drop_no_zombify";

    private DropStackMetadata() {
    }

    public static void setUpgradeData(ItemStack stack, long upgradeFlag, int usesRemaining) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putLong(UPGRADE_FLAG_TAG, upgradeFlag);
        tag.putInt(UPGRADE_USES_TAG, Math.max(0, usesRemaining));
        setTag(stack, tag);
    }

    public static long getUpgradeFlagOrZero(ItemStack stack) {
        CompoundTag tag = getTagOrNull(stack);
        if (tag == null || !tag.contains(UPGRADE_FLAG_TAG)) {
            return 0L;
        }
        return tag.getLong(UPGRADE_FLAG_TAG).orElse(0L);
    }

    public static int getUpgradeUsesOrDefault(ItemStack stack, int fallbackUses) {
        CompoundTag tag = getTagOrNull(stack);
        if (tag == null || !tag.contains(UPGRADE_USES_TAG)) {
            return Math.max(0, fallbackUses);
        }
        return Math.max(0, tag.getInt(UPGRADE_USES_TAG).orElse(fallbackUses));
    }

    public static void setSoldierUses(ItemStack stack, int usesRemaining) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putInt(SOLDIER_USES_TAG, Math.max(0, usesRemaining));
        setTag(stack, tag);
    }

    public static int getSoldierUsesOrDefault(ItemStack stack, int fallbackUses) {
        CompoundTag tag = getTagOrNull(stack);
        if (tag == null || !tag.contains(SOLDIER_USES_TAG)) {
            return Math.max(0, fallbackUses);
        }
        return Math.max(0, tag.getInt(SOLDIER_USES_TAG).orElse(fallbackUses));
    }

    /** Marks a doll as un-revivable by ender pearl zombification (wheat seeds immunity). */
    public static void setZombificationBlocked(ItemStack stack) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putBoolean(NO_ZOMBIFY_TAG, true);
        setTag(stack, tag);
    }

    public static boolean isZombificationBlocked(ItemStack stack) {
        CompoundTag tag = getTagOrNull(stack);
        return tag != null && tag.getBoolean(NO_ZOMBIFY_TAG).orElse(false);
    }

    public static void clearTransientData(ItemStack stack) {
        CompoundTag tag = getTagOrNull(stack);
        if (tag == null) {
            return;
        }

        tag.remove(UPGRADE_FLAG_TAG);
        tag.remove(UPGRADE_USES_TAG);
        tag.remove(SOLDIER_USES_TAG);
        tag.remove(NO_ZOMBIFY_TAG);

        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            setTag(stack, tag);
        }
    }

    private static CompoundTag getOrCreateTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private static CompoundTag getTagOrNull(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    private static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
