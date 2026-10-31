package com.leyline.harrower.client;

import com.leyline.harrower.Harrower;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Client-only rubble. It does not create blocks, items, collision, or damage. */
@EventBusSubscriber(modid="lc_harrower",value=Dist.CLIENT)
public final class HarrowerRubble {
 private record Stone(Vec3 pos,float sx,float sy,float sz,float rotation,float delay,BlockState state){}
 private static final Map<Harrower,List<Stone>> PILES=new WeakHashMap<>();
 private static net.minecraft.client.multiplayer.ClientLevel world;
 private static final BlockState[] STATES={Blocks.DEEPSLATE.defaultBlockState(),Blocks.COBBLED_DEEPSLATE.defaultBlockState(),Blocks.TUFF.defaultBlockState()};
 private static List<Stone> make(Harrower b){
  var result=new ArrayList<Stone>();var rng=new Random(b.getUUID().getLeastSignificantBits());
  Vec3 forward=Vec3.directionFromRotation(0,b.getYRot()),right=new Vec3(forward.z,0,-forward.x);
  for(int i=0;i<140;i++){
   double side=rng.nextGaussian()*1.65,along=3.2+rng.nextGaussian()*2.0;
   side=Mth.clamp(side,-4.3,4.3);along=Mth.clamp(along,-1.0,7.5);
   Vec3 origin=b.position().add(right.scale(side)).add(forward.scale(along));
   var hit=world.clip(new ClipContext(origin.add(0,5,0),origin.add(0,-10,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,b));
   if(hit.getType()!=HitResult.Type.BLOCK)continue;
   double mound=Math.max(0,1-Math.hypot(side/4.5,(along-3.2)/5));
   float sx=.6F+rng.nextFloat()*1.05F,sy=.4F+rng.nextFloat()*.7F,sz=.55F+rng.nextFloat()*1.0F;
   Vec3 pos=hit.getLocation().add(0,sy*.4+mound*rng.nextDouble()*1.3,0);
   result.add(new Stone(pos,sx,sy,sz,rng.nextFloat()*360,rng.nextFloat()*25,STATES[rng.nextInt(STATES.length)]));
  }return result;
 }
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  var mc=Minecraft.getInstance();if(mc.level!=world){PILES.clear();world=mc.level;}
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||world==null||mc.player==null)return;
  float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);var pose=event.getPoseStack();Vec3 camera=event.getCamera().getPosition();
  var original=mc.renderBuffers().bufferSource();boolean drew=false;
  for(Harrower boss:world.getEntitiesOfClass(Harrower.class,mc.player.getBoundingBox().inflate(80))){
   if(!(boss.isDeadOrDying()||boss.action()==Harrower.DEATH))continue;
   float clock=boss.actionSeconds(partial)*20;if(clock<340||clock>=600)continue;
   List<Stone> pile=PILES.computeIfAbsent(boss,HarrowerRubble::make);
   float alpha=Mth.clamp((600-clock)/140F,0,1);
   MultiBufferSource buffers=type->new AlphaConsumer(original.getBuffer(RenderType.translucent()),alpha);
   for(Stone s:pile){
    float appear=Mth.clamp((clock-340-s.delay)/35F,0,1);if(appear<=0)continue;
    float ease=appear*appear*(3-2*appear);Vec3 pos=s.pos.subtract(camera);
    pose.pushPose();pose.translate(pos.x,pos.y+(1-ease)*1.2,pos.z);pose.mulPose(Axis.YP.rotationDegrees(s.rotation));
    pose.scale(s.sx*ease,s.sy*ease,s.sz*ease);pose.translate(-.5,-.5,-.5);
    mc.getBlockRenderer().renderSingleBlock(s.state,pose,buffers,LevelRenderer.getLightColor(world,BlockPos.containing(s.pos)),OverlayTexture.NO_OVERLAY,ModelData.EMPTY,RenderType.translucent());pose.popPose();drew=true;
   }
  }if(drew)original.endBatch(RenderType.translucent());
 }
 private static final class AlphaConsumer implements VertexConsumer {
  private final VertexConsumer target;private final float alpha;
  AlphaConsumer(VertexConsumer target,float alpha){this.target=target;this.alpha=alpha;}
  public VertexConsumer addVertex(float x,float y,float z){target.addVertex(x,y,z);return this;}
  public VertexConsumer setColor(int r,int g,int b,int a){target.setColor(r,g,b,(int)(a*alpha));return this;}
  public VertexConsumer setUv(float u,float v){target.setUv(u,v);return this;}
  public VertexConsumer setUv1(int u,int v){target.setUv1(u,v);return this;}
  public VertexConsumer setUv2(int u,int v){target.setUv2(u,v);return this;}
  public VertexConsumer setNormal(float x,float y,float z){target.setNormal(x,y,z);return this;}
 }
 private HarrowerRubble(){}
}
