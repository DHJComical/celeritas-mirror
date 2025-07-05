plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.12.2"

stonecutter.parameters {
    replacement(eval(current.version, ">1.10.2"), "net.minecraft.client.renderer.VertexBuffer", "net.minecraft.client.renderer.BufferBuilder")
}