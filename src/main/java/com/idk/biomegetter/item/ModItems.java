package com.idk.biomegetter.item;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.item.custom.WandItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Function;


//  Как происходит регистрация предмета:
//  public static final Item ---> output.accept для регистрации в меню креатива --->
//  ---> ModModelProvider ---> itemModelGenerators для генерации типа модели
//  ---> lang +строка ---> textures.item новая текстура
//  НЕ ЗАБЫВАЕМ ПОСЛЕ ДОБАВЛЕНИЯ ФАЙЛА ЗАПУСКАТЬ DataGen
//  В противном случае текстуры  не подтянутся
public class ModItems {
    //    public static final Item BIOMES_STICK = registerItem("biome_stick",
//            properties -> new Item(properties));

    //  name участвует в en_us.json()
    //  Там же есть item. Мы его пишем потому, что в потоке регистрируем именно Item
    public static final Item BIOMES_STICK = registerItem("biome_stick",
            Item::new);
    public static final Item BIOMES_STICK_2 = registerItem("biome_stick_2",
            Item::new);

    public static final Item WAND = registerItem("wand",
            properties -> new WandItem(properties
                    .durability(32)));

    public static final Item UNICORN_SPAWN_EGG = registerItem("unicorn_spawn_egg",
            properties -> new SpawnEggItem(
                    properties.spawnEgg(ModEntities.UNICORN)
            ));

    public static final Item MEAL = registerItem("meal", Item::new);

    //  Регистрация предмета в потоке
    public static Item registerItem(String name, Function<Item.Properties, Item> function) {
        return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name),
                function.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name)))));
    }

    public static void registerModItems() {
        BiomeGetter.LOGGER.info("Registered Mod Items for " + BiomeGetter.MOD_ID);

        //  Добавление в существующее меню креатива
//        Теперь оно производится в ModCreativeModeTabs
//        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
//            output.accept(BIOMES_STICK);
//            output.accept(BIOMES_STICK_2);
//        });
    }
}
