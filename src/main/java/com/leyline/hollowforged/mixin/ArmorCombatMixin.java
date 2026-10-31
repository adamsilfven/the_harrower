package com.leyline.hollowforged.mixin;
import com.leyline.hollowforged.CombatHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
@Mixin(LivingEntity.class)
public abstract class ArmorCombatMixin {
 @ModifyArgs(method="getDamageAfterArmorAbsorb",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
 private void hollowforged$armor(Args args){LivingEntity victim=args.get(0);DamageSource source=args.get(2);float armor=args.get(3);args.set(3,armor*CombatHooks.armorMultiplier(victim,source));}
}
