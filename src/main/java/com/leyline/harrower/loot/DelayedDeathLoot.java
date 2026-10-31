package com.leyline.harrower.loot;

import com.leyline.harrower.*;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Rolls are committed at death; only their physical delivery waits for the existing crumble. */
@EventBusSubscriber(modid=HarrowerMod.ID)
public final class DelayedDeathLoot {
 public static final int RELEASE_TICK=410;
 public static final String KEY="harrower_delayed_loot";
 public static void capture(Harrower boss,Collection<ItemEntity> drops,int xp){
  var data=boss.getPersistentData();if(data.contains(KEY))return;
  ListTag items=new ListTag();for(ItemEntity entity:drops){CompoundTag saved=new CompoundTag();entity.saveWithoutId(saved);items.add(saved);}
  CompoundTag pending=new CompoundTag();pending.put("Items",items);pending.putInt("XP",xp);pending.putBoolean("Released",false);data.put(KEY,pending);drops.clear();
 }
 @SubscribeEvent public static void tick(EntityTickEvent.Post event){if(event.getEntity() instanceof Harrower boss&&boss.isDeadOrDying()&&boss.deathTime>=RELEASE_TICK)release(boss);}
 public static void release(Harrower boss){
  if(!(boss.level() instanceof ServerLevel level)||!boss.isDeadOrDying()||boss.deathTime<RELEASE_TICK)return;
  CompoundTag pending=boss.getPersistentData().getCompound(KEY);if(!boss.getPersistentData().contains(KEY)||pending.getBoolean("Released"))return;
  Vec3 center=boss.position().add(Vec3.directionFromRotation(0,boss.getYRot()).scale(3.2));
  if(!level.hasChunkAt(BlockPos.containing(center)))center=boss.position();
  var hit=level.clip(new ClipContext(center.add(0,5,0),center.add(0,-10,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss));
  center=(hit.getType()==HitResult.Type.BLOCK?hit.getLocation():boss.position()).add(0,1.3,0);
  ListTag items=pending.getList("Items",Tag.TAG_COMPOUND);
  for(int i=0;i<items.size();){
   CompoundTag saved=items.getCompound(i);
   // A successful insertion is removed from escrow immediately; later retries cannot duplicate it.
   if(saved.hasUUID("UUID")&&level.getEntity(saved.getUUID("UUID"))!=null){items.remove(i);continue;}
   ItemEntity item=new ItemEntity(level,center.x,center.y,center.z,ItemStack.EMPTY);item.load(saved);
   var random=net.minecraft.util.RandomSource.create(item.getUUID().getLeastSignificantBits());item.setPos(center.add((random.nextDouble()-.5)*1.5,0,(random.nextDouble()-.5)*1.5));item.setDefaultPickUpDelay();
   if(!item.getItem().isEmpty()&&level.addFreshEntity(item))items.remove(i);else i++;
  }
  if(items.isEmpty()){
   if(!pending.getBoolean("XPPaid")){ExperienceOrb.award(level,center,pending.getInt("XP"));pending.putBoolean("XPPaid",true);}
   pending.putBoolean("Released",true);
  }
 }
 private DelayedDeathLoot(){}
}
