package io.github.waqfs.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.lib.Renderer;
import io.github.waqfs.module.RenderModule;
import io.github.waqfs.precomputed.PyramidQuadrant;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;

public class DefenseViewer extends RenderModule implements ClientModInitializer {
    protected static final String MODULE_NAME = "Defense Viewer";
    protected static final String MODULE_TOOLTIP = "ESPs bed blocks and the defensive blocks around them.";
    protected static final String MODULE_ID = "bedwars.defenseesp";
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockesp", 1536, Renderer.BLOCK_ESP, RenderLayer.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1))).build(false));
    private final HashSet<BlockPos> bedBlocks = new HashSet<>();
    private final HashMap<BlockPos, Integer> defensiveBlocks = new HashMap<>();
    private static KeyBinding decreaseKeyBinding;
    private static KeyBinding increaseKeyBinding;
    private int layer = 0;


    private HashSet<BlockPos> getBlocksInLayer(BedwarsAgent.PersistentBed bed, int layer) {
        if (layer < 0 || layer > PyramidQuadrant.MAX_LAYER) {
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
        if (BedwarsAgent.isWool(state)) return 0x7FFFFFFF;
        if (BedwarsAgent.isEndStone(state)) return 0x7FFFFF00;
        if (BedwarsAgent.isWood(state)) return 0x7FFF0000;
        if (BedwarsAgent.isClay(state)) return 0x7F0000FF;
        if (BedwarsAgent.isObsidian(state)) return 0x7FFF00FF;
        if (BedwarsAgent.isGlass(state)) return 0x7F00FF00;
        return 0;
    }

    public DefenseViewer() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    public void onInitializeClient() {
        increaseKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("increase_defense.bedwars.cigarette.waqfs", InputUtil.Type.KEYSYM, GLFW.GLFW_NOT_INITIALIZED, "bedwars.cigarette.waqfs"));
        decreaseKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("decrease_defense.bedwars.cigarette.waqfs", InputUtil.Type.KEYSYM, GLFW.GLFW_NOT_INITIALIZED, "bedwars.cigarette.waqfs"));
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
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
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.bedBlocks.clear();
        this.defensiveBlocks.clear();
        HashSet<BedwarsAgent.PersistentBed> beds = BedwarsAgent.getVisibleBeds();
        for (BedwarsAgent.PersistentBed bed : beds) {
            boolean playerClose = bed.head().isWithinDistance(player.getPos(), 10);
            if (playerClose) {
                bedBlocks.add(bed.head());
                bedBlocks.add(bed.foot());
                continue;
            }
            HashSet<BlockPos> blocksInLayer = this.getBlocksInLayer(bed, this.layer);
            for (BlockPos pos : blocksInLayer) {
                BlockState state = world.getBlockState(pos);
                int color = getColorFromBlockState(state);
                if (color != 0) defensiveBlocks.put(pos, color);
            }
        }
        while (increaseKeyBinding.wasPressed()) {
            this.layer = Math.min(this.layer + 1, PyramidQuadrant.MAX_LAYER);
            System.out.println("layer = " + this.layer);
        }
        while (decreaseKeyBinding.wasPressed()) {
            this.layer = Math.max(this.layer - 1, 0);
            System.out.println("layer = " + this.layer);
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.bedBlocks.clear();
        this.defensiveBlocks.clear();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
