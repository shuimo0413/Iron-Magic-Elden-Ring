package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.tracking.TrackingIgnorePrefs;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家辉石追踪「排除」偏好的持久存储。
 * <p>
 * NeoForge 1.21.1 用 {@code AttachmentType} 表达这份数据；Forge 1.20.1 没有 attachment，
 * 等价物是实体 Capability。能力值随玩家存档持久化，死亡后由
 * {@code event/TrackingIgnoreSyncEvents#onPlayerClone} 手动搬运（对应旧 {@code copyOnDeath()}）。
 * <p>
 * 对外只暴露 {@link #getPrefs(Player)} / {@link #setPrefs(Player, TrackingIgnorePrefs)} 两个方法，
 * 业务代码（索敌过滤、网络包）不需要感知 Capability 细节。
 */
public final class ModAttachments {

    /**
     * capability 注册 id：同时作为 attach 到实体的 key。
     * 只是内部标识，与玩家偏好里各勾选项无关。
     */
    private static final ResourceLocation TRACKING_IGNORE_PREFS_ID =
            new ResourceLocation(EldenRingSpellsMod.MOD_ID, "tracking_ignore_prefs");

    /**
     * 玩家追踪排除偏好的 capability 句柄。
     * {@link CapabilityManager#get} 在类加载期即可安全调用（惰性解析），实际校验发生在
     * {@link RegisterCapabilitiesEvent}。
     */
    public static final Capability<PrefsHolder> TRACKING_IGNORE_PREFS =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private ModAttachments() {
    }

    /**
     * 读取玩家偏好；能力缺失（未 attach 的非玩家上下文 / 客户端未 attach）时回退默认值，
     * 不抛异常、不返回 null。
     */
    public static TrackingIgnorePrefs getPrefs(Player player) {
        return player.getCapability(TRACKING_IGNORE_PREFS)
                .map(PrefsHolder::get)
                .orElse(TrackingIgnorePrefs.DEFAULT);
    }

    /**
     * 覆写玩家偏好。记录是不可变的，因此这里替换 holder 里的整个实例。
     */
    public static void setPrefs(Player player, TrackingIgnorePrefs prefs) {
        player.getCapability(TRACKING_IGNORE_PREFS).ifPresent(holder -> holder.set(prefs));
    }

    /**
     * 注册 capability：
     * <ul>
     *   <li>{@link RegisterCapabilitiesEvent} 走 mod 事件总线；</li>
     *   <li>{@link AttachCapabilitiesEvent} 是通用事件，必须挂 Forge 事件总线并指定泛型 {@link Entity}。</li>
     * </ul>
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModAttachments::registerCapabilities);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, ModAttachments::attachTrackingPrefs);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PrefsHolder.class);
    }

    private static void attachTrackingPrefs(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(TRACKING_IGNORE_PREFS_ID, new PrefsProvider());
        }
    }

    /**
     * capability 值容器。{@link TrackingIgnorePrefs} 是 record（不可变），
     * 但 capability 的读取方需要在原地更新，因此用这层可变 holder 包一层。
     * <p>
     * 写成嵌套类而非独立文件，避免为纯管道类型新增源文件。
     */
    public static final class PrefsHolder {
        private TrackingIgnorePrefs value = TrackingIgnorePrefs.DEFAULT;

        public TrackingIgnorePrefs get() {
            return value;
        }

        public void set(TrackingIgnorePrefs prefs) {
            this.value = prefs;
        }
    }

    /**
     * capability 提供者：负责把四勾选项写进 NBT / 从 NBT 读回。
     * <p>
     * 选 NBT 而不是 {@code TrackingIgnorePrefs.CODEC}：Forge 的 {@link ICapabilitySerializable}
     * 直接约定 {@link CompoundTag}，手写四个布尔键更直观，也避免 DFU 编解码的额外样板。
     */
    public static final class PrefsProvider implements ICapabilitySerializable<CompoundTag> {

        private static final String NBT_IGNORE_PLAYERS = "ignore_players";
        private static final String NBT_IGNORE_PEACEFUL = "ignore_peaceful";
        private static final String NBT_IGNORE_NEUTRAL = "ignore_neutral";
        private static final String NBT_IGNORE_HOSTILE = "ignore_hostile";

        private final PrefsHolder holder = new PrefsHolder();
        private final LazyOptional<PrefsHolder> optional = LazyOptional.of(() -> holder);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            return TRACKING_IGNORE_PREFS.orEmpty(capability, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            TrackingIgnorePrefs prefs = holder.get();
            tag.putBoolean(NBT_IGNORE_PLAYERS, prefs.ignorePlayers());
            tag.putBoolean(NBT_IGNORE_PEACEFUL, prefs.ignorePeaceful());
            tag.putBoolean(NBT_IGNORE_NEUTRAL, prefs.ignoreNeutral());
            tag.putBoolean(NBT_IGNORE_HOSTILE, prefs.ignoreHostile());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            holder.set(new TrackingIgnorePrefs(
                    tag.getBoolean(NBT_IGNORE_PLAYERS),
                    tag.getBoolean(NBT_IGNORE_PEACEFUL),
                    tag.getBoolean(NBT_IGNORE_NEUTRAL),
                    tag.getBoolean(NBT_IGNORE_HOSTILE)
            ));
        }
    }
}
