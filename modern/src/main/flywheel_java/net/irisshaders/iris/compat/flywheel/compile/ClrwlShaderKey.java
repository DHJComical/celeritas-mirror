package net.irisshaders.iris.compat.flywheel.compile;

import net.irisshaders.iris.compat.flywheel.engine.uniform.ClrwlFrameUniforms;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.material.*;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.lib.util.ResourceUtil;

public record ClrwlShaderKey(InstanceType<?> instanceType,
                             MaterialShaders material,
                             FogShader fog,
                             CutoutShader cutout,
                             LightShader light,
                             Transparency transparency,
                             ContextShader context,
                             boolean isShadow,
                             boolean isDebugEnabled,
                             ClrwlPrograms.OitMode oit)
{
    public static ClrwlShaderKey fromMaterial(InstanceType<?> instanceType, Material material, ContextShader context, boolean isShadow, ClrwlPrograms.OitMode oit)
    {
        return new ClrwlShaderKey(instanceType, material.shaders(), material.fog(), material.cutout(), material.light(), material.transparency(), context, isShadow, ClrwlFrameUniforms.debugOn(), oit);
    }

    public String getPath()
    {
        var instanceName = ResourceUtil.toDebugFileNameNoExtension(instanceType.vertexShader());
        var materialName = ResourceUtil.toDebugFileNameNoExtension(material.vertexSource());
        var contextName = context.nameLowerCase();
        var debug = isDebugEnabled ? "_debug" : "";

        return instanceName + '/' + materialName + '_' + contextName + oit.name + debug;
    }
}
