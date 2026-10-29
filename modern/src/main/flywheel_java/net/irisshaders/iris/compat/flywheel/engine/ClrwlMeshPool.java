package net.irisshaders.iris.compat.flywheel.engine;

import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.backend.engine.IndexPool;
import dev.engine_room.flywheel.backend.gl.GlPrimitive;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.backend.util.ReferenceCounted;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.RetexturedMesh;
import net.irisshaders.iris.vertices.NormalHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.lwjgl.opengl.GL32;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Copy/Paste from MeshPool
// Changes are:
// - use of ColorwheelVertex instead of InternalVertex
// - tangents and midTextCoord being computed on upload
public class ClrwlMeshPool {
	private final ClrwlVertexView vertexView;
	private final Map<Mesh, PooledMesh> meshes = new HashMap<>();
	private final List<PooledMesh> meshList = new ArrayList<>();
	private final List<PooledMesh> recentlyAllocated = new ArrayList<>();

	private final GlBuffer vbo;
	private final IndexPool indexPool;

	private boolean dirty;
	private boolean anyToRemove;

	/**
	 * Create a new mesh pool.
	 */
	public ClrwlMeshPool() {
		vertexView = ClrwlVertex.createVertexView();
		vbo = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
		indexPool = new IndexPool();
	}

	/**
	 * Allocate a model in the arena.
	 *
	 * @param mesh The model to allocate.
	 * @return A handle to the allocated model.
	 */
	public PooledMesh alloc(Mesh mesh) {
		return meshes.computeIfAbsent(mesh, this::_alloc);
	}

	private PooledMesh _alloc(Mesh m) {
		PooledMesh bufferedModel = new PooledMesh(m);
		meshList.add(bufferedModel);
		recentlyAllocated.add(bufferedModel);

		dirty = true;
		return bufferedModel;
	}

	@Nullable
	public ClrwlMeshPool.PooledMesh get(Mesh mesh) {
		return meshes.get(mesh);
	}

	public void flush() {
        if (!dirty) {
            return;
        }

		if (anyToRemove) {
			anyToRemove = false;
			processDeletions();
		}

		if (!recentlyAllocated.isEmpty()) {
			// Otherwise, just update the index with the new counts.
			for (PooledMesh mesh : recentlyAllocated) {
				indexPool.updateCount(mesh.mesh.indexSequence(), mesh.indexCount());
			}
			indexPool.flush();
			recentlyAllocated.clear();
		}

		uploadAll();
        dirty = false;
    }

	private void processDeletions() {
		// remove deleted meshes
		meshList.removeIf(pooledMesh -> {
			boolean deleted = pooledMesh.isDeleted();
			if (deleted) {
				meshes.remove(pooledMesh.mesh);
			}
			return deleted;
		});
	}

	private void uploadAll() {
		long neededSize = 0;
		for (PooledMesh mesh : meshList) {
			neededSize += mesh.byteSize();
		}

		final var vertexBlock = MemoryBlock.malloc(neededSize);
		final long vertexPtr = vertexBlock.ptr();

		int byteIndex = 0;
		int baseVertex = 0;
		for (PooledMesh mesh : meshList)
		{
			mesh.baseVertex = baseVertex;

			vertexView.ptr(vertexPtr + byteIndex);
			vertexView.vertexCount(mesh.vertexCount());
			mesh.mesh.write(vertexView);

			mesh.meshCenter = computeMeshCenter(vertexView);

			if (!vertexView.consumeExtendedWriteFlag())
			{
				Mesh baseMesh = mesh.mesh;

				while (baseMesh instanceof RetexturedMesh retextured)
				{
					baseMesh = retextured.mesh();
				}

				if (baseMesh instanceof QuadMesh quad)
				{
					computeExtendedQuadData(quad, vertexView);
				}
				else
				{
					computeExtendedData(baseMesh, vertexView);
				}
			}
			else
			{
				// Vertex format is IrisVertexFormats.TERRAIN, therefore the mesh must be a QuadMesh
				patchMidUVCoords(mesh.mesh, vertexView);
			}

			byteIndex += mesh.byteSize();
			baseVertex += mesh.vertexCount();
		}

		vbo.upload(vertexBlock);

		vertexBlock.free();
	}

