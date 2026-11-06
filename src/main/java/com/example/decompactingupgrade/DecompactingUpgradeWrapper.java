package com.example.decompactingupgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Every ~1s, scans the storage and turns "compressed" items (iron block, etc.)
 * back into their components (9 iron ingots) using the vanilla/modded crafting recipes in reverse.
 * A recipe only counts as decompacting if: 1 item -> N items (N = 4 or 9)
 * AND N of those items crafted back (2x2 or 3x3) give exactly the original item.
 */
public class DecompactingUpgradeWrapper extends UpgradeWrapperBase<DecompactingUpgradeWrapper, DecompactingUpgradeItem>
        implements ITickableUpgrade {

    private static final int COOLDOWN_TICKS = 20;
    private long lastRun = 0;
    private final Map<net.minecraft.world.item.Item, Optional<ItemStack>> cache = new HashMap<>();

    public DecompactingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public void tick(@Nullable Entity entity, @Nullable Level level, @Nullable BlockPos pos) {
        if (level == null || level.isClientSide()) return;
        long now = level.getGameTime();
        if (now < lastRun + COOLDOWN_TICKS) return;
        lastRun = now;

        IItemHandlerModifiable inv = storageWrapper.getInventoryHandler();
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            Optional<ItemStack> result = cache.computeIfAbsent(stack.getItem(), item -> findDecompacted(level, stack));
            if (result.isEmpty()) continue;

            ItemStack out = result.get();
            // how many blocks can we process this pass (bounded by free space for the outputs)
            int canDo = stack.getCount();
            while (canDo > 0 && !fitsAll(inv, out, canDo, slot)) canDo--;
            if (canDo <= 0) continue;

            inv.extractItem(slot, canDo, false);
            ItemStack toInsert = out.copyWithCount(out.getCount() * canDo);
            for (int i = 0; i < inv.getSlots() && !toInsert.isEmpty(); i++) {
                toInsert = inv.insertItem(i, toInsert, false);
            }
            // safety net: if something is left (shouldn't be), put it back into the source slot
            if (!toInsert.isEmpty()) inv.insertItem(slot, toInsert, false);
        }
    }

    /** Simulates inserting out*count items after removing `count` source items from `sourceSlot`. */
    private boolean fitsAll(IItemHandlerModifiable inv, ItemStack out, int count, int sourceSlot) {
        // Simple conservative check: free capacity in non-source slots + capacity freed by removal.
        int needed = out.getCount() * count;
        int space = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            int limit = Math.min(inv.getSlotLimit(i), out.getMaxStackSize());
            if (s.isEmpty()) space += limit;
            else if (ItemStack.isSameItemSameComponents(s, out)) space += Math.max(0, limit - s.getCount());
        }
        return space >= needed;
    }

    private Optional<ItemStack> findDecompacted(Level level, ItemStack stack) {
        var rm = level.getRecipeManager();
        ItemStack single = stack.copyWithCount(1);
        Optional<RecipeHolder<CraftingRecipe>> rec =
                rm.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(1, 1, List.of(single)), level);
        if (rec.isEmpty()) return Optional.empty();

        ItemStack out = rec.get().value().assemble(CraftingInput.of(1, 1, List.of(single)), level.registryAccess());
        int n = out.getCount();
        if (out.isEmpty() || (n != 4 && n != 9) || ItemStack.isSameItemSameComponents(out, single)) return Optional.empty();

        // reverse check: n of `out` in a 2x2 / 3x3 grid must craft back the original
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
