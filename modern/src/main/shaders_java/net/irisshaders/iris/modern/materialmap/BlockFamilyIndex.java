package net.irisshaders.iris.modern.materialmap;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.taumc.celeritas.shaders.CeleritasShaders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Maps full blocks to the shape variants made from them ({@code stone} to {@code stone_slab}, {@code stone_stairs}...),
 * for vanilla and modded blocks alike. A variant is any block extending the vanilla class for its shape, and its base
 * is found from its registry name.
 */
public final class BlockFamilyIndex {
    private static final boolean DUMP = Boolean.getBoolean("celeritas.dumpBlockFamilies");

    /** Tried in order on the stem: {@code bricks} for {@code brick_slab}, {@code oak_planks} for {@code oak_slab}. */
    private static final String[] BASE_SUFFIXES = { "", "s", "_planks", "_block" };

    private static volatile BlockFamilyIndex instance;

    private final Map<Block, EnumMap<ShapeVariant, List<Block>>> variantsByBase;

    private BlockFamilyIndex(Map<Block, EnumMap<ShapeVariant, List<Block>>> variantsByBase) {
        this.variantsByBase = variantsByBase;
    }

    /** Builds the index on first use. The block registry must be complete by then. */
    public static BlockFamilyIndex get() {
        BlockFamilyIndex index = instance;
        if (index == null) {
            synchronized (BlockFamilyIndex.class) {
                index = instance;
                if (index == null) {
                    instance = index = build();
                }
            }
        }
        return index;
    }

    /** Returns the variants of the given shape made from {@code base}, most canonical first. */
    public List<Block> getVariants(Block base, ShapeVariant shape) {
        EnumMap<ShapeVariant, List<Block>> variants = variantsByBase.get(base);
        if (variants == null) {
            return List.of();
        }
        return variants.getOrDefault(shape, List.of());
    }

    private static String namespace(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace();
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static BlockFamilyIndex build() {
        long start = System.nanoTime();

        Map<String, TreeMap<String, Block>> blocksByPath = new HashMap<>();
        Set<String> namespaces = new HashSet<>();
        List<Block> variantBlocks = new ArrayList<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            blocksByPath.computeIfAbsent(key.getPath(), p -> new TreeMap<>()).put(key.getNamespace(), block);
            namespaces.add(key.getNamespace());
            if (ShapeVariant.of(block) != null) {
                variantBlocks.add(block);
            }
        }

        BaseFinder finder = new BaseFinder(blocksByPath, namespaces);
        Map<Block, EnumMap<ShapeVariant, List<Block>>> families = new Reference2ObjectOpenHashMap<>();
        List<Block> unmatched = DUMP ? new ArrayList<>() : null;

        for (Block variant : variantBlocks) {
            ShapeVariant shape = ShapeVariant.of(variant);
            var key = BuiltInRegistries.BLOCK.getKey(variant);
            String stem = stemOf(key.getPath(), shape);
            Block base = stem != null ? finder.findBase(key.getNamespace(), stem) : null;

            if (base == null) {
                if (unmatched != null) {
                    unmatched.add(variant);
                }
                continue;
            }

            families.computeIfAbsent(base, b -> new EnumMap<>(ShapeVariant.class))
                    .computeIfAbsent(shape, s -> new ArrayList<>())
                    .add(variant);
        }

        families.forEach((base, variants) -> {
            String baseNamespace = namespace(base);
            Comparator<Block> priority = Comparator
                    .comparingInt((Block b) -> {
                        String ns = namespace(b);
                        return ns.equals(baseNamespace) ? 0 : ns.equals("minecraft") ? 1 : 2;
                    })
                    .thenComparing(BlockFamilyIndex::id);
            variants.replaceAll((shape, list) -> {
                list.sort(priority);
                return List.copyOf(list);
            });
        });

        CeleritasShaders.logger().info("Indexed {} block families from {} shape variants in {} ms",
                families.size(), variantBlocks.size(), (System.nanoTime() - start) / 1_000_000);

        if (DUMP) {
            dump(families, unmatched);
        }

        return new BlockFamilyIndex(families);
    }

