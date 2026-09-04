package net.irisshaders.batchedentityrendering.impl;

/**
 * The drawing phase a render type belongs to. Phases are drawn in declaration order.
 * <p>
 * This mirrors the order in which vanilla flushes its own fixed buffers in {@code LevelRenderer#renderLevel}: the
 * opaque sheets are flushed right after block entities, then the translucent and glint sheets, then the water mask,
 * and finally lines after the translucent terrain pass. The difference is that vanilla only reorders a curated
 * allowlist of render types and leaves everything else in submission order, whereas this applies to every render type.
 */
public enum TransparencyType {
	/**
	 * Opaque, non transparent content.
	 */
	OPAQUE,
	/**
	 * Opaque, non transparent content that must be rendered after other opaque content, but before translucents.
	 * Armor trims are coplanar with the armor underneath, so the depth test alone can't order them.
	 */
	OPAQUE_DECAL,
	/**
	 * Generally transparent / translucent content.
	 * <p>
	 * This is the one phase that is drawn in submission order rather than being batched by render type, because
	 * blending is order-dependent. See {@link FullyBufferedMultiBufferSource}.
	 */
	GENERAL_TRANSPARENT,
	/**
	 * Enchantment glint and crumbling blocks
	 * These *must* be rendered after their corresponding opaque / transparent parts.
	 */
	DECAL,
	/**
	 * Water mask, should be drawn after pretty much everything except for translucent terrain and lines.
	 * Prevents water from appearing inside of boats.
	 */
	WATER_MASK,
	/**
	 * Block outlines and other debug things that are overlaid on to the world.
	 * Should be drawn last to avoid weirdness with entity shadows / banners.
	 */
	LINES
}
