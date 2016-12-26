package be.abyx.photoframe;

/**
 * Data class that contains weather conditions for a specific time.
 *
 * @author Pieter Verschaffelt
 */
public class WeatherData {
    private float temperature;
    private int humidity;
    private long airPressure;

    public WeatherData(float temperature, int humidity, long airPressure) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.airPressure = airPressure;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public long getAirPressure() {
        return airPressure;
    }

    public void setAirPressure(long airPressure) {
        this.airPressure = airPressure;
    }
}
