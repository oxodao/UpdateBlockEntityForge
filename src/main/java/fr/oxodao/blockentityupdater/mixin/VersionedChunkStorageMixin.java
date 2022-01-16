package fr.oxodao.blockentityupdater.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import fr.oxodao.blockentityupdater.BlockEntityUpdaterUtils;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Supplier;

@Mixin(ChunkStorage.class)
public class VersionedChunkStorageMixin {
    @Shadow @Final protected DataFixer fixerUpper;

    @ModifyArg(method = "upgradeChunkTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtUtils;update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/util/datafix/DataFixTypes;Lnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/nbt/CompoundTag;"), index = 3)
    private int beforeUpdateThirdArg(int version) {
        return BlockEntityUpdaterUtils.SAFE_UPGRADE_DATA_VERSION;
    }

    @ModifyArg(method = "upgradeChunkTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtUtils;update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/util/datafix/DataFixTypes;Lnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/nbt/CompoundTag;"), index = 2)
    private CompoundTag beforeUpdate(CompoundTag nbt) {
        int oldVersion = ChunkStorage.getVersion(nbt);
        if (oldVersion >= BlockEntityUpdaterUtils.CHUNK_LEVEL_FIX_DATA_VERSION) {
            // This fix only applies to pre-2842 to post-2842 for now
            return nbt;
        }
        // Update to data version 2841 first to apply any changes in previous versions
        nbt = NbtUtils.update(this.fixerUpper, DataFixTypes.CHUNK, nbt, oldVersion, BlockEntityUpdaterUtils.SAFE_UPGRADE_DATA_VERSION);
        CompoundTag context = nbt.getCompound("__context");
        CollectionTag blockEntities = nbt.getCompound("Level").getList("TileEntities", Tag.TAG_COMPOUND);
        context.put(BlockEntityUpdaterUtils.KEY, blockEntities);
        return nbt;
    }

    @Inject(method = "upgradeChunkTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;remove(Ljava/lang/String;)V"))
    private void afterUpdate(ResourceKey<Level> worldKey, Supplier<DimensionDataStorage> persistentStateManagerFactory, CompoundTag nbt, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> generatorCodecKey, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag context = nbt.getCompound("__context");
        if (context == null) {
            return;
        }
        CollectionTag blockEntities = context.getList(BlockEntityUpdaterUtils.KEY, Tag.TAG_COMPOUND);
        if (blockEntities == null) {
            // Updating between safe versions; no action necessary
            return;
        }
        if (blockEntities.size() > 0) {
            //UpdateBlockEntityMod.LOGGER.info("Updating " + blockEntities.size() + " block entities");

            for (int i = 0; i < blockEntities.size(); i++) {
                CompoundTag blockEntityNbt = (CompoundTag) blockEntities.get(i);
                blockEntities.set(i, BlockEntityUpdaterUtils.update(this.fixerUpper, blockEntityNbt, SharedConstants.getCurrentVersion().getWorldVersion()));
            }
            nbt.put("block_entities", blockEntities);
        }
        context.remove(BlockEntityUpdaterUtils.KEY);
    }
}
