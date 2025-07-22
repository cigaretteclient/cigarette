package io.github.waqfs.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.lib.Renderer;
import io.github.waqfs.module.BaseModule;
import io.github.waqfs.precomputed.PyramidQuadrant;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;

public class DefenseViewer extends BaseModule {
    protected static final String MODULE_NAME = "Defense Viewer";
    protected static final String MODULE_TOOLTIP = "ESPs bed blocks and the defensive blocks around them.";
    protected static final String MODULE_ID = "bedwars.defenseesp";
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockesp", 1536, Renderer.BLOCK_ESP, RenderLayer.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1))).build(false));
    private final HashSet<BlockPos> bedBlocks = new HashSet<>();
    private final HashMap<BlockPos, Integer> defensiveBlocks = new HashMap<>();
    private int layer = 0;


    private HashSet<BlockPos> getBlocksInLayer(BedwarsAgent.PersistentBed bed, int layer) {
        if (layer < 0 || layer >= PyramidQuadrant.MAX_LAYER) {
            throw new ArrayIndexOutOfBoundsException("Layer must be between 0 and " + PyramidQuadrant.MAX_LAYER + ", given " + layer);
        }

        Vec3i[] headLeft = PyramidQuadrant.getLeftQuadrant(bed.headDirection())[layer];
        Vec3i[] headRight = PyramidQuadrant.getRightQuadrant(bed.headDirection())[layer];
        Vec3i[] footLeft = PyramidQuadrant.getLeftQuadrant(bed.footDirection())[layer];
        Vec3i[] footRight = PyramidQuadrant.getRightQuadrant(bed.footDirection())[layer];

        HashSet<BlockPos> positions = new HashSet<>();
        for (Vec3i pos : headLeft) positions.add(bed.head().add(pos));
        for (Vec3i pos : headRight) positions.add(bed.head().add(pos));
        for (Vec3i pos : footLeft) positions.add(bed.foot().add(pos));
        for (Vec3i pos : footRight) positions.add(bed.foot().add(pos));

        return positions;
    }

    private int getColorFromBlockState(BlockState state) {
        if (state.isOf(Blocks.WHITE_WOOL) || state.isOf(Blocks.BLACK_WOOL) || state.isOf(Blocks.BLUE_WOOL) || state.isOf(Blocks.BROWN_WOOL) || state.isOf(Blocks.CYAN_WOOL) || state.isOf(Blocks.GRAY_WOOL) || state.isOf(Blocks.GREEN_WOOL) || state.isOf(Blocks.LIGHT_BLUE_WOOL) || state.isOf(Blocks.LIGHT_GRAY_WOOL) || state.isOf(Blocks.LIME_WOOL) || state.isOf(Blocks.MAGENTA_WOOL) || state.isOf(Blocks.ORANGE_WOOL) || state.isOf(Blocks.PINK_WOOL) || state.isOf(Blocks.PURPLE_WOOL) || state.isOf(Blocks.RED_WOOL) || state.isOf(Blocks.YELLOW_WOOL))
            return 0x7FFFFFFF;
        if (state.isOf(Blocks.END_STONE)) return 0x7FFFFF00;
        if (state.isOf(Blocks.OAK_PLANKS) || state.isOf(Blocks.ACACIA_PLANKS) || state.isOf(Blocks.SPRUCE_PLANKS) || state.isOf(Blocks.DARK_OAK_PLANKS) || state.isOf(Blocks.BIRCH_PLANKS) || state.isOf(Blocks.ACACIA_LOG) || state.isOf(Blocks.BIRCH_LOG) || state.isOf(Blocks.DARK_OAK_LOG) || state.isOf(Blocks.JUNGLE_LOG) || state.isOf(Blocks.OAK_LOG) || state.isOf(Blocks.SPRUCE_LOG))
            return 0x7FFF0000;
        if (state.isOf(Blocks.TERRACOTTA) || state.isOf(Blocks.BLACK_TERRACOTTA) || state.isOf(Blocks.BLUE_TERRACOTTA) || state.isOf(Blocks.BROWN_TERRACOTTA) || state.isOf(Blocks.CYAN_TERRACOTTA) || state.isOf(Blocks.GRAY_TERRACOTTA) || state.isOf(Blocks.GREEN_TERRACOTTA) || state.isOf(Blocks.LIGHT_BLUE_TERRACOTTA) || state.isOf(Blocks.LIGHT_GRAY_TERRACOTTA) || state.isOf(Blocks.LIME_TERRACOTTA) || state.isOf(Blocks.MAGENTA_TERRACOTTA) || state.isOf(Blocks.ORANGE_TERRACOTTA) || state.isOf(Blocks.PINK_TERRACOTTA) || state.isOf(Blocks.PURPLE_TERRACOTTA) || state.isOf(Blocks.RED_TERRACOTTA) || state.isOf(Blocks.WHITE_TERRACOTTA) || state.isOf(Blocks.YELLOW_TERRACOTTA))
            return 0x7F0000FF;
        if (state.isOf(Blocks.OBSIDIAN)) return 0x7FFF00FF;
        if (state.isOf(Blocks.BLACK_STAINED_GLASS) || state.isOf(Blocks.BLUE_STAINED_GLASS) || state.isOf(Blocks.BROWN_STAINED_GLASS) || state.isOf(Blocks.CYAN_STAINED_GLASS) || state.isOf(Blocks.GRAY_STAINED_GLASS) || state.isOf(Blocks.GREEN_STAINED_GLASS) || state.isOf(Blocks.LIGHT_BLUE_STAINED_GLASS) || state.isOf(Blocks.LIGHT_GRAY_STAINED_GLASS) || state.isOf(Blocks.LIME_STAINED_GLASS) || state.isOf(Blocks.MAGENTA_STAINED_GLASS) || state.isOf(Blocks.ORANGE_STAINED_GLASS) || state.isOf(Blocks.PINK_STAINED_GLASS) || state.isOf(Blocks.PURPLE_STAINED_GLASS) || state.isOf(Blocks.RED_STAINED_GLASS) || state.isOf(Blocks.WHITE_STAINED_GLASS) || state.isOf(Blocks.YELLOW_STAINED_GLASS))
            return 0x7F00FF00;
        return 0;
    }

