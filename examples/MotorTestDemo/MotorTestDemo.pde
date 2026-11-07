// MotorTestDemo
// Left 680 px: OnBot Java-style editor for data/FourMotorTest.java (Build Everything = save + compile).
// Right: robot view (top) and Driver Station (bottom). Click INIT -> PLAY -> STOP on the Driver Station.
import org.nycfirst.opmodesim.*;

OpModeSim sim;

void setup() {
  size(1280, 720);
  sim = new OpModeSim();
  OnBotEditor.attach(this, sim, new File(dataPath("FourMotorTest.java")));
  SimView.attach(this, sim);
}

void draw() {
  // Keep this (even empty): SimView draws the robot view and Driver Station each frame.
}
