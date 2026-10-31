package com.leyline.harrower.ritual;

import com.leyline.harrower.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Uses the boss's EXISTING terrain footprint, rather than treating its arm hitbox as a floor slab. */
public final class CairnSites {
 public static Vec3 find(ServerLevel level,ServerPlayer player,CairnFrame frame){
  double look=Math.atan2(player.getLookAngle().z,player.getLookAngle().x);
  Harrower probe=HarrowerMod.HARROWER.get().create(level);if(probe==null)return null;
  for(int turn=0;turn<36;turn++)for(int radius:new int[]{15,13,17,12,18}){
   double angle=look+(turn==0?0:((turn+1)/2)*(turn%2==1?1:-1)*Math.PI/18);
   int x=(int)Math.floor(player.getX()+Math.cos(angle)*radius),z=(int)Math.floor(player.getZ()+Math.sin(angle)*radius);
   if(!level.hasChunkAt(new BlockPos(x,player.getBlockY(),z)))continue;
   for(int dy:new int[]{-1,0,-2,1,-3,2,-4,3}){
    BlockPos ground=new BlockPos(x,player.getBlockY()+dy,z);
    if(!level.getFluidState(ground).isEmpty())continue;
    VoxelShape shape=level.getBlockState(ground).getCollisionShape(level,ground);
    if(shape.isEmpty())continue;
    Vec3 site=new Vec3(x+.5,ground.getY()+shape.max(Direction.Axis.Y),z+.5);
    if(valid(level,player,frame,site,probe))return site;
   }
  }return null;
 }
 public static boolean valid(ServerLevel level,ServerPlayer player,CairnFrame frame,Vec3 site){
  Harrower probe=HarrowerMod.HARROWER.get().create(level);return probe!=null&&valid(level,player,frame,site,probe);
 }
 private static boolean valid(ServerLevel level,ServerPlayer player,CairnFrame frame,Vec3 site,Harrower boss){
  double d=site.subtract(player.position()).horizontalDistance();
  if(d<11.5||d>18.75||Math.abs(site.y-player.getY())>3||site.distanceToSqr(frame.center())<36)return false;
  boss.moveTo(site.x,site.y,site.z,(float)(Math.toDegrees(Math.atan2(player.getZ()-site.z,player.getX()-site.x))-90),0);
  AABB silhouette=boss.getBoundingBox(),terrain=boss.terrainCollisionBox(silhouette);
  if(terrain.minY<level.getMinBuildHeight()||terrain.maxY>level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(silhouette))return false;
  if(!clear(level,terrain)||!level.noCollision(boss,terrain)||!supported(level,terrain,site.y))return false;
  if(!level.canSeeSky(BlockPos.containing(site.add(0,boss.getBbHeight()+.1,0))))return false;
  if(!level.getEntitiesOfClass(Harrower.class,silhouette.inflate(40),e->e.isAlive()).isEmpty())return false;
  var ray=level.clip(new ClipContext(player.getEyePosition(),site.add(0,2,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,player));
  if(ray.getType()!=HitResult.Type.MISS)return false;
  // Test the route against the boss's own navigation; no invented 9x9 perfectly level tunnel.
  BlockPos at=BlockPos.containing(site);
  if(!level.hasChunksAt(at.offset(-28,0,-28),at.offset(28,0,28)))return false;
  boss.setOnGround(true);boss.getNavigation().stop();
  var path=boss.getNavigation().createPath(player.blockPosition(),4,20);
  return path!=null&&path.canReach();
 }
 private static boolean clear(ServerLevel level,AABB box){
  for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
   if(!level.hasChunkAt(p)||!level.getFluidState(p).isEmpty())return false;
  return !level.getBlockCollisions(null,box.deflate(.001)).iterator().hasNext();
 }
 private static boolean supported(ServerLevel level,AABB box,double feet){
  int samples=0,support=0;
  for(double x=box.minX+.1;x<=box.maxX-.1;x+=.4)for(double z=box.minZ+.1;z<=box.maxZ-.1;z+=.4){
   samples++;if(floor(level,x,z,feet))support++;
  }
  // Stable central support plus broad sole support; shallow dips/grass/slabs do not invalidate a whole site.
  return samples>0&&support>=Math.ceil(samples*.8)&&floor(level,box.getCenter().x,box.getCenter().z,feet);
 }
 private static boolean floor(ServerLevel level,double x,double z,double feet){
  for(int dy=0;dy<2;dy++){
   BlockPos p=BlockPos.containing(x,feet-.01-dy,z);if(!level.hasChunkAt(p)||!level.getFluidState(p).isEmpty())return false;
   var shape=level.getBlockState(p).getCollisionShape(level,p);
   for(AABB local:shape.toAabbs()){
    AABB b=local.move(p);if(x>=b.minX&&x<=b.maxX&&z>=b.minZ&&z<=b.maxZ&&b.maxY<=feet+.01&&b.maxY>=feet-.6)return true;
   }
  }return false;
 }
 private CairnSites(){}
}