    public DefenseViewer() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!this.isEnabled() || client.world == null || client.player == null || GameDetector.rootGame != GameDetector.ParentGame.BEDWARS || GameDetector.subGame != GameDetector.ChildGame.INSTANCED_BEDWARS) {
                return;
            }
            HashSet<BedwarsAgent.PersistentBed> beds = BedwarsAgent.getVisibleBeds();

            bedBlocks.clear();
            defensiveBlocks.clear();
            for (BedwarsAgent.PersistentBed bed : beds) {
                boolean playerClose = bed.head().isWithinDistance(client.player.getPos(), 10);
                if (playerClose) {
                    bedBlocks.add(bed.head());
                    bedBlocks.add(bed.foot());
                    continue;
                }
                HashSet<BlockPos> blocksInLayer = this.getBlocksInLayer(bed, this.layer);
                for (BlockPos pos : blocksInLayer) {
                    BlockState state = client.world.getBlockState(pos);
                    int color = getColorFromBlockState(state);
                    if (color != 0) defensiveBlocks.put(pos, color);
                }
            }
        });
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(ctx -> {
            if (!this.isEnabled() || GameDetector.rootGame != GameDetector.ParentGame.BEDWARS || GameDetector.subGame != GameDetector.ChildGame.INSTANCED_BEDWARS) {
                return;
            }
            MatrixStack matrixStack = ctx.matrixStack();
            if (matrixStack == null) return;
            matrixStack.push();

            Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (BlockPos pos : bedBlocks) {
                Renderer.drawYQuad(buffer, matrix, 0xFFFF0000, pos.getY() + 0.5f, pos.getX(), pos.getZ(), pos.getX() + 1f, pos.getZ() + 1f);
                Renderer.drawYQuad(buffer, matrix, 0xFFFF0000, pos.getY(), pos.getX(), pos.getZ(), pos.getX() + 1f, pos.getZ() + 1f);
                Renderer.drawXQuad(buffer, matrix, 0xFFFF0000, pos.getX(), pos.getY(), pos.getZ(), pos.getY() + 0.5f, pos.getZ() + 1f);
                Renderer.drawXQuad(buffer, matrix, 0xFFFF0000, pos.getX() + 1f, pos.getY(), pos.getZ(), pos.getY() + 0.5f, pos.getZ() + 1f);
                Renderer.drawZQuad(buffer, matrix, 0xFFFF0000, pos.getZ(), pos.getX(), pos.getY(), pos.getX() + 1f, pos.getY() + 0.5f);
                Renderer.drawZQuad(buffer, matrix, 0xFFFF0000, pos.getZ() + 1f, pos.getX(), pos.getY(), pos.getX() + 1f, pos.getY() + 0.5f);
            }
            for (Map.Entry<BlockPos, Integer> entry : defensiveBlocks.entrySet()) {
                Renderer.drawBlock(buffer, matrix, entry.getValue(), entry.getKey());
            }

            BuiltBuffer built = buffer.endNullable();
            if (built != null) RENDER_LAYER.draw(built);
            matrixStack.pop();
        });
    }
}
