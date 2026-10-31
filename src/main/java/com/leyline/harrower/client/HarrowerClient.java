package com.leyline.harrower.client;
import com.leyline.harrower.HarrowerMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid=HarrowerMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HarrowerClient{
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(HarrowerModel.LAYER,HarrowerModel::layer);}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(HarrowerMod.HARROWER.get(),HarrowerRenderer::new);}
}
