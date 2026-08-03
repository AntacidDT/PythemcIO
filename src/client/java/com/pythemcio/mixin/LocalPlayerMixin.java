package com.pythemcio.mixin;

import com.pythemcio.compat.MCCompat;
import com.pythemcio.event.EventRegistry;
import com.pythemcio.event.EventType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "drop(Z)Z", at = @At("HEAD"))
    private void onItemDropped(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        Inventory inv = self.getInventory();
        ItemStack held = MCCompat.selectedItem(inv);
        if (!held.isEmpty()) {
            EventRegistry.fireEvent(EventType.ITEM_DROP, MCCompat.itemName(held));
        }
    }
}
