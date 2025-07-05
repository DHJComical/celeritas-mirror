package net.irisshaders.iris.api.v0;

import org.taumc.celeritas.api.v0.CeleritasShadersApi;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;

/**
 * The entry point to the Iris API, major version 0. This is currently the latest
 * version of the API.
 * <p>
 * To access the API, use {@link #getInstance()}.
 */
public interface IrisApi extends CeleritasShadersApi {
	/**
	 * @since API v0.0
	 */
	static IrisApi getInstance() {
		return (IrisApi)CeleritasShadersApi.getInstance();
	}

	/**
	 * Gets the minor revision of this API. This is incremented when
	 * new methods are added without breaking API. Mods can check this
	 * if they wish to check whether given API calls are available on
	 * the currently installed Iris version.
	 *
	 * @return The current minor revision. Currently, revision 2.
	 */
	int getMinorApiRevision();

	/**
	 * Gets a text vertex sink to render into.
	 *
	 * @param maxQuadCount   Maximum amount of quads that will be rendered with this sink
	 * @param bufferProvider An IntFunction that can provide a {@code ByteBuffer} with at minimum the bytes provided by the input parameter
	 * @since API 0.1
	 */
	IrisTextVertexSink createTextVertexSink(int maxQuadCount, IntFunction<ByteBuffer> bufferProvider);
}
