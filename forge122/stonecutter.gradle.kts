plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.12.2"

stonecutter.parameters {
    replacement(eval(metadata.version, ">1.10.2"), "VertexBuffer", "BufferBuilder")
}