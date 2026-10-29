package net.irisshaders.iris.compat.flywheel.shaderpack;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlBlendModeOverride;
import net.irisshaders.iris.compat.flywheel.engine.ClrwlOitAccumulateOverride;
import net.irisshaders.iris.compat.flywheel.util.Utils;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendMode;
import net.irisshaders.iris.gl.blending.BlendModeFunction;
import net.irisshaders.iris.gl.blending.BufferBlendInformation;
import net.irisshaders.iris.gl.texture.InternalTextureFormat;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.shaderpack.option.OrderBackedProperties;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
import net.irisshaders.iris.shaderpack.preprocessor.PropertiesPreprocessor;
import net.irisshaders.iris.shaderpack.properties.ShadowCullState;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.function.Consumer;

public class ClrwlShaderProperties
{
    private final Map<ClrwlProgramId, ClrwlBlendModeOverride> programBlendOverrides = new EnumMap<>(ClrwlProgramId.class);
    private final Map<ClrwlProgramId, List<BufferBlendInformation>> bufferBlendOverrides = new EnumMap<>(ClrwlProgramId.class);

    private OptionalBoolean shadowEnabled = OptionalBoolean.DEFAULT;

    private OptionalBoolean gbuffersOitEnabled = OptionalBoolean.DEFAULT;
    private int[] gbuffersOitCoeffRanks = new int[0];
    private final List<ClrwlOitAccumulateOverride> gbuffersOitAccumulateOverrides = new ArrayList<>();

    private OptionalBoolean shadowOitEnabled = OptionalBoolean.DEFAULT;
    private int[] shadowOitCoeffRanks = new int[0];
    private final List<ClrwlOitAccumulateOverride> shadowOitAccumulateOverrides = new ArrayList<>();

    private ShadowCullState shadowCulling = ShadowCullState.DEFAULT;
    private OptionalBoolean shadowOcclusionCulling = OptionalBoolean.DEFAULT;
    private OptionalBoolean frustumCulling = OptionalBoolean.DEFAULT;
    private OptionalBoolean occlusionCulling = OptionalBoolean.DEFAULT;

    public ClrwlShaderProperties()
    {
        // Empty
    }

