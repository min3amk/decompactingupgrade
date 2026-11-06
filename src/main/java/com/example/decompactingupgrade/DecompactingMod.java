package com.example.decompactingupgrade;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Mod(DecompactingMod.MODID)
public class DecompactingMod {
    public static final String MODID = "decompactingupgrade";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<Item, DecompactingUpgradeItem> DECOMPACTING_UPGRADE =
            ITEMS.register("decompacting_upgrade", DecompactingUpgradeItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.decompactingupgrade"))
                    .icon(() -> new ItemStack(DECOMPACTING_UPGRADE.get()))
                    .displayItems((params, out) -> out.accept(DECOMPACTING_UPGRADE.get()))
                    .build());

    public DecompactingMod(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
