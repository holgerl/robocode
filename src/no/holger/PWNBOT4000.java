package no.holger;

import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static java.awt.event.KeyEvent.*;
import static java.awt.event.KeyEvent.VK_A;
import static no.holger.Utils.clamp;

// C:\Users\Holger Ludvigsen\Dropbox-new\Dropbox\robocode\out\production\robocode
public class PWNBOT4000 extends AdvancedRobot {

    private static final Double bulletPower = Rules.MAX_BULLET_POWER;
    private static final int TICKS_BETWEEN_HISTORY = 1;
    private Vector leftIntersection;
    private Vector rightIntersection;
    private Long timeLastTurn = -10000L;
    private Long lastScannedRobotTime = -10000L;
    private ScannedRobotEvent lastScannedRobotEvent;
    private Vector lastScannedRobotPosition;
    private List<Vector> lastPositions = new ArrayList<>();
    private List<Vector> futurePositions = new ArrayList<>();
    protected Double forwardOrBackwards = 1.0;

    private static Long ticksWithScannedRobot = 0L;
    private static Long totalTicks = 0L;

    // depends on TICKS_BETWEEN_HISTORY

    // if TICKS_BETWEEN_HISTORY = 3:
    // 3.8 is full speed and full turn
    // against predictionBot:
    // 6.0 = 57%
    // 4.3 = 60%
    // 3.8 = 60%
    // 3.0 = 58%
    // 2.5 = 57%
    // 2.0 = 57%

    // if TICKS_BETWEEN_HISTORY = 1:
    // 11.4 is full speed and full turn
    // against predictionBot:
    //  6.0 = 58%
    // 10.0 = 60%
    // 11.0 = 61%
    // 11.4 = 63% (60% second time)
    // 12.0 = 61%
    // 13.0 = 60%
    private static Double linearDeviationFactor = 11.4;

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

            if (getTime() % TICKS_BETWEEN_HISTORY == 0 && lastScannedRobotPosition != null) {
                lastPositions.add(lastScannedRobotPosition.clone());
                if (lastPositions.size() > 3) lastPositions.remove(0);
            }

