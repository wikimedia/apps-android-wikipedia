package org.wikipedia.test.theories;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import java.util.Arrays;
import java.util.List;

public class TestedOnBoolSupplier extends ParameterSupplier {
    private static final String NAME = "bools";

    @Override public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
        return Arrays.asList(PotentialAssignment.forValue(NAME, false),
                PotentialAssignment.forValue(NAME, true));
    }
}
