package no.holger;

import robocode.Robot;

import java.awt.*;

public class Colors {
    public static void applyColors(Robot robot) {
        robot.setBodyColor(new Color(randomInt(255), randomInt(255), randomInt(255)));
        robot.setGunColor(Color.black);
        robot.setRadarColor(Color.orange);
        robot.setBulletColor(Color.cyan);
        robot.setScanColor(Color.cyan);
    }

    private static int randomInt(int max) {
        return new Double(Math.random()* max).intValue();
    }
}
