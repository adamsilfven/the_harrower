package com.leyline.hollowforged.client;
import com.leyline.hollowforged.Hollowforged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
/** Adds the equipped Mountain King arm over the normal first-person skin. */
@EventBusSubscriber(modid=Hollowforged.ID,value=Dist.CLIENT)
public final class FirstPersonArmor {
 private static LivingMountainModel model;
 @SubscribeEvent public static void render(RenderArmEvent e){
  var p=e.getPlayer();if(p.isInvisible()||!p.getItemBySlot(EquipmentSlot.CHEST).is(Hollowforged.CARAPACE.get()))return;
  if(model==null)model=new LivingMountainModel(HollowArmorGeometry.bake(EquipmentSlot.CHEST),EquipmentSlot.CHEST);
  model.prepare(p,Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
  var arm=e.getArm()==HumanoidArm.RIGHT?model.rightArm:model.leftArm;arm.xRot=arm.yRot=arm.zRot=0;
  var pose=e.getPoseStack();var buffer=e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(Hollowforged.id("textures/models/armor/hollowforged_layer_1.png")));
  pose.pushPose();arm.translateAndRotate(pose);
  for(String name:new String[]{"arm_underlayer","forearm_bracer","gauntlet_cuff","gauntlet_face_rim","gauntlet_face_olive","gauntlet_brass_tip","gauntlet_shell","knuckle_guard","finger_tip_0","finger_tip_1","finger_tip_2","thumb_base","thumb_tip"})if(arm.hasChild(name))arm.getChild(name).render(pose,buffer,e.getPackedLight(),OverlayTexture.NO_OVERLAY);
  pose.popPose();
  e.setCanceled(true); // Vanilla skin and sleeve would otherwise draw through the gauntlet.
  if(Boolean.getBoolean("lc_hollowforged.capture"))ReviewCapture.evidence("FIRST_PERSON_ARMOR "+e.getArm());
 }
}
