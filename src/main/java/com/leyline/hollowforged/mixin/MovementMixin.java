package com.leyline.hollowforged.mixin;
import com.leyline.hollowforged.HFState;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class MovementMixin {
 @Inject(method="move",at=@At("HEAD"),cancellable=true)private void hollowforged$externalMove(MoverType type,Vec3 movement,CallbackInfo ci){if(type!=MoverType.SELF&&type!=MoverType.PLAYER&&HFState.forceImmune((Entity)(Object)this))ci.cancel();}
 @Inject(method="push(DDD)V",at=@At("HEAD"),cancellable=true)private void hollowforged$push(double x,double y,double z,CallbackInfo ci){if(HFState.forceImmune((Entity)(Object)this))ci.cancel();}
 @Inject(method="makeStuckInBlock",at=@At("HEAD"),cancellable=true)private void hollowforged$web(BlockState state,Vec3 scale,CallbackInfo ci){if((Object)this instanceof LivingEntity e&&HFState.legs(e)&&state.is(Blocks.COBWEB))ci.cancel();}
 @Inject(method="isPushedByFluid",at=@At("RETURN"),cancellable=true)private void hollowforged$fluid(CallbackInfoReturnable<Boolean> cir){if(HFState.forceImmune((Entity)(Object)this))cir.setReturnValue(false);}
 @Inject(method="getBlockJumpFactor",at=@At("RETURN"),cancellable=true)private void hollowforged$honeyJump(CallbackInfoReturnable<Float> cir){if((Object)this instanceof LivingEntity e&&HFState.legs(e)&&(e.level().getBlockState(e.blockPosition()).is(Blocks.HONEY_BLOCK)||e.level().getBlockState(e.blockPosition().below()).is(Blocks.HONEY_BLOCK)))cir.setReturnValue(1F);}
 @Inject(method="getBlockSpeedFactor",at=@At("RETURN"),cancellable=true)private void hollowforged$terrain(CallbackInfoReturnable<Float> cir){
  if((Object)this instanceof LivingEntity e&&HFState.legs(e)){
   var here=e.level().getBlockState(e.blockPosition());var below=e.level().getBlockState(e.blockPosition().below());
   if(here.is(Blocks.SOUL_SAND)||here.is(Blocks.HONEY_BLOCK)||below.is(Blocks.SOUL_SAND)||below.is(Blocks.HONEY_BLOCK))cir.setReturnValue(1F);
  }
 }
}
