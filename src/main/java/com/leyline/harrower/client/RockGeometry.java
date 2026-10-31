package com.leyline.harrower.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Standard ModelParts with a few faceted stones. The animation/fragment visitor is unchanged. */
public final class RockGeometry {
 private static float n(JsonArray a,int i){return a.get(i).getAsFloat();}
 public static ModelPart bake(JsonObject model){
  Map<String,JsonObject> bones=new LinkedHashMap<>();
  for(var e:model.getAsJsonArray("bones")){var b=e.getAsJsonObject();bones.put(b.get("name").getAsString(),b);}
  JsonArray texture=model.getAsJsonArray("texture_size");Map<String,ModelPart> roots=new LinkedHashMap<>();
  for(var b:bones.values())if(b.get("parent").isJsonNull())roots.put(b.get("name").getAsString(),bone(b,bones,n(texture,0),n(texture,1)));
  return part(List.of(),roots,PartPose.ZERO);
 }
 private static ModelPart part(List<ModelPart.Cube> cubes,Map<String,ModelPart> children,PartPose pose){
  ModelPart p=new ModelPart(cubes,children);p.setInitialPose(pose);p.loadPose(pose);return p;
 }
 private static ModelPart bone(JsonObject b,Map<String,JsonObject> bones,float tw,float th){
  String name=b.get("name").getAsString();JsonArray pivot=b.getAsJsonArray("pivot"),r=b.getAsJsonArray("rotation");
  boolean top=b.get("parent").isJsonNull();JsonArray pp=top?null:bones.get(b.get("parent").getAsString()).getAsJsonArray("pivot");
  Map<String,ModelPart> children=new LinkedHashMap<>();int index=0;
  for(var item:b.getAsJsonArray("cubes")){
   JsonObject c=item.getAsJsonObject();JsonArray f=c.getAsJsonArray("from"),t=c.getAsJsonArray("to"),o=c.getAsJsonArray("origin"),rot=c.getAsJsonArray("rotation"),uv=c.getAsJsonArray("uv_offset");
   float x=n(f,0)-n(o,0),y=n(o,1)-n(t,1),z=n(f,2)-n(o,2),dx=n(t,0)-n(f,0),dy=n(t,1)-n(f,1),dz=n(t,2)-n(f,2);
   ModelPart.Cube cube=c.has("mesh")?new MeshCube(c,(int)n(uv,0),(int)n(uv,1),x,y,z,dx,dy,dz,tw,th):new ModelPart.Cube((int)n(uv,0),(int)n(uv,1),x,y,z,dx,dy,dz,0,0,0,false,tw,th,EnumSet.allOf(Direction.class));
   PartPose pose=PartPose.offsetAndRotation(n(o,0)-n(pivot,0),n(pivot,1)-n(o,1),n(o,2)-n(pivot,2),-n(rot,0)*Mth.DEG_TO_RAD,n(rot,1)*Mth.DEG_TO_RAD,-n(rot,2)*Mth.DEG_TO_RAD);
   children.put("cube_"+(index++),part(List.of(cube),Map.of(),pose));
  }
  for(var child:bones.values())if(!child.get("parent").isJsonNull()&&child.get("parent").getAsString().equals(name))children.put(child.get("name").getAsString(),bone(child,bones,tw,th));
  PartPose pose=PartPose.offsetAndRotation(n(pivot,0)-(top?0:n(pp,0)),(top?24:0)-n(pivot,1)+(top?0:n(pp,1)),n(pivot,2)-(top?0:n(pp,2)),-n(r,0)*Mth.DEG_TO_RAD,n(r,1)*Mth.DEG_TO_RAD,-n(r,2)*Mth.DEG_TO_RAD);
  return part(List.of(),children,pose);
 }
 /** Exact lowest mesh vertex after rotation, so tapered fragments do not rest on empty box corners. */
 public static double lowerExtent(ModelPart.Cube cube,Matrix4f transform){
  float cx=(cube.minX+cube.maxX)/32,cy=(cube.minY+cube.maxY)/32,cz=(cube.minZ+cube.maxZ)/32;
  if(cube instanceof MeshCube mesh){double lowest=Double.POSITIVE_INFINITY;for(Vector3f v:mesh.vertices){double y=transform.m01()*(v.x-cx)+transform.m11()*(v.y-cy)+transform.m21()*(v.z-cz);lowest=Math.min(lowest,y);}return -lowest;}
  return Math.abs(transform.m01())*(cube.maxX-cube.minX)/32+Math.abs(transform.m11())*(cube.maxY-cube.minY)/32+Math.abs(transform.m21())*(cube.maxZ-cube.minZ)/32;
 }
 private record Face(int[] indices,float[][] uv,Vector3f normal){}
 private static final class MeshCube extends ModelPart.Cube {
  final Vector3f[] vertices;final List<Face> faces=new ArrayList<>();
  MeshCube(JsonObject c,int u,int v,float x,float y,float z,float dx,float dy,float dz,float tw,float th){
   super(u,v,x,y,z,dx,dy,dz,0,0,0,false,tw,th,EnumSet.allOf(Direction.class));
   JsonObject mesh=c.getAsJsonObject("mesh");JsonArray points=mesh.getAsJsonArray("vertices"),origin=c.getAsJsonArray("origin");vertices=new Vector3f[points.size()];
   for(int i=0;i<points.size();i++){var p=points.get(i).getAsJsonArray();vertices[i]=new Vector3f((n(p,0)-n(origin,0))/16,(n(origin,1)-n(p,1))/16,(n(p,2)-n(origin,2))/16);}
   for(var e:mesh.getAsJsonArray("faces")){
    var f=e.getAsJsonObject();JsonArray ids=f.getAsJsonArray("vertices"),uv=f.getAsJsonArray("uv");int count=ids.size();int[] order=new int[count];float[][] tex=new float[count][2];
    // BB's Y-up to native Y-down conversion is a reflection: reverse winding once.
    for(int i=0;i<count;i++){int j=count-1-i;order[i]=ids.get(j).getAsInt();var q=uv.get(j).getAsJsonArray();tex[i][0]=n(q,0)/tw;tex[i][1]=n(q,1)/th;}
    Vector3f normal=new Vector3f();
    for(int i=2;i<count&&normal.lengthSquared()<1e-10F;i++)normal.set(vertices[order[1]]).sub(vertices[order[0]]).cross(new Vector3f(vertices[order[i]]).sub(vertices[order[0]]));
    if(normal.lengthSquared()<1e-10F)throw new IllegalArgumentException("Degenerate rock face: "+c.get("name"));
    faces.add(new Face(order,tex,normal.normalize()));
   }
  }
  @Override public void compile(PoseStack.Pose pose,VertexConsumer out,int light,int overlay,int color){
   for(Face f:faces){
    if(f.indices.length==4){for(int i=0;i<4;i++)vertex(pose,out,f,i,light,overlay,color);}
    else for(int i=1;i<f.indices.length-1;i++){vertex(pose,out,f,0,light,overlay,color);vertex(pose,out,f,i,light,overlay,color);vertex(pose,out,f,i+1,light,overlay,color);vertex(pose,out,f,i+1,light,overlay,color);}
   }
  }
  private void vertex(PoseStack.Pose pose,VertexConsumer out,Face f,int i,int light,int overlay,int color){
   Vector3f p=vertices[f.indices[i]];out.addVertex(pose,p.x,p.y,p.z).setColor(color).setUv(f.uv[i][0],f.uv[i][1]).setOverlay(overlay).setLight(light).setNormal(pose,f.normal.x,f.normal.y,f.normal.z);
  }
 }
 private RockGeometry(){}
}
