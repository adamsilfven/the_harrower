package com.leyline.harrower.mixin;
import com.leyline.harrower.Harrower;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Runs AFTER all LivingDamageEvent.Pre listeners, before absorption. No source-specific caps. */
@Mixin(LivingEntity.class)
public abstract class DamagePipelineMixin {
 @WrapOperation(method="actuallyHurt",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F",remap=false))
 private float harrower$finalDamage(LivingEntity entity,DamageContainer container,Operation<Float> original){
  float damage=original.call(entity,container);
  if(entity instanceof Harrower boss&&!container.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)){
   if(boss.combat().phaseTransition())damage*=.5F;
   if(damage>60)damage=60+.5F*(damage-60);container.setNewDamage(damage);
  }return damage;
 }
}
