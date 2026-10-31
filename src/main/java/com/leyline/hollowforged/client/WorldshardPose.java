package com.leyline.hollowforged.client;
import com.leyline.hollowforged.Hollowforged;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import java.util.WeakHashMap;
/** Per-wearer visual state. No packets, attack attributes or server timing changes. */
public final class WorldshardPose {
 private static final WeakHashMap<LivingEntity,State> STATES=new WeakHashMap<>();
 private static final class State {int tick=-1,lastTime=-1;boolean wasSwinging;float start=-100,fromPitch,fromYaw,fromRoll,fromAngle=42.5F,fromBearing=90,lastPitch,lastYaw,lastRoll,lastAngle=42.5F,lastBearing=90;int evidence;}
 public static boolean eligible(LivingEntity e){return e.getMainHandItem().is(Hollowforged.WORLDSHARD.get())&&!e.isSwimming()&&!e.isFallFlying()&&!e.isPassenger();}
 public static HammerSwingCurve.Sample sample(LivingEntity e,float age){
  if(!eligible(e))return HammerSwingCurve.sample(0);
  var s=STATES.computeIfAbsent(e,k->new State());
  if(s.tick!=e.tickCount){
   if(e.swinging&&e.swingingArm==InteractionHand.MAIN_HAND&&(!s.wasSwinging||e.swingTime<s.lastTime)){
    s.start=e.tickCount;s.fromPitch=s.lastPitch;s.fromYaw=s.lastYaw;s.fromRoll=s.lastRoll;s.fromAngle=s.lastAngle;s.fromBearing=s.lastBearing;s.evidence=0;
   }
   s.tick=e.tickCount;s.lastTime=e.swingTime;s.wasSwinging=e.swinging;
  }
  float t=(age-s.start)/HammerSwingCurve.DURATION;var pose=HammerSwingCurve.sample(t);
  // A repeated early click restarts from the current pose rather than snapping to rest.
  float blend=t>=0&&t<.30F?1-(float)Mth.smoothstep(t/.30F):0;
  float p=pose.pitch()+s.fromPitch*blend,y=pose.yaw()+s.fromYaw*blend,r=pose.roll()+s.fromRoll*blend;
  p=Mth.clamp(p,-2.1F,.65F);y=Mth.clamp(y,-.28F,.22F);r=Mth.clamp(r,-.15F,.15F);float angle=pose.shaftAngle()+(s.fromAngle-42.5F)*blend,bearing=pose.bearing()+(s.fromBearing-90)*blend;angle=Mth.clamp(angle,-16,125);bearing=Mth.clamp(bearing,90,90);s.lastPitch=p;s.lastYaw=y;s.lastRoll=r;s.lastAngle=angle;s.lastBearing=bearing;
  if(Boolean.getBoolean("lc_hollowforged.capture")&&t>0&&t<1){int stage=t<.34?1:t<.64?2:4;if((s.evidence&stage)==0){s.evidence|=stage;ReviewCapture.evidence("SWING_STAGE "+(stage==1?"LIFT":stage==2?"DRIVE":"RECOVER"));}}
  return new HammerSwingCurve.Sample(p,y,r,pose.twist(),pose.lift(),angle,bearing);
 }
 public static void apply(HumanoidModel<?> model,LivingEntity e,float walk,float amount,float age){
  if(!eligible(e)||e.isUsingItem())return;
  var pose=sample(e,age);
  boolean right=e.getMainArm()==HumanoidArm.RIGHT;float sign=right?1:-1;
  var arm=right?model.rightArm:model.leftArm;var other=right?model.leftArm:model.rightArm;
  float walkPitch=WorldshardGripSpec.REST_X+Mth.cos(walk*.6662F+(right?Mth.PI:0))*amount*.06F+(e.isCrouching()?.15F:0);
  arm.xRot=walkPitch+pose.pitch();arm.yRot=pose.yaw()*sign;arm.zRot=(WorldshardGripSpec.REST_Z+pose.roll())*sign;
  model.body.yRot=pose.twist()*sign;
  model.rightArm.x=-Mth.cos(model.body.yRot)*5;model.rightArm.z=Mth.sin(model.body.yRot)*5;
  model.leftArm.x=Mth.cos(model.body.yRot)*5;model.leftArm.z=-Mth.sin(model.body.yRot)*5;
  other.xRot=Mth.cos(walk*.6662F+(right?0:Mth.PI))*amount+(e.isCrouching()?.4F:0)-pose.lift()*.16F;
 }
}
