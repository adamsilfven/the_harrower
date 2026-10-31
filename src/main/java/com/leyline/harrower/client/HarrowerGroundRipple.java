package com.leyline.harrower.client;

import com.leyline.harrower.*;
import com.leyline.harrower.combat.CombatMove;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Surface replicas are visual only: no block/entity creation and no terrain mutation. */
@EventBusSubscriber(modid=HarrowerMod.ID,value=Dist.CLIENT)
public final class HarrowerGroundRipple {
 private record Tile(BlockPos position,BlockState state,double distance){}
 private record Wave(int serial,List<Tile> tiles){}
 private static final Map<Harrower,Wave> waves=new WeakHashMap<>();
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  var mc=Minecraft.getInstance();if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES||mc.level==null||mc.player==null)return;
  float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
  var buffers=mc.renderBuffers().bufferSource();boolean drew=false;
  for(Harrower boss:mc.level.getEntitiesOfClass(Harrower.class,mc.player.getBoundingBox().inflate(64))){
   boolean hollow=boss.action()==CombatMove.HOLLOWING.id;
   if(boss.action()!=Harrower.SLAM&&!hollow){waves.remove(boss);continue;}
   float clock=boss.actionSeconds(partial)*20,impact=hollow?80:40,speed=hollow?.5F:.35F;
   int range=hollow?24:12;if(clock<impact-4||clock>impact+range/speed+12)continue;
   Wave wave=waves.get(boss);
   if(wave==null||wave.serial()!=boss.actionSequence()){
    Vec3 origin=hollow?boss.position():StrikeVolumes.groundPoint(boss,"rupture",40);List<Tile> tiles=new ArrayList<>();
    for(int dx=-range-1;dx<=range+1;dx++)for(int dz=-range-1;dz<=range+1;dz++){
     int x=Mth.floor(origin.x)+dx,z=Mth.floor(origin.z)+dz;double distance=Math.hypot(x+.5-origin.x,z+.5-origin.z);if(distance>range)continue;
     for(int y=Mth.floor(boss.getY()+2);y>=Mth.floor(boss.getY()-5);y--){
      BlockPos pos=new BlockPos(x,y,z);if(!mc.level.hasChunkAt(pos))break;BlockState state=mc.level.getBlockState(pos);
      if(!state.isCollisionShapeFullBlock(mc.level,pos))continue;
      tiles.add(new Tile(pos,state,distance));BlockPos cap=pos.above();BlockState above=mc.level.getBlockState(cap);
      if(!above.isAir()&&(!above.getCollisionShape(mc.level,cap).isEmpty()||above.is(Blocks.SNOW)))tiles.add(new Tile(cap,above,distance));break;
     }
    }
    wave=new Wave(boss.actionSequence(),tiles);waves.put(boss,wave);
   }
   PoseStack pose=event.getPoseStack();Vec3 camera=event.getCamera().getPosition();
   for(Tile tile:wave.tiles()){
    double age=clock-impact-tile.distance()/speed;if(age<=0||age>=12)continue;
    float progress=(float)(age/12);float rise=Mth.sin(progress*Mth.PI)*1.05F;
    pose.pushPose();BlockPos pos=tile.position();pose.translate(pos.getX()-camera.x+.5,pos.getY()-camera.y+rise+.5,pos.getZ()-camera.z+.5);
    pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(progress*Mth.PI)*8));pose.translate(-.5,-.5,-.5);
    mc.getBlockRenderer().renderSingleBlock(tile.state(),pose,buffers,LevelRenderer.getLightColor(mc.level,pos.above()),net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,ModelData.EMPTY,null);
    pose.popPose();drew=true;
   }
  }
  if(drew)buffers.endBatch();
 }
 private HarrowerGroundRipple(){}
}
