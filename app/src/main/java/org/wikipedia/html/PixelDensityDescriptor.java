package org.wikipedia.html;

import androidx.annotation.FloatRange;

import org.wikipedia.model.BaseModel;

public class PixelDensityDescriptor extends BaseModel {
    @FloatRange(from = 0, fromInclusive = false) private final float density;

    public PixelDensityDescriptor(@FloatRange(from = 0, fromInclusive = false) float density) {
        this.density = density;
    }

    @FloatRange(from = 0, fromInclusive = false) public float density() {
        return density;
    }
}
