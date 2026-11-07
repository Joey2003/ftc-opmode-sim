package org.nycfirst.opmodesim;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeInternal;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** INIT -> PLAY -> STOP state machine. Runs the OpMode on its own thread. */
public class OpModeRunner {

  public enum State { IDLE, INIT, RUNNING, STOPPED }

  private static final long STOP_TIMEOUT_MS = 5000;

  private final SimMotor[] motors;
  private final SimTelemetry telemetry;
  private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

  private Class<? extends LinearOpMode> opModeClass;
  private String opModeName;
  private volatile State state = State.IDLE;
  private volatile LinearOpMode op;
  private volatile Thread thread;
  private volatile String error;

  public OpModeRunner(SimMotor[] motors, SimTelemetry telemetry) {
    this.motors = motors;
    this.telemetry = telemetry;
  }

  public synchronized void setOpMode(Class<? extends LinearOpMode> cls, String name) {
    if (state == State.INIT || state == State.RUNNING) stop();
    opModeClass = cls;
    opModeName = name;
    state = State.IDLE;
  }

  public synchronized void init() {
    if (opModeClass == null || state == State.INIT || state == State.RUNNING) return;
    if (thread != null && thread.isAlive()) {
      messages.add("Previous OpMode still stopping; try again.");
      return;
    }
    error = null;
    telemetry.reset();
    HardwareMap hw = new HardwareMap();
    for (SimMotor m : motors) {
      m.resetForOpMode();
      hw.put(m.name(), m);
    }
    final LinearOpMode instance;
    try {
      instance = opModeClass.getDeclaredConstructor().newInstance();
    } catch (Throwable t) {
      fail(t instanceof java.lang.reflect.InvocationTargetException ? t.getCause() : t);
      state = State.STOPPED;
      return;
    }
    OpModeInternal.attach(instance, hw, telemetry);
    op = instance;
    state = State.INIT;
    messages.add("INIT: " + opModeName);
    Thread t = new Thread(() -> {
      try {
        instance.runOpMode();
      } catch (InterruptedException e) {
        // normal after STOP
      } catch (Throwable e) {
        fail(e);
      } finally {
        for (SimMotor m : motors) m.stop();
        state = State.STOPPED;
        messages.add("STOPPED: " + opModeName);
      }
    }, "OpMode-" + opModeName);
    t.setDaemon(true);
    thread = t;
    t.start();
  }

  public synchronized void play() {
    if (state != State.INIT) return;
    OpModeInternal.start(op);
    state = State.RUNNING;
    messages.add("PLAY: " + opModeName);
  }

  public synchronized void stop() {
    if (state != State.INIT && state != State.RUNNING) return;
    final Thread t = thread;
    OpModeInternal.requestStop(op);
    t.interrupt();
    Thread watchdog = new Thread(() -> {
      try {
        t.join(STOP_TIMEOUT_MS);
      } catch (InterruptedException ignored) {
      }
      if (t.isAlive()) {
        for (SimMotor m : motors) m.stop();
        error = "User code is stuck and did not stop within " + (STOP_TIMEOUT_MS / 1000) + " seconds.";
        messages.add("ERROR: " + error);
      }
    }, "OpMode-stop-watchdog");
    watchdog.setDaemon(true);
    watchdog.start();
  }

  private void fail(Throwable e) {
    error = "User code threw an uncaught exception: " + e.getClass().getSimpleName() + " - " + e.getMessage();
    StringBuilder msg = new StringBuilder("ERROR: ").append(error);
    for (StackTraceElement el : e.getStackTrace()) {
      if (el.getClassName().startsWith("org.firstinspires.ftc.teamcode")) {
        msg.append("\n  at ").append(el.getFileName()).append(" line ").append(el.getLineNumber());
        break;
      }
    }
    messages.add(msg.toString());
  }

  public State state() { return state; }

  public String opModeName() { return opModeName; }

  /** "TeleOp" or "Autonomous", from the OpMode's annotation. Null before a build. */
  public synchronized String opModeKind() {
    if (opModeClass == null) return null;
    return opModeClass.isAnnotationPresent(com.qualcomm.robotcore.eventloop.opmode.Autonomous.class) ? "Autonomous" : "TeleOp";
  }

  /** Last error to show on the Driver Station, or null. */
  public String error() { return error; }

  /** Next queued event message (state changes, errors), or null. */
  public String pollMessage() { return messages.poll(); }
}
