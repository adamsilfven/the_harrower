package com.leyline.harrower.combat;

import com.leyline.harrower.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid=HarrowerMod.ID)
public final class CombatEvents {
 /** Freeze participant scaling before the first health loss. Final mitigation is applied
  * after the complete event bus by DamagePipelineMixin, so other mods run first. */
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void incoming(LivingDamageEvent.Pre event){
  if(!(event.getEntity() instanceof Harrower boss))return;
  if(event.getSource().getEntity() instanceof ServerPlayer p)boss.combat().encounter.start(p);

 }
 @SubscribeEvent public static void received(LivingDamageEvent.Post event){
  if(event.getSource().getEntity() instanceof Harrower attacker&&attacker.combat().debug()){
   var victim=event.getEntity();org.slf4j.LoggerFactory.getLogger("HarrowerCombat").info("{} MITIGATED move={} source={} victim={} damage={} armor={} toughness={} health={}",attacker.getUUID(),attacker.combat().move(),event.getSource().getMsgId(),victim.getUUID(),event.getNewDamage(),victim.getArmorValue(),victim.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS),victim.getHealth());
  }
  if(event.getSource().getEntity() instanceof Harrower attacker && event.getEntity() instanceof ServerPlayer player && !player.getPersistentData().getBoolean("HarrowerHideDamage")){
   float hp=event.getNewDamage();String move=attacker.combat().move().name().toLowerCase(java.util.Locale.ROOT).replace('_',' ');
   player.sendSystemMessage(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"[Harrower] %s: %.2f HP (%.2f hearts) | source: %s",move,hp,hp/2,event.getSource().getMsgId())));
  }
  if(event.getEntity() instanceof Harrower boss&&event.getNewDamage()>0){boss.combat().encounter.received(event.getSource(),event.getNewDamage());boss.combat().teammateDamage(event.getSource(),event.getNewDamage());boss.combat().significantHit(event.getNewDamage());}
 }
 @SubscribeEvent public static void mount(EntityMountEvent event){if(event.isDismounting()&&event.getEntityBeingMounted() instanceof Harrower boss&&boss.isAlive()&&boss.combat().captured(event.getEntityMounting()))event.setCanceled(true);}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity().getVehicle() instanceof Harrower boss)boss.combat().emergencyRelease();}
 @SubscribeEvent public static void death(LivingDeathEvent event){if(event.getEntity().getVehicle() instanceof Harrower boss)boss.combat().emergencyRelease();}
}
