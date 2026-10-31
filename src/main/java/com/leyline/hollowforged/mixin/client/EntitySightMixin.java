package com.leyline.hollowforged.mixin.client;
import com.leyline.hollowforged.client.AbilityControls;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class EntitySightMixin {
 @Inject(method="isCurrentlyGlowing",at=@At("RETURN"),cancellable=true)private void hollowforged$reveal(CallbackInfoReturnable<Boolean> cir){if(AbilityControls.revealed((Entity)(Object)this))cir.setReturnValue(true);}
 @Inject(method="isInvisibleTo",at=@At("RETURN"),cancellable=true)private void hollowforged$concealment(Player viewer,CallbackInfoReturnable<Boolean> cir){if(AbilityControls.revealed((Entity)(Object)this))cir.setReturnValue(false);}
}
