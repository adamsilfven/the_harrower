package com.leyline.hollowforged.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import java.util.Set;
/** Authored convex plate with real chamfer edges, shared with the editable mesh export. */
public final class MountainPlateCube extends ModelPart.Cube {
 private final float[][] vertices,uv;private final int[][] faces;
 public MountainPlateCube(float[][] vertices,int[][] faces,float[][] uv){
  super(0,0,-1,-1,-1,2,2,2,0,0,0,false,2048,2048,Set.of());this.vertices=vertices;this.faces=faces;this.uv=uv;
 }
 @Override public void compile(PoseStack.Pose pose,VertexConsumer out,int light,int overlay,int color){
  for(int f=0;f<faces.length;f++){
   var face=faces[f];var a=vertices[face[0]];var b=vertices[face[1]];var c=vertices[face[2]];
   float ax=b[0]-a[0],ay=b[1]-a[1],az=b[2]-a[2],bx=c[0]-a[0],by=c[1]-a[1],bz=c[2]-a[2];
   float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
   if(len<.000001F)continue;nx/=len;ny/=len;nz/=len;
   for(int i=0;i<4;i++){var p=vertices[face[i]];out.addVertex(pose,p[0]/16,p[1]/16,p[2]/16).setColor(color).setUv(uv[f][i*2]/2048,uv[f][i*2+1]/2048).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);}
  }
 }
}
