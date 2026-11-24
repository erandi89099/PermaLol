package com.permadeath.mixin;

import com.permadeath.mechanics.TotemRoll;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** 1% de fallo del totem: si falla, se cancela la comprobacion y el jugador muere. */
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true, require = 0)
    private void permadeath$totemRoll(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (TotemRoll.shouldFail((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /** Si el totem se activo, avisa en el chat con la probabilidad. */
    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), require = 0)
    private void permadeath$totemUsed(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            TotemRoll.activated((LivingEntity) (Object) this);
        }
    }
}
