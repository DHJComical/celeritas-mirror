FlwMaterial flw_material;

void _clrwl_main()
{
    #ifndef _CLRWL_IS_FALLBACK
        if (flw_material.useOverlay)
        {
            clrwl_overlayColor = texelFetch(flw_overlayTex, clrwl_in[0].flw_vertexOverlay, 0);
            clrwl_overlayColor.a = 1.0 - clrwl_overlayColor.a;
        }
    #else
        // Do the average as a best effort attempt
        clrwl_overlayColor = (clrwl_in[0].clrwl_overlayColor + clrwl_in[1].clrwl_overlayColor + clrwl_in[2].clrwl_overlayColor) / 3;
        clrwl_out.clrwl_overlayColor = clrwl_overlayColor;
    #endif

    _clrwl_shader_main();
}
