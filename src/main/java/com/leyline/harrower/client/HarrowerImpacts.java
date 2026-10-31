package com.leyline.harrower.client;

import com.leyline.harrower.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Presentation only. Debris particles cannot place blocks or damage entities. */
@EventBusSubscriber(modid=HarrowerMod.ID,value=Dist.CLIENT)
public final class HarrowerImpacts {
 @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event){
  if(Boolean.getBoolean("harrower.disableCameraShake"))return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  float strength=0;
  for(Harrower boss:mc.level.getEntitiesOfClass(Harrower.class,mc.player.getBoundingBox().inflate(32)))
   strength=Math.max(strength,boss.impactShake()*Mth.clamp(1-boss.distanceTo(mc.player)/32,0,1));
  double time=mc.level.getGameTime()+event.getPartialTick();
  event.setPitch(event.getPitch()+(float)Math.sin(time*2.17)*strength*.65F);
  event.setRoll(event.getRoll()+(float)Math.sin(time*1.63)*strength*.3F);
 }
 @SubscribeEvent public static void debris(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.isPaused()||mc.level.getGameTime()%2!=0)return;
  for(Harrower boss:mc.level.getEntitiesOfClass(Harrower.class,mc.player.getBoundingBox().inflate(48))){
   if(boss.action()==com.leyline.harrower.combat.CombatMove.HOLLOWING.id){
    float clock=boss.actionSeconds(1)*20;
    if(clock<80){
     Vec3 chest=boss.position().add(Vec3.directionFromRotation(0,boss.getYRot()).scale(1.5)).add(0,7,0);
     double contraction=1-clock/110.0;
     for(int i=0;i<6;i++){
      double a=clock*.16+i*Math.PI/3;Vec3 at=chest.add(Math.cos(a)*3*contraction,Math.sin(a*.7)*2*contraction,Math.sin(a)*3*contraction);
      Vec3 velocity=chest.subtract(at).normalize().scale(.12);
      mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,at.x,at.y,at.z,velocity.x,velocity.y,velocity.z);
      if(i%2==0)mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL,at.x,at.y,at.z,velocity.x,velocity.y,velocity.z);
     }
    }
   }
   if(boss.action()!=2)continue;
   float clock=boss.actionSeconds(1)*20;double radius=(clock-40)*.35;
   if(radius<=0||radius>12)continue;
   // Same authored ground origin as the server wave, including its ground projection.
   Vec3 center=com.leyline.harrower.StrikeVolumes.groundPoint(boss,"rupture",40);
   int count=32;for(int i=0;i<count;i++){
    double angle=2*Math.PI*i/count;double x=center.x+Math.cos(angle)*radius,z=center.z+Math.sin(angle)*radius;
    for(int y=Mth.floor(boss.getY()+2);y>=Mth.floor(boss.getY()-5);y--){
     BlockPos p=BlockPos.containing(x,y,z);var state=mc.level.getBlockState(p);var shape=state.getCollisionShape(mc.level,p);
     if(shape.isEmpty())continue;
     double top=y+shape.max(net.minecraft.core.Direction.Axis.Y);
     mc.particleEngine.add(new TerrainParticle(mc.level,x,top+.08,z,Math.cos(angle)*.06,.28+mc.level.random.nextDouble()*.12,Math.sin(angle)*.06,state).scale(4.5F));break;
    }
   }
  }
 }
 private HarrowerImpacts(){}
}
