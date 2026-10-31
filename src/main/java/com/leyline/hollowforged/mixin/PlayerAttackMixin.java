package com.leyline.hollowforged.mixin;
import com.leyline.hollowforged.CombatHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class PlayerAttackMixin {
 @Inject(method="isPushedByFluid",at=@At("RETURN"),cancellable=true)private void hollowforged$fluid(CallbackInfoReturnable<Boolean> cir){if(com.leyline.hollowforged.HFState.forceImmune((Player)(Object)this))cir.setReturnValue(false);}
 @Inject(method="attack",at=@At("HEAD")) private void hollowforged$begin(Entity target,CallbackInfo ci){CombatHooks.begin((Player)(Object)this,target);}
 @Inject(method="attack",at=@At("RETURN")) private void hollowforged$end(Entity target,CallbackInfo ci){CombatHooks.end((Player)(Object)this);}
}
