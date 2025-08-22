package dev.cigarette.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.cigarette.Cigarette;
import dev.cigarette.GameDetector;
import dev.cigarette.agent.BedwarsAgent;
import dev.cigarette.gui.widget.*;
import dev.cigarette.lib.Renderer;
import dev.cigarette.module.RenderModule;
import dev.cigarette.precomputed.PyramidQuadrant;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;

public class DefenseViewer extends RenderModule<ToggleWidget, Boolean> {
    public static final DefenseViewer INSTANCE = Cigarette.CONFIG.constructModule(new DefenseViewer("bedwars.defenseesp", "Defense Viewer", "ESPs bed blocks and the defensive blocks around them."), "Bedwars");

    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockesp", 1536, Renderer.BLOCK_ESP_PHASE, RenderLayer.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1))).build(false));
    private final HashSet<BlockPos> bedBlocks = new HashSet<>();
    private final HashMap<BlockPos, Integer> defensiveBlocks = new HashMap<>();
    private int layer = 0;

    private final KeybindWidget increaseKey = new KeybindWidget(Text.literal("Increase Layer"), Text.literal("Increases the layer shown by 1."));
    private final KeybindWidget decreaseKey = new KeybindWidget(Text.literal("Decrease Layer"), Text.literal("Decreases the layer shown by 1."));
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableBeds = ColorDropdownWidget.buildToggle(Text.literal("Bed Color"), Text.literal("The ESP color used to highlight bed blocks once you are within a small range of the bed.")).withDefaultColor(0xFFFF0000).withDefaultState(true);
    private final SliderWidget bedDistance = new SliderWidget(Text.literal("Distance"), Text.literal("The max distance the player must be away from the bed for this to stop highlighting blocks and to start highlighting the bed.")).withBounds(0, 10, 30).withAccuracy(1);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableWool = ColorDropdownWidget.buildToggle(Text.literal("Wool"), null).withDefaultColor(0x7FFFFFFF).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableEndStone = ColorDropdownWidget.buildToggle(Text.literal("Endstone"), null).withDefaultColor(0x7FFFFF00).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableWood = ColorDropdownWidget.buildToggle(Text.literal("Wood"), null).withDefaultColor(0x7FFF0000).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableClay = ColorDropdownWidget.buildToggle(Text.literal("Clay"), null).withDefaultColor(0x7F0000FF).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableObsidian = ColorDropdownWidget.buildToggle(Text.literal("Obsidian"), null).withDefaultColor(0x7FFF00FF).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableGlass = ColorDropdownWidget.buildToggle(Text.literal("Glass"), null).withDefaultColor(0x7F00FF00).withDefaultState(true);


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
        if (BedwarsAgent.isWool(state) && enableWool.getToggleState()) return enableWool.getStateARGB();
        if (BedwarsAgent.isEndStone(state) && enableEndStone.getToggleState()) return enableEndStone.getStateARGB();
        if (BedwarsAgent.isWood(state) && enableWood.getToggleState()) return enableWood.getStateARGB();
        if (BedwarsAgent.isClay(state) && enableClay.getToggleState()) return enableClay.getStateARGB();
        if (BedwarsAgent.isObsidian(state) && enableObsidian.getToggleState()) return enableObsidian.getStateARGB();
        if (BedwarsAgent.isGlass(state) && enableGlass.getToggleState()) return enableGlass.getStateARGB();
        return 0;
    }

    public DefenseViewer(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        TextWidget header = new TextWidget(Text.literal("Block Types")).withUnderline();
        this.setChildren(increaseKey, decreaseKey, enableBeds, bedDistance, header, enableWool, enableEndStone, enableWood, enableClay, enableObsidian, enableGlass);
        increaseKey.registerConfigKey(id + ".increase");
        decreaseKey.registerConfigKey(id + ".decrease");
        enableBeds.registerConfigKey(id + ".bed");
        bedDistance.registerConfigKey(id + ".distance");
        enableWood.registerConfigKey(id + ".wood");
        enableEndStone.registerConfigKey(id + ".endstone");
        enableWool.registerConfigKey(id + ".wool");
        enableClay.registerConfigKey(id + ".clay");
        enableObsidian.registerConfigKey(id + ".obsidian");
        enableGlass.registerConfigKey(id + ".glass");
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        if (enableBeds.getToggleState()) {
            for (BlockPos pos : bedBlocks) {
                Renderer.drawYQuad(buffer, matrix, enableBeds.getStateARGB(), pos.getY() + 0.5f, pos.getX(), pos.getZ(), pos.getX() + 1f, pos.getZ() + 1f);
                Renderer.drawYQuad(buffer, matrix, enableBeds.getStateARGB(), pos.getY(), pos.getX(), pos.getZ(), pos.getX() + 1f, pos.getZ() + 1f);
                Renderer.drawXQuad(buffer, matrix, enableBeds.getStateARGB(), pos.getX(), pos.getY(), pos.getZ(), pos.getY() + 0.5f, pos.getZ() + 1f);
                Renderer.drawXQuad(buffer, matrix, enableBeds.getStateARGB(), pos.getX() + 1f, pos.getY(), pos.getZ(), pos.getY() + 0.5f, pos.getZ() + 1f);
                Renderer.drawZQuad(buffer, matrix, enableBeds.getStateARGB(), pos.getZ(), pos.getX(), pos.getY(), pos.getX() + 1f, pos.getY() + 0.5f);
                Renderer.drawZQuad(buffer, matrix, enableBeds.getStateARGB(), pos.getZ() + 1f, pos.getX(), pos.getY(), pos.getX() + 1f, pos.getY() + 0.5f);
            }
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
            boolean playerClose = bed.head().isWithinDistance(player.getPos(), bedDistance.getRawState());
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
        while (increaseKey.getKeybind().wasPressed()) {
            this.layer = Math.min(this.layer + 1, PyramidQuadrant.MAX_LAYER);
        }
        while (decreaseKey.getKeybind().wasPressed()) {
            this.layer = Math.max(this.layer - 1, 0);
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.bedBlocks.clear();
        this.defensiveBlocks.clear();
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
