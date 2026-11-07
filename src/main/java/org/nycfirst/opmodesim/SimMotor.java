package org.nycfirst.opmodesim;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Simulated drive motor. Wheels-up: no robot motion, just wheel spin + encoder.
 *
 * Mirroring: motors on the left side are mounted flipped, so +power spins the
 * left wheels backward until the student calls setDirection(REVERSE).
 * Encoder counts follow commanded power (direction-adjusted, like the real SDK):
 * +power always counts up.
 */
public class SimMotor implements DcMotor {

  /** Encoder ticks per second at full power. Not a specific motor model. */
  public static final double TICKS_PER_SEC_AT_FULL_POWER = 2000.0;

  private final String name;
  private final int port;
  private final boolean leftSide;

  private Direction direction = Direction.FORWARD;
  private RunMode mode = RunMode.RUN_WITHOUT_ENCODER;
  private ZeroPowerBehavior zeroPowerBehavior = ZeroPowerBehavior.BRAKE; // not modeled
  private double power = 0;
  private double encoder = 0;
  private long lastNanos = System.nanoTime();

  public SimMotor(String name, int port, boolean leftSide) {
    this.name = name;
    this.port = port;
    this.leftSide = leftSide;
  }

  private void advance() {
    long now = System.nanoTime();
    encoder += power * TICKS_PER_SEC_AT_FULL_POWER * (now - lastNanos) / 1e9;
    lastNanos = now;
  }

  // ---- DcMotor ----

  @Override public synchronized void setPower(double p) {
    advance();
    power = Math.max(-1, Math.min(1, p));
  }

  @Override public synchronized double getPower() { return power; }

  @Override public synchronized void setDirection(Direction d) { direction = d; }

  @Override public synchronized Direction getDirection() { return direction; }

  @Override public synchronized void setMode(RunMode m) {
    advance();
    if (m == RunMode.STOP_AND_RESET_ENCODER) {
      encoder = 0;
      power = 0;
    }
    mode = m;
  }

  @Override public synchronized RunMode getMode() { return mode; }

  @Override public synchronized void setZeroPowerBehavior(ZeroPowerBehavior b) { zeroPowerBehavior = b; }

  @Override public synchronized ZeroPowerBehavior getZeroPowerBehavior() { return zeroPowerBehavior; }

  @Override public synchronized int getCurrentPosition() {
    advance();
    return (int) encoder;
  }

  @Override public int getPortNumber() { return port; }

  @Override public String getDeviceName() { return name; }

  // ---- sim-only ----

  public String name() { return name; }

  public boolean isLeftSide() { return leftSide; }

  /**
   * Physical wheel spin, -1..1. Positive = wheel rolls the robot forward.
   * Direction REVERSE flips it; left-side mounting flips it again.
   */
  public synchronized double wheelSpin() {
    double s = direction == Direction.REVERSE ? -power : power;
    return leftSide ? -s : s;
  }

  /** What the SDK does at each OpMode init: power 0, FORWARD, default mode. Encoder keeps counting. */
  public synchronized void resetForOpMode() {
    advance();
    power = 0;
    direction = Direction.FORWARD;
    mode = RunMode.RUN_WITHOUT_ENCODER;
  }

  public synchronized void stop() {
    advance();
    power = 0;
  }
}