    public ClrwlShaderProperties(String str, ShaderPackOptions options, Iterable<StringPair> environmentDefines)
    {
        String preprocessedStr = PropertiesPreprocessor.preprocessSource(str, options, environmentDefines);
        Properties properties = new OrderBackedProperties();

        try
        {
            properties.load(new StringReader(preprocessedStr));
        }
        catch (IOException e)
        {
            Colorwheel.LOGGER.error("Could not load colorwheel.properties", e);
        }

        for (Map.Entry<Object, Object> property : properties.entrySet())
        {
            String key = (String) property.getKey();
            String value = (String) property.getValue();
            String[] path = key.split("\\.");

            if (path.length == 0)
            {
                continue;
            }

            if (path[0].equals("shadow"))
            {
                if (path.length == 2)
                {
                    if (path[1].equals("enabled"))
                    {
                        parseBoolean(value, bool -> shadowEnabled = bool);
                    }
                    else if (path[1].equals("culling"))
                    {
                        if ("false".equals(value)) {
                            shadowCulling = ShadowCullState.DISTANCE;
                        } else if ("true".equals(value)) {
                            shadowCulling = ShadowCullState.ADVANCED;
                        } else if ("reversed".equals(value)) {
                            shadowCulling = ShadowCullState.REVERSED;
                        } else {
                            Colorwheel.LOGGER.error("Unrecognized shadow culling setting: " + value);
                            continue;
                        }
                    }
                }
                else if (path.length == 3)
                {
                    if (path[1].equals("occlusion") && path[2].equals("culling"))
                    {
                        parseBoolean(value, bool -> shadowOcclusionCulling = bool);
                    }
                }
            }
            else if (path[0].equals("frustum"))
            {
                if (path.length == 2 && path[1].equals("culling"))
                {
                    parseBoolean(value, bool -> frustumCulling = bool);
                }
            }
            else if (path[0].equals("occlusion"))
            {
                if (path.length == 2 && path[1].equals("culling"))
                {
                    parseBoolean(value, bool -> occlusionCulling = bool);
                }
            }
            else if (path[0].equals("blend"))
            {
                if (path.length == 2)
                {
                    var maybeProgramId = ClrwlProgramId.fromName(path[1]);

                    if (maybeProgramId.isEmpty())
                    {
                        Colorwheel.LOGGER.warn("Unknown program: {}", path[1]);
                        continue;
                    }

                    var programId = maybeProgramId.get();

                    if (value.equals("off"))
                    {
                        programBlendOverrides.put(programId, new ClrwlBlendModeOverride(null));
                    }
                    else
                    {
                        parseBlendMode(value).ifPresent(bm -> programBlendOverrides.put(programId, new ClrwlBlendModeOverride(bm)));
                    }
                }
                else if (path.length == 3)
                {
                    if (!IrisRenderSystem.supportsBufferBlending())
                    {
                        throw new RuntimeException("Buffer blending not supported");
                    }

                    var maybeProgramId = ClrwlProgramId.fromName(path[1]);

                    if (maybeProgramId.isEmpty())
                    {
                        Colorwheel.LOGGER.warn("Unknown program: {}", path[1]);
                        continue;
                    }

                    var programId = maybeProgramId.get();
                    String buffer = path[2];

                    int id;

                    if (programId.group() == ClrwlProgramGroup.SHADOW)
                    {
                        if (!buffer.startsWith("shadowcolor"))
                        {
                            Colorwheel.LOGGER.error("Attempt to disable blend on invalid buffer: {}", buffer);
                            continue;
                        }

                        try
                        {
                            id = Integer.parseInt(buffer.substring("shadowcolor".length()));
                        }
                        catch (NumberFormatException e)
                        {
                            Colorwheel.LOGGER.error("Attempt to disable blend on invalid buffer: {}", buffer);
                            continue;
                        }
                    }
                    else
                    {
                        if (!buffer.startsWith("colortex"))
                        {
                            Colorwheel.LOGGER.error("Attempt to disable blend on invalid buffer: {}", buffer);
                            continue;
                        }

                        try
                        {
                            id = Integer.parseInt(buffer.substring("colortex".length()));
                        }
                        catch (NumberFormatException e)
                        {
                            Colorwheel.LOGGER.error("Attempt to disable blend on invalid buffer: {}", buffer);
                            continue;
                        }
                    }

                    var list = bufferBlendOverrides.computeIfAbsent(programId, (s) -> new ArrayList<>());

                    if (value.equals("off"))
                    {
                        list.add(new BufferBlendInformation(id, null));
                    }
                    else
                    {
                        parseBlendMode(value).ifPresent(bm -> list.add(new BufferBlendInformation(id, bm)));
                    }
                }

                continue;
            }
            else if (path[0].equals("oit"))
            {
                if (path.length == 1)
                {
                    parseBoolean(value, bool ->
                    {
                        gbuffersOitEnabled = bool;
                        shadowOitEnabled = bool;
                    });
                    continue;
                }

                if (path[1].equals("gbuffers"))
                {
                    if (path.length == 2)
                    {
                        parseBoolean(value, bool -> gbuffersOitEnabled = bool);
                        continue;
                    }
                    else if (path[2].equals("coefficientRanks") && path.length == 3)
                    {
                        parseCoefficientsRanks(value)
                                .ifPresent(ranks -> gbuffersOitCoeffRanks = ranks);
                        continue;
                    }
                    else if (path[2].startsWith("colortex"))
                    {
                        int drawBuffer;

                        try
                        {
                            drawBuffer = Integer.parseUnsignedInt(path[2].substring(8));
                        }
                        catch (NumberFormatException e)
                        {
                            Colorwheel.LOGGER.error("Invalid attachment: {}", path[2]);
                            continue;
                        }

                        if (path.length == 3)
                        {
                            Optional<Integer> coefficientId;

                            if (value.equals("frontmost"))
                            {
                                coefficientId = Optional.empty();
                            }
                            else
                            {
                                try
                                {
                                    coefficientId = Optional.of(Integer.parseUnsignedInt(value));
                                }
                                catch (NumberFormatException e)
                                {
                                    Colorwheel.LOGGER.error("Invalid coefficient id: {}", value);
                                    continue;
                                }
                            }

                            Utils.findFirst(gbuffersOitAccumulateOverrides, t -> t.drawBuffer() == drawBuffer)
                                    .ifPresentOrElse(t -> t.setCoefficientId(coefficientId),
                                                     () -> gbuffersOitAccumulateOverrides.add(new ClrwlOitAccumulateOverride(drawBuffer, coefficientId)));

                            continue;
                        }
                        else if (path.length == 4)
                        {
                            if (path[3].equals("format"))
                            {
                                try
                                {
                                    var format = InternalTextureFormat.valueOf(value);

                                    Utils.findFirst(gbuffersOitAccumulateOverrides, t -> t.drawBuffer() == drawBuffer)
                                            .ifPresentOrElse(t -> t.setFormat(format),
                                                    () -> gbuffersOitAccumulateOverrides.add(new ClrwlOitAccumulateOverride(drawBuffer, format)));
                                }
                                catch (IllegalArgumentException e)
                                {
                                    Colorwheel.LOGGER.error("Unknown format: {}", value);
                                    continue;
                                }

                                continue;
                            }
                        }
                    }

                    Colorwheel.LOGGER.error("Unknown OIT key: {}", path[2]);
                    continue;
                }
                else if (path[1].equals("shadow"))
                {
                    if (path.length == 2)
                    {
                        parseBoolean(value, bool -> shadowOitEnabled = bool);
                        continue;
                    }
                    else if (path[2].equals("coefficientRanks") && path.length == 3)
                    {
                        parseCoefficientsRanks(value)
                                .ifPresent(ranks -> shadowOitCoeffRanks = ranks);
                        continue;
                    }
                    else if (path[2].startsWith("shadowcolor"))
                    {
                        int drawBuffer;

                        try
                        {
                            drawBuffer = Integer.parseUnsignedInt(path[2].substring(11));
                        }
                        catch (NumberFormatException e)
                        {
                            Colorwheel.LOGGER.error("Invalid attachment: {}", path[2]);
                            continue;
                        }

                        if (path.length == 3)
                        {
                            Optional<Integer> coefficientId;

                            if (value.equals("frontmost"))
                            {
                                coefficientId = Optional.empty();
                            }
                            else
                            {
                                try
                                {
                                    coefficientId = Optional.of(Integer.parseUnsignedInt(value));
                                }
                                catch (NumberFormatException e)
                                {
                                    Colorwheel.LOGGER.error("Invalid coefficient id: {}", value);
                                    continue;
                                }
                            }

                            Utils.findFirst(shadowOitAccumulateOverrides, t -> t.drawBuffer() == drawBuffer)
                                    .ifPresentOrElse(t -> t.setCoefficientId(coefficientId),
                                                     () -> shadowOitAccumulateOverrides.add(new ClrwlOitAccumulateOverride(drawBuffer, coefficientId)));

                            continue;
                        }
                        else if (path.length == 4)
                        {
                            if (path[3].equals("format"))
                            {
                                try
                                {
                                    var format = InternalTextureFormat.valueOf(value);

                                    Utils.findFirst(shadowOitAccumulateOverrides, t -> t.drawBuffer() == drawBuffer)
                                            .ifPresentOrElse(t -> t.setFormat(format),
                                                             () -> shadowOitAccumulateOverrides.add(new ClrwlOitAccumulateOverride(drawBuffer, format)));
                                }
                                catch (IllegalArgumentException e)
                                {
                                    Colorwheel.LOGGER.error("Unknown format: {}", value);
                                    continue;
                                }

                                continue;
                            }
                        }
                    }

                    Colorwheel.LOGGER.error("Unknown OIT key: {}", path[2]);
                    continue;
                }
                else
                {
                    Colorwheel.LOGGER.error("Unknown program groug for OIT: {}", path[1]);
                    continue;
                }
            }
        }
    }

