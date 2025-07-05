package org.embeddedt.embeddium.impl.render;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.Celeritas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements a caching layer over predicate-based render type selection logic. There is quite a bit
 * of overhead involved in dealing with the arbitrary predicates, so we cache a list of render layers
 * for each state (lazily), and just return that list.
 */
public class EmbeddiumRenderLayerCache<STATE, LAYER> {
    private static final boolean DISABLE_CACHE = Boolean.getBoolean("embeddium.disableRenderLayerCache");

    private final Reference2ReferenceOpenHashMap<LAYER, ImmutableList<LAYER>> singleLayers;
    private final Reference2ReferenceOpenHashMap<STATE, ImmutableList<LAYER>> stateToLayerMap = new Reference2ReferenceOpenHashMap<>();
    private final List<LAYER> layers;
    private final Predicate<STATE, LAYER> predicate;

    public EmbeddiumRenderLayerCache(List<LAYER> layers, Predicate<STATE, LAYER> predicate) {
        this.layers = layers;
        this.singleLayers = new Reference2ReferenceOpenHashMap<>(layers.size());
        for(var layer : layers) {
            singleLayers.put(layer, ImmutableList.of(layer));
        }
        this.predicate = predicate;
    }

    /**
     * Retrieve the list of render layers for the given block/fluid state.
     * @param state a BlockState or FluidState
     * @return a list of render layers that the block/fluid state should be rendered on
     */
    public List<LAYER> forState(STATE state) {
        if(DISABLE_CACHE) {
            return generateList(state);
        }

        ImmutableList<LAYER> list = stateToLayerMap.get(state);

        if(list == null) {
            list = createList(state);
        }

        return list;
    }

    private List<LAYER> generateList(STATE state) {
        List<LAYER> foundLayers = new ArrayList<>(2);

        for (var layer : this.layers) {
            if (this.predicate.canRenderInLayer(state, layer)) {
                foundLayers.add(layer);
            }
        }

        return foundLayers;
    }

    private ImmutableList<LAYER> createList(STATE state) {
        List<LAYER> foundLayers = generateList(state);

        ImmutableList<LAYER> layerList;

        // Deduplicate simple lists
        if(foundLayers.isEmpty()) {
            layerList = ImmutableList.of();
        } else if(foundLayers.size() == 1) {
            layerList = singleLayers.get(foundLayers.get(0));
            Objects.requireNonNull(layerList);
        } else {
            layerList = ImmutableList.copyOf(foundLayers);
        }

        stateToLayerMap.put(state, layerList);

        return layerList;
    }

    public interface Predicate<STATE, LAYER> {
        boolean canRenderInLayer(STATE state, LAYER layer);
    }

    static {
        if(DISABLE_CACHE) {
            Celeritas.logger().warn("Render layer cache is disabled, performance will be affected.");
        }
    }
}