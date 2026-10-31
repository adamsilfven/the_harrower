package com.leyline.harrower.checks;
import com.leyline.harrower.*;
import com.leyline.hollowforged.Hollowforged;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** No manual entity ticks or synthetic tick events: verify the production server scheduler. */
public final class NaturalDelayedDropCheck {
 final ServerLevel level;final List<String> lines;final Runnable finish;int ticks,paidTick,xp;Harrower corpse;
 NaturalDelayedDropCheck(MinecraftServer server,List<String> lines,Runnable finish){level=server.overworld();this.lines=lines;this.finish=finish;level.setChunkForced(0,1,true);}
 static void start(MinecraftServer server,List<String> lines,Runnable finish){NeoForge.EVENT_BUS.register(new NaturalDelayedDropCheck(server,lines,finish));}
 int cores(){return level.getEntitiesOfClass(ItemEntity.class,RitualChecks.AREA,e->!e.isRemoved()).stream().filter(e->e.getItem().is(Hollowforged.HOLLOW_CORE.get())).mapToInt(e->e.getItem().getCount()).sum();}
 @SubscribeEvent public void tick(ServerTickEvent.Post event){
  try{
   ticks++;
   if(ticks==40){
    for(var e:level.getEntitiesOfClass(Entity.class,RitualChecks.AREA))if(e instanceof Harrower||e instanceof ItemEntity||e instanceof ExperienceOrb)e.discard();
    corpse=HarrowerMod.HARROWER.get().create(level);corpse.setPos(2,100,20);corpse.setHealth(1);level.addFreshEntity(corpse);corpse.hurt(RitualChecks.player.damageSources().playerAttack(RitualChecks.player),10000);
    if(!corpse.isDeadOrDying())throw new AssertionError("native lethal hit did not kill");
   }
   if(corpse==null)return;
   if(corpse.deathTime<410){if(cores()!=0||LootChecks.xp()!=0)throw new AssertionError("premature native payout at "+corpse.deathTime);}
   else if(paidTick==0){
    xp=LootChecks.xp();if(cores()!=1||xp<1000||xp>1500)throw new AssertionError("missing native crumble payout: cores="+cores()+" xp="+xp+" deathTime="+corpse.deathTime);
    paidTick=ticks;lines.add("PASS natural scheduler: zero Core/XP before crumble; one Core and "+xp+" XP appeared at native death tick "+corpse.deathTime);
   }else if(ticks-paidTick>=20){if(cores()!=1||LootChecks.xp()!=xp)throw new AssertionError("duplicate native payout");lines.add("NATURAL_DELAYED_LOOT_PASS: actual server ticking; no synthetic ticks; payout remains single");end();}
   if(ticks>500)throw new AssertionError("native death clock stalled: "+corpse.deathTime);
  }catch(Throwable failure){lines.add("NATURAL_DELAYED_LOOT_FAIL "+failure);end();}
 }
 void end(){NeoForge.EVENT_BUS.unregister(this);level.setChunkForced(0,1,false);finish.run();}
}
