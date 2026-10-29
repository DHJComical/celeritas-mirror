package net.irisshaders.iris.compat.flywheel.shaderpack;

public enum ClrwlProgramGroup
{
    GBUFFERS("gbuffers"),
    SHADOW("shadow");

    private final String name;

    ClrwlProgramGroup(String name)
    {
        this.name = name;
    }

    public String groupName()
    {
        return name;
    }

    public static ClrwlProgramGroup fromShadow(boolean isShadow)
    {
        return isShadow ? SHADOW : GBUFFERS;
    }
}
