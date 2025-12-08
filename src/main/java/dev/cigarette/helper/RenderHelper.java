package dev.cigarette.helper;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RenderHelper {
    public static final RenderPipeline BLOCK_ESP_PHASE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET).withLocation("pipeline/cigarette.blockesp").withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withCull(false).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
    public static final RenderPipeline BLOCK_ESP_NOPHASE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET).withLocation("pipeline/cigarette.blockespnophase").withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withCull(false).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).build());

    public static final RenderPipeline TRI_ESP_PHASE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET).withLocation("pipeline/cigarette.triesp").withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES).withCull(false).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
    public static final RenderPipeline TRI_ESP_NOPHASE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET).withLocation("pipeline/cigarette.triespnophase").withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES).withCull(false).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).build());

    private static final int ICOSPHERE_REFINEMENT_STEPS = 4;
    private static final List<Vec3d[]> SPHERE_TRIANGLES;

    static {
        // sphere precalculation

        // step 1: creating the icosahedron

        // golden ratio
        double t = (1.0 + Math.sqrt(5.0)) / 2.0;

        Vec3d[] vertices = {
                new Vec3d(-1, t, 0).normalize(),
                new Vec3d(1, t, 0).normalize(),
                new Vec3d(-1, -t, 0).normalize(),
                new Vec3d(1, -t, 0).normalize(),
                
                new Vec3d(0, -1, t).normalize(),
                new Vec3d(0, 1, t).normalize(),
                new Vec3d(0, -1, -t).normalize(),
                new Vec3d(0, 1, -t).normalize(),
                
                new Vec3d(t, 0, -1).normalize(),
                new Vec3d(t, 0, 1).normalize(),
                new Vec3d(-t, 0, -1).normalize(),
                new Vec3d(-t, 0, 1).normalize(),
        };
        
        List<Vec3d[]> triangles = new ArrayList<>();

        triangles.add(new Vec3d[]{ vertices[0], vertices[11], vertices[5] });
        triangles.add(new Vec3d[]{ vertices[0], vertices[5], vertices[1] });
        triangles.add(new Vec3d[]{ vertices[0], vertices[1], vertices[7] });
        triangles.add(new Vec3d[]{ vertices[0], vertices[7], vertices[10] });
        triangles.add(new Vec3d[]{ vertices[0], vertices[10], vertices[11] });

        triangles.add(new Vec3d[]{ vertices[1], vertices[5], vertices[9] });
        triangles.add(new Vec3d[]{ vertices[5], vertices[11], vertices[4] });
        triangles.add(new Vec3d[]{ vertices[11], vertices[10], vertices[2] });
        triangles.add(new Vec3d[]{ vertices[10], vertices[7], vertices[6] });
        triangles.add(new Vec3d[]{ vertices[7], vertices[1], vertices[8] });

        triangles.add(new Vec3d[]{ vertices[3], vertices[9], vertices[4] });
        triangles.add(new Vec3d[]{ vertices[3], vertices[4], vertices[2] });
        triangles.add(new Vec3d[]{ vertices[3], vertices[2], vertices[6] });
        triangles.add(new Vec3d[]{ vertices[3], vertices[6], vertices[8] });
        triangles.add(new Vec3d[]{ vertices[3], vertices[8], vertices[9] });

        triangles.add(new Vec3d[]{ vertices[4], vertices[9], vertices[5] });
        triangles.add(new Vec3d[]{ vertices[2], vertices[4], vertices[11] });
        triangles.add(new Vec3d[]{ vertices[6], vertices[2], vertices[10] });
        triangles.add(new Vec3d[]{ vertices[8], vertices[6], vertices[7] });
        triangles.add(new Vec3d[]{ vertices[9], vertices[8], vertices[1] });

        // step 2: refine to icosphere

        for (int i = 0; i < ICOSPHERE_REFINEMENT_STEPS; i++) {
            List<Vec3d[]> newTriangles = new ArrayList<>();
            for (Vec3d[] triangle : triangles) {
                Vec3d[] mid = { triangle[0].add(triangle[1]).multiply(0.5).normalize(), triangle[1].add(triangle[2]).multiply(0.5).normalize(), triangle[2].add(triangle[0]).multiply(0.5).normalize() };
                Vec3d[] t1 = { triangle[0], mid[0], mid[2] };
                Vec3d[] t2 = { triangle[1], mid[1], mid[0] };
                Vec3d[] t3 = { triangle[2], mid[2], mid[1] };
                Collections.addAll(newTriangles, mid, t1, t2, t3);
            }
            triangles = newTriangles;
        }

        SPHERE_TRIANGLES = triangles;
    }

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

    public static void drawSphere(BufferBuilder buffer, Matrix4f matrix, int argb, List<Vec3d[]> translatedTriangles) {
        for (Vec3d[] triangle : translatedTriangles) {
            for (Vec3d point : triangle) {
                buffer.vertex(matrix, (float) point.getX(), (float) point.getY(), (float) point.getZ()).color(argb);
            }
        }
    }

    public static List<Vec3d[]> calculateSphere(Vec3d pos, float width, Vec3d camera) {
        float radius = width / 2;
        List<TriangleDistance> translatedTriangles = new ArrayList<>();

        for (Vec3d[] triangle : SPHERE_TRIANGLES) {
            Vec3d[] translatedTriangle = new Vec3d[3];
            for (int i = 0; i < triangle.length; i++) {
                translatedTriangle[i] = triangle[i].multiply(radius).add(pos);
            }

            Vec3d center = translatedTriangle[0].add(translatedTriangle[1]).add(translatedTriangle[2]).multiply(1.0 / 3.0);
            float distance = (float) center.squaredDistanceTo(camera);

            translatedTriangles.add(new TriangleDistance(translatedTriangle, distance));
        }

        translatedTriangles.sort(Comparator.comparingDouble((TriangleDistance td) -> td.distance).reversed());
        return translatedTriangles.stream().map(td -> td.triangle).collect(Collectors.toList());
    }

    private static class TriangleDistance {
        Vec3d[] triangle;
        double distance;

        TriangleDistance(Vec3d[] triangle, double distance) {
            this.triangle = triangle;
            this.distance = distance;
        }
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
        RenderHelper.drawZQuad(buffer, matrix, argb, pos.getZ(), pos.getX(), pos.getY(), pos.getX() + 1f, pos.getY() + 1f);
    }

    public static void drawSouthBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        RenderHelper.drawZQuad(buffer, matrix, argb, pos.getZ() + 1f, pos.getX(), pos.getY(), pos.getX() + 1f, pos.getY() + 1f);
    }

    public static void drawXQuad(BufferBuilder buffer, Matrix4f matrix, int argb, float x, float startY, float startZ, float endY, float endZ) {
        buffer.vertex(matrix, x, startY, startZ).color(argb);
        buffer.vertex(matrix, x, endY, startZ).color(argb);
        buffer.vertex(matrix, x, endY, endZ).color(argb);
        buffer.vertex(matrix, x, startY, endZ).color(argb);
    }

    public static void drawEastBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        RenderHelper.drawXQuad(buffer, matrix, argb, pos.getX() + 1f, pos.getY(), pos.getZ(), pos.getY() + 1f, pos.getZ() + 1f);
    }

    public static void drawWestBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        RenderHelper.drawXQuad(buffer, matrix, argb, pos.getX(), pos.getY(), pos.getZ(), pos.getY() + 1f, pos.getZ() + 1f);
    }

    public static void drawYQuad(BufferBuilder buffer, Matrix4f matrix, int argb, float y, float startX, float startZ, float endX, float endZ) {
        buffer.vertex(matrix, startX, y, startZ).color(argb);
        buffer.vertex(matrix, endX, y, startZ).color(argb);
        buffer.vertex(matrix, endX, y, endZ).color(argb);
        buffer.vertex(matrix, startX, y, endZ).color(argb);
    }

    public static void drawUpBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        RenderHelper.drawYQuad(buffer, matrix, argb, pos.getY() + 1, pos.getX(), pos.getZ(), pos.getX() + 1f, pos.getZ() + 1f);
    }

    public static void drawDownBlockFace(BufferBuilder buffer, Matrix4f matrix, int argb, BlockPos pos) {
        RenderHelper.drawYQuad(buffer, matrix, argb, pos.getY(), pos.getX(), pos.getZ(), pos.getX() + 1f, pos.getZ() + 1f);
    }
}
