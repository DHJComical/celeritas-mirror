package net.irisshaders.iris.compat.flywheel.compile.transform;

import java.util.Collections;
import java.util.Map;

public record ClrwlTransformOutput(String code, Map<Integer, String> outputs)
{
    public static ClrwlTransformOutput unprocessed(String code)
    {
        return new ClrwlTransformOutput(code, Collections.emptyMap());
    }
}
