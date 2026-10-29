package net.irisshaders.iris.shaderpack.preprocessor;

import net.irisshaders.iris.helpers.StringPair;
import org.anarres.cpp.Feature;
import org.anarres.cpp.LexerException;
import org.anarres.cpp.Preprocessor;
import org.anarres.cpp.StringLexerSource;
import org.anarres.cpp.Token;

public class JcppProcessor {
	// Derived from GlShader from Canvas, licenced under LGPL
	public static String glslPreprocessSource(String source, Iterable<StringPair> environmentDefines) {
		if (source.contains(GlslCollectingListener.VERSION_MARKER)
			|| source.contains(GlslCollectingListener.EXTENSION_MARKER)) {
			throw new RuntimeException("Some shader author is trying to exploit internal Iris implementation details, stop!");
		}

		// Note: This is an absolutely awful hack. But JCPP's lack of extensibility leaves me with no choice...
		//       We should write our own preprocessor at some point to avoid this.
		//
		// Why are we doing this awful hack instead of just using the preprocessor like a normal person? Because it lets
		// us only hoist #extension directives if they're actually used. This is needed for shader packs written on
		// lenient drivers that allow #extension directives to be placed anywhere to work on strict drivers like Mesa
		// that require #extension directives to occur at the top.
		//
		// TODO: This allows #version to not appear as the first non-comment non-whitespace thing in the file.
		//       That's not the behavior we want. If you're reading this, don't rely on this behavior.
		source = hideDirectivesAndNulls(source);

		GlslCollectingListener listener = new GlslCollectingListener();

		@SuppressWarnings("resource") final Preprocessor pp = new Preprocessor();

		// Add the values of the environment defines without actually modifying the source code
		// of the shader program, one step down the road of having accurate line number reporting
		// in errors...
		try {
			for (StringPair envDefine : environmentDefines) {
				pp.addMacro(envDefine.key(), envDefine.value());
			}
		} catch (LexerException e) {
			throw new RuntimeException("Unexpected LexerException processing macros", e);
		}

		pp.setListener(listener);
		pp.addInput(new StringLexerSource(source, true));
		pp.addFeature(Feature.KEEPCOMMENTS);

		final StringBuilder builder = new StringBuilder();

		try {
			for (; ; ) {
				final Token tok = pp.token();
				if (tok == null) break;
				if (tok.getType() == Token.EOF) break;
				builder.append(tok.getText());
			}
		} catch (final Exception e) {
			throw new RuntimeException("GLSL source pre-processing failed", e);
		}

		builder.append("\n");

		source = listener.collectLines() + builder;

		return source;
	}

	/**
	 * Replaces #version and #extension with their markers, then removes null characters, which some packs (such as
	 * Chocapic High Performance) contain and which trip up JCPP. Done in one pass that jumps between '#' and null
	 * characters, because String.replace checks every character one by one once the source contains anything
	 * outside Latin-1, and on large packs that took longer than the preprocessing itself.
	 */
	private static String hideDirectivesAndNulls(String source) {
		StringBuilder builder = new StringBuilder(source.length() + 256);
		int copiedUpTo = 0;
		int hash = source.indexOf('#');
		int nul = source.indexOf('\u0000');

		while (hash >= 0 || nul >= 0) {
			if (nul >= 0 && (hash < 0 || nul < hash)) {
				builder.append(source, copiedUpTo, nul);
				copiedUpTo = nul + 1;
				nul = source.indexOf('\u0000', copiedUpTo);
			} else {
				if (source.startsWith("#version", hash)) {
					builder.append(source, copiedUpTo, hash).append(GlslCollectingListener.VERSION_MARKER);
					copiedUpTo = hash + "#version".length();
				} else if (source.startsWith("#extension", hash)) {
					builder.append(source, copiedUpTo, hash).append(GlslCollectingListener.EXTENSION_MARKER);
					copiedUpTo = hash + "#extension".length();
				}
				hash = source.indexOf('#', hash + 1);
			}
		}

		return builder.append(source, copiedUpTo, source.length()).toString();
	}
}
