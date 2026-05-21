package io.github.joshiat.claylegion.mixin;

import io.github.joshiat.claylegion.entity.drop.DropStackMetadata;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupSanitizeMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void clayLegion$stripTransientDropDataOnPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();
        if (stack.isEmpty()) {
            return;
        }

        DropStackMetadata.clearTransientData(stack);
        self.setItem(stack);
    }
}
