package com.gst.mixin;

import com.gst.world.GravityUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zıplama ilk hızını ve düşme hasarını gezegenin yerçekimi katsayısına
 * (PlanetData#gravityFactor) göre ölçekler. PlanetGravityHandler sadece
 * düşüş SIRASINDAKİ ivmeyi (tick-tick arası) telafi ediyor; zıplama anındaki
 * ilk hız ve düşme hasarı formülü vanilla içinde ayrı birer sabit olduğu
 * için burada doğrudan mixin atmak gerekiyor.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * LivingEntity'nin kendi WASD girdisi (yaw'a göre henüz döndürülmemiş, ham
     * sideways/forward değerleri). Mixin sınıfları hedef sınıfın field'larına
     * doğrudan erişemediği için @Shadow ile bildirmemiz gerekiyor.
     */
    @Shadow
    public float sidewaysSpeed;

    @Shadow
    public float forwardSpeed;

    /**
     * Zıplama anında yöne doğru eklenen ekstra itişin taban büyüklüğü (vanilla'nın kendi
     * sprint-zıplama itişiyle aynı mertebede - bkz. LivingEntity#jump, 0.2 sin/cos(yaw)).
     * gst$scaleJumpForGravity içinde gravityScale ile çarpılarak asıl itiş elde edilir.
     */
    private static final double JUMP_DIRECTION_PUSH = 0.5;

    /**
     * jumpPower = 0.42 * sqrt(g_vanilla / g_yerel)
     *
     * Düşük g'de havada kalma süresi ~1/g, menzil de ~1/g ile arttığından,
     * fiziksel olarak tutarlı bir zıplama için ilk dikey hız 1/sqrt(g) ile
     * ölçeklenmeli. jump() vanilla dikey hızı zaten setVelocity ile atadıktan
     * SONRA (TAIL) devreye giriyoruz, böylece jump boost efekti gibi diğer
     * katkılar da (tutarlı biçimde) toplam hıza dahil olmuş oluyor.
     *
     * OYUN HİSSİ İTİŞİ: Oyuncu zıplama anında bir yöne hareket etmeye çalışıyorsa
     * (sidewaysSpeed/forwardSpeed - yani WASD basılıysa), o yönde ekstra bir itiş
     * ekliyoruz. Bu vanilla'nın sprint-zıplama itişiyle aynı ruhta (bkz. LivingEntity#jump
     * içindeki 0.2*sin/cos(yaw) itişi) ama TÜM yönlere (sadece ileri değil, WASD'ın
     * bileşkesine) çalışıyor ve dikey ölçeklemeyle AYNI "scale" faktörüyle büyüyüp
     * küçülüyor: g=1'de scale=1 olduğundan itiş sıfır (vanilla'ya dokunulmuyor), düşük
     * g'de scale büyüdükçe itiş de büyüyor (Ay'da atılan bir adımın çok daha ileriye
     * gitmesi gibi), yüksek g'de scale 1'in altına düştüğü için itiş sıfırda kalıyor
     * (negatife/geriye itiş yapmıyoruz - sadece sönümleniyor).
     */
    @Inject(method = "jump", at = @At("TAIL"))
    private void gst$scaleJumpForGravity(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        float factor = GravityUtil.getGravityFactor(self.getWorld(), self.getBlockPos());
        if (factor <= 0.0f || GravityUtil.isVanillaGravity(factor)) {
            return;
        }

        Vec3d velocity = self.getVelocity();
        if (velocity.y <= 0.0) {
            return; // zıplama gerçekleşmemiş (ör. jumping=true ama engellenmiş)
        }

        double scale = Math.sqrt(1.0 / factor);
        double newY = velocity.y * scale;

        double sideways = this.sidewaysSpeed;
        double forward = this.forwardSpeed;
        double inputMagSq = sideways * sideways + forward * forward;

        double newX = velocity.x;
        double newZ = velocity.z;
        if (inputMagSq > 1.0E-7) {
            // Vanilla'nın kendi movementInputToVelocity'siyle aynı dönüşüm: (sideways,forward)
            // çiftini normalize edip yaw'a göre dünya eksenine çeviriyoruz.
            double invLen = inputMagSq > 1.0 ? 1.0 / Math.sqrt(inputMagSq) : 1.0;
            double s = sideways * invLen;
            double f = forward * invLen;
            float sin = MathHelper.sin(self.getYaw() * ((float) Math.PI / 180.0F));
            float cos = MathHelper.cos(self.getYaw() * ((float) Math.PI / 180.0F));
            double dirX = s * cos - f * sin;
            double dirZ = f * cos + s * sin;

            double push = JUMP_DIRECTION_PUSH * Math.max(0.0, scale - 1.0);
            newX += dirX * push;
            newZ += dirZ * push;
        }

        self.setVelocity(newX, newY, newZ);
        self.velocityModified = true;
    }

    /**
     * Düşme hasarı ∝ g: aynı yükseklikten düşüşün son (terminal olmayan,
     * serbest düşüş) hızı v ∝ sqrt(g), kinetik enerji (ve dolayısıyla hasar)
     * ise v^2 ∝ g ile orantılıdır. Vanilla'nın computeFallDamage'ı sadece
     * düşülen blok sayısına bakar, g'yi hesaba katmaz - burada telafi ediyoruz.
     *
     * AYRICA: fallDistance, o gezegende zıplayarak ulaşılabilecek maksimum
     * yüksekliğin (bkz. GravityUtil#getMaxJumpHeight) altında veya eşitse hiç
     * hasar verilmiyor - yani kendi zıplamanla çıkabildiğin bir yükseklikten
     * düşmek asla acıtmamalı, vanilla'nın sabit "3 blok güvenli düşüş" eşiği
     * düşük g'de bunu garanti etmiyordu.
     */
    @Inject(method = "computeFallDamage", at = @At("RETURN"), cancellable = true)
    private void gst$scaleFallDamageForGravity(float fallDistance, float damageMultiplier, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        if (original <= 0) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        float factor = GravityUtil.getGravityFactor(self.getWorld(), self.getBlockPos());
        if (GravityUtil.isVanillaGravity(factor)) {
            return;
        }

        if (fallDistance <= GravityUtil.getMaxJumpHeight(factor)) {
            cir.setReturnValue(0);
            return;
        }

        cir.setReturnValue(MathHelper.ceil((float) original * factor));
    }
}