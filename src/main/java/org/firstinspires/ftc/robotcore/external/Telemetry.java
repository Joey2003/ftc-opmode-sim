package org.firstinspires.ftc.robotcore.external;

/** Sim stand-in for the FTC SDK Telemetry. */
public interface Telemetry {

  Item addData(String caption, String format, Object... args);
  Item addData(String caption, Object value);
  Line addLine();
  Line addLine(String lineCaption);
  void clear();
  void clearAll();
  boolean update();

  interface Item {
    Item addData(String caption, String format, Object... args);
    Item addData(String caption, Object value);
  }

  interface Line {
    Item addData(String caption, String format, Object... args);
    Item addData(String caption, Object value);
  }
}
