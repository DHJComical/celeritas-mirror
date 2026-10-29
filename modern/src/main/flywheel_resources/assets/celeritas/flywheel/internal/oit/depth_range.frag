out vec2 _flw_depthRange_out;

void _clrwl_post_shader()
{
    float linearDepth = _clrwl_linear_depth();

    // Pad the depth by some unbalanced epsilons because minecraft has a lot of single-quad tranparency.
    // The unbalance means our fragment will be considered closer to the screen in the normalization,
    // which helps prevent unnecessary noise as it'll be closer to the edge of our tent function.

    _flw_depthRange_out = vec2(-linearDepth + 1e-5, linearDepth + 1e-2);
}
