package com.leyline.hollowforged.mixin.client;
import com.leyline.hollowforged.client.WorldshardPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HumanoidModel.class)
public abstract class WorldshardPoseMixin {
 @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
 private void hollowforged$weight(LivingEntity e,float walk,float amount,float age,float yaw,float pitch,CallbackInfo ci){WorldshardPose.apply((HumanoidModel<?>)(Object)this,e,walk,amount,age);}
}
