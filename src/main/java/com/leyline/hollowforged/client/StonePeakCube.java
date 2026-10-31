package com.leyline.hollowforged.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import java.util.Set;
/** Continuous triangular stone prism; fits ordinary native ModelPart transforms. */
public final class StonePeakCube extends ModelPart.Cube {
 private final float[][] vertices;private final int u,v;
 private static final int[][] FACES={{0,1,2,2},{3,5,4,4},{0,2,5,3},{1,4,5,2},{0,3,4,1}};
 public StonePeakCube(float w,float h,float d,int u,int v){
  super(u,v,-w,-h,-d,w*2,h*2,d*2,0,0,0,false,2048,2048,Set.of());this.u=u;this.v=v;
  vertices=new float[][]{{-w,h,-d},{w,h,-d},{w*.13F,-h,-d*.45F},{-w,h,d},{w,h,d},{w*.13F,-h,d*.35F}};
 }
 @Override public void compile(PoseStack.Pose pose,VertexConsumer out,int light,int overlay,int color){
  for(int[] face:FACES){var a=vertices[face[0]];var b=vertices[face[1]];var c=vertices[face[2]];
   float ax=b[0]-a[0],ay=b[1]-a[1],az=b[2]-a[2],bx=c[0]-a[0],by=c[1]-a[1],bz=c[2]-a[2];
   float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);nx/=length;ny/=length;nz/=length;
   for(int i=0;i<4;i++){var p=vertices[face[i]];float tu=i==0?0:i==1?48:face[2]==face[3]?27:i==2?48:0;float tv=i<2?48:0;
    out.addVertex(pose,p[0]/16,p[1]/16,p[2]/16).setColor(color).setUv((u+tu)/2048F,(v+tv)/2048F).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
   }
  }
 }
}
