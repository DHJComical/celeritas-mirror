package org.embeddedt.embeddium.impl.render.clouds;

import org.embeddedt.embeddium.impl.gl.array.GlVertexArray;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferUsage;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.shader.GlProgram;
import org.embeddedt.embeddium.impl.gl.shader.GlShader;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.ShaderConstants;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat4v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.embeddedt.embeddium.impl.gl.state.GlStateService;
import org.embeddedt.embeddium.impl.gl.texture.GlBufferTexture;
import org.embeddedt.embeddium.impl.render.chunk.SharedQuadIndexBuffer;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.shader.ShaderLoader;
import org.taumc.celeritas.lwjgl.GL11;
import org.taumc.celeritas.lwjgl.GL12;
import org.taumc.celeritas.lwjgl.GL13;
import org.taumc.celeritas.lwjgl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * Draws clouds without vertex data, like vanilla since 1.21.6. The visible faces are written to a buffer texture, and
 * the vertex shader builds each vertex from {@code gl_VertexID}. The face format matches vanilla's, so shader packs can
 * generate the same vertices. Unlike vanilla, each cell is tinted by its pixel in the clouds texture.
 */
public final class CloudRenderer {
    public static final int FACES_TEXTURE_UNIT = 3;
    public static final int COLORS_TEXTURE_UNIT = 4;

    private static final int DIRECTION_DOWN = 0;
    private static final int DIRECTION_UP = 1;
    private static final int DIRECTION_NORTH = 2;
    private static final int DIRECTION_SOUTH = 3;
    private static final int DIRECTION_WEST = 4;
    private static final int DIRECTION_EAST = 5;

    private static final int FLAG_INSIDE_FACE = 1 << 4;
    private static final int FLAG_USE_TOP_COLOR = 1 << 5;

    // A 256 px clouds texture makes 12 block cells
    private static final float TEXTURE_SIZE_IN_BLOCKS = 3072.0f;
    private static final float CLOUD_THICKNESS = 4.0f;
    private static final float CLOUD_ALPHA = 0.8f;
    private static final double BLOCKS_PER_TICK = 0.03;
    // Inside faces are kept this far outside the clouds, so the near plane clipping a face doesn't show a hole
    private static final float INSIDE_FACE_MARGIN = 0.1f;

    private static final int BYTES_PER_FACE = 3 * Short.BYTES;

    private enum CameraPosition {
        ABOVE_CLOUDS,
        INSIDE_CLOUDS,
        BELOW_CLOUDS
    }

    private final GlStateService glState;
    private final CloudTexture texture;
    private final float cellSizeX, cellSizeZ;

    private final GlProgram<ShaderInterface> program;
    private final GlMutableBuffer faceBuffer;
    private final GlBufferTexture faceTexture;
    private final SharedQuadIndexBuffer indexBuffer;
    private final GlVertexArray vertexArray;
    private final int colorTexture;

    private ByteBuffer faceData = ByteBuffer.allocateDirect(1024 * BYTES_PER_FACE).order(ByteOrder.nativeOrder());
    private int faceCount;

    private boolean needsRebuild = true;
    private int prevCellX, prevCellZ, prevRadius;
    private CameraPosition prevCameraPosition;
    private boolean prevInsideFaces;
    private boolean prevFancy;

    private int centerCellX, centerCellZ;
    private float offsetX, offsetY, offsetZ;

    /**
     * {@return whether the context can run this renderer} It needs GLSL 1.50 for buffer textures, {@code texelFetch} and
     * {@code gl_VertexID}, which can't be downgraded to the GLSL 1.20 used on old GL. Platforms should fall back to
     * vanilla clouds otherwise.
     */
    public static boolean isSupported() {
        return !ShaderLoader.useLegacyGlsl();
    }

    public CloudRenderer(CommandList commandList, GlStateService glState, CloudTexture texture) {
        this.glState = glState;
        this.texture = texture;
        this.cellSizeX = TEXTURE_SIZE_IN_BLOCKS / texture.width;
        this.cellSizeZ = TEXTURE_SIZE_IN_BLOCKS / texture.height;

        this.program = createProgram();
        this.faceBuffer = commandList.createMutableBuffer();
        this.faceTexture = new GlBufferTexture();
        this.indexBuffer = new SharedQuadIndexBuffer(commandList, QuadPrimitiveType.TRIANGULATED);
        this.vertexArray = new GlVertexArray();
        this.colorTexture = this.createColorTexture();
    }

