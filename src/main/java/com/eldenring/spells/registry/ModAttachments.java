package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.tracking.TrackingIgnorePrefs;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 本模组 {@link AttachmentType}：挂在玩家实体上的持久数据。
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, EldenRingSpellsMod.MOD_ID);

    /**
     * 玩家辉石追踪排除偏好。写入存档，死亡后保留（{@code copyOnDeath}）。
     */
    public static final Supplier<AttachmentType<TrackingIgnorePrefs>> TRACKING_IGNORE_PREFS =
            ATTACHMENT_TYPES.register(
                    "tracking_ignore_prefs",
                    () -> AttachmentType.builder(() -> TrackingIgnorePrefs.DEFAULT)
                            .serialize(TrackingIgnorePrefs.CODEC)
                            .copyOnDeath()
                            .build()
            );

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
