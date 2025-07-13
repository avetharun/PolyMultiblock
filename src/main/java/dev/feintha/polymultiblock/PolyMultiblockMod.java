package dev.feintha.polymultiblock;

import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.function.Function;

public class PolyMultiblockMod implements ModInitializer {

//    static <T extends Item> T registerItem(String name, Function<Item.Settings, T> itemFactory, Item.Settings settings) {
//        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("polymultiblock", name));
//        T item = itemFactory.apply(settings.registryKey(itemKey));
//        Registry.register(Registries.ITEM, itemKey, item);
//
//        return item;
//    }
//    static <T extends Block> T registerBlock(String name, Function<Block.Settings, T> blockFactory, Block.Settings settings) {
//        RegistryKey<Block> itemKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of("polymultiblock", name));
//        T item = blockFactory.apply(settings.registryKey(itemKey));
//        Registry.register(Registries.BLOCK, itemKey, item);
//        registerItem(name, settings1 -> new PolymerBlockItem(item, settings1), new Item.Settings());
//        return item;
//    }

    @Override
    public void onInitialize() {

    }
}
