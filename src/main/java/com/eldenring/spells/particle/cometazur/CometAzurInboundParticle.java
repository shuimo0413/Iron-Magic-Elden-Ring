package com.eldenring.spells.particle.cometazur;

import com.eldenring.spells.spell.CometAzurSpell;

import com.eldenring.spells.client.render.AdditiveParticleRenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 蓄力汇聚粒子：沿对数螺线 {@code r = 外半径 × e^(wA)} 走向中心。
 * <p>
 * {@code A} 从出生角走到 {@code 12π}，寿命固定 2 秒，和漩涡一起收完。
 * {@code xd/yd/zd} 分别是 w、出生角 A、臂相位；平面坐标由漩涡通过 {@link #bindSpiralFrame} 注入。
 */
public class CometAzurInboundParticle extends TextureSheetParticle {

    /**
     * 对数螺线所在平面：圆心 + 两个正交轴（垂直于施法朝向）。
     */
    public record SpiralFrame(Vec3 center, Vec3 rightAxis, Vec3 upAxis) {
    }

    /**
     * 客户端 {@code addParticle} → Provider 同步调用链上的平面。
     * SimpleParticleType 带不下 9 个 double，故用 ThreadLocal；仅漩涡铺螺线时写入。
     */
    private static final ThreadLocal<SpiralFrame> CLIENT_SPIRAL_FRAME = new ThreadLocal<>();

    public static void bindSpiralFrame(SpiralFrame spiralFrame) {
        CLIENT_SPIRAL_FRAME.set(spiralFrame);
    }

    public static void clearSpiralFrame() {
        CLIENT_SPIRAL_FRAME.remove();
    }

    /**
     * 周围粒子贴图种类。寿命相同，只是四边形大小和闪星选帧不一样。
     */
    public enum Kind {
        /** mote_1 / mote_2 十字闪星。 */
        MOTE(CometAzurFx.STARTUP_MOTE_QUAD_SIZE_BLOCKS, true),
        /** impact 八芒星。 */
        IMPACT(CometAzurFx.STARTUP_IMPACT_QUAD_SIZE_BLOCKS, false),
        /** head 星团。 */
        HEAD(CometAzurFx.STARTUP_HEAD_QUAD_SIZE_BLOCKS, false),
        /** dust 碎星尘。 */
        DUST(CometAzurFx.STARTUP_DUST_QUAD_SIZE_BLOCKS, false);

        final float quadSizeBlocks;
        /** true：只取 mote 序列里的 mote_1 / mote_2。 */
        final boolean pickMoteSpriteFromLastTwoFrames;

        Kind(float quadSizeBlocks, boolean pickMoteSpriteFromLastTwoFrames) {
            this.quadSizeBlocks = quadSizeBlocks;
            this.pickMoteSpriteFromLastTwoFrames = pickMoteSpriteFromLastTwoFrames;
        }
    }

    private final SpiralFrame spiralFrame;
    private final float spiralW;
    private final float angleBirthRadians;
    private final float armPhaseRadians;
    private final float birthQuadSize;
    private final float birthAlpha;

    protected CometAzurInboundParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double spiralWPayload,
            double angleBirthPayload,
            double armPhasePayload,
            SpriteSet sprites,
            Kind kind
    ) {
        super(level, x, y, z);
        SpiralFrame boundFrame = CLIENT_SPIRAL_FRAME.get();
        this.spiralFrame = boundFrame != null
                ? boundFrame
                : new SpiralFrame(new Vec3(x, y, z), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0));
        this.spiralW = (float) spiralWPayload;
        this.angleBirthRadians = (float) angleBirthPayload;
        this.armPhaseRadians = (float) armPhasePayload;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.lifetime = CometAzurSpell.STARTUP_DURATION_TICKS;
        this.birthQuadSize = kind.quadSizeBlocks * (0.82f + level.random.nextFloat() * 0.36f);
        this.quadSize = this.birthQuadSize;
        this.birthAlpha = 0.90f;
        this.alpha = this.birthAlpha;
        // 蓄力螺线偏墨绿，和喷流星河统一。
        this.rCol = 0.48f;
        this.gCol = 0.92f;
        this.bCol = 0.84f;
        if (kind.pickMoteSpriteFromLastTwoFrames) {
            int moteFrameIndex = 1 + level.random.nextInt(2);
            setSprite(sprites.get(moteFrameIndex, 2));
        } else {
            setSprite(sprites.get(0, 1));
        }
        moveToAngle(this.angleBirthRadians);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            remove();
            return;
        }

        float lifeFraction = (float) this.age / (float) this.lifetime;
        float currentAngleRadians = Mth.lerp(
                lifeFraction,
                this.angleBirthRadians,
                CometAzurFx.STARTUP_SPIRAL_MAX_ANGLE_RADIANS
        );
        moveToAngle(currentAngleRadians);

        if (lifeFraction < 0.82f) {
            this.alpha = this.birthAlpha;
            this.quadSize = this.birthQuadSize * (1.0f - 0.35f * lifeFraction);
        } else {
            float fade = (1.0f - lifeFraction) / 0.18f;
            this.alpha = this.birthAlpha * fade;
            this.quadSize = this.birthQuadSize * 0.45f * fade;
        }
    }

    /**
     * {@code r = 外半径 × e^(wA)}，平面角 = {@code A + 臂相位}。
     */
    private void moveToAngle(float angleRadians) {
        double radiusBlocks = CometAzurFx.STARTUP_SPIRAL_OUTER_RADIUS_BLOCKS
                * Math.exp(this.spiralW * angleRadians);
        double planeAngleRadians = angleRadians + this.armPhaseRadians;
        double cosineAngle = Math.cos(planeAngleRadians);
        double sineAngle = Math.sin(planeAngleRadians);
        Vec3 worldOffset = this.spiralFrame.rightAxis().scale(radiusBlocks * cosineAngle)
                .add(this.spiralFrame.upAxis().scale(radiusBlocks * sineAngle));
        Vec3 worldPosition = this.spiralFrame.center().add(worldOffset);
        this.x = worldPosition.x;
        this.y = worldPosition.y;
        this.z = worldPosition.z;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticleRenderType.ADDITIVE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xd,
                double yd,
                double zd
        ) {
            return new CometAzurInboundParticle(level, x, y, z, xd, yd, zd, this.sprites, this.kind);
        }
    }
}
