package com.eldenring.spells.item;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.network.AzurStaffSettingsPayload;
import com.eldenring.spells.network.TrackingIgnorePrefsPayload;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class AzurStaffBalance {
    public static final double DEFAULT_CAST_TIME_REDUCTION = 0.15D;
    public static final double DEFAULT_MANA_COST_MULTIPLIER = 1.20D;

    /**
     * 吟唱速度修饰符的稳定 UUID。
     * 1.20.1 的 {@link AttributeModifier} 用 UUID（而不是 1.21 的 ResourceLocation）标识一条修饰符，
     * 因此「替换」只能靠同一 UUID 先删后加；固定成常量才能在世界配置热重载时精确命中旧条目。
     */
    private static final UUID CAST_SPEED_UUID = UUID.fromString("3a7c1f52-2e64-4b8d-9c07-5d1e2f3a4b6c");

    /** 修饰符显示名（调试 / 日志用），与上面 UUID 一一对应。 */
    private static final String CAST_SPEED_MODIFIER_NAME = "azur_cast_speed";

    private static volatile Values values =
            new Values(DEFAULT_CAST_TIME_REDUCTION, DEFAULT_MANA_COST_MULTIPLIER);

    private AzurStaffBalance() {
    }

    public static double manaCostMultiplier() {
        return values.manaCostMultiplier();
    }

    /** 世界配置卸载后恢复本地预览默认值，不再向正在停止的服务器安排工作。 */
    public static void resetDefaults() {
        values = new Values(DEFAULT_CAST_TIME_REDUCTION, DEFAULT_MANA_COST_MULTIPLIER);
    }

    public static boolean isMainhandAzur(Player player) {
        return player != null && player.getMainHandItem().getItem() instanceof AzurGlintstoneStaffItem;
    }

    public static double currentMultiplier(Player player) {
        return isMainhandAzur(player) ? manaCostMultiplier() : 1.0D;
    }

    public static AttributeModifier castSpeedModifier() {
        return new AttributeModifier(CAST_SPEED_UUID, CAST_SPEED_MODIFIER_NAME, values.castTimeReduction(),
                AttributeModifier.Operation.MULTIPLY_BASE);
    }

    @SubscribeEvent
    public static void addConfiguredAttributes(ItemAttributeModifierEvent event) {
        // 本事件对所有槽位都会触发；只有主手吃到吟唱加成，其余槽位早退，否则副手也会被塞一条。
        if (!(event.getItemStack().getItem() instanceof AzurGlintstoneStaffItem)
                || event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }
        Attribute castTimeReductionAttribute = AttributeRegistry.CAST_TIME_REDUCTION.get();
        // 1.20.1 没有 NeoForge 的 replaceModifier；同 UUID 先删后加达到同样效果，
        // 顺便清掉世界配置热重载后残留的旧数值。先拷贝再删，避免边遍历边改事件里的 multimap。
        for (AttributeModifier existing : new ArrayList<>(event.getModifiers().get(castTimeReductionAttribute))) {
            if (existing.getId().equals(CAST_SPEED_UUID)) {
                event.removeModifier(castTimeReductionAttribute, existing);
            }
        }
        event.addModifier(castTimeReductionAttribute, castSpeedModifier());
    }

    public static void configure(double castTimeReduction, double manaCostMultiplier) {
        Values updated = new Values(castTimeReduction, manaCostMultiplier);
        if (values.equals(updated)) {
            return;
        }
        values = updated;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Equipment attributes otherwise refresh only when the equipped stack changes.
            server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> {
                syncTo(player);
                if (isMainhandAzur(player)) {
                    AttributeInstance attribute = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION.get());
                    if (attribute != null) {
                        attribute.removeModifier(CAST_SPEED_UUID);
                        attribute.addTransientModifier(castSpeedModifier());
                    }
                }
            }));
        }
    }

    public static void acceptServerSettings(double castTimeReduction, double manaCostMultiplier) {
        values = new Values(castTimeReduction, manaCostMultiplier);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncTo(player);
        }
    }

    private static void syncTo(ServerPlayer player) {
        // Fake players and unnegotiated test connections do not support mod payloads.
        if (TrackingIgnorePrefsPayload.CHANNEL.isRemotePresent(player.connection.connection)) {
            Values current = values;
            TrackingIgnorePrefsPayload.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new AzurStaffSettingsPayload(current.castTimeReduction(), current.manaCostMultiplier()));
        }
    }

    private record Values(double castTimeReduction, double manaCostMultiplier) {
    }
}
