import java.nio.file.*;
import net.minecraft.client.renderer.block.model.BlockModel;
public final class ModelParserCheck {
 public static void main(String[] args) throws Exception {
  try {
   BlockModel.fromString(Files.readString(Path.of(args[1])));
   throw new AssertionError("Original broken model unexpectedly parsed");
  } catch (com.google.gson.JsonParseException expected) {
   if (!expected.getMessage().contains("got 0")) throw expected;
   System.out.println("Original model rejected as expected: " + expected.getMessage());
  }
  BlockModel model=BlockModel.fromString(Files.readString(Path.of(args[0])));
  if(model.getElements().size()!=155)throw new AssertionError("Wrong visible element count");
  int faces=model.getElements().stream().mapToInt(e->e.faces.size()).sum();
  if(faces!=276)throw new AssertionError("Wrong face count: "+faces);
  System.out.println("Corrected model accepted by Minecraft BlockModel parser: 155 elements, "+faces+" faces");
 }
}
