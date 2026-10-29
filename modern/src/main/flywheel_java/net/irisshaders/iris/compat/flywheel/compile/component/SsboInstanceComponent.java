package net.irisshaders.iris.compat.flywheel.compile.component;

import net.irisshaders.iris.compat.flywheel.Colorwheel;
import net.irisshaders.iris.compat.flywheel.indirect.ClrwlBufferBindings;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.backend.compile.component.InstanceAssemblerComponent;
import dev.engine_room.flywheel.backend.glsl.generate.*;
import dev.engine_room.flywheel.lib.math.MoreMath;

import java.util.ArrayList;

// https://github.com/Engine-Room/Flywheel/blob/b9fbb6fb0cdda7e35b75bd3d0aff6e50004b6dbd/common/src/backend/java/dev/engine_room/flywheel/backend/compile/component/SsboInstanceComponent.java#L15

public class SsboInstanceComponent extends InstanceAssemblerComponent
{
	public SsboInstanceComponent(InstanceType<?> type) {
		super(type);
	}

	@Override
	public String name()
	{
		return Colorwheel.rl("ssbo_instance_assembler").toString();
	}

	@Override
	protected void generateUnpacking(GlslBuilder builder)
	{
		var fnBody = new GlslBlock();

		int uintCount = MoreMath.ceilingDiv(layout.byteSize(), 4);

		fnBody.add(GlslStmt.raw("uint base = " + UNPACK_ARG + " * " + uintCount + "u;"));

		for (int i = 0; i < uintCount; i++)
		{
			// Retrieve all the uints for the given instance ahead of time to simplify the unpacking generators.
			fnBody.add(GlslStmt.raw("uint u" + i + " = _flw_instances[base + " + i + "u];"));
		}

		var unpackArgs = new ArrayList<GlslExpr>();
		for (Layout.Element element : layout.elements())
		{
			unpackArgs.add(unpackElement(element));
		}

		fnBody.ret(GlslExpr.call(STRUCT_NAME, unpackArgs));

		builder._raw("layout(std430, binding = " + (ClrwlBufferBindings.base() + ClrwlBufferBindings.INSTANCE) + ") restrict readonly buffer InstanceBuffer {\n"
				+ "    uint _flw_instances[];\n"
				+ "};");
		builder.blankLine();
		builder.function()
				.signature(FnSignature.create()
						.returnType(STRUCT_NAME)
						.name(UNPACK_FN_NAME)
						.arg("uint", UNPACK_ARG)
						.build())
				.body(fnBody);
	}

	@Override
	protected GlslExpr access(int uintOffset)
	{
		return GlslExpr.variable("u" + uintOffset);
	}
}
