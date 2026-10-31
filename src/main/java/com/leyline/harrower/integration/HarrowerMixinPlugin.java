package com.leyline.harrower.integration;
import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class HarrowerMixinPlugin implements IMixinConfigPlugin {
 public void onLoad(String mixinPackage){}
 public String getRefMapperConfig(){return null;}
 public boolean shouldApplyMixin(String target,String mixin){return !mixin.endsWith("CoreLeylineMixin")||CoreFingerprint.matches();}
 public void acceptTargets(Set<String> mine,Set<String> other){}
 public List<String> getMixins(){return null;}
 public void preApply(String name,ClassNode target,String mixin,IMixinInfo info){}
 public void postApply(String name,ClassNode target,String mixin,IMixinInfo info){if(mixin.endsWith("CoreLeylineMixin"))IntegrationFlags.coreMixinApplied=true;}
}
