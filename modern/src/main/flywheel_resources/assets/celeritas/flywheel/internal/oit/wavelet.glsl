// -------------------------------------------------------------------------
// WRITING
// -------------------------------------------------------------------------

void _clrwl_add_to_index(inout vec4[4] coefficients, int index, float addend)
{
    coefficients[index >> 2][index & 3] = addend;
}

void _clrwl_add_absorbance(inout vec4[4] coefficients, float signal, float depth, int rank)
{
    int count = 1 << (rank + 1);

    depth *= float(count-1) / count;

    int index = clamp(int(floor(depth * count)), 0, count - 1);
    index += count - 1;

    for (int i = 0; i < (rank+1); ++i) {
        int power = rank - i;
        float power_f = float(power);

        int new_index = (index - 1) >> 1;
        float k = float((new_index + 1) & ((1 << power) - 1));

        float wavelet_sign = float(((index & 1) << 1) - 1);
        float wavelet_phase = float((index + 1) & 1) * exp2(-power_f);
        float addend = fma(fma(-exp2(-power_f), k, depth), wavelet_sign, wavelet_phase) * exp2(power_f * 0.5) * signal;
        _clrwl_add_to_index(coefficients, new_index, addend);

        index = new_index;
    }

    float addend = fma(signal, -depth, signal);
    _clrwl_add_to_index(coefficients, count - 1, addend);
}

void _clrwl_add_transmittance(inout vec4[4] coefficients, float transmittance, float depth, int rank)
{
    float absorbance = -log(max(transmittance, 0.00001));// transforming the signal from multiplicative transmittance to additive absorbance
    _clrwl_add_absorbance(coefficients, absorbance, depth, rank);
}

// -------------------------------------------------------------------------
// READING
// -------------------------------------------------------------------------

// TODO: maybe we could reduce the number of texel fetches below?
float _clrwl_get_coefficients(in sampler2DArray coefficients, int index) {
    return texelFetch(coefficients, ivec3(gl_FragCoord.xy, index >> 2), 0)[index & 3];
}

/// Compute the total absorbance, as if at infinite depth.
float _clrwl_total_absorbance(in sampler2DArray coefficients, int rank)
{
    int count = 1 << (rank + 1);

    float scale_coefficient = _clrwl_get_coefficients(coefficients, count - 1);
    if (scale_coefficient == 0) {
        return 0;
    }

    int index_b = count - 1;

    index_b += count - 1;

    float b = scale_coefficient;

    for (int i = 0; i < (rank+1); ++i) {
        int power = rank - i;

        int new_index_b = (index_b - 1) >> 1;
        int wavelet_sign_b = ((index_b & 1) << 1) - 1;
        float coeff_b = _clrwl_get_coefficients(coefficients, new_index_b);
        b -= exp2(float(power) * 0.5) * coeff_b * wavelet_sign_b;
        index_b = new_index_b;
    }

    return b;
}

/// Compute the absorbance at a given normalized depth.
float _clrwl_absorbance(in sampler2DArray coefficients, float depth, int rank)
{
    int count = 1 << (rank + 1);

    float scale_coefficient = _clrwl_get_coefficients(coefficients, count - 1);
    if (scale_coefficient == 0) {
        return 0;
    }

    depth *= float(count-1) / count;

    float coefficient_depth = depth * count;
    int index_b = clamp(int(floor(coefficient_depth)), 0, count - 1);
    bool sample_a = index_b >= 1;
    int index_a = sample_a ? (index_b - 1) : index_b;

    index_b += count - 1;
    index_a += count - 1;

    float b = scale_coefficient;
    float a = sample_a ? scale_coefficient : 0;

    for (int i = 0; i < (rank+1); ++i) {
        int power = rank - i;

        int new_index_b = (index_b - 1) >> 1;
        int wavelet_sign_b = ((index_b & 1) << 1) - 1;
        float coeff_b = _clrwl_get_coefficients(coefficients, new_index_b);
        b -= exp2(float(power) * 0.5) * coeff_b * wavelet_sign_b;
        index_b = new_index_b;

        if (sample_a) {
            int new_index_a = (index_a - 1) >> 1;
            int wavelet_sign_a = ((index_a & 1) << 1) - 1;
            float coeff_a = (new_index_a == new_index_b) ? coeff_b : _clrwl_get_coefficients(coefficients, new_index_a);
            a -= exp2(float(power) * 0.5) * coeff_a * wavelet_sign_a;
            index_a = new_index_a;
        }
    }

    float t = coefficient_depth >= count ? 1.0 : fract(coefficient_depth);

    return mix(a, b, t);
}

