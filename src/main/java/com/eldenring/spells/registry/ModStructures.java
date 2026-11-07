package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.worldgen.structure.AcademyBlendExtentPiece;
import com.eldenring.spells.worldgen.structure.AcademyPiece;
import com.eldenring.spells.worldgen.structure.AcademyStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义结构类型与结构片段类型注册。
 * <p>
 * 结构本身的 biomes / spacing 仍走数据包 JSON；这里只注册
 * {@code "type": "iss_elden_ring:academy"} 所需的 {@link StructureType}，
 * 以及存档反序列化 {@link AcademyPiece} / {@link AcademyBlendExtentPiece} 所需的 {@link StructurePieceType}。
 */
public final class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, EldenRingSpellsMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, EldenRingSpellsMod.MOD_ID);

    /**
     * 雷亚卢卡利亚学院：固定格子模板结构（非 jigsaw）。
     */
    public static final DeferredHolder<StructureType<?>, StructureType<AcademyStructure>> ACADEMY =
            STRUCTURE_TYPES.register("academy", () -> () -> AcademyStructure.CODEC);

    /**
     * 学院单片模板片段；世界存档读写依赖此类型名。
     */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ACADEMY_PIECE =
            STRUCTURE_PIECES.register(
                    "academy",
                    () -> (StructurePieceType.StructureTemplateType) AcademyPiece::new
            );

    /**
     * 缓坡外扩空片段：扩大 StructureStart 包围盒，让贴边外侧列所在区块也会 afterPlace。
     */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ACADEMY_BLEND_EXTENT =
            STRUCTURE_PIECES.register(
                    "academy_blend_extent",
                    () -> (StructurePieceType.ContextlessType) AcademyBlendExtentPiece::new
            );

    private ModStructures() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
    }
}
