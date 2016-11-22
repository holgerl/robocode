package no.holger;

public class Utils {
    public static Double clamp(Double x, Double min, Double max) {
        return x < min ? (x > max ? max : x) : min;
    }
}