/// Compute the absorbance at a given normalized depth,
/// correcting for self-occlusion by undoing the previously recorded absorbance event.
float _clrwl_signal_corrected_absorbance(in sampler2DArray coefficients, float depth, float signal, int rank)
{
    int count = 1 << (rank + 1);

    float scale_coefficient = _clrwl_get_coefficients(coefficients, count - 1);
    if (scale_coefficient == 0) {
        return 0;
    }

    depth *= float(count-1) / count;

    float scale_coefficient_addend = fma(signal, -depth, signal);
    scale_coefficient -= scale_coefficient_addend;

    float coefficient_depth = depth * count;
    int index_b = clamp(int(floor(coefficient_depth)), 0, count - 1);
    bool sample_a = index_b >= 1;
    int index_a = sample_a ? (index_b - 1) : index_b;

    index_b += count - 1;
    index_a += count - 1;

    float b = scale_coefficient;
    float a = sample_a ? scale_coefficient : 0;

    for (int i = 0; i < (rank+1); ++i) {
        int power = rank - i;
        float power_f = float(power);

        int new_index_b = (index_b - 1) >> 1;
        int wavelet_sign_b = ((index_b & 1) << 1) - 1;
        float coeff_b = _clrwl_get_coefficients(coefficients, new_index_b);

        float wavelet_phase_b = ((index_b + 1) & 1) * exp2(-power_f);
        float k = float((new_index_b + 1) & ((1 << power) - 1));
        float addend = fma(fma(-exp2(-power_f), k, depth), wavelet_sign_b, wavelet_phase_b) * exp2(power_f * 0.5) * signal;
        coeff_b -= addend;

        b -= exp2(power_f * 0.5) * coeff_b * wavelet_sign_b;
        index_b = new_index_b;

        if (sample_a) {
            int new_index_a = (index_a - 1) >> 1;
            int wavelet_sign_a = ((index_a & 1) << 1) - 1;
            float coeff_a = (new_index_a == new_index_b) ? coeff_b : _clrwl_get_coefficients(coefficients, new_index_a);// No addend here on purpose, the original signal didn't contribute to this coefficient
            a -= exp2(power_f * 0.5) * coeff_a * wavelet_sign_a;
            index_a = new_index_a;
        }
    }

    float t = coefficient_depth >= count ? 1.0 : fract(coefficient_depth);

    return mix(a, b, t);
}

// Helpers below to deal directly in transmittance.

#define _CRLWL_ABSORBANCE_TO_TRANSMITTANCE(a) clamp(exp(-(a)), 0., 1.)

float _clrwl_total_transmittance(in sampler2DArray coefficients, int rank)
{
    return _CRLWL_ABSORBANCE_TO_TRANSMITTANCE(_clrwl_total_absorbance(coefficients, rank));
}

float _clrwl_transmittance(in sampler2DArray coefficients, float depth, int rank)
{
    return _CRLWL_ABSORBANCE_TO_TRANSMITTANCE(_clrwl_absorbance(coefficients, depth, rank));
}

float _clrwl_signal_corrected_transmittance(in sampler2DArray coefficients, float depth, float signal, int rank)
{
    return _CRLWL_ABSORBANCE_TO_TRANSMITTANCE(_clrwl_signal_corrected_absorbance(coefficients, depth, signal, rank));
}