    private void parseBoolean(String value, Consumer<OptionalBoolean> block)
    {
        if (value.equalsIgnoreCase("true"))
        {
            block.accept(OptionalBoolean.TRUE);
        }
        else if (value.equalsIgnoreCase("false"))
        {
            block.accept(OptionalBoolean.FALSE);
        }
        else
        {
            Colorwheel.LOGGER.error("Could not parse boolean: {}", value);
        }
    }

    private Optional<BlendMode> parseBlendMode(String value)
    {
        String[] modeStrs = value.split(" ");
        int[] modeFcts = new int[modeStrs.length];

        if (modeFcts.length != 4)
        {
            Colorwheel.LOGGER.error("Invalid blend function count: {} (expected 4)", modeFcts.length);
            return Optional.empty();
        }

        for (int i = 0; i < modeStrs.length; i++)
        {
            var maybeFct = BlendModeFunction.fromString(modeStrs[i]);

            if (maybeFct.isEmpty())
            {
                Colorwheel.LOGGER.error("Unknown blend function: {}", modeStrs[i]);
                continue;
            }

            modeFcts[i] = maybeFct.get().getGlId();
        }

        return Optional.of(new BlendMode(modeFcts[0], modeFcts[1], modeFcts[2], modeFcts[3]));
    }

