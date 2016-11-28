package no.holger;

import java.awt.*;

public class Vector {
    double x;
    double y;

    public Vector(Double x, Double y) {
        this.x = x;
        this.y = y;
    }

    public Vector(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Vector(double radiansFromUp) {
        double radiansFromRight = -radiansFromUp + Math.PI/2;
        this.x = Math.cos(radiansFromRight);
        this.y = Math.sin(radiansFromRight);
    }

    @Override
    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }

    public Vector rotateLeft(double n) {
        double rx = (this.x * Math.cos(n)) - (this.y * Math.sin(n));
        double ry = (this.x * Math.sin(n)) + (this.y * Math.cos(n));
        x = rx;
        y = ry;
        return this;
    }

    public Vector add(Vector other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    public Vector sub(Vector other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    public Double distanceTo(Vector other) {
        return other.clone().sub(this).length();
    }

    public Double length() {
        return Math.sqrt(x*x + y*y);
    }

    public Vector normalize() {
        return multiply(1/length());
    }

    public Vector clone() {
        return new Vector(x, y);
    }

    public Vector multiply(double n) {
        this.x *= n;
        this.y *= n;
        return this;
    }

    public void draw(Graphics2D g, Color color) {
        g.setColor(color);
        Integer radius = 20;
        g.fillOval((int) x - radius/2, (int) y - radius/2, radius, radius);
    }

    public void draw(Graphics2D g) {
        draw(g, java.awt.Color.RED);
    }

    public void drawLine(Vector endVector, Graphics2D g) {
        drawLine(endVector, g, Color.RED);
    }

    public void drawLine(Vector endVector, Graphics2D g, Color color) {
        g.setColor(color);
        g.drawLine(
                (int) x,
                (int) y,
                (int) endVector.x,
                (int) endVector.y
        );
    }

    public boolean isInsideBox(Double x0, Double y0, Double x1, Double y1) {
        return x >= x0 && x <= x1 && y >= y0 && y <= y1;
    }

    public Double angleTo(Vector other) {
        Vector thisNormalized = this.clone().normalize();
        Vector otherNormalized = other.clone().normalize();
        // http://www.euclideanspace.com/maths/algebra/vectors/angleBetween/
        return Math.atan2(thisNormalized.y, thisNormalized.x) - Math.atan2(otherNormalized.y, otherNormalized.x);
    }
}
