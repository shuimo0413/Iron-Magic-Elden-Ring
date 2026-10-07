package com.eldenring.spells;

import com.eldenring.spells.client.CarianGreatswordClientHold;
import com.eldenring.spells.client.AdulasMoonbladeClientHold;
import com.eldenring.spells.client.CarianPiercerClientHold;
import com.eldenring.spells.client.CarianSlicerClientHold;
import com.eldenring.spells.client.ClientEntityRenderers;
import com.eldenring.spells.client.ClientItemModels;
import com.eldenring.spells.client.ClientParticleProviders;
import com.eldenring.spells.registry.ModBlocks;
import com.eldenring.spells.registry.ModItems;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import io.redspace.ironsspellbooks.render.SpellBookCurioRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * 客户端入口：粒子 / 实体渲染 / 卷轴模型 / 魔法书 Curios 渲染。
 * <p>
 * Forge 1.20.1 约束：
 * <ul>
 *   <li>同一个 modid 只能有一个 {@code @Mod} 类，且 {@code @Mod} 没有 {@code dist} 参数；
 *       所以本类不再是第二个 {@code @Mod}，改为纯 MOD 总线订阅者。它的处理器全部是 MOD 总线事件
 *       （{@code FMLClientSetupEvent} / {@code RegisterParticleProvidersEvent} / {@code ModelEvent} /
 *       {@code EntityRenderersEvent}），因此必须显式写 {@code bus = Mod.EventBusSubscriber.Bus.MOD}。</li>
 *   <li>NeoForge 的 {@code IConfigScreenFactory} 与自动生成 {@code ConfigurationScreen} 不存在。
 *       本次降级为「无内置配置界面」：{@code config/iss_elden_ring-server.toml} 与
 *       {@code -common.toml} 仍可手动编辑，加载 / 热重载时照样 apply 到运行时字段，
 *       只是少了游戏内的图形化编辑入口。</li>
 *   <li>物品 / 流体客户端扩展（法杖握持姿势、起源药剂染色）分别由铁魔法 {@code StaffItem}
 *       自带实现与 {@code fluid/ModFluids} 的 {@code FluidType#initializeClient} 负责，见下方说明。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EldenRingSpellsClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        EldenRingSpellsMod.LOGGER.info(
                "Iron's Spells 'n Spellbooks: Elden Ring client ready. Player={}",
                Minecraft.getInstance().getUser().getName()
        );
        // 十字面片水晶必须走 cutout，否则透明像素会糊成黑块
        event.enqueueWork(() -> {
            for (ModBlocks.ColorSet set : ModBlocks.BY_COLOR.values()) {
                ItemBlockRenderTypes.setRenderLayer(set.cluster.get(), RenderType.cutout());
            }
            // 星星法典 / 起源秘典：复用铁魔法 SpellBookCurioRenderer，腰侧显示立体书模型
            CuriosRendererRegistry.register(ModItems.STAR_CODEX.get(), SpellBookCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ORIGIN_CODEX.get(), SpellBookCurioRenderer::new);
            // 迅剑 / 大剑专用层：无 MirrorModifier / 准星跟臂，避免右→左被翻成左→右。
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    CarianSlicerClientHold.CARIAN_SLICER_ANIMATION_LAYER,
                    60,
                    player -> new ModifierLayer<>()
            );
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    CarianGreatswordClientHold.CARIAN_GREATSWORD_ANIMATION_LAYER,
                    60,
                    player -> new ModifierLayer<>()
            );
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    CarianPiercerClientHold.CARIAN_PIERCER_ANIMATION_LAYER,
                    60,
                    player -> new ModifierLayer<>()
            );
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    AdulasMoonbladeClientHold.ADULAS_MOONBLADE_ANIMATION_LAYER,
                    60,
                    player -> new ModifierLayer<>()
            );
        });
    }

    /*
     * 原先的 registerClientExtensions(RegisterClientExtensionsEvent) 在 Forge 1.20.1 已整体移除：
     *  1) 法杖握持姿势：Forge 无 RegisterClientExtensionsEvent（该事件 1.21 才有）。
     *     铁魔法 1.20.1 的 StaffItem 自带 initializeClient()，已应用 StaffArmPose，无需本模组再注册；
     *     ClientStaffItemExtensions 类在 1.20.1 铁魔法中也不存在。
     *  2) 起源药剂流体染色：已移交 registry agent，在 fluid/ModFluids 的
     *     FluidType#initializeClient(Consumer<IClientFluidTypeExtensions>) 内实现
     *     （水 still/flow 贴图 + 0xFF2FADA2 青色染色），本文件不再引用 ModFluids。
     */

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        ClientParticleProviders.register(event);
    }

    @SubscribeEvent
    static void registerScrollModels(ModelEvent.RegisterAdditional event) {
        ClientItemModels.register(event);
    }

    @SubscribeEvent
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ClientEntityRenderers.registerLayerDefinitions(event);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientEntityRenderers.registerRenderers(event);
    }

    @SubscribeEvent
    static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        ClientEntityRenderers.addPlayerLayers(event);
    }
}
