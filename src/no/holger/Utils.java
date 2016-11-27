package no.holger;

public class Utils {
    public static Double clamp(Double value, Double min, Double max) {
        return value > min ? (value < max ? value : max) : min;
    }
}
