package com.example.decompactingupgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DecompactingUpgradeWrapper extends UpgradeWrapperBase<DecompactingUpgradeWrapper, DecompactingUpgradeItem>
        implements ITickableUpgrade {

    private static final int COOLDOWN_TICKS = 40;
    private static final long CACHE_LIFETIME = 6000; // refresh recipe results every 5 minutes

    // Shared by all upgrades, so the slow recipe lookup happens once per item type
    private static final Map<Item, Optional<ItemStack>> CACHE = new ConcurrentHashMap<>();
    private static long cacheStart = 0;

    private long lastRun = 0;

    public DecompactingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public void tick(@Nullable Entity entity, @Nullable Level level, @Nullable BlockPos pos) {
        if (level == null || level.isClientSide()) return;
        long now = level.getGameTime();
        if (now < lastRun + COOLDOWN_TICKS) return;
        lastRun = now;

        if (now < cacheStart || now - cacheStart > CACHE_LIFETIME) {
            CACHE.clear();
            cacheStart = now;
        }

        IItemHandlerModifiable inv = storageWrapper.getInventoryHandler();
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty() || !stack.getComponentsPatch().isEmpty()) continue;

            Optional<ItemStack> result = CACHE.get(stack.getItem());
            if (result == null) {
                result = findDecompacted(level, stack);
                CACHE.put(stack.getItem(), result);
            }
            if (result.isEmpty()) continue;

            ItemStack out = result.get();
            int space = freeSpaceFor(inv, out);
            int canDo = Math.min(stack.getCount(), space / out.getCount());
            if (canDo <= 0) continue;

            inv.extractItem(slot, canDo, false);
            ItemStack toInsert = out.copyWithCount(out.getCount() * canDo);
            for (int i = 0; i < inv.getSlots() && !toInsert.isEmpty(); i++) {
                toInsert = inv.insertItem(i, toInsert, false);
            }
            if (!toInsert.isEmpty()) inv.insertItem(slot, toInsert, false);
        }
    }

    private int freeSpaceFor(IItemHandlerModifiable inv, ItemStack out) {
        int space = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            int limit = Math.min(inv.getSlotLimit(i), out.getMaxStackSize());
            if (s.isEmpty()) space += limit;
            else if (ItemStack.isSameItemSameComponents(s, out)) space += Math.max(0, limit - s.getCount());
        }
        return space;
    }

    private static Optional<ItemStack> findDecompacted(Level level, ItemStack stack) {
        var rm = level.getRecipeManager();
        ItemStack single = stack.copyWithCount(1);
        CraftingInput in = CraftingInput.of(1, 1, List.of(single));
        Optional<RecipeHolder<CraftingRecipe>> rec = rm.getRecipeFor(RecipeType.CRAFTING, in, level);
        if (rec.isEmpty()) return Optional.empty();

        ItemStack out = rec.get().value().assemble(in, level.registryAccess());
        int n = out.getCount();
        if (out.isEmpty() || (n != 4 && n != 9) || ItemStack.isSameItem(out, single)) return Optional.empty();

        int size = n == 4 ? 2 : 3;
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < n; i++) grid.add(out.copyWithCount(1));
        CraftingInput back = CraftingInput.of(size, size, grid);
        Optional<RecipeHolder<CraftingRecipe>> backRec = rm.getRecipeFor(RecipeType.CRAFTING, back, level);
        if (backRec.isEmpty()) return Optional.empty();
        ItemStack backOut = backRec.get().value().assemble(back, level.registryAccess());
        if (backOut.getCount() != 1 || !ItemStack.isSameItem(backOut, single)) return Optional.empty();

        return Optional.of(out);
    }
}
