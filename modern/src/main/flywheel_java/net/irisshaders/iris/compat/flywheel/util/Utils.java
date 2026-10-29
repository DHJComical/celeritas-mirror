package net.irisshaders.iris.compat.flywheel.util;

import dev.engine_room.flywheel.api.material.Transparency;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.BlendModeFunction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utils
{
    public static <V, K> Map<V, K> reverse(Map<K, V> map)
    {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public static <T> Optional<T> findFirst(List<T> list, Predicate<T> predicate)
    {
        for (T e : list)
        {
            if (predicate.test(e))
            {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    public static BlendMode transparencyToBlendMode(Transparency transparency)
    {
        switch (transparency)
        {
            case OPAQUE ->
            {
                return new BlendMode(BlendModeFunction.ONE.getGlId(), BlendModeFunction.ZERO.getGlId(),
                                     BlendModeFunction.ONE.getGlId(), BlendModeFunction.ZERO.getGlId());
            }
            case ADDITIVE ->
            {
                return new BlendMode(BlendModeFunction.ONE.getGlId(), BlendModeFunction.ONE.getGlId(),
                                     BlendModeFunction.ONE.getGlId(), BlendModeFunction.ONE.getGlId());
            }
            case LIGHTNING ->
            {
                return new BlendMode(BlendModeFunction.SRC_ALPHA.getGlId(), BlendModeFunction.ONE.getGlId(),
                                     BlendModeFunction.SRC_ALPHA.getGlId(), BlendModeFunction.ONE.getGlId());
            }
            case GLINT ->
            {
                return new BlendMode(BlendModeFunction.SRC_COLOR.getGlId(), BlendModeFunction.ONE.getGlId(),
                                     BlendModeFunction.ZERO.getGlId(),      BlendModeFunction.ONE.getGlId());
            }
            case CRUMBLING ->
            {
                return new BlendMode(BlendModeFunction.DST_COLOR.getGlId(), BlendModeFunction.SRC_COLOR.getGlId(),
                                     BlendModeFunction.ONE.getGlId(),       BlendModeFunction.ZERO.getGlId());
            }
            case TRANSLUCENT, ORDER_INDEPENDENT ->
            {
                return new BlendMode(BlendModeFunction.SRC_ALPHA.getGlId(), BlendModeFunction.ONE_MINUS_SRC_ALPHA.getGlId(),
                                     BlendModeFunction.ONE.getGlId(),       BlendModeFunction.ONE_MINUS_SRC_ALPHA.getGlId());
            }
        }

        throw new RuntimeException("Unknown transparency: " + transparency);
    }
}
