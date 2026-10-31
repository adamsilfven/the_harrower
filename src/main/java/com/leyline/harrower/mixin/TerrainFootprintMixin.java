package com.leyline.harrower.mixin;

import com.leyline.harrower.Harrower;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep the full combat target; terrain support comes from the weight-bearing body. */
@Mixin(Entity.class)
public abstract class TerrainFootprintMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="getBbWidth",at=@At("HEAD"),cancellable=true)
 private void harrower$navigationWidth(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> result){if((Object)this instanceof Harrower boss&&boss.navigationGeometry())result.setReturnValue(boss.navigationWidth());}
 @org.spongepowered.asm.mixin.injection.Inject(method="getBoundingBox",at=@At("RETURN"),cancellable=true)
 private void harrower$navigationBounds(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<AABB> result){if((Object)this instanceof Harrower boss&&boss.navigationGeometry())result.setReturnValue(boss.navigationBounds(result.getReturnValue()));}
 @ModifyExpressionValue(method="collide",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
 private AABB harrower$terrainFootprint(AABB bounds){
  return (Object)this instanceof Harrower boss?boss.terrainCollisionBox(bounds):bounds;
 }
 @ModifyExpressionValue(method="updateFluidHeightAndDoFluidPushing()V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"),remap=false)
 private AABB harrower$fluidFootprint(AABB bounds){return (Object)this instanceof Harrower boss?boss.terrainCollisionBox(bounds):bounds;}

}
