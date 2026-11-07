// MotorTestDemo (Day 3: OnBot Java-style editor + temporary key controls)
// Left 680 px: editor for data/FourMotorTest.java. Build Everything saves + compiles it.
// Right 600 px: placeholder sim view until the robot view and Driver Station (Day 4).
// Keys (click the right side first): i = INIT, p = PLAY, s = STOP.
import org.nycfirst.opmodesim.*;

final int SIM_X = 680;

OpModeSim sim;
OnBotEditor editor;

void setup() {
  size(1280, 720);
  sim = new OpModeSim();
  editor = OnBotEditor.attach(this, sim, new File(dataPath("FourMotorTest.java")));
}

void draw() {
  sim.update();  // also prints events, telemetry and motor state to the console
  background(30);
  fill(255);
  textSize(14);
  int x = SIM_X + 16;
  String name = sim.runner.opModeName();
  text("OpMode: " + (name != null ? name : "(build first)"), x, 28);
  text("State: " + sim.runner.state(), x, 48);
  fill(180);
  text("Click here, then  i = INIT   p = PLAY   s = STOP", x, 68);
  if (sim.runner.error() != null) {
    fill(255, 120, 120);
    text(sim.runner.error(), x, 92, width - x - 16, 60);
  }
  fill(255);
  textSize(12);
  int y = 160;
  for (SimMotor m : sim.motors) {
    double w = m.wheelSpin();
    String wheel = w > 0 ? "FWD" : w < 0 ? "BACK" : "-";
    text(String.format("%-10s  p=%+.2f  %-4s  enc=%d", m.name(), m.getPower(), wheel, m.getCurrentPosition()), x, y);
    y += 18;
  }
  y += 12;
  for (String line : sim.telemetry.lines()) {
    text(line, x, y);
    y += 16;
  }
}

void keyPressed() {
  if (key == 'i') sim.init();
  if (key == 'p') sim.play();
  if (key == 's') sim.stop();
}
