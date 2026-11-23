package org.taumc.celeritas.impl.render.entity;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * Collects the shadows of every entity drawn by the world entity loop and draws them with one call at the end of
 * the loop. The geometry matches vanilla's {@code Render.renderShadow}.
 */
public class EntityShadowBatch {
    private static final ResourceLocation SHADOW_TEXTURES = new ResourceLocation("textures/misc/shadow.png");
    private static final int NO_LIGHT = -1;

    // A POSITION_TEX_COLOR vertex: 3 position floats, 2 texture floats, packed color
    private static final int INTS_PER_VERTEX = 6;

    private final BufferBuilder buffer = new BufferBuilder(0x4000);
    private final int[] quad = new int[4 * INTS_PER_VERTEX];
    private final WorldVertexBufferUploader uploader = new WorldVertexBufferUploader();
    private final Long2IntOpenHashMap lightCache = new Long2IntOpenHashMap();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

    /**
     * Only this entity's shadow is batched. Renderers can draw other entities from inside their own render call
     * under a transformed matrix, and those shadows must stay on the vanilla path.
     */
    private Entity topLevelEntity;
    private boolean building;

    public EntityShadowBatch() {
        this.lightCache.defaultReturnValue(NO_LIGHT);
    }

    public void begin() {
        if (this.building) {
            this.buffer.finishDrawing();
            this.building = false;
        }
        this.lightCache.clear();
        this.topLevelEntity = null;
    }

    public void setTopLevelEntity(Entity entity) {
        this.topLevelEntity = entity;
    }

    public boolean record(World world, Entity entity, double x, double y, double z, float shadowAlpha,
                          float partialTicks, float shadowSize) {
        if (entity != this.topLevelEntity || world == null) {
            return false;
        }

        float size = shadowSize;
        if (entity instanceof EntityLiving living) {
            size *= living.getRenderSizeModifier();
            if (living.isChild()) {
                size *= 0.5F;
            }
        }

        double entityX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
        double entityY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
        double entityZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;
        int minX = MathHelper.floor(entityX - (double)size);
        int maxX = MathHelper.floor(entityX + (double)size);
        int minY = MathHelper.floor(entityY - (double)size);
        int maxY = MathHelper.floor(entityY);
        int minZ = MathHelper.floor(entityZ - (double)size);
        int maxZ = MathHelper.floor(entityZ + (double)size);
        double offsetX = x - entityX;
        double offsetY = y - entityY;
        double offsetZ = z - entityZ;

        if (!this.building) {
            this.buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            this.building = true;
        }

        for (int bz = minZ; bz <= maxZ; bz++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bx = minX; bx <= maxX; bx++) {
                    this.recordBlock(world, bx, by, bz, x, y, z, shadowAlpha, size, offsetX, offsetY, offsetZ);
                }
            }
        }
        return true;
    }

    private void recordBlock(World world, int bx, int by, int bz, double x, double y, double z, float shadowAlpha,
                             float size, double offsetX, double offsetY, double offsetZ) {
        BlockPos pos = this.pos.setPos(bx, by, bz);
        IBlockState state = world.getBlockState(this.below.setPos(bx, by - 1, bz));

        if (state.getRenderType() == EnumBlockRenderType.INVISIBLE || !state.isFullCube()) {
            return;
        }

        int light = this.getLight(world, pos);
        if (light <= 3) {
            return;
        }

        double alpha = ((double)shadowAlpha - (y - ((double)by + offsetY)) / 2.0D) * 0.5D
                * (double)world.provider.getLightBrightnessTable()[light];
        if (alpha < 0.0D) {
            return;
        }
        if (alpha > 1.0D) {
            alpha = 1.0D;
        }

        // Vanilla queries the bounds of the block below at the shadowed position, not its own
        AxisAlignedBB box = state.getBoundingBox(world, pos);
        double x0 = (double)bx + box.minX + offsetX;
        double x1 = (double)bx + box.maxX + offsetX;
        double surfaceY = (double)by + box.minY + offsetY + 0.015625D;
        double z0 = (double)bz + box.minZ + offsetZ;
        double z1 = (double)bz + box.maxZ + offsetZ;
        float u0 = (float)((x - x0) / 2.0D / (double)size + 0.5D);
        float u1 = (float)((x - x1) / 2.0D / (double)size + 0.5D);
        float v0 = (float)((z - z0) / 2.0D / (double)size + 0.5D);
        float v1 = (float)((z - z1) / 2.0D / (double)size + 0.5D);
        // White with the shadow's alpha, rounded like BufferBuilder.color(float...)
        int color = 0x00FFFFFF | ((int)((float)alpha * 255.0F) << 24);

        this.putVertex(0, x0, surfaceY, z0, u0, v0, color);
        this.putVertex(1, x0, surfaceY, z1, u0, v1, color);
        this.putVertex(2, x1, surfaceY, z1, u1, v1, color);
        this.putVertex(3, x1, surfaceY, z0, u1, v0, color);
        this.buffer.addVertexData(this.quad);
    }

    private void putVertex(int vertex, double x, double y, double z, float u, float v, int color) {
        int[] quad = this.quad;
        int i = vertex * INTS_PER_VERTEX;
        quad[i] = Float.floatToRawIntBits((float)x);
        quad[i + 1] = Float.floatToRawIntBits((float)y);
        quad[i + 2] = Float.floatToRawIntBits((float)z);
        quad[i + 3] = Float.floatToRawIntBits(u);
        quad[i + 4] = Float.floatToRawIntBits(v);
        quad[i + 5] = color;
    }

    private int getLight(World world, BlockPos pos) {
        long key = pos.toLong();
        int light = this.lightCache.get(key);
        if (light == NO_LIGHT) {
            light = world.getLightFromNeighbors(pos);
            this.lightCache.put(key, light);
        }
        return light;
    }

    public void flush() {
        this.topLevelEntity = null;
        this.lightCache.clear();
        if (!this.building) {
            return;
        }
        this.building = false;
        this.buffer.finishDrawing();

        if (this.buffer.getVertexCount() == 0) {
            return;
        }

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        Minecraft.getMinecraft().getTextureManager().bindTexture(SHADOW_TEXTURES);
        GlStateManager.depthMask(false);
        this.uploader.draw(this.buffer);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
    }
}
