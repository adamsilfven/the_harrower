import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.client.renderer.block.model.BlockModel;
public final class HollowCoreModelCheck {
 public static void main(String[] args)throws Exception{
  var root=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
  if(!root.get("loader").getAsString().equals("neoforge:separate_transforms"))throw new AssertionError("separate GUI model required");
  var base=root.getAsJsonObject("base");var parsed=BlockModel.fromString(base.toString());
  if(parsed.getElements().size()!=110||parsed.getElements().stream().anyMatch(e->e.faces.size()!=6))throw new AssertionError("heart relief geometry missing");
  var display=base.getAsJsonObject("display");
  for(String key:new String[]{"firstperson_righthand","firstperson_lefthand","thirdperson_righthand","thirdperson_lefthand","ground","fixed","head"})if(!display.has(key))throw new AssertionError(key);
  if(display.getAsJsonObject("ground").getAsJsonArray("rotation").get(0).getAsInt()!=90)throw new AssertionError("ground must lie flat");
  var gui=root.getAsJsonObject("perspectives").getAsJsonObject("gui");BlockModel.fromString(gui.toString());
  if(!gui.get("parent").getAsString().equals("minecraft:item/generated"))throw new AssertionError("native pixel icon required");
  System.out.println("HOLLOW_CORE_MODEL_PASS: native parser accepted 110 six-face stone pixels, both hands, flat ground transform and separate GUI icon");
 }
}
