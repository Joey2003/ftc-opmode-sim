// MotorTestDemo (Day 2: console + plain text view)
// Compiles FourMotorTest.java at runtime and runs it on the sim.
// Keys: i = INIT, p = PLAY, s = STOP, b = rebuild from file.
import org.nycfirst.opmodesim.*;

OpModeSim sim;

void setup() {
  size(1280, 720);
  sim = new OpModeSim();
  sim.load(new File(sketchPath("FourMotorTest.java")));
}

void draw() {
  sim.update();  // prints events, telemetry and motor state to the console
  background(30);
  fill(255);
  textSize(16);
  text("State: " + sim.runner.state() + "    keys: i=INIT  p=PLAY  s=STOP  b=rebuild", 20, 30);
  if (sim.runner.error() != null) {
    fill(255, 120, 120);
    text(sim.runner.error(), 20, 55);
    fill(255);
  }
  text(sim.motorSummary(), 20, 85);
  textSize(14);
  int y = 120;
  for (String line : sim.telemetry.lines()) {
    text(line, 20, y);
    y += 18;
  }
}

void keyPressed() {
  if (key == 'i') sim.init();
  if (key == 'p') sim.play();
  if (key == 's') sim.stop();
  if (key == 'b') sim.load(new File(sketchPath("FourMotorTest.java")));
}
