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

    public static final Double bulletPower = Rules.MAX_BULLET_POWER;
    Vector leftIntersection;
    Vector rightIntersection;
    Long timeLastTurn = 0L;
    Double radarDirection = 1.0;
    private Long lastScannedRobotTime = 0L;
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
            calculateIntersections();

            long diffSinceLastScan = getTime() - lastScannedRobotTime;

            if (diffSinceLastScan == 0 && lastScannedRobotPosition != null) {
                Double angleBetween = getAngleBetweenExpectedHitAndGun();

                setTurnGunRightRadians(angleBetween);

                if (angleBetween < 0.05
//                        && getExpectedBulletHitPosition().isInsideBox(-50.0, -50.0, getBattleFieldWidth()+50, getBattleFieldHeight()+50)
                        ) setFire(bulletPower);
            }

            if (getTime() == lastScannedRobotTime) ticksWithScannedRobot++;
            totalTicks++;
            setDebugProperty("scanRatio", Double.toString(ticksWithScannedRobot.doubleValue() / totalTicks));

            moveBot();
            moveRadar();

            setDebugProperty("headingVector", new Vector(getHeadingRadians()).toString());
            setDebugProperty("headingVectorLeft", new Vector(getHeadingRadians()).rotateLeft(Math.PI / 180 * 20).toString());
            setDebugProperty("headingVectorRight", new Vector(getHeadingRadians()).rotateLeft(- Math.PI / 180 * 20).toString());

            execute();
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

        if (lastScannedRobotPosition != null) {
            lastScannedRobotPosition.draw(g, java.awt.Color.YELLOW);
            double nofTurnsForBulletToHit = lastScannedRobotPosition.distanceTo(position) / Rules.getBulletSpeed(bulletPower);
            getExpectedEnemyPosition(nofTurnsForBulletToHit).draw(g, java.awt.Color.GREEN);
        }

    }

    private Vector getExpectedEnemyPosition(Double nofTurnsInFuture) {
        Vector scannedRobotHeading = new Vector(lastScannedRobotEvent.getHeadingRadians());
        double scannedRobotSpeed = lastScannedRobotEvent.getVelocity();
        Vector expectedPosition = lastScannedRobotPosition.clone().add(
                scannedRobotHeading.multiply(scannedRobotSpeed * nofTurnsInFuture)
        );
        expectedPosition.x = clamp(expectedPosition.x, 0.0, getBattleFieldWidth());
        expectedPosition.y = clamp(expectedPosition.y, 0.0, getBattleFieldHeight());
        return expectedPosition;
    }

    private Vector estimateLastScannedRobotPosition(ScannedRobotEvent event) {
        Vector position = new Vector(getX(), getY());
        Vector direction = new Vector(getHeadingRadians() + event.getBearingRadians());
        Vector estimate = position.clone().add(direction.multiply(event.getDistance()));
        return estimate;
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

        Double angleBetween = getAngleBetweenExpectedHitAndGun();

        setDebugProperty("angleBetween", angleBetween.toString());
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
