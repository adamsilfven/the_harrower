package com.leyline.harrower.client;
import com.leyline.harrower.Harrower;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
public final class HarrowerRenderer extends MobRenderer<Harrower,HarrowerModel>{
 private static final ResourceLocation TEX=ResourceLocation.fromNamespaceAndPath("lc_harrower","textures/entity/harrower.png");
 public HarrowerRenderer(EntityRendererProvider.Context c){super(c,new HarrowerModel(HarrowerModel.rockyRoot()),3.8F);addLayer(new EyesLayer<Harrower,HarrowerModel>(this){@Override public RenderType renderType(){return RenderType.eyes(ResourceLocation.fromNamespaceAndPath("lc_harrower","textures/entity/harrower_emissive.png"));}});}
 @Override public ResourceLocation getTextureLocation(Harrower e){return TEX;}
 @Override protected void setupRotations(Harrower entity,com.mojang.blaze3d.vertex.PoseStack pose,float age,float yaw,float partial,float scale){
  super.setupRotations(entity,pose,age,yaw,partial,scale);
  if(entity.isDeadOrDying()||entity.action()==Harrower.DEATH){getModel().corpseWorldPose(entity,yaw,partial,scale);pose.translate(0,entity.corpseLift(),0);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-entity.corpseTilt()));pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-entity.corpseRoll()));}
 }
 @Override public void render(Harrower entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
  if((entity.isDeadOrDying()||entity.action()==Harrower.DEATH)&&entity.actionSeconds(partial)*20>=440)return;
  super.render(entity,yaw,partial,pose,buffers,light);
 }
 @Override protected float getFlipDegrees(Harrower e){return 0;}
}
