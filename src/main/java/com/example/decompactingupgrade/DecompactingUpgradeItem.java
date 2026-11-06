package com.example.decompactingupgrade;

import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DecompactingUpgradeItem extends UpgradeItemBase<DecompactingUpgradeWrapper> {
    public static final UpgradeType<DecompactingUpgradeWrapper> TYPE =
            new UpgradeType<>(DecompactingUpgradeWrapper::new);

    // 1 upgrade max per storage
    private static final IUpgradeCountLimitConfig LIMIT = new IUpgradeCountLimitConfig() {
        @Override
        public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeName) {
            return 1;
        }

        @Override
        public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup) {
            return 1;
        }
    };

    public DecompactingUpgradeItem() {
        super(LIMIT);
    }

    @Override
    public UpgradeType<DecompactingUpgradeWrapper> getType() {
        return TYPE;
    }

    // No conflicts with other upgrades. Raw List on purpose, so it matches the base interface's exact type.
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List getUpgradeConflicts() {
        return List.of();
    }
}
