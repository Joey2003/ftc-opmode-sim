import org.firstinspires.ftc.teamcode.PackageTest;

void setup() {
  size(1280, 720);
  pixelDensity(displayDensity());
}

void draw() {
  background(30);

  // robot area
  noStroke();
  fill(50);
  rect(0, 0, 560, 720);

  // driver station area
  fill(90, 0, 0);
  rect(560, 0, 720, 720);

  fill(255);
  textSize(24);
  text(PackageTest.hello(), 40, 60);
}