            if (getTime() % 2 == 0 && lastScannedRobotPosition != null) {
                Vector position = new Vector(getX(), getY());
                if (isTargetLocked()) futurePositions.add(getExpectedEnemyPositionLinearDeviated(20.0));
                if (futurePositions.size() > 10) futurePositions.remove(0);
            }

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
        setDebugProperty("getLinearDeviation", getLinearDeviation());
        setDebugProperty("linearDeviationFactor", linearDeviationFactor);
    }

    protected void fireGun() {
        if (lastScannedRobotPosition != null) {
            Double angleBetween = getAngleBetweenExpectedHitAndGun();
            if (isTargetLocked() && angleBetween < 0.05) setFire(bulletPower);
        }
    }

    private boolean isTargetLocked() {
        long diffSinceLastScan = getTime() - lastScannedRobotTime;
        return diffSinceLastScan == 0;
    }

    private void moveGun() {
        if (lastScannedRobotPosition != null) {
            Double angleBetween = getAngleBetweenExpectedHitAndGun();
            setTurnGunRightRadians(angleBetween);
        }
    }

    protected void moveBot() {
        //setAhead(500); setTurnRightRadians(Math.PI / 180 * 3); if (true) return;

        Vector position = new Vector(getX(), getY());
        Double leftLength = leftIntersection.clone().sub(position).length();
        Double rightLength = rightIntersection.clone().sub(position).length();

        Double shortestLength = leftLength < rightLength ? leftLength : rightLength;

        Double speedFactor = clamp(shortestLength / 150, 1.0 / 6, 1.0);

        Double direction = leftLength < rightLength ? 1.0 : -1.0;
        boolean farAwayFromEdges = leftLength > 200 && rightLength > 200;
        if (farAwayFromEdges && speedFactor == 1.0) direction = Math.signum(Math.random()*2-1);
//        if (!farAwayFromEdges && getTime() % 30 == 0) forwardOrBackwards =  Math.signum(Math.random()-0.5);

        setDebugProperty("speedFactor", speedFactor.toString());

        this.setMaxVelocity(Rules.MAX_VELOCITY * speedFactor);
        setAhead(500 * forwardOrBackwards);

        if (getTime() - timeLastTurn > 30) {
            timeLastTurn = getTime();
            Double degrees = 360.0 * direction;
            setTurnRightRadians(Math.PI / 180 * degrees);
        }
    }

    private void calculateIntersections() {
        int spread = 20;
        Vector left = new Vector(getHeadingRadians()).rotateLeft(Math.PI / 180 * spread);
        Vector right = new Vector(getHeadingRadians()).rotateLeft(-Math.PI / 180 * spread);

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

        for (int i = 1; i < futurePositions.size(); i++) {
            futurePositions.get(i-1).drawLine(futurePositions.get(i), g, Color.GRAY);
        }

        //getExpectedEnemyPositionHistoric((double) 40).draw(g, Color.ORANGE);
        //getExpectedEnemyPositionHistoric((double) 20).draw(g, Color.MAGENTA);
//        getExpectedEnemyPositionHistoric((double) TICKS_BETWEEN_HISTORY).draw(g, Color.BLUE);
        //getExpectedEnemyPositionHistoric((double) 5).draw(g, Color.RED);
        //getExpectedEnemyPositionHistoric((double) 2).draw(g, Color.CYAN);

        if (lastScannedRobotPosition != null) {
            lastScannedRobotPosition.draw(g, java.awt.Color.YELLOW);
            double nofTurnsForBulletToHit = lastScannedRobotPosition.distanceTo(position) / Rules.getBulletSpeed(bulletPower);  // TODO: This is not DRY
            getExpectedEnemyPositionLinear(nofTurnsForBulletToHit).draw(g, java.awt.Color.GREEN);
//            getExpectedEnemyPositionHistoric((double) TICKS_BETWEEN_HISTORY).draw(g, Color.BLUE);
            getExpectedEnemyPositionLinearDeviated(nofTurnsForBulletToHit).draw(g, Color.ORANGE);

            lastScannedRobotPosition.drawLine(lastScannedRobotPosition.clone().add(new Vector(lastScannedRobotEvent.getHeadingRadians()).multiply(10 * lastScannedRobotEvent.getVelocity())), g);
        }

    }

    private Vector getExpectedEnemyPositionLinearDeviated(Double nofTurnsForBulletToHit) {
        Vector linearPos = getExpectedEnemyPositionLinear(nofTurnsForBulletToHit);
        Vector positionToExpected = linearPos.clone().sub(lastScannedRobotPosition);
        if (positionToExpected.length() < 0.01) return linearPos; // Happens when enemy standing still
        Vector linearDirection = positionToExpected.clone().normalize();
        Vector directionDeviated = linearDirection.clone().rotateLeft(-getLinearDeviation() * linearDeviationFactor);

        return lastScannedRobotPosition.clone().add(directionDeviated.clone().multiply(positionToExpected.length()));
    }

    private Vector getExpectedEnemyPosition(Double nofTurnsInFuture) {
        return getExpectedEnemyPositionLinearDeviated(nofTurnsInFuture);
//        return getExpectedEnemyPositionLinear(nofTurnsInFuture);
    }

    private Vector getExpectedEnemyPositionHistoric(Double nofTurnsInFuture) {
        if (lastPositions.size() < 3) return getExpectedEnemyPositionLinear(nofTurnsInFuture);

        Vector a = lastPositions.get(0);
        Vector b = lastPositions.get(1);
        Vector c = lastPositions.get(2);

        Vector ab = b.clone().sub(a);
        Vector bc = c.clone().sub(b);
        Double abcAngle = ab.angleTo(bc);
        Double bcLength = bc.length();

        Vector bcNormalized = bc.clone().normalize();
        Vector cdNormalized = bcNormalized.rotateLeft(-abcAngle);
        Vector cd = cdNormalized.multiply(bcLength * nofTurnsInFuture/TICKS_BETWEEN_HISTORY);
        Vector d = c.clone().add(cd);

        return d;
    }

    private Double getLinearDeviation() {
        if (lastPositions.size() < 3) return 0.0;

        Vector a = lastPositions.get(0);
        Vector b = lastPositions.get(1);
        Vector c = lastPositions.get(2);

        Vector ab = b.clone().sub(a);
        Vector bc = c.clone().sub(b);
        Double abcAngle = ab.angleTo(bc);

        if (abcAngle.isNaN() || Math.abs(abcAngle) > 0.8) return 0.0; // When a, b or c are very close

        return abcAngle;
    }

    private Vector getExpectedEnemyPositionLinear(Double nofTurnsInFuture) {
        Vector scannedRobotHeading = new Vector(lastScannedRobotEvent.getHeadingRadians());
        double scannedRobotSpeed = lastScannedRobotEvent.getVelocity();
        Vector expectedPosition = lastScannedRobotPosition.clone().add(
                scannedRobotHeading.multiply(scannedRobotSpeed * nofTurnsInFuture)
        );

        System.out.println("expectedPosition 1 " + expectedPosition.toString()); // If this line is removed, expectedPosition sometimes equals lastScannedRobotPosition for some weird reason

        if (!expectedPosition.isInsideBox(0.0, 0.0, getBattleFieldWidth(), getBattleFieldHeight())) {
            Vector rayBattlefieldIntersection = getRayBattlefieldIntersection(new Ray(lastScannedRobotPosition, expectedPosition.clone().sub(lastScannedRobotPosition)));
            return rayBattlefieldIntersection.clone();

//            Vector throughWall = expectedPosition.clone().sub(rayBattlefieldIntersection);
//            Vector backlash = throughWall.clone().rotateLeft(Math.PI);
//            return rayBattlefieldIntersection.clone().add(backlash);
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
//            Vector targetRadarDirection = lastScannedRobotPosition.clone().sub(position).normalize();
            Vector targetRadarDirection = getExpectedEnemyPosition((double) diffSinceLastScan).sub(position).normalize();
            Vector radarDirection = new Vector(getRadarHeadingRadians());
            Double angleBetween = radarDirection.angleTo(targetRadarDirection);

            Double exaggeration = 0.0; // Exaggerate to scan past the enemy

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
        double nofTurnsForBulletToHit = lastScannedRobotPosition.distanceTo(position) / Rules.getBulletSpeed(bulletPower); // TODO: THis is not correct. It should be distance to target for bullet
        Vector targetGunDirection = getExpectedEnemyPosition(nofTurnsForBulletToHit).sub(position).normalize();
        Vector gunDirection = new Vector(getGunHeadingRadians());
        return gunDirection.angleTo(targetGunDirection);
    }

    private Double getDistanceToExpectedEnemyPosition() {
        Vector position = new Vector(getX(), getY());
        double nofTurnsForBulletToHit = lastScannedRobotPosition.distanceTo(position) / Rules.getBulletSpeed(bulletPower);
        return getExpectedEnemyPosition(nofTurnsForBulletToHit).distanceTo(position);
    }

    public void onHitRobot(HitRobotEvent e) {
//        // If he's in front of us, set back up a bit.
//        if (e.getBearing() > -90 && e.getBearing() < 90) {
//            back(100);
//        } else { // else he's in back of us, so set ahead a bit.
//            ahead(100);
//        }
    }

    public void onKeyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_UP:
            case VK_W:
                linearDeviationFactor += 0.1;
                break;

            case VK_DOWN:
            case VK_S:
                linearDeviationFactor -= 0.1;
                break;

            case VK_RIGHT:
            case VK_D:
                // Arrow right key: turn direction = right
                break;

            case VK_LEFT:
            case VK_A:
                // Arrow left key: turn direction = left
                break;
        }
    }
}
