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

    private static final Double bulletPower = Rules.MAX_BULLET_POWER;
    private Vector leftIntersection;
    private Vector rightIntersection;
    private Long timeLastTurn = -10000L;
    private Long lastScannedRobotTime = -10000L;
    private ScannedRobotEvent lastScannedRobotEvent;
    private Vector lastScannedRobotPosition;

    private static Long ticksWithScannedRobot = 0L;
    private static Long totalTicks = 0L;

    public void run() {
        Colors.applyColors(this);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            if (getTime() == lastScannedRobotTime) ticksWithScannedRobot++;
            totalTicks++;

            if (lastScannedRobotEvent != null) setDebugProperties();

            calculateIntersections();

            moveBot();
            moveRadar();
            moveGun();
            fireGun();

            execute();
        }
    }

    private void setDebugProperty(String key, Object value) {
        setDebugProperty(key, value != null ? value.toString() : "null");
    }

    private void setDebugProperties() {
        setDebugProperty("scanRatio", Double.toString(ticksWithScannedRobot.doubleValue() / totalTicks));
        setDebugProperty("lastScannedRobotPosition", lastScannedRobotPosition);
        setDebugProperty("lastScannedRobotTime", lastScannedRobotTime);
        setDebugProperty("lastScannedRobotEvent.velocity", lastScannedRobotEvent.getVelocity());
        setDebugProperty("lastScannedRobotEvent.distance", Double.toString(lastScannedRobotEvent.getDistance()));
        setDebugProperty("lastScannedRobotEvent.heading", Double.toString(lastScannedRobotEvent.getHeadingRadians()));
        setDebugProperty("lastScannedRobotEvent.bearing", Double.toString(lastScannedRobotEvent.getBearingRadians()));
        setDebugProperty("angleBetween", getAngleBetweenExpectedHitAndGun().toString());
        setDebugProperty("getTime", Long.toString(getTime()));
    }

    private void fireGun() {
        if (lastScannedRobotPosition != null) {
            long diffSinceLastScan = getTime() - lastScannedRobotTime;
            Double angleBetween = getAngleBetweenExpectedHitAndGun();
            if (diffSinceLastScan == 0 && angleBetween < 0.05) setFire(bulletPower);
        }
    }

    private void moveGun() {
        if (lastScannedRobotPosition != null) {
            Double angleBetween = getAngleBetweenExpectedHitAndGun();
            setTurnGunRightRadians(angleBetween);
        }
    }

    private void moveBot() {
        //setAhead(500); setTurnRightRadians(Math.PI / 180 * 3); if (true) return;

        Vector position = new Vector(getX(), getY());
        Double leftLength = leftIntersection.clone().sub(position).length();
        Double rightLength = rightIntersection.clone().sub(position).length();

        Double direction = leftLength < rightLength ? 1.0 : -1.0;
        if (leftLength > 200 && rightLength > 200) direction = Math.signum(Math.random()*2-1);

        Double shortestLength = leftLength < rightLength ? leftLength : rightLength;

        Double speedFactor = clamp(shortestLength / 150, 1.0 / 6, 1.0);

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

        if (lastScannedRobotPosition != null) {
            lastScannedRobotPosition.draw(g, java.awt.Color.YELLOW);
            double nofTurnsForBulletToHit = lastScannedRobotPosition.distanceTo(position) / Rules.getBulletSpeed(bulletPower);
            getExpectedEnemyPosition(nofTurnsForBulletToHit).draw(g, java.awt.Color.GREEN);

            lastScannedRobotPosition.drawLine(lastScannedRobotPosition.clone().add(new Vector(lastScannedRobotEvent.getHeadingRadians()).multiply(10 * lastScannedRobotEvent.getVelocity())), g);
        }

    }

    private Vector getExpectedEnemyPosition(Double nofTurnsInFuture) {
        Vector scannedRobotHeading = new Vector(lastScannedRobotEvent.getHeadingRadians());
        double scannedRobotSpeed = lastScannedRobotEvent.getVelocity();
        Vector expectedPosition = lastScannedRobotPosition.clone().add(
                scannedRobotHeading.multiply(scannedRobotSpeed * nofTurnsInFuture)
        );

        System.out.println("expectedPosition 1 " + expectedPosition.toString()); // If this line is removed, expectedPosition sometimes equals lastScannedRobotPosition for some weird reason

        if (!expectedPosition.isInsideBox(0.0, 0.0, getBattleFieldWidth(), getBattleFieldHeight())) {
            Vector rayBattlefieldIntersection = getRayBattlefieldIntersection(new Ray(lastScannedRobotPosition, expectedPosition.clone().sub(lastScannedRobotPosition)));
            return rayBattlefieldIntersection;
        }

        return expectedPosition;
    }

    private Vector estimateLastScannedRobotPosition(ScannedRobotEvent event) {
        Vector position = new Vector(getX(), getY());
        Vector direction = new Vector(getHeadingRadians() + event.getBearingRadians());
        Vector estimate = position.clone().add(direction.multiply(event.getDistance()));
        return estimate;
    }

    private Vector getRayBattlefieldIntersection(Ray ray) {
        Ray bottomLine = new Ray(new Vector(0, 0), new Vector(1, 0));
        Ray topLine = new Ray(new Vector(0.0, getBattleFieldHeight()-1), new Vector(1, 0));
        Ray leftLine = new Ray(new Vector(0, 0), new Vector(0, 1));
        Ray rightLine = new Ray(new Vector(getBattleFieldWidth()-1, 0.0), new Vector(0, 1));

        java.util.List<Ray> edges = Arrays.<Ray>asList(rightLine, bottomLine, topLine, leftLine);

        for (Ray edge : edges) {
            Vector intersection = ray.intersection(edge);
            if (intersection != null && intersection.isInsideBox(-1.0, -1.0, getBattleFieldWidth(), getBattleFieldHeight())) {
                return intersection;
            }
        }

        System.out.println("NO INTERSECTION " + ray.toString());

        return null;
    }

    private void moveRadar() {
        long diffSinceLastScan = getTime() - lastScannedRobotTime;

        Double radians = 100000.0;

        if (diffSinceLastScan < 6 && lastScannedRobotPosition != null) {
            Vector position = new Vector(getX(), getY());
            Vector targetRadarDirection = getExpectedEnemyPosition(2.0).sub(position).normalize();
            Vector radarDirection = new Vector(getRadarHeadingRadians());
            Double angleBetween = radarDirection.angleTo(targetRadarDirection);

            Double exaggeration = 2.0; // Exaggerate to scan past the enemy

            radians = angleBetween + Math.signum(angleBetween)* Math.PI/180*exaggeration;
        }

        setTurnRadarRightRadians(radians);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        lastScannedRobotTime = getTime();
        lastScannedRobotEvent = e; // TODO: Replace with lastScannedRobotVelocity and lastScannedRobotDirection
        lastScannedRobotPosition = estimateLastScannedRobotPosition(e);
    }

    private Double getAngleBetweenExpectedHitAndGun() {
        Vector position = new Vector(getX(), getY());
        double nofTurnsForBulletToHit = lastScannedRobotPosition.distanceTo(position) / Rules.getBulletSpeed(bulletPower);
        Vector targetGunDirection = getExpectedEnemyPosition(nofTurnsForBulletToHit).sub(position).normalize();
        Vector gunDirection = new Vector(getGunHeadingRadians());
        return gunDirection.angleTo(targetGunDirection);
    }

    public void onHitRobot(HitRobotEvent e) {
//        // If he's in front of us, set back up a bit.
//        if (e.getBearing() > -90 && e.getBearing() < 90) {
//            back(100);
//        } else { // else he's in back of us, so set ahead a bit.
//            ahead(100);
//        }
    }
}