    /**
     * Updates the faces for the frame.
     *
     * @return whether there is anything to draw
     */
    public boolean prepare(CommandList commandList, CloudFrame frame) {
        double textureWidthBlocks = this.texture.width * this.cellSizeX;
        double textureHeightBlocks = this.texture.height * this.cellSizeZ;

        double cloudX = frame.cameraX() + frame.ticks() * BLOCKS_PER_TICK;
        double cloudZ = frame.cameraZ() + 3.96;
        cloudX -= Math.floor(cloudX / textureWidthBlocks) * textureWidthBlocks;
        cloudZ -= Math.floor(cloudZ / textureHeightBlocks) * textureHeightBlocks;

        int cellX = (int) Math.floor(cloudX / this.cellSizeX);
        int cellZ = (int) Math.floor(cloudZ / this.cellSizeZ);

        float relativeBottomY = frame.cloudHeight() - (float) frame.cameraY() + 0.33f;
        float relativeTopY = relativeBottomY + CLOUD_THICKNESS;

        CameraPosition cameraPosition;
        float verticalDistance;

        if (relativeTopY < 0.0f) {
            cameraPosition = CameraPosition.ABOVE_CLOUDS;
            verticalDistance = -relativeTopY;
        } else if (relativeBottomY > 0.0f) {
            cameraPosition = CameraPosition.BELOW_CLOUDS;
            verticalDistance = relativeBottomY;
        } else {
            cameraPosition = CameraPosition.INSIDE_CLOUDS;
            verticalDistance = 0.0f;
        }

        // Every cloud has faded out, such as underwater far below them
        if (verticalDistance >= frame.fogEnd()) {
            return false;
        }

        boolean insideFaces = verticalDistance <= INSIDE_FACE_MARGIN;

        float cellSize = Math.min(this.cellSizeX, this.cellSizeZ);
        int maxRadius = (int) Math.ceil(frame.range() / cellSize);
        // Cells past the fade end are invisible. Rounding up keeps a fade end that changes gradually, like water fog
        // while the player's eyes adjust, from rebuilding the faces every frame.
        int fadeRadius = ((int) Math.ceil(frame.fogEnd() / cellSize) + 1 + 15) & ~15;
        int radius = Math.min(maxRadius, fadeRadius);

        if (this.needsRebuild || cellX != this.prevCellX || cellZ != this.prevCellZ || radius != this.prevRadius
                || cameraPosition != this.prevCameraPosition || insideFaces != this.prevInsideFaces
                || frame.fancy() != this.prevFancy) {
            this.needsRebuild = false;
            this.prevCellX = cellX;
            this.prevCellZ = cellZ;
            this.prevRadius = radius;
            this.prevCameraPosition = cameraPosition;
            this.prevInsideFaces = insideFaces;
            this.prevFancy = frame.fancy();

            this.buildFaces(cameraPosition, insideFaces, cellX, cellZ, frame.fancy(), radius);

            this.faceData.flip();
            commandList.uploadData(this.faceBuffer, this.faceData, GlBufferUsage.DYNAMIC_DRAW);
            this.indexBuffer.ensureCapacity(commandList, this.faceCount * 6);
        }

        this.centerCellX = cellX;
        this.centerCellZ = cellZ;
        this.offsetX = -(float) (cloudX - cellX * this.cellSizeX);
        this.offsetY = relativeBottomY;
        this.offsetZ = -(float) (cloudZ - cellZ * this.cellSizeZ);

        return this.faceCount != 0;
    }

    /**
     * Draws the faces from {@link #prepare} with our own program.
     */
    public void render(CommandList commandList, CloudRenderState state, CloudFrame frame) {
        state.setup(frame.fancy());

        this.program.bind();
        this.program.getInterface().setup(this, frame);
        this.bindTextures();
        this.draw(commandList);
        this.program.unbind();

        state.restore();
    }

    /**
     * Binds the faces to {@link #FACES_TEXTURE_UNIT} and the cell colors to {@link #COLORS_TEXTURE_UNIT}.
     */
    public void bindTextures() {
        this.glState.bindTexture(COLORS_TEXTURE_UNIT, GL11.GL_TEXTURE_2D, this.colorTexture);

        int previous = this.glState.activateTextureUnit(GL13.GL_TEXTURE0 + FACES_TEXTURE_UNIT);
        this.faceTexture.bind(RenderDevice.INSTANCE.getDeviceFunctions().textureBufferFunctions(), this.faceBuffer, GL30.GL_R16I);
        this.glState.activateTextureUnit(previous);
    }

