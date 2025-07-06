package net.irisshaders.iris.shaderpack.programs;

import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.embeddedt.embeddium.impl.gl.shader.ShaderType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class ProgramSource {
	private final String name;
    private final Map<ShaderType, String> sources = new EnumMap<>(ShaderType.class);
    private final Map<ShaderType, String> sourcesView = Collections.unmodifiableMap(sources);
	private final ProgramDirectives directives;
	private final ProgramSet parent;

	public ProgramSource(String name, Map<ShaderType, String> sources,
						 ProgramSet parent, ShaderProperties properties, BlendModeOverride defaultBlendModeOverride) {
		this.name = name;
        for (var entry : sources.entrySet()) {
            if (entry.getValue() != null) {
                this.sources.put(entry.getKey(), entry.getValue());
            }
        }
		this.parent = parent;
		this.directives = new ProgramDirectives(this, properties,
			PackRenderTargetDirectives.BASELINE_SUPPORTED_RENDER_TARGETS, defaultBlendModeOverride);
	}

	public String getName() {
		return name;
	}

    public Optional<String> getSource(ShaderType type) {
        return Optional.ofNullable(getSourceNullable(type));
    }

    public String getSourceNullable(ShaderType type) {
        return sources.get(type);
    }

    public Map<ShaderType, String> getSourcesMap() {
        return sourcesView;
    }

	public ProgramDirectives getDirectives() {
		return this.directives;
	}

	public ProgramSet getParent() {
		return parent;
	}

	public boolean isValid() {
		return sources.containsKey(ShaderType.VERTEX) && sources.containsKey(ShaderType.FRAGMENT);
	}

	public Optional<ProgramSource> requireValid() {
		if (this.isValid()) {
			return Optional.of(this);
		} else {
			return Optional.empty();
		}
	}
}
