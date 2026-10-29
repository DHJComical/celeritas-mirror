package net.irisshaders.iris.compat.flywheel.compile.core;

import net.irisshaders.iris.compat.flywheel.indirect.ClrwlBufferBindings;
import net.irisshaders.iris.compat.flywheel.gl.ClrwlGlShader;
import net.irisshaders.iris.compat.flywheel.gl.ClrwlShaderType;
import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.compile.core.Compilation;
import dev.engine_room.flywheel.backend.compile.core.FailedCompilation;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.SourceFile;
import dev.engine_room.flywheel.lib.util.StringUtil;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.helpers.StringPair;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder style class for compiling shaders.
 * <p>
 * Keeps track of the source files and components used to compile a shader,
 * and interprets/pretty prints any errors that occur.
 */
public class ClrwlCompilation
{
	private final List<SourceFile> files = new ArrayList<>();
	private final StringBuilder generatedSource = new StringBuilder();
	private final StringBuilder fullSource = new StringBuilder();
	private int generatedLines = 0;

	public ClrwlShaderResult compile(ClrwlShaderType shaderType, String name)
	{
		int handle = GL20.glCreateShader(shaderType.glEnum);
		var source = fullSource.toString();

		GlCompat.safeShaderSource(handle, source);
		GL20.glCompileShader(handle);

		var shaderName = name + "." + shaderType.extension;
		dumpSource(source, shaderName);

		var infoLog = GL20.glGetShaderInfoLog(handle);

		if (compiledSuccessfully(handle))
		{
			return ClrwlShaderResult.success(new ClrwlGlShader(handle, shaderType, shaderName), infoLog);
		}

		GL20.glDeleteShader(handle);
		return ClrwlShaderResult.failure(new FailedCompilation(shaderName, files, generatedSource.toString(), source, infoLog));
	}

	public void version(GlslVersion version)
	{
		fullSource.append("#version ")
				.append(version.version)
				.append('\n');
		define("_CLRWL_SSBO_BASE", Integer.toString(ClrwlBufferBindings.base()));
	}

	public void enableExtension(String ext)
	{
		fullSource.append("#extension ")
				.append(ext)
				.append(" : enable\n");
	}

	public void requireExtension(String ext)
	{
		fullSource.append("#extension ")
				.append(ext)
				.append(" : require\n");
	}

	public void define(StringPair pair)
	{
		define(pair.key(), pair.value());
	}

	public void define(String key, String value)
	{
		fullSource.append("#define ")
				.append(key)
				.append(' ')
				.append(value)
				.append('\n');
	}

	public void define(String key)
	{
		fullSource.append("#define ")
				.append(key)
				.append('\n');
	}

	public void appendComponent(SourceComponent component)
	{
		var source = component.source();

		appendHeader(component, source);

		fullSource.append(source);
	}

	private void appendHeader(SourceComponent component, String source)
	{
		if (component instanceof SourceFile file) {
			int fileId = files.size() + 1;

			files.add(file);

			fullSource.append("\n#line 0 ")
					.append(fileId)
					.append(" // ")
					.append(file.name())
					.append('\n');
		} else {
			// Add extra newline to keep line numbers consistent
			generatedSource.append(source)
					.append('\n');

			fullSource.append("\n#line ")
					.append(generatedLines)
					.append(" 0 // (generated) ") // all generated code is put in file 0
					.append(component.name())
					.append('\n');

			generatedLines += StringUtil.countLines(source);
		}
	}

	private static void dumpSource(String source, String fileName)
	{
		if (!(Compilation.DUMP_SHADER_SOURCE || Iris.getIrisConfig().areDebugOptionsEnabled()))
		{
			return;
		}

		File file = new File(new File(Minecraft.getInstance().gameDirectory, "colorwheel_sources"), fileName);
		// mkdirs of the parent so we don't create a directory named by the leaf file we want to write
		file.getParentFile()
				.mkdirs();
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(source);
		} catch (Exception e) {
			FlwPrograms.LOGGER.error("Could not dump source.", e);
		}
	}

	public static boolean compiledSuccessfully(int handle)
	{
		return GL20.glGetShaderi(handle, GL20.GL_COMPILE_STATUS) == GL20.GL_TRUE;
	}
}
