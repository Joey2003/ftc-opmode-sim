package org.nycfirst.opmodesim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * Telemetry like the real SDK: lines build up, update() publishes them to the
 * Driver Station and clears the pending list. Captions/values use " : ".
 */
public class SimTelemetry implements Telemetry {

  private static final String CAPTION_SEP = " : ";
  private static final String ITEM_SEP = " | ";

  private final List<LineImpl> pending = new ArrayList<>();
  private volatile List<String> published = Collections.emptyList();
  private volatile long publishCount = 0;

  private final class ItemImpl implements Item {
    final String caption;
    final String format;
    final Object[] args;
    final Object value;

    ItemImpl(String caption, String format, Object[] args, Object value) {
      this.caption = caption;
      this.format = format;
      this.args = args;
      this.value = value;
    }

    String render() {
      String v = format != null ? String.format(format, args) : String.valueOf(value);
      return caption + CAPTION_SEP + v;
    }

    @Override public Item addData(String c, String f, Object... a) { return SimTelemetry.this.addData(c, f, a); }
    @Override public Item addData(String c, Object v) { return SimTelemetry.this.addData(c, v); }
  }

  private final class LineImpl implements Line {
    final String caption;
    final List<ItemImpl> items = new ArrayList<>();

    LineImpl(String caption) { this.caption = caption; }

    String render() {
      StringBuilder sb = new StringBuilder(caption);
      for (int i = 0; i < items.size(); i++) {
        if (i > 0) sb.append(ITEM_SEP);
        sb.append(items.get(i).render());
      }
      return sb.toString();
    }

    @Override public Item addData(String c, String f, Object... a) {
      ItemImpl it = new ItemImpl(c, f, a, null);
      items.add(it);
      return it;
    }

    @Override public Item addData(String c, Object v) {
      ItemImpl it = new ItemImpl(c, null, null, v);
      items.add(it);
      return it;
    }
  }

  @Override public synchronized Item addData(String caption, String format, Object... args) {
    LineImpl line = new LineImpl("");
    pending.add(line);
    return line.addData(caption, format, args);
  }

  @Override public synchronized Item addData(String caption, Object value) {
    LineImpl line = new LineImpl("");
    pending.add(line);
    return line.addData(caption, value);
  }

  @Override public synchronized Line addLine() { return addLine(""); }

  @Override public synchronized Line addLine(String lineCaption) {
    LineImpl line = new LineImpl(lineCaption);
    pending.add(line);
    return line;
  }

  @Override public synchronized void clear() { pending.clear(); }

  @Override public synchronized void clearAll() { pending.clear(); }

  @Override public synchronized boolean update() {
    List<String> out = new ArrayList<>(pending.size());
    for (LineImpl l : pending) out.add(l.render());
    pending.clear();
    published = Collections.unmodifiableList(out);
    publishCount++;
    return true;
  }

  // ---- sim-only ----

  /** Lines currently shown on the Driver Station. */
  public List<String> lines() { return published; }

  /** Increments on every update(); lets the UI tell when telemetry changed. */
  public long publishCount() { return publishCount; }

  public synchronized void reset() {
    pending.clear();
    published = Collections.emptyList();
    publishCount++;
  }
}
