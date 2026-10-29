package net.irisshaders.iris;

import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import org.embeddedt.embeddium.impl.util.ResourceLocationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds the shader pack and pipeline status to the debug screen.
 */
public final class IrisDebugEntries {
	private IrisDebugEntries() {
	}

	public static void register(RegisterDebugEntriesEvent event) {
		Identifier id = ResourceLocationUtil.make("iris", "shaders");

		event.register(id, (displayer, level, clientChunk, serverChunk) -> {
			List<String> lines = new ArrayList<>();

			if (Iris.getIrisConfig().areShadersEnabled()) {
				lines.add("Shaderpack: " + Iris.getCurrentPackName() + (Iris.isFallback() ? " (fallback)" : ""));
				Iris.getCurrentPack().ifPresent(pack -> lines.add(pack.getProfileInfo()));
				lines.add("Color space: " + IrisVideoSettings.colorSpace.name());
			}

			Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.addDebugText(lines));

			displayer.addToGroup(id, lines);
		});
		event.includeInProfile(id, DebugScreenProfile.DEFAULT, DebugScreenEntryStatus.IN_OVERLAY);
	}
}
