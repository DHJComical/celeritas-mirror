package net.irisshaders.iris.compat.flywheel.indirect;

import net.irisshaders.iris.compat.flywheel.engine.ClrwlMeshPool;
import net.irisshaders.iris.compat.flywheel.engine.embed.EmbeddedEnvironment;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.backend.engine.MaterialEncoder;
import org.lwjgl.system.MemoryUtil;

public class ClrwlIndirectDraw
{
	private final ClrwlIndirectInstancer<?> instancer;
	private final Material material;
	private final ClrwlMeshPool.PooledMesh mesh;
	private final int bias;
	private final int indexOfMeshInModel;

	private final int packedFogAndCutout;
	private final int packedMaterialProperties;
	private boolean deleted;

	public ClrwlIndirectDraw(ClrwlIndirectInstancer<?> instancer, Material material, ClrwlMeshPool.PooledMesh mesh, int bias, int indexOfMeshInModel)
	{
		this.instancer = instancer;
		this.material = material;
		this.mesh = mesh;
		this.bias = bias;
		this.indexOfMeshInModel = indexOfMeshInModel;

		mesh.acquire();

		this.packedFogAndCutout = MaterialEncoder.packUberShader(material);
		this.packedMaterialProperties = MaterialEncoder.packProperties(material);
	}

	public boolean deleted()
	{
		return deleted;
	}

	public Material material()
	{
		return material;
	}

	public boolean isEmbedded()
	{
		return instancer.environment instanceof EmbeddedEnvironment;
	}

	public ClrwlMeshPool.PooledMesh mesh()
	{
		return mesh;
	}

	public int bias()
	{
		return bias;
	}

	public int indexOfMeshInModel()
	{
		return indexOfMeshInModel;
	}

	public void write(long ptr)
	{
		MemoryUtil.memPutInt(ptr, mesh.indexCount()); // count
		MemoryUtil.memPutInt(ptr + 4, 0); // instanceCount - to be set by the apply shader
		MemoryUtil.memPutInt(ptr + 8, mesh.firstIndex()); // firstIndex
		MemoryUtil.memPutInt(ptr + 12, mesh.baseVertex()); // baseVertex
		MemoryUtil.memPutInt(ptr + 16, instancer.baseInstance()); // baseInstance

		MemoryUtil.memPutInt(ptr + 20, instancer.modelIndex()); // modelIndex

		MemoryUtil.memPutInt(ptr + 24, instancer.environment.matrixIndex()); // matrixIndex

		MemoryUtil.memPutInt(ptr + 28, packedFogAndCutout); // packedFogAndCutout
		MemoryUtil.memPutInt(ptr + 32, packedMaterialProperties); // packedMaterialProperties

		MemoryUtil.memPutFloat(ptr + 36, mesh.meshCenter().x());
		MemoryUtil.memPutFloat(ptr + 40, mesh.meshCenter().y());
		MemoryUtil.memPutFloat(ptr + 44, mesh.meshCenter().z());
	}

	public void writeWithOverrides(long ptr, int instanceIndex, Material materialOverride)
	{
		MemoryUtil.memPutInt(ptr, mesh.indexCount()); // count
		MemoryUtil.memPutInt(ptr + 4, 1); // instanceCount - only drawing one instance
		MemoryUtil.memPutInt(ptr + 8, mesh.firstIndex()); // firstIndex
		MemoryUtil.memPutInt(ptr + 12, mesh.baseVertex()); // baseVertex
		MemoryUtil.memPutInt(ptr + 16, instancer.local2GlobalInstanceIndex(instanceIndex)); // baseInstance

		MemoryUtil.memPutInt(ptr + 20, instancer.modelIndex()); // modelIndex

		MemoryUtil.memPutInt(ptr + 24, instancer.environment.matrixIndex()); // matrixIndex

		MemoryUtil.memPutInt(ptr + 28, MaterialEncoder.packUberShader(materialOverride)); // packedFogAndCutout
		MemoryUtil.memPutInt(ptr + 32, MaterialEncoder.packProperties(materialOverride)); // packedMaterialProperties

		MemoryUtil.memPutFloat(ptr + 36, mesh.meshCenter().x());
		MemoryUtil.memPutFloat(ptr + 40, mesh.meshCenter().y());
		MemoryUtil.memPutFloat(ptr + 44, mesh.meshCenter().z());
	}

	public void delete()
	{
		if (deleted)
		{
			return;
		}

		mesh.release();

		deleted = true;
	}
}
