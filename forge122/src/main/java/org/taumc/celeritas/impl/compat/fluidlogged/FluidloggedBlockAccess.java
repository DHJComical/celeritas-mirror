package org.taumc.celeritas.impl.compat.fluidlogged;

import git.jbredwards.fluidlogged_api.api.world.IFluidStateProvider;
import git.jbredwards.fluidlogged_api.api.world.IWorldProvider;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.Optional;

@Optional.InterfaceList({
        @Optional.Interface(modid = FluidloggedCompat.MODID, iface = "git.jbredwards.fluidlogged_api.api.world.IFluidStateProvider"),
        @Optional.Interface(modid = FluidloggedCompat.MODID, iface = "git.jbredwards.fluidlogged_api.api.world.IWorldProvider")
})
public interface FluidloggedBlockAccess extends IBlockAccess, IFluidStateProvider, IWorldProvider {
}
