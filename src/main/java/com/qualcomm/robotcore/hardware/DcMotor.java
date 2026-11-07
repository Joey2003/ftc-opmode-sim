package com.qualcomm.robotcore.hardware;

/** Sim stand-in for the FTC SDK DcMotor. */
public interface DcMotor extends DcMotorSimple {

  enum ZeroPowerBehavior { UNKNOWN, BRAKE, FLOAT }

  enum RunMode { RUN_WITHOUT_ENCODER, RUN_USING_ENCODER, RUN_TO_POSITION, STOP_AND_RESET_ENCODER }

  void setMode(RunMode mode);
  RunMode getMode();
  void setZeroPowerBehavior(ZeroPowerBehavior behavior);
  ZeroPowerBehavior getZeroPowerBehavior();
  int getCurrentPosition();
  int getPortNumber();
}
