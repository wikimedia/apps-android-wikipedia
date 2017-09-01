package org.wikipedia.html;

import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;

import java.util.Locale;

public class PixelDensityDescriptorParser {
    /** @throws ParseException */
    @NonNull public PixelDensityDescriptor parse(@NonNull String descriptor) {
        Float density = parseDensity(descriptor);
        return new PixelDensityDescriptor(density);
    }

    /** @throws ParseException */
    @FloatRange(from = 0, fromInclusive = false) private float parseDensity(@NonNull String descriptor) {
        String descriptorLowercase = descriptor.toLowerCase(Locale.ENGLISH);
        if (descriptorLowercase.contains("x")) {
            float density;
            try {
                density = Float.parseFloat(descriptorLowercase.replaceFirst("x", ""));
            } catch (NumberFormatException e) {
                throw new ParseException(e);
            }

            if (density <= 0) {
                throw new ParseException("Density must be positive");
            }

            return density;
        }

        throw new ParseException("Pixel density descriptor not present");
    }
}
