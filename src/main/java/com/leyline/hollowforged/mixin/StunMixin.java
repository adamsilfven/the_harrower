package com.leyline.hollowforged.mixin;
import com.leyline.hollowforged.CombatHooks;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class StunMixin {
 @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)private void hollowforged$stun(CallbackInfo ci){if(CombatHooks.stunned((Mob)(Object)this))ci.cancel();}
}
