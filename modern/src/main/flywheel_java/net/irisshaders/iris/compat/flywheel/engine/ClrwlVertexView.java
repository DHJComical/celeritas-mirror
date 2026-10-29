package net.irisshaders.iris.compat.flywheel.engine;

import dev.engine_room.flywheel.lib.math.DataPacker;
import dev.engine_room.flywheel.lib.vertex.AbstractVertexView;
import org.lwjgl.system.MemoryUtil;

public class ClrwlVertexView extends AbstractVertexView
{
	public static final long STRIDE = 56;

	private boolean isPreviousWriteExtended = false;

	public void flagWriteAsExtended()
	{
		isPreviousWriteExtended = true;
	}

	public boolean consumeExtendedWriteFlag()
	{
		var state = isPreviousWriteExtended;
		isPreviousWriteExtended = false;
		return state;
	}

	@Override
	public long stride() {
		return STRIDE;
	}

	@Override
	public float x(int index) {
		return MemoryUtil.memGetFloat(ptr + index * STRIDE);
	}

	@Override
	public float y(int index) {
		return MemoryUtil.memGetFloat(ptr + index * STRIDE + 4);
	}

	@Override
	public float z(int index) {
		return MemoryUtil.memGetFloat(ptr + index * STRIDE + 8);
	}

	@Override
	public float r(int index) {
		return DataPacker.unpackNormU8(MemoryUtil.memGetByte(ptr + index * STRIDE + 12));
	}

	@Override
	public float g(int index) {
		return DataPacker.unpackNormU8(MemoryUtil.memGetByte(ptr + index * STRIDE + 13));
	}

	@Override
	public float b(int index) {
		return DataPacker.unpackNormU8(MemoryUtil.memGetByte(ptr + index * STRIDE + 14));
	}

	@Override
	public float a(int index) {
		return DataPacker.unpackNormU8(MemoryUtil.memGetByte(ptr + index * STRIDE + 15));
	}

	@Override
	public float u(int index) {
		return MemoryUtil.memGetFloat(ptr + index * STRIDE + 16);
	}

	@Override
	public float v(int index) {
		return MemoryUtil.memGetFloat(ptr + index * STRIDE + 20);
	}

	@Override
	public int light(int index) {
		return MemoryUtil.memGetInt(ptr + index * STRIDE + 24);
	}

	@Override
	public float normalX(int index) {
		return DataPacker.unpackNormI8(MemoryUtil.memGetByte(ptr + index * STRIDE + 28));
	}

	@Override
	public float normalY(int index) {
		return DataPacker.unpackNormI8(MemoryUtil.memGetByte(ptr + index * STRIDE + 29));
	}

	@Override
	public float normalZ(int index) {
		return DataPacker.unpackNormI8(MemoryUtil.memGetByte(ptr + index * STRIDE + 30));
	}

	public short entityX(int index) {
		return MemoryUtil.memGetShort(ptr + index * STRIDE + 32);
	}

	public short entityY(int index) {
		return MemoryUtil.memGetShort(ptr + index * STRIDE + 34);
	}

	public int packedEntity(int index) {
		return MemoryUtil.memGetInt(ptr + index * STRIDE + 32);
	}

	public float midU(int index) {
		return MemoryUtil.memGetFloat(ptr + index * STRIDE + 38);
	}

	public float midV(int index) {
		return MemoryUtil.memGetFloat(ptr + index * STRIDE + 40);
	}

	public float tangentX(int index) {
		return DataPacker.unpackNormI8(MemoryUtil.memGetByte(ptr + index * STRIDE + 44));
	}

	public float tangentY(int index) {
		return DataPacker.unpackNormI8(MemoryUtil.memGetByte(ptr + index * STRIDE + 45));
	}

	public float tangentZ(int index) {
		return DataPacker.unpackNormI8(MemoryUtil.memGetByte(ptr + index * STRIDE + 46));
	}

	public float tangentW(int index) {
		return DataPacker.unpackNormI8(MemoryUtil.memGetByte(ptr + index * STRIDE + 47));
	}

	public int packedTangent(int index) {
		return MemoryUtil.memGetInt(ptr + index * STRIDE + 44);
	}

	public byte midBlockX(int index) {
		return MemoryUtil.memGetByte(ptr + index * STRIDE + 48);
	}

