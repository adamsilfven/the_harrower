package com.leyline.hollowforged.client;
import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.EquipmentSlot;
/** Mountain King plates: native rendering and editable export share authored mesh data. */
public final class HollowArmorGeometry {
 private static final String[] PARTS={"head","hat","body","right_arm","left_arm","right_leg","left_leg"};
 private static final float[][] PIVOTS={{0,0,0},{0,0,0},{0,0,0},{-5,2,0},{5,2,0},{-1.9F,12,0},{1.9F,12,0}};
 private static final JsonArray DATA=read();
 private static JsonArray read(){try(var input=HollowArmorGeometry.class.getResourceAsStream("/assets/lc_hollowforged/mountain_king_geometry.json")){if(input==null)throw new IllegalStateException("Missing armor geometry");return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonArray();}catch(java.io.IOException e){throw new IllegalStateException(e);}}
 public static LayerDefinition layer(EquipmentSlot slot){var mesh=new MeshDefinition();var root=mesh.getRoot();for(int i=0;i<PARTS.length;i++){var p=PIVOTS[i];root.addOrReplaceChild(PARTS[i],CubeListBuilder.create(),PartPose.offset(p[0],p[1],p[2]));}return LayerDefinition.create(mesh,2048,2048);}
 private static float[] row(JsonArray a){float[] r=new float[a.size()];for(int i=0;i<r.length;i++)r[i]=a.get(i).getAsFloat();return r;}
 private static float[][] rows(JsonArray a){float[][] r=new float[a.size()][];for(int i=0;i<r.length;i++)r[i]=row(a.get(i).getAsJsonArray());return r;}
 public static ModelPart bake(EquipmentSlot slot){
  var bones=new HashMap<String,ModelPart>();
  for(int i=0;i<PARTS.length;i++){
   var children=new LinkedHashMap<String,ModelPart>();
   for(var entry:DATA){var b=entry.getAsJsonObject();if(!b.get("slot").getAsString().equals(slot.name())||!b.get("part").getAsString().equals(PARTS[i]))continue;
    var faceRows=rows(b.getAsJsonArray("faces"));int[][] faces=new int[faceRows.length][];for(int n=0;n<faces.length;n++){faces[n]=new int[faceRows[n].length];for(int j=0;j<faces[n].length;j++)faces[n][j]=(int)faceRows[n][j];}
    var child=new ModelPart(List.of(new MountainPlateCube(rows(b.getAsJsonArray("vertices")),faces,rows(b.getAsJsonArray("uv")))),Map.of());var c=row(b.getAsJsonArray("center"));var r=row(b.getAsJsonArray("rotation"));child.setInitialPose(PartPose.offsetAndRotation(c[0],c[1],c[2],r[0],r[1],r[2]));child.resetPose();children.put(b.get("name").getAsString(),child);
   }
   var parent=new ModelPart(List.of(),children);var p=PIVOTS[i];parent.setInitialPose(PartPose.offset(p[0],p[1],p[2]));parent.resetPose();bones.put(PARTS[i],parent);
  }
  return new ModelPart(List.of(),bones);
 }
}
