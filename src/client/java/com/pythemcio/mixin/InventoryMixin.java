package com.pythemcio.mixin;

import com.pythemcio.event.EventRegistry;
import com.pythemcio.event.EventType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private void onItemAdded(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && !stack.isEmpty()) {
            String itemName = stack.getItem().builtInRegistryHolder().key().location().toString();
            EventRegistry.fireEvent(EventType.ITEM_PICKUP, itemName);
        }
    }
}
