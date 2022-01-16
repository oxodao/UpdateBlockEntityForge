package fr.oxodao.blockentityupdater;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.References;

public class BlockEntityUpdaterUtils {
    private BlockEntityUpdaterUtils() {}

    public static final int SAFE_UPGRADE_DATA_VERSION = 2841;
    public static final int CHUNK_LEVEL_FIX_DATA_VERSION = 2842;
    public static final String KEY = "UpdateBlockEntity";

    public static CompoundTag update(DataFixer dataFixer, CompoundTag nbt, int target) {

        return (CompoundTag) dataFixer.update(References.BLOCK_ENTITY, new Dynamic(NbtOps.INSTANCE, nbt), SAFE_UPGRADE_DATA_VERSION, target).getValue();
    }
}