	public byte midBlockY(int index) {
		return MemoryUtil.memGetByte(ptr + index * STRIDE + 49);
	}

	public byte midBlockZ(int index) {
		return MemoryUtil.memGetByte(ptr + index * STRIDE + 50);
	}

	public byte midBlockW(int index) {
		return MemoryUtil.memGetByte(ptr + index * STRIDE + 51);
	}

	public int packedMidBlock(int index) {
		return MemoryUtil.memGetInt(ptr + index * STRIDE + 48);
	}

	// Overlay is added at the end to make a single memcpy with IrisTerrainVertexView
	@Override
	public int overlay(int index) {
		return MemoryUtil.memGetInt(ptr + index * STRIDE + 52);
	}


	@Override
	public void x(int index, float x) {
		MemoryUtil.memPutFloat(ptr + index * STRIDE, x);
	}

	@Override
	public void y(int index, float y) {
		MemoryUtil.memPutFloat(ptr + index * STRIDE + 4, y);
	}

	@Override
	public void z(int index, float z) {
		MemoryUtil.memPutFloat(ptr + index * STRIDE + 8, z);
	}

	@Override
	public void r(int index, float r) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 12, DataPacker.packNormU8(r));
	}

	@Override
	public void g(int index, float g) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 13, DataPacker.packNormU8(g));
	}

	@Override
	public void b(int index, float b) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 14, DataPacker.packNormU8(b));
	}

	@Override
	public void a(int index, float a) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 15, DataPacker.packNormU8(a));
	}

	@Override
	public void u(int index, float u) {
		MemoryUtil.memPutFloat(ptr + index * STRIDE + 16, u);
	}

	@Override
	public void v(int index, float v) {
		MemoryUtil.memPutFloat(ptr + index * STRIDE + 20, v);
	}

	@Override
	public void light(int index, int light) {
		MemoryUtil.memPutInt(ptr + index * STRIDE + 24, light);
	}

	@Override
	public void normalX(int index, float normalX)
	{
		MemoryUtil.memPutByte(ptr + index * STRIDE + 28, DataPacker.packNormI8(normalX));
	}

	@Override
	public void normalY(int index, float normalY)
	{
		MemoryUtil.memPutByte(ptr + index * STRIDE + 29, DataPacker.packNormI8(normalY));
	}

	@Override
	public void normalZ(int index, float normalZ) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 30, DataPacker.packNormI8(normalZ));
	}

	public void entityX(int index, short entityX) {
		MemoryUtil.memPutShort(ptr + index * STRIDE + 32, entityX);
	}

	public void entityY(int index, short entityY) {
		MemoryUtil.memPutShort(ptr + index * STRIDE + 34, entityY);
	}

	public void packedEntity(int index, int packed) {
		MemoryUtil.memPutInt(ptr + index * STRIDE + 32, packed);
	}

	public void midU(int index, float midU) {
		MemoryUtil.memPutFloat(ptr + index * STRIDE + 36, midU);
	}

	public void midV(int index, float midV) {
		MemoryUtil.memPutFloat(ptr + index * STRIDE + 40, midV);
	}

	public void tangentX(int index, float tangentX) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 44, DataPacker.packNormI8(tangentX));
	}

	public void tangentY(int index, float tangentY) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 45, DataPacker.packNormI8(tangentY));
	}

	public void tangentZ(int index, float tangentZ) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 46, DataPacker.packNormI8(tangentZ));
	}

	public void tangentW(int index, float tangentW) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 47, DataPacker.packNormI8(tangentW));
	}

	public void packedTangent(int index, int packed) {
		MemoryUtil.memPutInt(ptr + index * STRIDE + 44, packed);
	}

	public void midBlockX(int index, byte midX) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 48, midX);
	}

	public void midBlockY(int index, byte midY) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 49, midY);
	}

	public void midBlockZ(int index, byte midZ) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 50, midZ);
	}

	public void midBlockW(int index, byte midW) {
		MemoryUtil.memPutByte(ptr + index * STRIDE + 51, midW);
	}

	public void packedMidBlock(int index, int packed) {
		MemoryUtil.memPutInt(ptr + index * STRIDE + 48, packed);
	}

	@Override
	public void overlay(int index, int overlay) {
		MemoryUtil.memPutInt(ptr + index * STRIDE + 52, overlay);
	}
}
