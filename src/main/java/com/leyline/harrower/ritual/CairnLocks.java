package com.leyline.harrower.ritual;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** Dimension-local locks survive unload/restart without loading any chunks. */
public final class CairnLocks extends SavedData {
 private final Map<UUID,Vec3> locks=new HashMap<>();
 public static CairnLocks get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new Factory<>(CairnLocks::new,CairnLocks::load),"lc_harrower_cairns");}
 public boolean near(Vec3 at){return locks.values().stream().anyMatch(p->p.distanceToSqr(at)<64*64);}
 public void reserve(UUID id,Vec3 at){locks.put(id,at);setDirty();}
 public void release(UUID id){if(locks.remove(id)!=null)setDirty();}
 private static CairnLocks load(CompoundTag tag,HolderLookup.Provider lookup){
  CairnLocks result=new CairnLocks();ListTag list=tag.getList("Locks",Tag.TAG_COMPOUND);
  for(int i=0;i<list.size();i++){CompoundTag row=list.getCompound(i);if(row.hasUUID("Id"))result.locks.put(row.getUUID("Id"),new Vec3(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z")));}return result;
 }
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){
  ListTag list=new ListTag();locks.forEach((id,p)->{CompoundTag row=new CompoundTag();row.putUUID("Id",id);row.putDouble("X",p.x);row.putDouble("Y",p.y);row.putDouble("Z",p.z);list.add(row);});tag.put("Locks",list);return tag;
 }
}
