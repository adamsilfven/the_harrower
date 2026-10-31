package com.leyline.harrower.mixin;

import com.leyline.harrower.Harrower;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve enchantments, Resistance and gear passives after partial armor penetration. */
@Mixin(CombatRules.class)
public abstract class GroundPressureMixin {
 @ModifyReturnValue(method="getDamageAfterAbsorb",at=@At("RETURN"))
 private static float harrower$pressure(float reduced,LivingEntity victim,float amount,DamageSource source,float armor,float toughness){
  var id=source.typeHolder().unwrapKey().map(key->key.location()).orElse(null);
  if(source.getEntity() instanceof Harrower&&id!=null&&id.getNamespace().equals("lc_harrower")&&(id.getPath().equals("ground_wave")||id.getPath().equals("ground_crush")))return reduced*.65F+amount*.35F;
  return reduced;
 }
}
