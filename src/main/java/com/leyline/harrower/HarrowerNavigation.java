package com.leyline.harrower;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;

/** Scope the physical body width to synchronous navigation calls; combat retains the full silhouette. */
final class HarrowerNavigation extends GroundPathNavigation {
 private final Harrower boss;
 HarrowerNavigation(Harrower boss,Level level){super(boss,level);this.boss=boss;}
 @Override protected Path createPath(Set<BlockPos> targets,int range,boolean offset,int accuracy,float distance){
  boolean previous=boss.navigationGeometry();boss.navigationGeometry(true);
  try{return super.createPath(targets,range,offset,accuracy,distance);}finally{boss.navigationGeometry(previous);}
 }
 @Override public void tick(){boolean previous=boss.navigationGeometry();boss.navigationGeometry(true);try{super.tick();}finally{boss.navigationGeometry(previous);}}
}
