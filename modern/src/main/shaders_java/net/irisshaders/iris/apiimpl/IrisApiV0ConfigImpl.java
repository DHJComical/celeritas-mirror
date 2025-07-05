package net.irisshaders.iris.apiimpl;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.api.v0.IrisApiConfig;
import net.irisshaders.iris.config.IrisConfig;

import java.io.IOException;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;

public class IrisApiV0ConfigImpl implements IrisApiConfig {
	@Override
	public boolean areShadersEnabled() {
		return IrisCommon.getIrisConfig().areShadersEnabled();
	}

	@Override
	public void setShadersEnabledAndApply(boolean enabled) {
		IrisConfig config = IrisCommon.getIrisConfig();

		config.setShadersEnabled(enabled);

		try {
			config.save();
		} catch (IOException e) {
			IRIS_LOGGER.error("Error saving configuration file!", e);
		}

		try {
			Iris.reload();
		} catch (IOException e) {
			IRIS_LOGGER.error("Error reloading shader pack while applying changes!", e);
		}
	}
}
