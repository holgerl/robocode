package no.holger;

import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.util.Arrays;

import static no.holger.Utils.clamp;

// C:\Users\Holger Ludvigsen\Dropbox-new\Dropbox\robocode\out\production\robocode
public class PWNBOT4000 extends AdvancedRobot {

    Vector leftIntersection;
    Vector rightIntersection;
    Long timeLastTurn = 0L;
    Double radarDirection = 1.0;
    private long lastScannedRobotTime = 0L;
    private ScannedRobotEvent lastScannedRobotEvent;

    public void run() {
        Colors.applyColors(this);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {

            calculateIntersections();
            moveBot();
            moveGunRadar();

            execute();

            setDebugProperty("headingVector", new Vector(getHeadingRadians()).toString());
            setDebugProperty("headingVectorLeft", new Vector(getHeadingRadians()).rotateLeft(Math.PI / 180 * 20).toString());
            setDebugProperty("headingVectorRight", new Vector(getHeadingRadians()).rotateLeft(- Math.PI / 180 * 20).toString());
        }
    }

    private void moveBot() {
        Vector position = new Vector(getX(), getY());
        Double leftLength = leftIntersection.clone().sub(position).length();
        Double rightLength = rightIntersection.clone().sub(position).length();

        Double direction = leftLength < rightLength ? 1.0 : -1.0;
        if (leftLength > 200 && rightLength > 200) direction = Math.signum(Math.random()*2-1);

        Double shortestLength = leftLength < rightLength ? leftLength : rightLength;
        Double turnFactor = 1 + 1/shortestLength * 600;

        Double speedFactor = clamp(shortestLength / 200, 1.0 / 8, 1.0);

        setDebugProperty("speedFactor", speedFactor.toString());

        this.setMaxVelocity(Rules.MAX_VELOCITY * speedFactor);
        setAhead(500);

        if (getTime() - timeLastTurn > 30) {
            timeLastTurn = getTime();
            Double degrees = 360.0 * direction;
            setTurnRightRadians(Math.PI / 180 * degrees);
        }
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
        Vector position = new Vector(getX(), getY());

        position.drawLine(leftIntersection, g);
        position.drawLine(rightIntersection, g);

        leftIntersection.draw(g);
        rightIntersection.draw(g);

        if (lastScannedRobotEvent != null) {
            getLastScannedRobotPosition().draw(g, java.awt.Color.YELLOW);
            getExpectedBulletHitPosition().draw(g, java.awt.Color.GREEN);
        }

    }

    private Vector getExpectedBulletHitPosition() {
        double nofTurnsForBulletToHit = lastScannedRobotEvent.getDistance() / Rules.getBulletSpeed(2);
        Vector scannedRobotHeading = new Vector(lastScannedRobotEvent.getHeadingRadians());
        double scannedRobotSpeed = lastScannedRobotEvent.getVelocity();
        return getLastScannedRobotPosition().add(
                scannedRobotHeading.multiply(scannedRobotSpeed * nofTurnsForBulletToHit)
        );
    }

    private Vector getLastScannedRobotPosition() {
        Vector position = new Vector(getX(), getY());
        Vector direction = new Vector(getHeadingRadians() + lastScannedRobotEvent.getBearingRadians());
        return position.clone().add(direction.multiply(lastScannedRobotEvent.getDistance()));
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
                //System.out.println(intersection.toString());
                return intersection;
            }
        }

        System.out.println("NO INTERSECTION " + ray.toString());

        return null;
    }


    private void moveGunRadar() {
        long diffSinceLastScan = getTime() - lastScannedRobotTime;

        if (diffSinceLastScan == 10) {
            //radarDirection *= -1;
        } else if (diffSinceLastScan == 1) {
            radarDirection *= -1;
        }

        Double turnFactor = clamp(diffSinceLastScan/5.0, 0.1, 1.0);

        setTurnRadarRightRadians(0.2 * turnFactor * radarDirection);
        setTurnGunRightRadians(0.2 * turnFactor * radarDirection);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        lastScannedRobotTime = getTime();
        lastScannedRobotEvent = e;

        Vector position = new Vector(getX(), getY());
        Vector targetGunDirection = getExpectedBulletHitPosition().sub(position).normalize();
        Vector gunDirection = new Vector(getGunHeadingRadians());
        Double angleBetween = gunDirection.angleTo(targetGunDirection);

        setDebugProperty("angleBetween", angleBetween.toString());

        turnGunRightRadians(angleBetween);
        if (angleBetween < 0.05) setFire(2);
    }

    public void onHitRobot(HitRobotEvent e) {
        // If he's in front of us, set back up a bit.
        if (e.getBearing() > -90 && e.getBearing() < 90) {
            back(100);
        } else { // else he's in back of us, so set ahead a bit.
            ahead(100);
        }
    }
}
