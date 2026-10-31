package com.leyline.harrower.mixin;
import com.leyline.harrower.integration.CoreLeylineAdapter;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional and bytecode-fingerprinted: gates effects only, never ownership/progression/validation. */
@Pseudo
@Mixin(targets="com.leylineconvergence.core.endgame.EndgameService",remap=false)
public abstract class CoreLeylineMixin {
 @Inject(method="effectBinding",at=@At("HEAD"),cancellable=true,require=1)
 private static void harrower$normalEffect(ServerPlayer player,String color,CallbackInfoReturnable<Object> result){if(CoreLeylineAdapter.ownerColorSuppressed(player.getUUID(),color))result.setReturnValue(null);}
 @Inject(method="contains",at=@At("HEAD"),cancellable=true,require=1)
 private static void harrower$entityEffect(LivingEntity entity,@Coerce Object binding,CallbackInfoReturnable<Boolean> result){if(CoreLeylineAdapter.bindingSuppressed(binding))result.setReturnValue(false);}
 @WrapOperation(method="insideAnyRed",at=@At(value="INVOKE",target="Lcom/leylineconvergence/core/land/LandRules;inRange(Lcom/leylineconvergence/core/land/LandRules$Binding;Ljava/lang/String;DDDD)Z"),require=1)
 private static boolean harrower$redRegion(@Coerce Object binding,String dimension,double x,double y,double z,double radius,Operation<Boolean> original){return !CoreLeylineAdapter.bindingSuppressed(binding)&&original.call(binding,dimension,x,y,z,radius);}
}
