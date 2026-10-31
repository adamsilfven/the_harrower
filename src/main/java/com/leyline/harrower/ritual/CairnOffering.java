package com.leyline.harrower.ritual;

import com.leyline.harrower.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid=HarrowerMod.ID)
public final class CairnOffering {
 @SubscribeEvent public static void tick(EntityTickEvent.Post event){
  if(!(event.getEntity() instanceof ItemEntity item)||!(item.level() instanceof ServerLevel level)||item.tickCount%5!=0||item.getItem().isEmpty())return;
  if(!BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString().equals("hollow_fragment_addon:hollow_fragment"))return;
  if(item.getPersistentData().getBoolean("lc_cairn_used")||item.getPersistentData().getBoolean("lc_cairn_refund")||!(item.getOwner() instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator()||player.serverLevel()!=level||player.distanceToSqr(item)>24*24)return;
  long now=level.getGameTime();if(item.getPersistentData().getLong("lc_cairn_retry")>now)return;
  CairnFrame frame=CairnFrame.find(level,item.position());if(frame==null)return;
  item.getPersistentData().putLong("lc_cairn_retry",now+60);
  if(level.getDifficulty()==Difficulty.PEACEFUL){tell(player,"The Cairn cannot answer in Peaceful difficulty.");return;}
  if(CairnLocks.get(level).near(item.position())||!level.getEntitiesOfClass(CairnRitual.class,item.getBoundingBox().inflate(64)).isEmpty()||!level.getEntitiesOfClass(Harrower.class,item.getBoundingBox().inflate(48),e->e.isAlive()).isEmpty()){
   tell(player,"Another summoning or Harrower is too close. The Fragment remains yours.");return;
  }
  var site=CairnSites.find(level,player,frame);
  if(site==null){tell(player,"The Harrower needs open, level ground nearby. The Fragment remains yours.");return;}
  CairnRitual ritual=CairnRegistry.TYPE.create(level);if(ritual==null)return;
  ritual.prepare(player,frame,site,item.getItem());
  if(!level.addFreshEntity(ritual))return;
  CairnLocks.get(level).reserve(ritual.getUUID(),frame.center());
  item.getPersistentData().putBoolean("lc_cairn_used",true);
  item.getItem().shrink(1);if(item.getItem().isEmpty())item.discard();else item.setItem(item.getItem().copy());
 }
 private static void tell(ServerPlayer player,String text){player.displayClientMessage(Component.literal(text),true);}
 private CairnOffering(){}
}
