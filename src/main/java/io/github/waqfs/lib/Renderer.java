package io.github.waqfs.lib;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Renderer {
    public static final RenderPipeline BLOCK_ESP_PHASE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET).withLocation("pipeline/cigarette.blockesp").withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withCull(false).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
    public static final RenderPipeline BLOCK_ESP_NOPHASE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET).withLocation("pipeline/cigarette.blockespnophase").withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withCull(false).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).build());

    public static Matrix4f getCameraTranslatedMatrix(MatrixStack matrixStack, WorldRenderContext ctx) {
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        Vec3d cam = ctx.camera().getPos();
        matrix.translate((float) -cam.x, (float) -cam.y, (float) -cam.z);
        return matrix;
    }

    public static void drawBlock(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        drawNorthBlockFace(buffer, matrix, argb, pos);
        drawSouthBlockFace(buffer, matrix, argb, pos);
        drawEastBlockFace(buffer, matrix, argb, pos);
        drawWestBlockFace(buffer, matrix, argb, pos);
        drawUpBlockFace(buffer, matrix, argb, pos);
        drawDownBlockFace(buffer, matrix, argb, pos);
    }

    public static void drawCube(BufferBuilder buffer, Matrix4f matrix, int argb, Vec3d pos, float width) {
        float radius = width / 2;
        drawXQuad(buffer, matrix, argb, (float) pos.x - radius, (float) pos.y - radius, (float) pos.z - radius, (float) pos.y + radius, (float) pos.z + radius);
        drawXQuad(buffer, matrix, argb, (float) pos.x + radius, (float) pos.y - radius, (float) pos.z - radius, (float) pos.y + radius, (float) pos.z + radius);
        drawYQuad(buffer, matrix, argb, (float) pos.y - radius, (float) pos.x - radius, (float) pos.z - radius, (float) pos.x + radius, (float) pos.z + radius);
        drawYQuad(buffer, matrix, argb, (float) pos.y + radius, (float) pos.x - radius, (float) pos.z - radius, (float) pos.x + radius, (float) pos.z + radius);
        drawZQuad(buffer, matrix, argb, (float) pos.z - radius, (float) pos.x - radius, (float) pos.y - radius, (float) pos.x + radius, (float) pos.y + radius);
        drawZQuad(buffer, matrix, argb, (float) pos.z + radius, (float) pos.x - radius, (float) pos.y - radius, (float) pos.x + radius, (float) pos.y + radius);
    }

    public static void drawFakeLine(BufferBuilder buffer, Matrix4f matrix, int argb, Vector3f start, Vector3f end, float thickness) {
        buffer.vertex(matrix, start.x - thickness, start.y - thickness, start.z - thickness).color(argb);
        buffer.vertex(matrix, end.x - thickness, end.y - thickness, end.z - thickness).color(argb);
        buffer.vertex(matrix, end.x - thickness, end.y + thickness, end.z - thickness).color(argb);
        buffer.vertex(matrix, start.x - thickness, start.y + thickness, start.z - thickness).color(argb);
        buffer.vertex(matrix, start.x + thickness, start.y - thickness, start.z + thickness).color(argb);
        buffer.vertex(matrix, end.x + thickness, end.y - thickness, end.z + thickness).color(argb);
        buffer.vertex(matrix, end.x + thickness, end.y + thickness, end.z + thickness).color(argb);
        buffer.vertex(matrix, start.x + thickness, start.y + thickness, start.z + thickness).color(argb);
        buffer.vertex(matrix, start.x - thickness, start.y + thickness, start.z - thickness).color(argb);
        buffer.vertex(matrix, end.x - thickness, end.y + thickness, end.z - thickness).color(argb);
        buffer.vertex(matrix, end.x + thickness, end.y + thickness, end.z + thickness).color(argb);
        buffer.vertex(matrix, start.x + thickness, start.y + thickness, start.z + thickness).color(argb);
        buffer.vertex(matrix, start.x - thickness, start.y - thickness, start.z - thickness).color(argb);
        buffer.vertex(matrix, end.x - thickness, end.y - thickness, end.z - thickness).color(argb);
        buffer.vertex(matrix, end.x + thickness, end.y - thickness, end.z + thickness).color(argb);
        buffer.vertex(matrix, start.x + thickness, start.y - thickness, start.z + thickness).color(argb);
        buffer.vertex(matrix, start.x - thickness, start.y - thickness, start.z - thickness).color(argb);
        buffer.vertex(matrix, start.x - thickness, start.y + thickness, start.z - thickness).color(argb);
        buffer.vertex(matrix, start.x + thickness, start.y + thickness, start.z + thickness).color(argb);
        buffer.vertex(matrix, start.x + thickness, start.y - thickness, start.z + thickness).color(argb);
        buffer.vertex(matrix, end.x - thickness, end.y - thickness, end.z - thickness).color(argb);
        buffer.vertex(matrix, end.x - thickness, end.y + thickness, end.z - thickness).color(argb);
        buffer.vertex(matrix, end.x + thickness, end.y + thickness, end.z + thickness).color(argb);
        buffer.vertex(matrix, end.x + thickness, end.y - thickness, end.z + thickness).color(argb);
    }

    public static void drawBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos, Direction direction) {
        switch (direction) {
            case NORTH -> drawNorthBlockFace(buffer, matrix, argb, pos);
            case SOUTH -> drawSouthBlockFace(buffer, matrix, argb, pos);
            case EAST -> drawEastBlockFace(buffer, matrix, argb, pos);
            case WEST -> drawWestBlockFace(buffer, matrix, argb, pos);
            case UP -> drawUpBlockFace(buffer, matrix, argb, pos);
            case DOWN -> drawDownBlockFace(buffer, matrix, argb, pos);
        }
    }

    public static void drawZQuad(BufferBuilder buffer, Matrix4f matrix, int argb, float z, float startX, float startY, float endX, float endY) {
        buffer.vertex(matrix, startX, startY, z).color(argb);
        buffer.vertex(matrix, endX, startY, z).color(argb);
        buffer.vertex(matrix, endX, endY, z).color(argb);
        buffer.vertex(matrix, startX, endY, z).color(argb);
    }

    public static void drawNorthBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        Renderer.drawZQuad(buffer, matrix, argb, pos.getZ(), pos.getX(), pos.getY(), pos.getX() + 1f, pos.getY() + 1f);
    }

    public static void drawSouthBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        Renderer.drawZQuad(buffer, matrix, argb, pos.getZ() + 1f, pos.getX(), pos.getY(), pos.getX() + 1f, pos.getY() + 1f);
    }

    public static void drawXQuad(BufferBuilder buffer, Matrix4f matrix, int argb, float x, float startY, float startZ, float endY, float endZ) {
        buffer.vertex(matrix, x, startY, startZ).color(argb);
        buffer.vertex(matrix, x, endY, startZ).color(argb);
        buffer.vertex(matrix, x, endY, endZ).color(argb);
        buffer.vertex(matrix, x, startY, endZ).color(argb);
    }

    public static void drawEastBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        Renderer.drawXQuad(buffer, matrix, argb, pos.getX() + 1f, pos.getY(), pos.getZ(), pos.getY() + 1f, pos.getZ() + 1f);
    }

    public static void drawWestBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        Renderer.drawXQuad(buffer, matrix, argb, pos.getX(), pos.getY(), pos.getZ(), pos.getY() + 1f, pos.getZ() + 1f);
    }

    public static void drawYQuad(BufferBuilder buffer, Matrix4f matrix, int argb, float y, float startX, float startZ, float endX, float endZ) {
        buffer.vertex(matrix, startX, y, startZ).color(argb);
        buffer.vertex(matrix, endX, y, startZ).color(argb);
        buffer.vertex(matrix, endX, y, endZ).color(argb);
        buffer.vertex(matrix, startX, y, endZ).color(argb);
    }

    public static void drawUpBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        Renderer.drawYQuad(buffer, matrix, argb, pos.getY() + 1, pos.getX(), pos.getZ(), pos.getX() + 1f, pos.getZ() + 1f);
    }

    public static void drawDownBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        Renderer.drawYQuad(buffer, matrix, argb, pos.getY(), pos.getX(), pos.getZ(), pos.getX() + 1f, pos.getZ() + 1f);
    }
}
