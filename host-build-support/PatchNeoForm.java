import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public final class PatchNeoForm {
 public static void main(String[] args)throws Exception {
  String target="net/neoforged/neoform/runtime/engine/NeoFormEngine.class";int replacements=0;
  try(ZipFile input=new ZipFile(args[0]);ZipOutputStream output=new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))){
   for(var entries=input.entries();entries.hasMoreElements();){var entry=entries.nextElement();byte[] bytes=input.getInputStream(entry).readAllBytes();
    if(entry.getName().equals(target)||entry.getName().equals("net/neoforged/neoform/runtime/actions/ExternalJavaToolAction.class")){
     int before=replacements;
     var node=new ClassNode();new ClassReader(bytes).accept(node,0);
     for(var method:node.methods)for(var insn:method.instructions.toArray()){
      if(insn instanceof MethodInsnNode call&&call.owner.equals("java/lang/ProcessHandle")&&call.name.equals("current")){
       var remove=new ArrayList<AbstractInsnNode>();AbstractInsnNode cursor=insn;
       while(remove.size()<5){if(cursor.getOpcode()>=0)remove.add(cursor);cursor=cursor.getNext();}
       if(!(remove.get(3) instanceof MethodInsnNode optional)||!optional.owner.equals("java/util/Optional")||!optional.name.equals("orElseThrow")||remove.get(4).getOpcode()!=Opcodes.CHECKCAST)throw new IllegalStateException("Unexpected NeoForm constructor shape");
       var replacement=new InsnList();replacement.add(new LdcInsnNode("java.home"));replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/System","getProperty","(Ljava/lang/String;)Ljava/lang/String;",false));replacement.add(new LdcInsnNode("/bin/java"));replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/lang/String","concat","(Ljava/lang/String;)Ljava/lang/String;",false));
       method.instructions.insertBefore(insn,replacement);remove.forEach(method.instructions::remove);replacements++;
      }
     }
     if(replacements!=before+1)throw new IllegalStateException("Expected one Java-path fallback in each target class");
     var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);bytes=writer.toByteArray();
    }
    var next=new ZipEntry(entry.getName());next.setTime(0);output.putNextEntry(next);output.write(bytes);output.closeEntry();
   }
  }
  if(replacements!=2)throw new IllegalStateException("Targets not patched");System.out.println("Applied two build-host Java-path fallbacks; game classes untouched.");
 }
}
