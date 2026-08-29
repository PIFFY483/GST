package com.gst.mixin;

import com.gst.world.GravityUtil;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Ok/mızrak gibi PersistentProjectileEntity'lerin tick()'inde her tick
 * dikey hıza uygulanan sabit 0.05F yerçekimi değerini, mermi o an hangi
 * gezegen hücresinin üzerindeyse oranın gravityFactor'üne göre ölçekler.
 *
 * NOT: 1.20.1'de (Entity#getGravity() henüz yok, bu API 1.20.5'te eklendi)
 * yerçekimi tick() içine gömülü bir float sabiti olarak yazılı - bu yüzden
 * getGravity() override etmek yerine sabiti @ModifyConstant ile yakalıyoruz.
 */
@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin {

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.05F))
    private float gst$scaleArrowGravity(float original) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        float factor = GravityUtil.getGravityFactor(self.getWorld(), self.getBlockPos());
        if (GravityUtil.isVanillaGravity(factor)) {
            return original;
        }
        return original * factor;
    }
}
