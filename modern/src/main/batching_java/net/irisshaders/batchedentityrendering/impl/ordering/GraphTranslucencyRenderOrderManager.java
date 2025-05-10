package net.irisshaders.batchedentityrendering.impl.ordering;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.irisshaders.iris.vendored.digraph.Digraph;
import net.irisshaders.iris.vendored.digraph.Digraphs;
import net.irisshaders.iris.vendored.digraph.MapDigraph;
import net.irisshaders.iris.vendored.digraph.util.fas.FeedbackArcSet;
import net.irisshaders.iris.vendored.digraph.util.fas.FeedbackArcSetPolicy;
import net.irisshaders.iris.vendored.digraph.util.fas.FeedbackArcSetProvider;
import net.irisshaders.iris.vendored.digraph.util.fas.SimpleFeedbackArcSetProvider;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class GraphTranslucencyRenderOrderManager implements RenderOrderManager {
    private static final TransparencyType[] TRANSPARENCY_TYPES = TransparencyType.values();
	private final FeedbackArcSetProvider feedbackArcSetProvider;
	private final EnumMap<TransparencyType, Digraph<RenderType>> types;
	private final EnumMap<TransparencyType, RenderType> currentTypes;
	private boolean inGroup = false;

	public GraphTranslucencyRenderOrderManager() {
		feedbackArcSetProvider = new SimpleFeedbackArcSetProvider();
		types = new EnumMap<>(TransparencyType.class);
		currentTypes = new EnumMap<>(TransparencyType.class);

        initializeTransparencyDigraphs();
	}

    private void initializeTransparencyDigraphs() {
        for (TransparencyType type : TRANSPARENCY_TYPES) {
            types.put(type, makeDigraph());
        }
    }

    private MapDigraph<RenderType> makeDigraph() {
        return new MapDigraph<>(Object2ObjectLinkedOpenHashMap::new, source -> new Object2IntLinkedOpenHashMap<>());
    }

	private static TransparencyType getTransparencyType(RenderType type) {
		while (type instanceof WrappableRenderType) {
			type = ((WrappableRenderType) type).unwrap();
		}

		if (type instanceof BlendingStateHolder) {
			return ((BlendingStateHolder) type).getTransparencyType();
		}

		// Default to "generally transparent" if we can't figure it out.
		return TransparencyType.GENERAL_TRANSPARENT;
	}

	public void begin(RenderType renderType) {
		TransparencyType transparencyType = getTransparencyType(renderType);
		Digraph<RenderType> graph = types.get(transparencyType);
		graph.add(renderType);

		if (inGroup) {
			RenderType previous = currentTypes.put(transparencyType, renderType);

			if (previous == null) {
				return;
			}

			int weight = graph.get(previous, renderType).orElse(0);
			weight += 1;
			graph.put(previous, renderType, weight);
		}
	}

	public void startGroup() {
		if (inGroup) {
			throw new IllegalStateException("Already in a group");
		}

		currentTypes.clear();
		inGroup = true;
	}

	public boolean maybeStartGroup() {
		if (inGroup) {
			return false;
		}

		currentTypes.clear();
		inGroup = true;
		return true;
	}

	public void endGroup() {
		if (!inGroup) {
			throw new IllegalStateException("Not in a group");
		}

		currentTypes.clear();
		inGroup = false;
	}

	@Override
	public void reset() {
		// TODO: Is reallocation efficient?
		types.clear();

		initializeTransparencyDigraphs();
	}

	@Override
	public void resetType(TransparencyType type) {
		// TODO: Is reallocation efficient?
		types.put(type, makeDigraph());
	}

	public List<RenderType> getRenderOrder() {
		int layerCount = 0;

		for (Digraph<RenderType> graph : types.values()) {
			layerCount += graph.getVertexCount();
		}

		List<RenderType> allLayers = new ArrayList<>(layerCount);

		for (Digraph<RenderType> graph : types.values()) {

            // Hoist trivial check for no cycles to avoid allocation of an empty feedback arc set in a lot of cases.
            if (!Digraphs.isTriviallyAcyclic(graph)) {
                // TODO: Make sure that FAS can't become a bottleneck!
                // Running NP-hard algorithms in a real time rendering loop might not be an amazing idea.
                // This shouldn't be necessary in sane scenes, though, and if there aren't cycles,
                // then this *should* be relatively inexpensive, since it'll bail out and return an empty set.
                FeedbackArcSet<RenderType> arcSet =
                        feedbackArcSetProvider.getFeedbackArcSet(graph, graph, FeedbackArcSetPolicy.MIN_WEIGHT);

                if (arcSet.getEdgeCount() > 0) {
                    // This means that our dependency graph had cycles!!!
                    // This is very weird and isn't expected - but we try to handle it gracefully anyways.

                    // Our feedback arc set algorithm finds some dependency links that can be removed hopefully
                    // without disrupting the overall order too much. Hopefully it isn't too slow!
                    for (RenderType source : arcSet.vertices()) {
                        for (RenderType target : arcSet.targets(source)) {
                            graph.remove(source, target);
                        }
                    }
                }
            }

            if (graph.getVertexCount() == 0) {
                // Nothing to add
                continue;
            } else if (graph.getVertexCount() == 1) {
                // Single vertex to add, no need to toposort
                allLayers.add(graph.vertices().iterator().next());
            } else {
                List<RenderType> renderTypesInReverseOrder = Digraphs.toposort(graph, true);

                // Add in reverse order (using this rather than descending=false avoids extra effort to reverse the list)
                for (int i = renderTypesInReverseOrder.size() - 1; i >= 0; i--) {
                    allLayers.add(renderTypesInReverseOrder.get(i));
                }
            }
		}

		return allLayers;
	}
}
