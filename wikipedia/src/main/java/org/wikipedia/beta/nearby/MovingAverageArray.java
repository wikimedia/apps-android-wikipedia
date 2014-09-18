package org.wikipedia.beta.nearby;

import java.util.Arrays;

/**
 * Implements an array of numbers that automatically keeps a moving average of each element.
 * Serves as a low-pass filter, useful for reducing jitter from sensor data, among other things.
 */
public class MovingAverageArray {
    private float[] curData;
    private float[][] maData;
    private int maSize;
    private int maIndex = 0;
    private boolean initialized = false;

    public MovingAverageArray(int length, int maSize) {
        this.maSize = maSize;
        curData = new float[length];
        maData = new float[maSize][length];
    }

    public void addData(float[] data) {
        if (!initialized) {
            initialized = true;
            for (int i = 0; i < maSize; i++) {
                System.arraycopy(data, 0, maData[i], 0, data.length);
            }
        }
        System.arraycopy(data, 0, maData[maIndex++], 0, data.length);
        if (maIndex >= maSize) {
            maIndex = 0;
        }
        Arrays.fill(curData, 0);
        for (int i = 0; i < maSize; i++) {
            for (int j = 0; j < curData.length; j++) {
                curData[j] += maData[i][j];
            }
        }
        for (int i = 0; i < curData.length; i++) {
            curData[i] /= maSize;
        }
    }

    public float[] getData() {
        return curData;
    }
}
