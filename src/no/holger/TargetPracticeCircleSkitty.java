package no.holger;

public class TargetPracticeCircleSkitty extends TargetPracticeCircle {
    protected void moveBot() {
        if (getTime() % 10 == 0) forwardOrBackwards *= -1;
        super.moveBot();
    }
}
