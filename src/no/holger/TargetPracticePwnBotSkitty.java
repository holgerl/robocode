package no.holger;

public class TargetPracticePwnBotSkitty extends PWNBOT4000 {
    protected void moveBot() {

        Vector position = new Vector(getX(), getY());
        Double skittyWallLimit = 100.0;
        int skitEveryXTicks = 30; // 30 is experimentally the best
        boolean awayFromWall = position.isInsideBox(skittyWallLimit, skittyWallLimit, getBattleFieldWidth() - skittyWallLimit, getBattleFieldHeight() - skittyWallLimit);
        if ((getTime() % skitEveryXTicks == 0) && awayFromWall) forwardOrBackwards *= -1;

        super.moveBot();
    }
}