    /**
     * Strips the shape keyword from a registry path, keeping any directories ({@code rock/bricks/granite_slab} gives
     * {@code rock/bricks/granite}). Returns null for names without the keyword and for shapes vanilla doesn't have.
     */
    private static @Nullable String stemOf(String path, ShapeVariant shape) {
        int segmentStart = Math.max(path.lastIndexOf('/'), path.lastIndexOf('.')) + 1;
        String segment = path.substring(segmentStart);
        String words = "_" + segment + "_";

        if (words.contains("_vertical_") || words.contains("_double_")) {
            return null;
        }

        for (String keyword : shape.keywords) {
            if (segment.endsWith("_" + keyword)) {
                return path.substring(0, path.length() - keyword.length() - 1);
            }
        }

        for (String keyword : shape.keywords) {
            if (segment.startsWith(keyword + "_")) {
                return path.substring(0, segmentStart) + segment.substring(keyword.length() + 1);
            }
        }

        return null;
    }

    private static final class BaseFinder {
        private final Map<String, TreeMap<String, Block>> blocksByPath;
        private final Set<String> namespaces;
        private final Map<Block, Boolean> validBases = new IdentityHashMap<>();

        BaseFinder(Map<String, TreeMap<String, Block>> blocksByPath, Set<String> namespaces) {
            this.blocksByPath = blocksByPath;
            this.namespaces = namespaces;
        }

        /**
         * Looks for the base in the namespaces a variant's base usually lives in, most specific first: one embedded
         * in the path ({@code additionalplacements:blockus.x}), the variant's own, one named inside an addon's
         * namespace ({@code extshape_blockus}), vanilla, then any.
         */
        @Nullable Block findBase(String namespace, String stem) {
            Block base;

            int separator = indexOfSeparator(stem);
            if (separator > 0) {
                String embedded = stem.substring(0, separator);
                if (namespaces.contains(embedded) && (base = findIn(embedded, stem.substring(separator + 1))) != null) {
                    return base;
                }
            }

            if ((base = findIn(namespace, stem)) != null) {
                return base;
            }

            for (int i = namespace.indexOf('_'); i >= 0; i = namespace.indexOf('_', i + 1)) {
                String inner = namespace.substring(i + 1);
                if (namespaces.contains(inner) && (base = findIn(inner, stem)) != null) {
                    return base;
                }
            }

            if ((base = findIn("minecraft", stem)) != null) {
                return base;
            }

            for (String suffix : BASE_SUFFIXES) {
                TreeMap<String, Block> candidates = blocksByPath.get(stem + suffix);
                if (candidates != null) {
                    for (Block candidate : candidates.values()) {
                        if (isValidBase(candidate)) {
                            return candidate;
                        }
                    }
                }
            }

            return null;
        }

        private @Nullable Block findIn(String namespace, String stem) {
            for (String suffix : BASE_SUFFIXES) {
                TreeMap<String, Block> candidates = blocksByPath.get(stem + suffix);
                if (candidates != null) {
                    Block candidate = candidates.get(namespace);
                    if (candidate != null && isValidBase(candidate)) {
                        return candidate;
                    }
                }
            }
            return null;
        }

        private static int indexOfSeparator(String path) {
            int dot = path.indexOf('.');
            int slash = path.indexOf('/');
            if (dot < 0) {
                return slash;
            }
            return slash < 0 ? dot : Math.min(dot, slash);
        }

        /** A base must be a full cube, which rules out same-named plants and crops ({@code bamboo}, {@code nether_wart}). */
        private boolean isValidBase(Block block) {
            return validBases.computeIfAbsent(block, b -> {
                if (ShapeVariant.of(b) != null || b.hasDynamicShape()) {
                    return false;
                }
                try {
                    return Block.isShapeFullBlock(b.defaultBlockState().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
                } catch (Exception e) {
                    return false;
                }
            });
        }
    }

    private static void dump(Map<Block, EnumMap<ShapeVariant, List<Block>>> families, List<Block> unmatched) {
        StringBuilder sb = new StringBuilder("Block families:\n");
        families.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(BlockFamilyIndex::id)))
                .forEach(entry -> {
                    sb.append(" - ").append(id(entry.getKey())).append(':');
                    entry.getValue().forEach((shape, variants) -> {
                        sb.append(' ').append(shape).append('=');
                        sb.append(variants.stream().map(BlockFamilyIndex::id).toList());
                    });
                    sb.append('\n');
                });

        sb.append("Shape variants with no base found:\n");
        unmatched.stream()
                .map(BlockFamilyIndex::id)
                .sorted()
                .forEach(id -> sb.append(" - ").append(id).append('\n'));

        CeleritasShaders.logger().info(sb.toString());
    }
}