    /**
     * Draws the faces with the bound program.
     */
    public void draw(CommandList commandList) {
        commandList.bindVertexArray(this.vertexArray);
        commandList.bindBuffer(GlBufferTarget.ELEMENT_BUFFER, this.indexBuffer.getBufferObject());
        LWJGL.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, this.faceCount * 6, GL11.GL_UNSIGNED_INT, 0L, 0);
        commandList.unbindVertexArray();
    }

    public int getCenterCellX() {
        return this.centerCellX;
    }

    public int getCenterCellZ() {
        return this.centerCellZ;
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    public float getOffsetY() {
        return this.offsetY;
    }

    public float getOffsetZ() {
        return this.offsetZ;
    }

    public float getCellSizeX() {
        return this.cellSizeX;
    }

    public float getCellSizeZ() {
        return this.cellSizeZ;
    }

    public float getCellHeight() {
        return CLOUD_THICKNESS;
    }

    public static float getAlpha() {
        return CLOUD_ALPHA;
    }

    public void delete(CommandList commandList) {
        this.program.delete();
        commandList.deleteBuffer(this.faceBuffer);
        this.faceTexture.delete();
        this.indexBuffer.delete(commandList);
        commandList.deleteVertexArray(this.vertexArray);
        LWJGL.glDeleteTextures(this.colorTexture);
    }

    private void buildFaces(CameraPosition cameraPosition, boolean insideFaces, int centerCellX, int centerCellZ, boolean fancy, int radius) {
        this.faceData.clear();
        this.faceCount = 0;

        // Rings of cells around the camera, so the faces are drawn front to back
        for (int ring = 0; ring <= 2 * radius; ring++) {
            for (int x = -ring; x <= ring; x++) {
                int z = ring - Math.abs(x);

                if (z > radius || x * x + z * z > radius * radius) {
                    continue;
                }

                if (z != 0) {
                    this.buildCell(cameraPosition, insideFaces, centerCellX, centerCellZ, fancy, x, -z);
                }

                this.buildCell(cameraPosition, insideFaces, centerCellX, centerCellZ, fancy, x, z);
            }
        }
    }

    private void buildCell(CameraPosition cameraPosition, boolean insideFaces, int centerCellX, int centerCellZ, boolean fancy, int x, int z) {
        int cell = this.texture.cell(centerCellX + x, centerCellZ + z);

        if (cell == 0) {
            return;
        }

        if (!fancy) {
            this.addFace(x, z, DIRECTION_DOWN, FLAG_USE_TOP_COLOR);
            return;
        }

        if (cameraPosition != CameraPosition.BELOW_CLOUDS) {
            this.addFace(x, z, DIRECTION_UP, 0);
        }

        if (cameraPosition != CameraPosition.ABOVE_CLOUDS) {
            this.addFace(x, z, DIRECTION_DOWN, 0);
        }

        // Only the sides that face the camera
        if ((cell & CloudTexture.OPEN_NORTH) != 0 && z > 0) {
            this.addFace(x, z, DIRECTION_NORTH, 0);
        }

        if ((cell & CloudTexture.OPEN_SOUTH) != 0 && z < 0) {
            this.addFace(x, z, DIRECTION_SOUTH, 0);
        }

        if ((cell & CloudTexture.OPEN_WEST) != 0 && x > 0) {
            this.addFace(x, z, DIRECTION_WEST, 0);
        }

        if ((cell & CloudTexture.OPEN_EAST) != 0 && x < 0) {
            this.addFace(x, z, DIRECTION_EAST, 0);
        }

        // Seen from inside the clouds, or just outside them where the near plane can clip the outer faces
        if (insideFaces && Math.abs(x) <= 1 && Math.abs(z) <= 1) {
            for (int direction = DIRECTION_DOWN; direction <= DIRECTION_EAST; direction++) {
                this.addFace(x, z, direction, FLAG_INSIDE_FACE);
            }
        }
    }

    private void addFace(int x, int z, int direction, int flags) {
        if (this.faceData.remaining() < BYTES_PER_FACE) {
            ByteBuffer grown = ByteBuffer.allocateDirect(this.faceData.capacity() * 2).order(ByteOrder.nativeOrder());
            this.faceData.flip();
            grown.put(this.faceData);
            this.faceData = grown;
        }

        // The low bits of the cell position are stored with the flags, as vanilla does
        int directionAndFlags = direction | flags | ((x & 1) << 7) | ((z & 1) << 6);

        this.faceData.putShort((short) (x >> 1));
        this.faceData.putShort((short) (z >> 1));
        this.faceData.putShort((short) directionAndFlags);
        this.faceCount++;
    }

    private int createColorTexture() {
        int texture = LWJGL.glGenTextures();

        ByteBuffer pixels = ByteBuffer.allocateDirect(this.texture.pixels.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        // ABGR as little endian ints is RGBA in memory
        pixels.asIntBuffer().put(this.texture.pixels);

        this.glState.bindTexture(COLORS_TEXTURE_UNIT, GL11.GL_TEXTURE_2D, texture);
        int previous = this.glState.activateTextureUnit(GL13.GL_TEXTURE0 + COLORS_TEXTURE_UNIT);

        LWJGL.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        LWJGL.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        LWJGL.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        LWJGL.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        LWJGL.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, this.texture.width, this.texture.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        LWJGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);

        this.glState.activateTextureUnit(previous);

        return texture;
    }

    private static GlProgram<ShaderInterface> createProgram() {
        ShaderConstants constants = ShaderConstants.builder().build();
        GlShader vertexShader = ShaderLoader.loadShader(ShaderType.VERTEX, "sodium:clouds/clouds.vsh", constants);
        GlShader fragmentShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, "sodium:clouds/clouds.fsh", constants);

        try {
            return GlProgram.builder("celeritas:clouds")
                    .attachShader(vertexShader)
                    .attachShader(fragmentShader)
                    .bindFragmentData("fragColor", 0)
                    .link(ShaderInterface::new);
        } finally {
            vertexShader.delete();
            fragmentShader.delete();
        }
    }

    private static final class ShaderInterface {
        private final GlUniformMatrix4f modelViewMatrix, projectionMatrix;
        private final GlUniformFloat4v colorModulator;
        private final GlUniformFloat3v cloudOffset, cellSize;
        private final GlUniformInt centerCellX, centerCellZ;
        private final GlUniformFloat fogEnd;
        private final GlUniformInt facesSampler, colorsSampler;

        private ShaderInterface(ShaderBindingContext context) {
            this.modelViewMatrix = context.bindUniform("ModelViewMat", GlUniformMatrix4f::new);
            this.projectionMatrix = context.bindUniform("ProjMat", GlUniformMatrix4f::new);
            this.colorModulator = context.bindUniform("ColorModulator", GlUniformFloat4v::new);
            this.cloudOffset = context.bindUniform("CloudOffset", GlUniformFloat3v::new);
            this.cellSize = context.bindUniform("CellSize", GlUniformFloat3v::new);
            this.centerCellX = context.bindUniform("CenterCellX", GlUniformInt::new);
            this.centerCellZ = context.bindUniform("CenterCellZ", GlUniformInt::new);
            this.fogEnd = context.bindUniform("FogEnd", GlUniformFloat::new);
            this.facesSampler = context.bindUniform("CloudFaces", GlUniformInt::new);
            this.colorsSampler = context.bindUniform("CloudColors", GlUniformInt::new);
        }

        private void setup(CloudRenderer renderer, CloudFrame frame) {
            this.modelViewMatrix.set(frame.modelView());
            this.projectionMatrix.set(frame.projection());
            this.colorModulator.set(new float[] { frame.red(), frame.green(), frame.blue(), CLOUD_ALPHA });
            this.cloudOffset.set(renderer.offsetX, renderer.offsetY, renderer.offsetZ);
            this.cellSize.set(renderer.cellSizeX, CLOUD_THICKNESS, renderer.cellSizeZ);
            this.centerCellX.setInt(renderer.centerCellX);
            this.centerCellZ.setInt(renderer.centerCellZ);
            this.fogEnd.setFloat(frame.fogEnd());
            this.facesSampler.setInt(FACES_TEXTURE_UNIT);
            this.colorsSampler.setInt(COLORS_TEXTURE_UNIT);
        }
    }
}
