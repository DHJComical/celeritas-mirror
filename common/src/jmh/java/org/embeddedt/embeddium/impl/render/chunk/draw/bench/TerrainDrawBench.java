package org.embeddedt.embeddium.impl.render.chunk.draw.bench;

import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.DefaultChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.bench.voxel.BenchRenderPasses;
import org.embeddedt.embeddium.impl.render.chunk.fog.FogService;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.bench.MultiDrawBench;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.bench.Cameras;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.bench.HeadlessGl;
import org.embeddedt.embeddium.impl.render.chunk.occlusion.bench.WorldType;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkFogMode;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderComponent;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

/**
 * GPU cost of drawing terrain: whole frames of the real terrain passes into an offscreen framebuffer, timed to
 * completion with {@code glFinish}. The fetch path and vertex format are fixed per JVM, so compare runs with
 * {@code -Dceleritas.preferVertexPulling=true} and {@code -Dceleritas.bench.vertexType=VANILLA_LIKE} appended.
 * <p>
 * Setup prints a checksum of the first frame, which should not depend on the fetch path.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class TerrainDrawBench {
    private static final FogService NO_FOG = new FogService() {
        @Override
        public float getFogEnd() {
            return 0;
        }

        @Override
        public float getFogStart() {
            return 0;
        }

        @Override
        public float getFogDensity() {
            return 0;
        }

        @Override
        public int getFogShapeIndex() {
            return FOG_SHAPE_SPHERICAL;
        }

        @Override
        public float getFogCutoff() {
            return 0;
        }

        @Override
        public float[] getFogColor() {
            return new float[4];
        }

        @Override
        public ChunkShaderComponent.Factory<?> getFogMode() {
            return ChunkFogMode.NONE;
        }
    };

    @State(Scope.Benchmark)
    public static class DrawState {
        @Param({ "32" })
        public int renderDistance;

        /** Framebuffer width; the height follows the bench cameras' 16:9 aspect. Low widths are vertex-bound. */
        @Param({ "640", "2560" })
        public int width;

        private int height;

        private MultiDrawBench.FrameState frames;
        private ChunkRenderMatrices[] matrices;
        private DefaultChunkRenderer renderer;

        private int framebuffer;
        private int colorTexture;
        private int depthRenderbuffer;
        private int blockTexture;
        private int lightTexture;

        private int frameIndex;

        private int timerQuery;
        private long gpuNanos;
        private long submitNanos;
        private int timedFrames;

        @Setup
        public void setup() {
            this.frames = new MultiDrawBench.FrameState();
            this.frames.world = WorldType.SURFACE;
            this.frames.renderDistance = this.renderDistance;
            this.frames.caveWidth = 0.03;
            this.frames.useBlockFaceCulling = true;
            this.frames.setup();

            this.height = this.width * 9 / 16;
            this.matrices = createMatrices(this.renderDistance);

            this.renderer = new DefaultChunkRenderer(RenderDevice.INSTANCE, BenchRenderPasses.configuration(), NO_FOG) {
                @Override
                protected void configureShaderInterface(ChunkShaderInterface shader) {
                    shader.setTextureSlot(ChunkShaderTextureSlot.BLOCK, 0);
                    shader.setTextureSlot(ChunkShaderTextureSlot.LIGHT, 2);
                }
            };

            this.createFramebuffer();
            this.blockTexture = createBlockTexture();
            this.lightTexture = createLightTexture();

            this.timerQuery = GL15C.glGenQueries();

            this.drawFrame(0);

            System.out.printf("%n[TerrainDrawBench] %s | pass fetch=%s format=%s | %dx%d | first frame crc=%08x covered=%d%n",
                    HeadlessGl.describe(), BenchRenderPasses.SOLID.vertexFetchMode(),
                    BenchRenderPasses.SOLID.vertexType().getClass().getSimpleName(),
                    this.width, this.height, this.checksum(), this.coveredPixels());
        }

        private static ChunkRenderMatrices[] createMatrices(int renderDistance) {
            var matrices = new ChunkRenderMatrices[MultiDrawBench.ROTATION_STEPS];
            var projection = new Matrix4f().perspective((float) Math.toRadians(Cameras.FOV_DEGREES), Cameras.ASPECT,
                    Cameras.NEAR_PLANE, Cameras.farPlane(renderDistance));

            // Must match Cameras.yawRing, which built the frames' visibility lists
            for (int i = 0; i < matrices.length; i++) {
                float yaw = (360.0f / matrices.length) * i;
                var modelView = new Matrix4f()
                        .rotateX((float) Math.toRadians(Cameras.DEFAULT_PITCH))
                        .rotateY((float) Math.toRadians(yaw + 180.0f));

                matrices[i] = new ChunkRenderMatrices(projection, modelView);
            }

            return matrices;
        }

        private void createFramebuffer() {
            this.colorTexture = GL11C.glGenTextures();
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, this.colorTexture);
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, this.width, this.height, 0,
                    GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, (ByteBuffer) null);

            this.depthRenderbuffer = GL30C.glGenRenderbuffers();
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, this.depthRenderbuffer);
            GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL14C.GL_DEPTH_COMPONENT24, this.width, this.height);

            this.framebuffer = GL30C.glGenFramebuffers();
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebuffer);
            GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, this.colorTexture, 0);
            GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_RENDERBUFFER, this.depthRenderbuffer);

            int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);

            if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Framebuffer incomplete: 0x" + Integer.toHexString(status));
            }
        }

        /** A 16x16 noise sprite with mipmaps, standing in for one atlas sprite per block face. */
        private static int createBlockTexture() {
            var random = new Random(42);
            ByteBuffer pixels = MemoryUtil.memAlloc(16 * 16 * 4);

            for (int i = 0; i < 16 * 16; i++) {
                pixels.put((byte) (64 + random.nextInt(192)))
                        .put((byte) (64 + random.nextInt(192)))
                        .put((byte) (64 + random.nextInt(192)))
                        .put((byte) 255);
            }

            pixels.flip();

            int texture = GL11C.glGenTextures();
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, 16, 16, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, pixels);
            GL30C.glGenerateMipmap(GL11C.GL_TEXTURE_2D);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST_MIPMAP_LINEAR);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);

            MemoryUtil.memFree(pixels);

            return texture;
        }

        private static int createLightTexture() {
            ByteBuffer pixels = MemoryUtil.memAlloc(16 * 16 * 4);

            for (int sky = 0; sky < 16; sky++) {
                for (int block = 0; block < 16; block++) {
                    int level = Math.max(sky, block) * 17;
                    pixels.put((byte) level).put((byte) level).put((byte) level).put((byte) 255);
                }
            }

            pixels.flip();

            int texture = GL11C.glGenTextures();
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, 16, 16, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, pixels);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);

            MemoryUtil.memFree(pixels);

            return texture;
        }

        void drawFrame(int rotation) {
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebuffer);
            GL11C.glViewport(0, 0, this.width, this.height);
            GL11C.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT | GL11C.GL_DEPTH_BUFFER_BIT);

            GL11C.glEnable(GL11C.GL_DEPTH_TEST);
            GL11C.glDepthFunc(GL11C.GL_LEQUAL);
            GL11C.glEnable(GL11C.GL_CULL_FACE);

            GL13C.glActiveTexture(GL13C.GL_TEXTURE2);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, this.lightTexture);
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, this.blockTexture);

            var lists = this.frames.frames[rotation];
            var camera = this.frames.camera;

            long submitStart = System.nanoTime();
            GL15C.glBeginQuery(GL33C.GL_TIME_ELAPSED, this.timerQuery);

            try (var commandList = RenderDevice.INSTANCE.createCommandList()) {
                for (TerrainRenderPass pass : new TerrainRenderPass[] { BenchRenderPasses.SOLID, BenchRenderPasses.CUTOUT_MIPPED }) {
                    this.renderer.render(this.matrices[rotation], commandList, lists, pass, camera, camera);
                }
            }

            GL15C.glEndQuery(GL33C.GL_TIME_ELAPSED);
            this.submitNanos += System.nanoTime() - submitStart;

            GL11C.glFinish();

            this.gpuNanos += GL33C.glGetQueryObjecti64(this.timerQuery, GL15C.GL_QUERY_RESULT);
            this.timedFrames++;
        }

        @Setup(Level.Iteration)
        public void resetTimers() {
            this.gpuNanos = 0;
            this.submitNanos = 0;
            this.timedFrames = 0;
        }

        /** Splits each iteration's frame time into GPU execution and CPU submission. */
        @TearDown(Level.Iteration)
        public void reportTimers() {
            if (this.timedFrames > 0) {
                System.out.printf("%n[TerrainDrawBench] gpu=%.1f us submit=%.1f us over %d frames%n",
                        this.gpuNanos / 1000.0 / this.timedFrames, this.submitNanos / 1000.0 / this.timedFrames, this.timedFrames);
            }
        }

        private long checksum() {
            ByteBuffer pixels = this.readPixels();
            var crc = new CRC32();
            crc.update(pixels);
            MemoryUtil.memFree(pixels);

            return crc.getValue();
        }

        private int coveredPixels() {
            ByteBuffer pixels = this.readPixels();
            int covered = 0;

            for (int i = 3; i < pixels.capacity(); i += 4) {
                if (pixels.get(i) != 0) {
                    covered++;
                }
            }

            MemoryUtil.memFree(pixels);

            return covered;
        }

        private ByteBuffer readPixels() {
            ByteBuffer pixels = MemoryUtil.memAlloc(this.width * this.height * 4);
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebuffer);
            GL11C.glReadPixels(0, 0, this.width, this.height, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, pixels);

            return pixels;
        }

        int nextRotation() {
            int rotation = this.frameIndex;
            this.frameIndex = (rotation + 1) % this.matrices.length;
            return rotation;
        }

        @TearDown
        public void tearDown() {
            try (var commandList = RenderDevice.INSTANCE.createCommandList()) {
                this.renderer.delete(commandList);
            }

            GL15C.glDeleteQueries(this.timerQuery);
            GL30C.glDeleteFramebuffers(this.framebuffer);
            GL30C.glDeleteRenderbuffers(this.depthRenderbuffer);
            GL11C.glDeleteTextures(new int[] { this.colorTexture, this.blockTexture, this.lightTexture });

            this.frames.tearDown();
        }
    }

    /** Solid and cutout passes for one frame, rotating through the camera ring. */
    @Benchmark
    public void drawOpaque(DrawState state) {
        state.drawFrame(state.nextRotation());
    }
}