	private Vector3f computeMeshCenter(ClrwlVertexView vertexView)
	{
		float minX =  Float.MAX_VALUE;
		float minY =  Float.MAX_VALUE;
		float minZ =  Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		float maxZ = -Float.MAX_VALUE;

		for (int i = 0; i < vertexView.vertexCount(); i++)
		{
			float x = vertexView.x(i);
			float y = vertexView.y(i);
			float z = vertexView.z(i);

			if (x < minX)
			{
				minX = x;
			}

			if (x > maxX)
			{
				maxX = x;
			}

			if (y < minY)
			{
				minY = y;
			}

			if (y > maxY)
			{
				maxY = y;
			}

			if (z < minZ)
			{
				minZ = z;
			}

			if (z > maxZ)
			{
				maxZ = z;
			}
		}

		return new Vector3f((minX + maxX) / 2.0f, (minY + maxY) / 2.0f, (minZ + maxZ) / 2.0f);
	}

	private void computeExtendedQuadData(QuadMesh mesh, ClrwlVertexView vertexView)
	{
		var quadCnt = mesh.vertexCount() / 4;

		var uArr = new float[4];
		var vArr = new float[4];
		var uvCnt = 0;

		for (int q = 0; q < quadCnt; q++)
		{
			int base = q * 4;

			float normalX = 0;
			float normalY = 0;
			float normalZ = 0;
			uvCnt = 0;

top:		for (int vId = 0; vId < 4; vId++)
			{
				int idx = base + vId;

				normalX += vertexView.normalX(idx);
				normalY += vertexView.normalY(idx);
				normalZ += vertexView.normalZ(idx);

				float u = vertexView.u(idx);
				float v = vertexView.v(idx);

				for (int i = 0; i < uvCnt; i++)
				{
					if (uArr[i] == u && vArr[i] == v)
					{
						continue top;
					}
				}

				uArr[uvCnt] = u;
				vArr[uvCnt] = v;
				uvCnt += 1;
			}

			normalX /= 4.0F;
			normalY /= 4.0F;
			normalZ /= 4.0F;

			float midU = 0;
			float midV = 0;

			for (int i = 0; i < uvCnt; i++)
			{
				midU += uArr[i];
				midV += vArr[i];
			}

			midU /= uvCnt;
			midV /= uvCnt;

			int tangent = NormalHelper.computeTangent(normalX, normalY, normalZ,
										vertexView.x(base + 0), vertexView.y(base + 0), vertexView.z(base + 0), vertexView.u(base + 0), vertexView.v(base + 0),
										vertexView.x(base + 1), vertexView.y(base + 1), vertexView.z(base + 1), vertexView.u(base + 1), vertexView.v(base + 1),
										vertexView.x(base + 2), vertexView.y(base + 2), vertexView.z(base + 2), vertexView.u(base + 2), vertexView.v(base + 2));

			for (int vId = 0; vId < 4; vId++)
			{
				vertexView.packedEntity(base + vId, 0xFFFFFFFF);
				vertexView.midU(base + vId, midU);
				vertexView.midV(base + vId, midV);
				vertexView.packedTangent(base + vId, tangent);
				vertexView.packedMidBlock(base + vId, 0xFF << 24);
			}
		}
	}

	private void computeExtendedData(Mesh mesh, ClrwlVertexView vertexView)
	{
		// TODO

		for (int i = 0; i < mesh.vertexCount(); i++)
		{
			vertexView.packedEntity(i, 0xFFFFFFFF);
			vertexView.midU(i, 0);
			vertexView.midV(i, 0);
			vertexView.packedTangent(i, 0);
			vertexView.packedMidBlock(i, 0xFF << 24);
		}
	}

