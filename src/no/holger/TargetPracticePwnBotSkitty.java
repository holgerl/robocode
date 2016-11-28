package no.holger;

public class TargetPracticePwnBotSkitty extends PWNBOT4000 {
    protected void moveBot() {
        Vector position = new Vector(getX(), getY());

        Double limit = 100.0;

        boolean awayFromWall = position.isInsideBox(limit, limit, getBattleFieldWidth() - limit, getBattleFieldHeight() - limit);

        int skittynessInverted = 10;

        if ((getTime() % skittynessInverted == 0) && awayFromWall) forwardOrBackwards *= -1;

        super.moveBot();
    }
}
