package com.leyline.harrower.client;

import com.leyline.harrower.*;
import com.leyline.harrower.combat.CombatMove;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Soul expulsion is presentation only. Hollowing's existing server wave owns all damage. */
@EventBusSubscriber(modid=HarrowerMod.ID,value=Dist.CLIENT)
public final class HarrowerHollowing {
 private static net.minecraft.client.multiplayer.ClientLevel level;
 private static final Map<Harrower,State> states=new WeakHashMap<>();
 private static final class State {int serial;float clock;List<Vec3> aims=List.of();State(Harrower b){serial=b.actionSequence();clock=b.actionSeconds(1)*20;}}
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();if(level!=mc.level){states.clear();level=mc.level;}if(level==null||mc.player==null||mc.isPaused())return;
  for(Harrower b:level.getEntitiesOfClass(Harrower.class,mc.player.getBoundingBox().inflate(64))){
   if(b.action()!=CombatMove.HOLLOWING.id){states.remove(b);continue;}
   State s=states.computeIfAbsent(b,State::new);float clock=b.actionSeconds(1)*20;
   if(s.serial!=b.actionSequence()){s.serial=b.actionSequence();s.clock=clock;s.aims=List.of();}
   Vec3 chest=b.position().add(Vec3.directionFromRotation(0,b.getYRot()).scale(1.5)).add(0,7,0);
   if(s.clock<80&&clock>=80){
    s.aims=level.players().stream().filter(p->!p.isSpectator()&&p.isAlive()&&p.distanceToSqr(b)<24*24).limit(4).map(p->p.getEyePosition()).toList();
    for(int i=0;i<70;i++){Vec3 v=new Vec3(level.random.nextGaussian(),level.random.nextGaussian()*.45,level.random.nextGaussian()).normalize().scale(.32);level.addParticle(ParticleTypes.LARGE_SMOKE,chest.x,chest.y,chest.z,v.x,v.y,v.z);}
   }
   if(clock>=80&&clock<128)for(Vec3 aim:s.aims){
    double distance=chest.subtract(aim).horizontalDistance(),travel=(clock-80)*.5;
    // A short train of outward soul streams; no projectile entities or hit callbacks.
    for(int trail=0;trail<7;trail++){
     double u=(travel-trail*.32)/Math.max(1,distance);if(u<0||u>1.08)continue;Vec3 at=chest.lerp(aim,u);Vec3 direction=aim.subtract(chest).normalize();
     for(int strand=0;strand<3;strand++){double a=clock*.4+strand*Math.PI*2/3;Vec3 p=at.add(Math.cos(a)*.38,Math.sin(a)*.3,Math.sin(a)*.38);level.addParticle(ParticleTypes.SOUL,p.x,p.y,p.z,direction.x*.15,direction.y*.15,direction.z*.15);}
     if(trail%2==0)level.addParticle(ParticleTypes.LARGE_SMOKE,at.x,at.y,at.z,direction.x*.06,.015,direction.z*.06);
    }
   }
   s.clock=clock;
  }
 }
 private HarrowerHollowing(){}
}
