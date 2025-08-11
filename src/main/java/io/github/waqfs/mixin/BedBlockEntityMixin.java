package io.github.waqfs.mixin;

import io.github.waqfs.agent.BedwarsAgent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlockEntity.class)
public class BedBlockEntityMixin extends BlockEntity {

    public BedBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "getColor", at = @At("HEAD"))
    private void getColor(CallbackInfoReturnable<DyeColor> ci) {
        BedwarsAgent.addBed(this.pos, this.getCachedState());
    }
}
