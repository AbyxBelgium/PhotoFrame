package be.abyx.photoframe;

/**
 * This file contains some functions that convert values between different units.
 *
 * @author Pieter Verschaffelt
 */
public class UnitConverter {
    public static float convertFahrenheitToCelsius(float fahrenheit) {
        return (fahrenheit - 32) * ((float) 5 / (float) 9);
    }

    public static long convertInchHGToHpa(float inchHg) {
        return Math.round(inchHg * 33.86389);
    }
}
