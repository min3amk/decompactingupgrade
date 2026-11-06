package com.example.decompactingupgrade;

import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import org.jetbrains.annotations.Nullable;

public class DecompactingUpgradeItem extends UpgradeItemBase<DecompactingUpgradeWrapper> {
    public static final UpgradeType<DecompactingUpgradeWrapper> TYPE =
            new UpgradeType<>(DecompactingUpgradeWrapper::new);

    // 1 upgrade max per storage (no per-storage-type override)
    private static final IUpgradeCountLimitConfig LIMIT = new IUpgradeCountLimitConfig() {
        @Override
        public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeName) {
            return 1;
        }

        @Override
        public int getMaxUpgradesInGroupPerStorage(String storageType, @Nullable ResourceLocation upgradeName) {
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
}