	// Some Create blocks, like waterwheels, are QuadMesh but with degenerated triangles
	// This causes issues with the method used by Iris to compute midTexCoord
	private void patchMidUVCoords(Mesh mesh, ClrwlVertexView vertexView)
	{
		var quadCnt = mesh.vertexCount() / 4;

		var uArr = new float[4];
		var vArr = new float[4];
		var uvCnt = 0;

		for (int q = 0; q < quadCnt; q++)
		{
			int base = q * 4;

			uvCnt = 0;

top: 		for (int vId = 0; vId < 4; vId++)
			{
				int idx = base + vId;
				float u = vertexView.u(idx);
				float v = vertexView.v(idx);

				for (int i = 0; i < uvCnt; i++)
				{
					if (uArr[i] == u && vArr[i] == v)
					{
						continue top;
					}
				}

				uArr[uvCnt] = u;
				vArr[uvCnt] = v;
				uvCnt += 1;
			}

			float midU = 0;
			float midV = 0;

			for (int i = 0; i < uvCnt; i++)
			{
				midU += uArr[i];
				midV += vArr[i];
			}

			midU /= uvCnt;
			midV /= uvCnt;

			for (int vId = 0; vId < 4; vId++)
			{
				vertexView.midU(base + vId, midU);
				vertexView.midV(base + vId, midV);
			}
		}
	}

	public void bind(GlVertexArray vertexArray) {
		indexPool.bind(vertexArray);
		vertexArray.bindVertexBuffer(0, vbo.handle(), 0, ClrwlVertex.STRIDE);
		vertexArray.bindAttributes(0, 0, ClrwlVertex.ATTRIBUTES);
	}

	public void delete() {
		vbo.delete();
		indexPool.delete();
		meshes.clear();
		meshList.clear();
	}

	public class PooledMesh extends ReferenceCounted {
		public static final int INVALID_BASE_VERTEX = -1;

		private final Mesh mesh;
		private Vector3f meshCenter;
		private int baseVertex = INVALID_BASE_VERTEX;

		private PooledMesh(Mesh mesh)
		{
			this.mesh = mesh;
			this.meshCenter = new Vector3f(0, 0, 0);
		}

		public int vertexCount() {
			return mesh.vertexCount();
		}

		public int byteSize() {
			return mesh.vertexCount() * ClrwlVertex.STRIDE;
		}

		public int indexCount() {
			return mesh.indexCount();
		}

		public int baseVertex() {
			return baseVertex;
		}

		public int firstIndex() {
			return ClrwlMeshPool.this.indexPool.firstIndex(mesh.indexSequence());
		}

		public long firstIndexByteOffset() {
			return (long) firstIndex() * Integer.BYTES;
		}

		public boolean isInvalid() {
			return mesh.vertexCount() == 0 || baseVertex == INVALID_BASE_VERTEX || isDeleted();
		}

		public Vector4fc boundingSphere()
		{
			return mesh.boundingSphere();
		}

		public Vector3fc meshCenter()
		{
			return meshCenter;
		}

		public void draw(int instanceCount) {
			if (instanceCount > 1) {
				GL32.glDrawElementsInstancedBaseVertex(GlPrimitive.TRIANGLES.glEnum, mesh.indexCount(), GL32.GL_UNSIGNED_INT, firstIndexByteOffset(), instanceCount, baseVertex);
			} else {
				GL32.glDrawElementsBaseVertex(GlPrimitive.TRIANGLES.glEnum, mesh.indexCount(), GL32.GL_UNSIGNED_INT, firstIndexByteOffset(), baseVertex);
			}
		}

		@Override
		protected void _delete() {
			ClrwlMeshPool.this.dirty = true;
			ClrwlMeshPool.this.anyToRemove = true;
		}
	}
}
