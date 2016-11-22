package no.holger;

public class Ray {
    public Vector origin;
    public Vector direction;

    public Ray(Vector origin, Vector direction) {
        this.origin = origin.clone();
        this.direction = direction.clone().normalize();
    }

    public Vector intersection(Ray other) {
        Vector as = this.origin.clone();
        Vector bs = other.origin.clone();
        Vector ad = this.direction.clone();
        Vector bd = other.direction.clone();

        // http://stackoverflow.com/questions/2931573/determining-if-two-rays-intersect
        //Double u = (as.y*bd.x + bd.y*bs.x - bs.y*bd.x - bd.y*as.x ) / (ad.x*bd.y - ad.y*bd.x);
        //Double v = (as.x + ad.x * u - bs.x) / bd.x;

        Double dx = bs.x - as.x;
        Double dy = bs.y - as.y;
        Double det = bd.x * ad.y - bd.y * ad.x;
        Double u = (dy * bd.x - dx * bd.y) / det;
        Double v = (dy * ad.x - dx * ad.y) / det;

        // u > 0 && v > 0 => intersection
        System.out.println("U: " + u + " V: " + v);
        if (u >= -0 && v >= -0)
            return as.add(ad.multiply(u));
        else {
            return null;
        }
    }

    public String toString() {
        return "origin: " + origin.toString() + " direction: " + direction.toString();
    }
}
