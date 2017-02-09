package org.wikipedia.test;

import android.support.annotation.NonNull;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** A {@link RobolectricTestRunner} with support for loading Gradle properties. */
public class TestRunner extends RobolectricTestRunner {
    private static final String GRADLE_PROPERTIES_FILENAME = "gradle.properties";

    public TestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        initProperties();
    }

    private void initProperties() {
        File propertiesFile = new File(GRADLE_PROPERTIES_FILENAME);
        if (propertiesFile.isFile()) {
            setProperties(propertiesFile);
        }
    }

    private void setProperties(@NonNull File file) {
        setProperties(readProperties(file));
    }

    private void setProperties(@NonNull Properties properties) {
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            System.setProperty(name, value);
        }
    }

    @NonNull
    private Properties readProperties(@NonNull File file) {
        try {
            return readProperties(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private Properties readProperties(@NonNull InputStream inputStream) {
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }
}
