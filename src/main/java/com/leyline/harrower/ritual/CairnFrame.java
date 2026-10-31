package com.leyline.harrower.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Origin is always the minimum X/Z perimeter corner. No terrain changes. */
public record CairnFrame(BlockPos origin,boolean alongX) {
 public BlockPos cell(int u,int v){return origin.offset(alongX?u:v,0,alongX?v:u);}
 public Vec3 center(){return Vec3.atLowerCornerOf(origin).add(alongX?2:1.5,1.002,alongX?1.5:2);}
 public boolean valid(ServerLevel level){
  for(int u=0;u<4;u++)for(int v=0;v<3;v++){
   BlockPos p=cell(u,v);if(!level.hasChunkAt(p))return false;
   if(u==0||u==3||v==0||v==2){if(!level.getBlockState(p).is(Blocks.POLISHED_DEEPSLATE))return false;}
   else { // Both opening cells and their headroom must remain empty and dry.
    for(int dy=0;dy<=1;dy++){BlockPos q=p.above(dy);if(!level.getBlockState(q).isAir()||!level.getFluidState(q).isEmpty())return false;}
   }
  }
  return true;
 }
 public static CairnFrame find(ServerLevel level,Vec3 item){
  BlockPos at=BlockPos.containing(item);
  for(int y=at.getY();y>=at.getY()-1;y--)for(boolean x:new boolean[]{true,false})for(int u=1;u<=2;u++){
   BlockPos o=new BlockPos(at.getX()-(x?u:1),y,at.getZ()-(x?1:u));CairnFrame frame=new CairnFrame(o,x);
   if(item.y>=y-.1&&item.y<=y+1.5&&frame.valid(level))return frame;
  }
  return null;
 }
}
