package com.pythemcio.mixin;

import com.pythemcio.event.EventRegistry;
import com.pythemcio.event.EventType;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "drop(Z)Z", at = @At("RETURN"))
    private void onItemDropped(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            EventRegistry.fireEvent(EventType.ITEM_DROP);
        }
    }
}
