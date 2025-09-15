package net.irisshaders.iris.gl.state;

/**
 * A
 */
public interface ValueUpdateNotifier {
    ValueUpdateNotifier NONE = listener -> {};

	/**
	 * Sets up a listener with this notifier.
	 */
	void setListener(Runnable listener);
}
