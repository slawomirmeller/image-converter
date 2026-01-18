package com.imageconverter;

/**
 * Launcher class for jpackage.
 * This is required because JavaFX Application classes cannot be directly launched by jpackage.
 */
public class Launcher {
    public static void main(String[] args) {
        ImageConverterApp.main(args);
    }
}
