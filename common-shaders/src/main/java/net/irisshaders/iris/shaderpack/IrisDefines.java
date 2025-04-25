package net.irisshaders.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.parsing.BiomeCategories;
import org.taumc.celeritas.CeleritasShaderVersionService;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class IrisDefines {
	private static final Pattern SEMVER_PATTERN = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.*(?<bugfix>\\d*)(.*)");

	private static void define(List<StringPair> defines, String key) {
		defines.add(new StringPair(key, ""));
	}

	private static void define(List<StringPair> defines, String key, String value) {
		defines.add(new StringPair(key, value));
	}

	public static ImmutableList<StringPair> createIrisReplacements() {
		ArrayList<StringPair> s = new ArrayList<>(StandardMacros.createStandardEnvironmentDefines());

        CeleritasShaderVersionService.INSTANCE.processBiomeMap((key, value) -> define(s, key, value));

		BiomeCategories[] categories = BiomeCategories.values();
		for (int i = 0; i < categories.length; i++) {
			define(s, "CAT_" + categories[i].name().toUpperCase(Locale.ROOT), String.valueOf(i));
		}

		return ImmutableList.copyOf(s);
	}
}
