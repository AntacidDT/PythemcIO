package com.pythemcio.mixin;

import com.pythemcio.event.EventRegistry;
import com.pythemcio.event.EventType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void onCheckTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self instanceof net.minecraft.client.player.LocalPlayer) {
                EventRegistry.fireEvent(EventType.TOTEM, source.getMsgId(), 0);
            }
        }
    }
}
