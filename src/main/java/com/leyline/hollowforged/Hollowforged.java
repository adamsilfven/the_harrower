package com.leyline.hollowforged;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
@Mod(Hollowforged.ID)
public final class Hollowforged {
 public static final String ID="lc_hollowforged";
 public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ID,path);}
 public static final DeferredRegister<ArmorMaterial> MATERIALS=DeferredRegister.create(Registries.ARMOR_MATERIAL,ID);
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
 private static DeferredHolder<ArmorMaterial,ArmorMaterial> material(String name,ArmorItem.Type type,int armor,float toughness,float knockback){
  return MATERIALS.register(name,()->new ArmorMaterial(Map.of(type,armor),20,SoundEvents.ARMOR_EQUIP_NETHERITE,()->Ingredient.EMPTY,List.of(new ArmorMaterial.Layer(id("hollowforged"))),toughness,knockback));
 }
 public static final DeferredHolder<ArmorMaterial,ArmorMaterial> CROWN_MATERIAL=material("crown",ArmorItem.Type.HELMET,5,5,.15F);
 public static final DeferredHolder<ArmorMaterial,ArmorMaterial> CARAPACE_MATERIAL=material("carapace",ArmorItem.Type.CHESTPLATE,12,6,.20F);
 public static final DeferredHolder<ArmorMaterial,ArmorMaterial> GREAVES_MATERIAL=material("greaves",ArmorItem.Type.LEGGINGS,9,5,.20F);
 public static final DeferredHolder<ArmorMaterial,ArmorMaterial> TREADS_MATERIAL=material("treads",ArmorItem.Type.BOOTS,4,4,.15F);
 public static final DeferredItem<Item> HOLLOW_CORE=ITEMS.register("hollow_core",()->new HollowCoreItem(new Item.Properties().rarity(Rarity.EPIC)));
 public static final DeferredItem<ArmorItem> CROWN=armor("crown_of_the_hollow",CROWN_MATERIAL,ArmorItem.Type.HELMET);
 public static final DeferredItem<ArmorItem> CARAPACE=armor("carapace_of_the_unbroken",CARAPACE_MATERIAL,ArmorItem.Type.CHESTPLATE);
 public static final DeferredItem<ArmorItem> GREAVES=armor("greaves_of_the_stillborn",GREAVES_MATERIAL,ArmorItem.Type.LEGGINGS);
 public static final DeferredItem<ArmorItem> TREADS=armor("treads_of_the_worldbreaker",TREADS_MATERIAL,ArmorItem.Type.BOOTS);
 public static final DeferredItem<WorldshardItem> WORLDSHARD=ITEMS.register("worldshard",()->new WorldshardItem(relicProperties().attributes(hammerAttributes())));
 public static final List<DeferredItem<? extends Item>> RELICS=List.of(CROWN,CARAPACE,GREAVES,TREADS,WORLDSHARD);
 private static DeferredItem<ArmorItem> armor(String name,Holder<ArmorMaterial> material,ArmorItem.Type type){return ITEMS.register(name,()->new RelicArmorItem(material,type,relicProperties()));}
 private static Item.Properties relicProperties(){
  // Keep MAX_DAMAGE for native enchanting; no fireResistant() so dropped items retain normal loss rules.
  return new Item.Properties().durability(2031).component(DataComponents.UNBREAKABLE,new Unbreakable(true)).rarity(Rarity.EPIC);
 }
 private static ItemAttributeModifiers hammerAttributes(){
  return ItemAttributeModifiers.builder()
   .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,29,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
   .add(Attributes.ATTACK_SPEED,new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,-3.2,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
   .add(Attributes.ENTITY_INTERACTION_RANGE,new AttributeModifier(id("worldshard_reach"),2,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build();
 }
 public static boolean isRelic(ItemStack stack){return RELICS.stream().anyMatch(i->stack.is(i.get()));}
 public Hollowforged(IEventBus bus){
  MATERIALS.register(bus);ITEMS.register(bus);bus.addListener(Hollowforged::creative);TwoHandedUse.register();LockedPassives.register();CombatAbilities.register();CombatHooks.register();EncounterRewards.register();bus.addListener(HFNetwork::register);
  NeoForge.EVENT_BUS.addListener(DevelopmentChecks::started);NeoForge.EVENT_BUS.addListener(ReviewScene::started);NeoForge.EVENT_BUS.addListener(ReviewScene::joined);
 }
 private static void creative(BuildCreativeModeTabContentsEvent event){
  if(event.getTabKey()==CreativeModeTabs.COMBAT)for(var item:RELICS)event.accept(item.get());
  if(event.getTabKey()==CreativeModeTabs.INGREDIENTS)event.accept(HOLLOW_CORE.get());
 }
}
