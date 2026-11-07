package org.nycfirst.opmodesim;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Sim entry point. Fixed config: frontLeft 0, frontRight 1, backLeft 2, backRight 3.
 * Call update() often (e.g. from draw()); it prints events, telemetry and motor state.
 */
public class OpModeSim {

  public static final String VERSION = "0.1.0";

  public final SimMotor[] motors = {
      new SimMotor("frontLeft", 0, true),
      new SimMotor("frontRight", 1, false),
      new SimMotor("backLeft", 2, true),
      new SimMotor("backRight", 3, false),
  };
  public final SimTelemetry telemetry = new SimTelemetry();
  public final OpModeRunner runner = new OpModeRunner(motors, telemetry);

  /** Console status print interval while an OpMode is running. */
  public long printIntervalMs = 1000;

  private PrintStream out = System.out;
  private long lastPrint = 0;
  private long lastPrintedTelemetry = -1;

  public static String hello() {
    return "ftc-opmode-sim " + VERSION + " loaded";
  }

  public void setOutput(PrintStream out) { this.out = out; }

  /** Reads, compiles and loads an OpMode source file. Prints errors. */
  public boolean load(File javaFile) {
    String source;
    try {
      source = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
    } catch (IOException e) {
      out.println("Could not read " + javaFile + ": " + e);
      return false;
    }
    return compile(javaFile.getName(), source);
  }

  /** Compiles OpMode source. fileName must match the public class (e.g. "FourMotorTest.java"). */
  public boolean compile(String fileName, String source) {
    OpModeCompiler.Result r = OpModeCompiler.compile(fileName, source);
    for (String e : r.errors) out.println(e);
    if (!r.ok) {
      out.println("Build FAILED.");
      return false;
    }
    runner.setOpMode(r.opModeClass, r.opModeName);
    out.println("Build succeeded!");
    out.println("OpMode \"" + r.opModeName + "\" is now available on the Driver Station.");
    return true;
  }

  public void init() { runner.init(); }
  public void play() { runner.play(); }
  public void stop() { runner.stop(); }

  public void update() {
    boolean changed = false;
    for (String m; (m = runner.pollMessage()) != null; ) {
      out.println(">> " + m);
      changed = true;
    }
    OpModeRunner.State s = runner.state();
    long now = System.currentTimeMillis();
    boolean active = s == OpModeRunner.State.INIT || s == OpModeRunner.State.RUNNING;
    boolean newTelemetry = telemetry.publishCount() != lastPrintedTelemetry;
    if (changed || (active && now - lastPrint >= printIntervalMs && newTelemetry)) {
      printStatus();
      lastPrint = now;
    }
  }

  public void printStatus() {
    lastPrintedTelemetry = telemetry.publishCount();
    out.println("---- " + runner.state() + (runner.opModeName() != null ? "  \"" + runner.opModeName() + "\"" : "") + " ----");
    for (String l : telemetry.lines()) out.println("  " + l);
    out.println("  [motors] " + motorSummary());
  }

  public String motorSummary() {
    StringBuilder sb = new StringBuilder();
    String[] tags = {"FL", "FR", "BL", "BR"};
    for (int i = 0; i < motors.length; i++) {
      SimMotor m = motors[i];
      double w = m.wheelSpin();
      String wheel = w > 0 ? "FWD " : w < 0 ? "BACK" : "----";
      if (i > 0) sb.append(" | ");
      sb.append(String.format("%s p=%+.2f %s enc=%d", tags[i], m.getPower(), wheel, m.getCurrentPosition()));
    }
    return sb.toString();
  }
}
