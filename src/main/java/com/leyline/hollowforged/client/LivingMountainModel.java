package com.leyline.hollowforged.client;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
/** Crafted Mountain King plates follows the native humanoid joints. */
public final class LivingMountainModel extends HumanoidModel<LivingEntity> {
 private final EquipmentSlot slot;private static float minimum=1,maximum=-1;private static boolean reported;
 public LivingMountainModel(ModelPart root,EquipmentSlot slot){super(root);this.slot=slot;}
 public void prepare(LivingEntity entity,float partial){
  float breath=(float)Math.sin((entity.tickCount+partial)*.052F);
  for(var parent:new ModelPart[]{head,hat,body,leftArm,rightArm,leftLeg,rightLeg})parent.getAllParts().filter(p->p!=parent).forEach(p->{p.loadPose(p.getInitialPose());p.xScale=.5F;p.yScale=.5F;p.zScale=.5F;});
  if(slot==EquipmentSlot.CHEST){
   body.getAllParts().filter(p->p!=body).forEach(p->{var initial=p.getInitialPose();boolean front=initial.z<0;p.xScale=.5F*(1+.003F*breath);p.zScale=.5F*(1+.015F*breath);p.x=initial.x*(1+.002F*breath);p.z=initial.z+(front?-.035F:.025F)*breath;p.y=initial.y-.015F*breath;});
   for(var arm:new ModelPart[]{leftArm,rightArm})arm.getAllParts().filter(p->p!=arm).forEach(p->{var initial=p.getInitialPose();if(initial.y<3){p.y=initial.y-.025F*breath;p.z=initial.z-.012F*breath;}});
   if(entity instanceof net.minecraft.world.entity.player.Player&&Boolean.getBoolean("lc_hollowforged.capture")){minimum=Math.min(minimum,breath);maximum=Math.max(maximum,breath);if(!reported&&maximum-minimum>1.8F){reported=true;ReviewCapture.evidence("BREATH true");}}
  }
 }
}
