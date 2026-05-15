/* ******************************************************************************************************************
   * Authors:   SanAndreasP
   * Copyright: SanAndreasP
   * License:   Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International
   *                http://creativecommons.org/licenses/by-nc-sa/4.0/
   *******************************************************************************************************************/
package de.sanandrew.mods.claysoldiers.registry.effect;

import de.sanandrew.mods.claysoldiers.api.CsmConstants;
import de.sanandrew.mods.claysoldiers.api.attribute.AttributeHelper;
import de.sanandrew.mods.claysoldiers.api.entity.soldier.ISoldier;
import de.sanandrew.mods.claysoldiers.api.entity.soldier.effect.ISoldierEffect;
import de.sanandrew.mods.claysoldiers.api.entity.soldier.effect.ISoldierEffectInst;
import net.minecraft.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public class EffectSlimeball
        implements ISoldierEffect
{
    public static final EffectSlimeball INSTANCE = new EffectSlimeball();

    private EffectSlimeball() { }

    @Override
    public void onAdded(ISoldier<?> soldier, ISoldierEffectInst effectInst) {
        AttributeHelper.tryApplyMoveSpeedModifier(soldier.getEntity(), STANDSTILL);
        AttributeHelper.tryApplyKnockbackResModifier(soldier.getEntity(), FULL_KB_RESIST);
    }

    @Override
    public void onExpired(ISoldier<?> soldier, ISoldierEffectInst effectInst) {
        AttributeHelper.tryRemoveMoveSpeedModifier(soldier.getEntity(), STANDSTILL);
        AttributeHelper.tryRemoveKnockbackResModifier(soldier.getEntity(), FULL_KB_RESIST);
    }

    @Override
    public boolean syncData() {
        return true;
    }

    private static final AttributeModifier STANDSTILL = new AttributeModifier(UUID.fromString("D9C0D2CC-4CB7-4854-A8DC-AC9EEE9B9AB2"), CsmConstants.ID + ".slimeball.mv", -1.0D, 2);
    private static final AttributeModifier FULL_KB_RESIST = new AttributeModifier(UUID.fromString("1622C34B-CF15-4329-9086-BBAC1989FE49"), CsmConstants.ID + ".slimeball.kb", 1.0D, 0);
}
