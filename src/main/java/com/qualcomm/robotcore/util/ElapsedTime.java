package com.qualcomm.robotcore.util;

/** Sim stand-in for the FTC SDK ElapsedTime. */
public class ElapsedTime {

  private long startNanos;

  public ElapsedTime() {
    reset();
  }

  public void reset() {
    startNanos = System.nanoTime();
  }

  public double startTime() {
    return startNanos / 1e9;
  }

  public long nanoseconds() {
    return System.nanoTime() - startNanos;
  }

  public double milliseconds() {
    return nanoseconds() / 1e6;
  }

  public double seconds() {
    return nanoseconds() / 1e9;
  }

  public double time() {
    return seconds();
  }

  @Override
  public String toString() {
    return String.format("%.4f seconds", seconds());
  }
}
