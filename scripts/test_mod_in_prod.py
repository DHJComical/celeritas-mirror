#!/usr/bin/env python3

import os
import sys
from pathlib import Path

from portablemc.fabric import FabricVersion
from portablemc.forge import ForgeVersion, _NeoForgeVersion
from portablemc.standard import Context, StreamRunner, Version, XmlStreamEvent

failed = False

class MyRunner(StreamRunner):
    def process_stream_event(self, event) -> None:
        if isinstance(event, XmlStreamEvent):
            print(event.message)
            if event.throwable is not None:
                print(event.throwable)
            throwable_str = str(event.throwable)
            if "Could not force-load" in event.message or "Mixin apply failed" in event.message or "MixinTransformerError" in throwable_str or "MixinInitialisationError" in throwable_str:
                global failed
                failed = True

main_dir = Path(os.path.realpath(__file__)).parent.parent / ".gradle/minecraft_prod"
work_dir = sys.argv[1]
mc_version = sys.argv[2]
loader = sys.argv[3]
loader_version = sys.argv[4]

context = Context(Path(main_dir), Path(work_dir))

if loader == "fabric":
    version = FabricVersion.with_fabric(mc_version, loader_version, context=context)
elif loader == "forge":
    version = ForgeVersion(loader_version, context=context)
elif loader == "neoforge":
    version = _NeoForgeVersion(loader_version, context=context)
else:
    raise ValueError("unknown loader: " + loader)

version.fixes[Version.FIX_LWJGL] = "3.3.2"
environment = version.install()
environment.jvm_args.append("-Dembeddium.auditAndExit=true")
environment.jvm_args.append("-XX:TieredStopAtLevel=1")

environment.run(MyRunner())

sys.exit(1 if failed else 0)
