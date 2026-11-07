package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/** Sim-only bridge so the sim can drive a LinearOpMode. Not part of the real SDK. */
public final class OpModeInternal {
  private OpModeInternal() {}

  public static void attach(LinearOpMode op, HardwareMap hw, Telemetry t) { op.simAttach(hw, t); }
  public static void start(LinearOpMode op) { op.simStart(); }
  public static void requestStop(LinearOpMode op) { op.simRequestStop(); }
}
