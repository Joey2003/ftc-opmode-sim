package com.qualcomm.robotcore.hardware;

/** Sim stand-in for the FTC SDK DcMotorSimple. */
public interface DcMotorSimple extends HardwareDevice {

  enum Direction {
    FORWARD, REVERSE;

    public Direction inverted() {
      return this == FORWARD ? REVERSE : FORWARD;
    }
  }

  void setDirection(Direction direction);
  Direction getDirection();
  void setPower(double power);
  double getPower();
}
