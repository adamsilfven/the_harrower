package com.leyline.hollowforged.mixin;
import com.leyline.hollowforged.HFState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FishingHook.class)
public abstract class FishingPullMixin {
 @Inject(method="pullEntity",at=@At("HEAD"),cancellable=true)private void hollowforged$pull(Entity entity,CallbackInfo ci){if(HFState.forceImmune(entity))ci.cancel();}
}
