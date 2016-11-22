package no.holger;

import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.util.Arrays;

public class PWNBOT4000 extends AdvancedRobot {

    Vector leftIntersection;
    Vector rightIntersection;
    Long timeLastTurn = 0L;

    public void run() {
        Colors.applyColors(this);

        while (true) {
            if (System.currentTimeMillis() - timeLastTurn > 500) {
                timeLastTurn = System.currentTimeMillis();
                calculateIntersections();
                turnToClosestIntersection();
            }

            execute();

            setDebugProperty("headingVector", new Vector(getHeadingRadians()).toString());
            setDebugProperty("headingVectorLeft", new Vector(getHeadingRadians()).rotateLeft(Math.PI / 180 * 20).toString());
            setDebugProperty("headingVectorRight", new Vector(getHeadingRadians()).rotateLeft(- Math.PI / 180 * 20).toString());
        }
    }

    private void turnToClosestIntersection() {
        Vector position = new Vector(getX(), getY());
        Double leftLength = leftIntersection.clone().sub(position).length();
        Double rightLength = rightIntersection.clone().sub(position).length();

        setAhead(500);
        Double degrees = leftLength < rightLength ? 200.0 : -200;
        setTurnRightRadians(Math.PI / 180 * degrees);
    }

    private void calculateIntersections() {
        Vector left = new Vector(getHeadingRadians()).rotateLeft(Math.PI / 180 * 20);
        Vector right = new Vector(getHeadingRadians()).rotateLeft(-Math.PI / 180 * 20);

        Vector position = new Vector(getX(), getY());
        Ray leftRay = new Ray(position, left);
        Ray rightRay = new Ray(position, right);
        leftIntersection = getRayBattlefieldIntersection(leftRay);
        rightIntersection = getRayBattlefieldIntersection(rightRay);
    }

    @Override
    public void onPaint(Graphics2D g) {
        g.setColor(java.awt.Color.RED);

        Vector position = new Vector(getX(), getY());

        position.drawLine(leftIntersection, g);
        position.drawLine(rightIntersection, g);

        leftIntersection.draw(g);
        rightIntersection.draw(g);
    }

    public Vector getRayBattlefieldIntersection(Ray ray) {
        Ray bottomLine = new Ray(new Vector(0, 0), new Vector(1, 0));
        Ray topLine = new Ray(new Vector(0.0, getBattleFieldHeight()-1), new Vector(1, 0));
        Ray leftLine = new Ray(new Vector(0, 0), new Vector(0, 1));
        Ray rightLine = new Ray(new Vector(getBattleFieldWidth()-1, 0.0), new Vector(0, 1));

        java.util.List<Ray> edges = Arrays.<Ray>asList(rightLine, bottomLine, topLine, leftLine);

        for (Ray edge : edges) {
            Vector intersection = ray.intersection(edge);
            if (intersection != null && intersection.isInsideBox(-1.0, -1.0, getBattleFieldWidth(), getBattleFieldHeight())) {
                System.out.println(intersection.toString());
                return intersection;
            }
        }

        System.out.println("NO INTERSECTION " + ray.toString());

        return null;
    }

    public void onHitRobot(HitRobotEvent e) {
        // If he's in front of us, set back up a bit.
        if (e.getBearing() > -90 && e.getBearing() < 90) {
            back(100);
        } else { // else he's in back of us, so set ahead a bit.
            ahead(100);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        fire(2);
    }
}
