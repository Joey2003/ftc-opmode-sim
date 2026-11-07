package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/** Sim stand-in for the FTC SDK LinearOpMode. */
public abstract class LinearOpMode {

  public HardwareMap hardwareMap;
  public Telemetry telemetry;

  private final Object startLock = new Object();
  private volatile boolean started = false;
  private volatile boolean stopRequested = false;

  public abstract void runOpMode() throws InterruptedException;

  /** Blocks until PLAY (or STOP) is pressed on the Driver Station. */
  public void waitForStart() {
    synchronized (startLock) {
      while (!started && !stopRequested) {
        try {
          startLock.wait();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  /** True after PLAY and before STOP. Like the real SDK, idles when active. */
  public final boolean opModeIsActive() {
    boolean active = started && !stopRequested;
    if (active) idle();
    return active;
  }

  public final boolean opModeInInit() {
    return !started && !stopRequested;
  }

  public final boolean isStarted() {
    return started;
  }

  public final boolean isStopRequested() {
    return stopRequested || Thread.currentThread().isInterrupted();
  }

  /** Real SDK yields; the sim sleeps 1 ms so busy loops don't pin a CPU core. */
  public final void idle() {
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public final void sleep(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // ---- sim hooks, reached through OpModeInternal ----

  void simAttach(HardwareMap hw, Telemetry t) {
    hardwareMap = hw;
    telemetry = t;
  }

  void simStart() {
    synchronized (startLock) {
      started = true;
      startLock.notifyAll();
    }
  }

  void simRequestStop() {
    synchronized (startLock) {
      stopRequested = true;
      startLock.notifyAll();
    }
  }
}
