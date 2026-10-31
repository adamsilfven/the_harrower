package com.leyline.harrower.client;

import com.leyline.harrower.*;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Bounded client-only stone fragments. No drops, damage, terrain edits or network entities. */
@EventBusSubscriber(modid=HarrowerMod.ID,value=Dist.CLIENT)
public final class HarrowerStoneChips {
 private static final List<Chip> chips=new ArrayList<>();
 private static final Map<Harrower,Seen> seen=new WeakHashMap<>();
 private static net.minecraft.client.multiplayer.ClientLevel level;
 private static final BlockState[] STONE={Blocks.DEEPSLATE.defaultBlockState(),Blocks.COBBLED_DEEPSLATE.defaultBlockState(),Blocks.TUFF.defaultBlockState()};
 private static final class Seen {int hit,cooldown;float clock;Seen(Harrower b){hit=b.chipSequence();clock=b.actionSeconds(1)*20;}}
 private static final class Chip {
  Vec3 at,old,velocity;final float size,spin;final BlockState stone;int age,restAge;boolean resting;
  Chip(Vec3 at,Vec3 v,float size,float spin,BlockState stone){this.at=at;old=at;velocity=v;this.size=size;this.spin=spin;this.stone=stone;}
 }
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();if(mc.level!=level){chips.clear();seen.clear();level=mc.level;}
  if(level==null||mc.player==null||mc.isPaused())return;
  chips.removeIf(c->++c.age>70);
  for(Chip c:chips){c.old=c.at;if(c.resting)continue;c.velocity=c.velocity.add(0,-.045,0).scale(.985);Vec3 next=c.at.add(c.velocity);
   var hit=level.clip(new ClipContext(c.at,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
   if(hit.getType()!=HitResult.Type.MISS){c.at=hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(c.size*.55));if(c.velocity.length()>.12&&hit.getDirection()==net.minecraft.core.Direction.UP)c.velocity=new Vec3(c.velocity.x*.5,-c.velocity.y*.24,c.velocity.z*.5);else{c.resting=true;c.restAge=c.age;c.velocity=Vec3.ZERO;}}
   else c.at=next;
  }
  for(Harrower b:level.getEntitiesOfClass(Harrower.class,mc.player.getBoundingBox().inflate(64))){
   Seen s=seen.get(b);if(s==null){s=new Seen(b);seen.put(b,s);}if(s.cooldown>0)s.cooldown--;
   if(s.hit!=b.chipSequence()){s.hit=b.chipSequence();if(s.cooldown==0&&level.random.nextFloat()<.65F){burst(b,3+level.random.nextInt(4),false);s.cooldown=6;}}
   float clock=b.actionSeconds(1)*20;
   if(b.action()==Harrower.DEATH){for(int beat:new int[]{28,62,96,120,342,355,368,381,394})if(s.clock<beat&&clock>=beat)burst(b,beat>=342?18:beat==120?28:12,true);}
   s.clock=clock;
  }
 }
 private static void burst(Harrower b,int count,boolean death){
  var r=level.random;float clock=b.actionSeconds(1)*20;double collapse=death?Math.min(1,Math.max(0,(clock-30)/90)):0;
  Vec3 forward=Vec3.directionFromRotation(0,b.getYRot()),right=new Vec3(forward.z,0,-forward.x);
  for(int i=0;i<count&&chips.size()<192;i++){
   double side=(r.nextDouble()-.5)*5.8,forwardOffset=(r.nextDouble()-.5)*1.8+collapse*5.5;
   double height=(6+r.nextDouble()*3)*(1-collapse)+(.5+r.nextDouble()*1.6)*collapse;
   Vec3 at=b.position().add(right.scale(side)).add(forward.scale(forwardOffset)).add(0,height+b.corpseLift(),0);
   Vec3 v=right.scale(side*.025).add(forward.scale((r.nextDouble()-.5)*.16)).add(0,.02+r.nextDouble()*.14,0);
   chips.add(new Chip(at,v,.13F+r.nextFloat()*.24F,(r.nextFloat()-.5F)*14,STONE[r.nextInt(STONE.length)]));
  }
 }
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||chips.isEmpty())return;
  var mc=Minecraft.getInstance();if(mc.level!=level)return;var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();Vec3 camera=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
  for(Chip c:chips){Vec3 at=c.old.lerp(c.at,partial).subtract(camera);float shrink=Math.min(1,(70-c.age-partial)/12F);float size=c.size*Math.max(0,shrink);
   pose.pushPose();pose.translate(at.x,at.y,at.z);pose.mulPose(Axis.XP.rotationDegrees(c.spin*(c.resting?c.restAge:c.age+partial)));pose.mulPose(Axis.ZP.rotationDegrees(c.spin*.7F*(c.resting?c.restAge:c.age+partial)));pose.scale(size,size*.7F,size*.85F);pose.translate(-.5,-.5,-.5);
   mc.getBlockRenderer().renderSingleBlock(c.stone,pose,buffers,LevelRenderer.getLightColor(level,BlockPos.containing(c.at)),OverlayTexture.NO_OVERLAY);pose.popPose();
  }
 }
 private HarrowerStoneChips(){}
}
