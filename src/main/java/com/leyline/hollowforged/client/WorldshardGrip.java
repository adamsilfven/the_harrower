package com.leyline.hollowforged.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Orientation changes about the solved butt grip, so the handle cannot slide through the hand. */
public final class WorldshardGrip {
 private static final float RAD=(float)Math.PI/180;
 private static Quaternionf display(float[] rotation,float sign){return new Quaternionf().rotationXYZ(rotation[0]*RAD,rotation[1]*RAD*sign,rotation[2]*RAD*sign);}
 public static void thirdPerson(PoseStack stack,HumanoidModel<?> model,LivingEntity entity,HumanoidArm hand){
  float sign=hand==HumanoidArm.RIGHT?1:-1;var part=hand==HumanoidArm.RIGHT?model.rightArm:model.leftArm;
  var swing=WorldshardPose.sample(entity,entity.tickCount+Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
  Quaternionf arm=new Quaternionf().rotationZYX(part.zRot,part.yRot,part.xRot);
  Quaternionf base=new Quaternionf().rotationX(-(float)Math.PI/2).rotateY((float)Math.PI);
  Quaternionf desired=new Quaternionf().rotationY(model.body.yRot).rotateX(swing.shaftAngle()*RAD).rotateZ(sign*(float)Math.PI);
  Quaternionf correction=new Quaternionf(base).invert().mul(new Quaternionf(arm).invert()).mul(desired).mul(display(WorldshardGripSpec.THIRD_ROTATION,sign).invert());
  stack.mulPose(correction);
  if(Boolean.getBoolean("lc_hollowforged.capture")&&!reported&&Math.abs(swing.shaftAngle()-42.5F)<.001F){reported=true;ReviewCapture.evidence("BUTT_GRIP true angle=42.5 modelY=-10.8");}
 }
 private static boolean reported;
 public static void firstPerson(PoseStack stack,HumanoidArm arm,HammerSwingCurve.Sample swing){
  float sign=arm==HumanoidArm.RIGHT?1:-1,t=swing.shaftAngle()*RAD,b=25*RAD;
  Vector3f direction=new Vector3f(sign*(float)(Math.sin(t)*Math.sin(b)),(float)Math.cos(t),(float)(-Math.sin(t)*Math.cos(b)));
  Vector3f normal=new Vector3f(0,0,1).sub(new Vector3f(direction).mul(direction.z)).normalize();
  Vector3f across=new Vector3f(direction).cross(normal).normalize();
  var frame=new Matrix3f().setColumn(0,across).setColumn(1,direction).setColumn(2,normal);
  stack.mulPose(new Quaternionf().setFromNormalized(frame).mul(display(WorldshardGripSpec.FIRST_ROTATION,sign).invert()));
 }
}
