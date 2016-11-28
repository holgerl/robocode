package no.holger;

import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static no.holger.Utils.clamp;

public class TargetPracticeCircle extends AdvancedRobot {

    public void run() {
        Colors.applyColors(this);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        while (true) {
            moveBot();
            execute();
        }
    }

    private void moveBot() {
        setAhead(500);

        setTurnRightRadians(500);
    }
}
