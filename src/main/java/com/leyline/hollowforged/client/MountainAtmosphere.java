package com.leyline.hollowforged.client;
import com.leyline.hollowforged.Hollowforged;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;
/** A few local flakes released at the end of a breath; no terrain mutation. */
// Mountain King uses quiet crafted plates; the old shedding effect is not registered.
public final class MountainAtmosphere {
 @SubscribeEvent public static void tick(ClientTickEvent.Post event){
  var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.isPaused())return;
  for(var p:mc.level.players()){
   if(p.distanceToSqr(mc.player)>48*48||p.isInvisible()||p.isSpectator()||p.isInWater()||!p.getItemBySlot(EquipmentSlot.CHEST).is(Hollowforged.CARAPACE.get()))continue;
   if((p.tickCount+p.getId()*13)%121!=87)continue;
   double yaw=Math.toRadians(p.yBodyRot);double side=p.getRandom().nextBoolean()?.40:-.40;
   double x=p.getX()+Math.cos(yaw)*side,z=p.getZ()+Math.sin(yaw)*side;
   for(int i=0;i<2;i++)mc.level.addParticle(new DustParticleOptions(new Vector3f(.34F,.40F,.46F),.55F),x,p.getY()+1.32-i*.09,z,.006*side,-.025,0);
  }
 }
}
