package net.irisshaders.iris.compat.flywheel.engine;

public record ClrwlInstanceVisual(Type type, int irisId, int lightEmission)
{
    // shaders.properties is incorrect: the default value when entry is missing is -1
    private static final int UNDEFINED_ID = -1;
    private static final ClrwlInstanceVisual UNDEFINED = new ClrwlInstanceVisual(Type.UNDEFINED, UNDEFINED_ID, 0);

    public static ClrwlInstanceVisual undefined()
    {
        return UNDEFINED;
    }

    public static ClrwlInstanceVisual blockEntity(int irisId, int lightEmission)
    {
        return new ClrwlInstanceVisual(Type.BLOCKENTITY, irisId, lightEmission);
    }

    public static ClrwlInstanceVisual entity(int irisId)
    {
        return new ClrwlInstanceVisual(Type.ENTITY, irisId, 0);
    }

    public int getBlockEntity()
    {
        if (type == Type.BLOCKENTITY)
        {
            return irisId;
        }
        return UNDEFINED_ID;
    }

    public int getEntity()
    {
        if (type == Type.ENTITY)
        {
            return irisId;
        }

        return UNDEFINED_ID;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof ClrwlInstanceVisual rhs)
        {
            switch (type)
            {
                case UNDEFINED ->
                {
                    return rhs.type == Type.UNDEFINED;
                }
                case BLOCKENTITY ->
                {
                    return rhs.type == Type.BLOCKENTITY && rhs.irisId == this.irisId;
                }
                case ENTITY ->
                {
                    return rhs.type == Type.ENTITY && rhs.irisId == this.irisId;
                }
            }
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        int prime = 31;
        int result = 1;

        result = prime * result + this.type.hashCode();

        if (type != Type.UNDEFINED)
        {
            result = prime * result + this.irisId;
        }

        return result;
    }

    public enum Type
    {
        UNDEFINED,
        BLOCKENTITY,
        ENTITY
    }
}