    private Optional<int[]> parseCoefficientsRanks(String value)
    {
        String[] rankStrs = value.split(",");
        int[] ranks = new int[rankStrs.length];

        for (int i = 0; i < rankStrs.length; i++)
        {
            String rankStr = rankStrs[i].trim();
            int rank;

            try
            {
                rank = Integer.parseUnsignedInt(rankStr);
            }
            catch (NumberFormatException e)
            {
                Colorwheel.LOGGER.error("Invalid OIT coefficients ranks: {}", value);
                return Optional.empty();
            }

            if (rank == 0 || rank > 3)
            {
                Colorwheel.LOGGER.error("Invalid OIT coefficients ranks: {} (must be >= 1 && <= 3)", value);
                return Optional.empty();
            }

            ranks[i] = rank;
        }

        return Optional.of(ranks);
    }

    public Map<ClrwlProgramId, ClrwlBlendModeOverride> getBlendModeOverride()
    {
        return programBlendOverrides;
    }

    public Map<ClrwlProgramId, List<BufferBlendInformation>> getBufferBlendModeOverrides()
    {
        return bufferBlendOverrides;
    }

    public boolean shouldRenderShadow()
    {
        return shadowEnabled.orElse(true);
    }

    public boolean isOitEnabled(ClrwlProgramGroup group)
    {
        switch (group)
        {
            case GBUFFERS ->
            {
                return gbuffersOitEnabled.orElse(false);
            }
            case SHADOW ->
            {
                return shadowOitEnabled.orElse(false);
            }
        }

        throw new RuntimeException("Unknown program group: " + group);
    }

    public int[] getOitCoeffRanks(ClrwlProgramGroup group)
    {
        switch (group)
        {
            case GBUFFERS ->
            {
                return gbuffersOitCoeffRanks;
            }
            case SHADOW ->
            {
                return shadowOitCoeffRanks;
            }
        }

        throw new RuntimeException("Unknown program group: " + group);
    }

    public List<ClrwlOitAccumulateOverride> getOitAccumulateOverrides(ClrwlProgramGroup group)
    {
        switch (group)
        {
            case GBUFFERS ->
            {
                return gbuffersOitAccumulateOverrides;
            }
            case SHADOW ->
            {
                return shadowOitAccumulateOverrides;
            }
        }

        throw new RuntimeException("Unknown program group: " + group);
    }

    public ShadowCullState getShadowCullState()
    {
        return shadowCulling;
    }

    public OptionalBoolean getShadowOcclusionCulling()
    {
        return shadowOcclusionCulling;
    }

    public OptionalBoolean getFrustumCulling()
    {
        return frustumCulling;
    }

    public OptionalBoolean getOcclusionCulling()
    {
        return occlusionCulling;
    }
}
