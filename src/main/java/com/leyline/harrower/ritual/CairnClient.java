package com.leyline.harrower.ritual;

import com.leyline.harrower.HarrowerMod;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.util.*;

@EventBusSubscriber(modid=HarrowerMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CairnClient {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(CairnRegistry.TYPE,VoidRenderer::new);}
 public static final class VoidRenderer extends EntityRenderer<CairnRitual> {
  public VoidRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
  @Override public ResourceLocation getTextureLocation(CairnRitual entity){return ResourceLocation.withDefaultNamespace("textures/misc/white.png");}
  @Override public void render(CairnRitual entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
   float x=entity.alongX()?1:.5F,z=entity.alongX()?.5F:1;
   VertexConsumer out=buffers.getBuffer(RenderType.debugQuads());var matrix=pose.last().pose();
   // Full rectangular membrane: no ellipse, recess, exposed corners or terrain changes.
   // A black base and opaque tiled surface cover every point of both opening cells.
   float[][] p={{-x,0,-z},{x,0,-z},{x,0,z},{-x,0,z},{-x,-1,-z},{x,-1,-z},{x,-1,z},{-x,-1,z}};
   int[][] faces={{0,3,2,1},{4,5,6,7},{0,1,5,4},{1,2,6,5},{2,3,7,6},{3,0,4,7}};
   for(int[] face:faces)for(int i:face)out.addVertex(matrix,p[i][0],p[i][1],p[i][2]).setColor(0,0,0,255);
   double time=(entity.age()+partial)*.025;
   // Flowing charcoal grain and sparse pale soul-like wisps, unique to the Cairn.
   // Position-based shading moves within the surface while its rectangular silhouette stays fixed.
   int nx=entity.alongX()?64:32,nz=entity.alongX()?32:64;
   for(int ix=0;ix<nx;ix++)for(int iz=0;iz<nz;iz++){
    for(int corner:new int[]{0,1,2,3}){
     double u=(ix+(corner==1||corner==2?1:0))/(double)nx;
     double v=(iz+(corner>=2?1:0))/(double)nz;
     double px=(u*2-1)*x,pz=(v*2-1)*z;
     double warp=Math.sin(px*5+time*.8)*.35+Math.sin(pz*6-time*.65)*.22;
     double ribbon=Math.pow(Math.max(0,Math.cos(px*4+pz*7+warp-time*1.1)),18);
     double cloud=(Math.sin(px*9-time*.4+Math.sin(pz*5+time))*.5+.5);
     double fade=Math.min(1,Math.min(Math.min(u,1-u),Math.min(v,1-v))*14);
     int shade=(int)((2+cloud*5+ribbon*18)*(.4+.6*fade));
     out.addVertex(matrix,(float)px,.003F,(float)pz).setColor(shade,(int)(shade*.91),(int)(shade*.98),255);
    }
   }

  }
 }
 @EventBusSubscriber(modid=HarrowerMod.ID,value=Dist.CLIENT)
 public static final class Souls {
  private static Object world;
  private static final Map<UUID,Integer> seen=new HashMap<>();
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
   var mc=Minecraft.getInstance();if(mc.level!=world){seen.clear();world=mc.level;}if(mc.level==null||mc.player==null||mc.isPaused())return;
   Set<UUID> live=new HashSet<>();
   for(CairnRitual ritual:mc.level.getEntitiesOfClass(CairnRitual.class,mc.player.getBoundingBox().inflate(128))){
    live.add(ritual.getUUID());int age=ritual.age(),previous=seen.getOrDefault(ritual.getUUID(),Math.max(0,age-1));
    // Forty staggered surges of fifteen souls: 600 total over the existing 2–8 second stage.
    for(int t=Math.max(40,previous+1);t<=Math.min(age,159);t++)if((t-40)%3==0)for(int surge=0;surge<15;surge++){
     var r=mc.level.random;double x=ritual.getX()+(r.nextDouble()-.5)*(ritual.alongX()?1.8:.8),z=ritual.getZ()+(r.nextDouble()-.5)*(ritual.alongX()?.8:1.8);
     mc.level.addAlwaysVisibleParticle(ParticleTypes.SOUL,x,ritual.getY()+.04,z,(r.nextDouble()-.5)*.06,.18+r.nextDouble()*.12,(r.nextDouble()-.5)*.06);
    }
    seen.put(ritual.getUUID(),age);
   }
   seen.keySet().retainAll(live);
  }
 }
 private CairnClient(){}
}
