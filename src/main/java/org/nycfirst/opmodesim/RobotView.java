package org.nycfirst.opmodesim;

import java.awt.*;
import java.awt.geom.*;
import java.math.BigDecimal;

/**
 * Top-down robot view, 600 x 360. Layout from Figma "Robot View" (node 6:2).
 * Wheels-up bench test: wheels spin, robot stays put.
 */
public class RobotView {

  public static final int WIDTH = 600;
  public static final int HEIGHT = 360;

  static final Color BG = Draw.rgb(0x2B2F33);
  static final Color HEADER = Draw.rgb(0x3A4046);
  static final Color TITLE = Draw.rgb(0xE6EDF3);
  static final Color BENCH = Draw.rgb(0xF2C94C);
  static final Color FRONT = Draw.rgb(0x9AA4AE);
  static final Color FRAME = Draw.rgb(0xB8BEC4);
  static final Color WHEEL = Draw.rgb(0x3A3A3A);
  static final Color ROLLER = Draw.rgb(0xE8B500);
  static final Color LABEL = Draw.rgb(0xC9D1D9);
  static final Color GREEN = Draw.rgb(0x3FB950); // FL + BR
  static final Color BLUE = Draw.rgb(0x58A6FF);  // FR + BL (not in the Figma frame; matches its palette)
  static final Color CIRCLE_EDGE = Draw.rgb(0x222222);

  /** Roller stripe speed at full power, px/s. */
  static final double ROLLER_SPEED = 80;

  // Index order matches OpModeSim.motors: FL, FR, BL, BR.
  private static final float[][] WHEEL_XY = {{162, 60}, {402, 60}, {162, 230}, {402, 230}};
  private static final float[][] ARROW_XY = {{136, 64}, {444, 64}, {136, 234}, {444, 234}};
  private static final boolean[] BACKSLASH = {true, false, false, true}; // FL/BR "\", FR/BL "/"
  private static final Color[] PAIR = {GREEN, BLUE, BLUE, GREEN};

  /** The 10 chart patterns (front = open end of the H). */
  public enum Move {
    NONE, FORWARD, BACKWARD, FORWARD_LEFT, FORWARD_RIGHT, BACKWARD_LEFT, BACKWARD_RIGHT,
    STRAFE_LEFT, STRAFE_RIGHT, TURN_CCW, TURN_CW
  }

  private final OpModeSim sim;
  private final double[] phase = new double[4];
  private long lastNanos = System.nanoTime();

  public RobotView(OpModeSim sim) {
    this.sim = sim;
  }

  /** Classifies the wheel spin directions (signs only). FL, FR, BL, BR. */
  public static Move classify(double fl, double fr, double bl, double br) {
    int a = sgn(fl), b = sgn(fr), c = sgn(bl), d = sgn(br);
    if (a == 1 && b == 1 && c == 1 && d == 1) return Move.FORWARD;
    if (a == -1 && b == -1 && c == -1 && d == -1) return Move.BACKWARD;
    if (a == 0 && d == 0 && b == 1 && c == 1) return Move.FORWARD_LEFT;
    if (a == 0 && d == 0 && b == -1 && c == -1) return Move.BACKWARD_RIGHT;
    if (b == 0 && c == 0 && a == 1 && d == 1) return Move.FORWARD_RIGHT;
    if (b == 0 && c == 0 && a == -1 && d == -1) return Move.BACKWARD_LEFT;
    if (b == 1 && c == 1 && a == -1 && d == -1) return Move.STRAFE_LEFT;
    if (b == -1 && c == -1 && a == 1 && d == 1) return Move.STRAFE_RIGHT;
    if (a == -1 && c == -1 && b == 1 && d == 1) return Move.TURN_CCW;
    if (a == 1 && c == 1 && b == -1 && d == -1) return Move.TURN_CW;
    return Move.NONE;
  }

  private static int sgn(double v) {
    return v > 1e-6 ? 1 : v < -1e-6 ? -1 : 0;
  }

  public void paint(Graphics2D g0) {
    Graphics2D g = (Graphics2D) g0.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    long now = System.nanoTime();
    double dt = Math.min(0.1, (now - lastNanos) / 1e9);
    lastNanos = now;
    double[] spin = new double[4];
    for (int i = 0; i < 4; i++) {
      spin[i] = sim.motors[i].wheelSpin();
      phase[i] = (phase[i] + spin[i] * ROLLER_SPEED * dt) % 10.0;
    }

    g.setColor(BG);
    g.fillRect(0, 0, WIDTH, HEIGHT);
    g.setColor(HEADER);
    g.fillRect(0, 0, WIDTH, 28);
    Draw.text(g, "ROBOT VIEW · TOP DOWN", Fonts.bold(12), TITLE, 12, 6, Draw.LEFT);
    Draw.text(g, "BENCH TEST: wheels off ground", Fonts.semibold(12), BENCH, 588, 6, Draw.RIGHT);
    Draw.text(g, "▲ FRONT", Fonts.bold(11), FRONT, 300, 36, Draw.CENTER);

    // H-frame, open end at the front
    g.setColor(FRAME);
    g.fill(new RoundRectangle2D.Float(202, 58, 26, 250, 4, 4));
    g.fill(new RoundRectangle2D.Float(372, 58, 26, 250, 4, 4));
    g.fill(new RoundRectangle2D.Float(228, 170, 144, 24, 4, 4));
    g.fill(new RoundRectangle2D.Float(228, 282, 144, 26, 4, 4));

    for (int i = 0; i < 4; i++) {
      wheel(g, WHEEL_XY[i][0], WHEEL_XY[i][1], BACKSLASH[i], phase[i]);
      if (spin[i] != 0) arrow(g, ARROW_XY[i][0], ARROW_XY[i][1], spin[i] > 0, PAIR[i]);
      label(g, i);
    }

    directionCircle(g, classify(spin[0], spin[1], spin[2], spin[3]));

    Draw.text(g, "▲ forward   ▼ reverse   green = FL+BR   blue = FR+BL",
        Fonts.semibold(12), LABEL, 12, 336, Draw.LEFT);
    g.dispose();
  }

  private void wheel(Graphics2D g0, float x, float y, boolean backslash, double phase) {
    Graphics2D g = (Graphics2D) g0.create();
    Shape body = new RoundRectangle2D.Float(x, y, 36, 76, 14, 14);
    g.setColor(WHEEL);
    g.fill(body);
    g.clip(body);
    g.translate(x, y);
    g.setColor(ROLLER);
    g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    // Forward spin: the top of the wheel moves toward the front, so stripes move up.
    float off = (float) -phase;
    for (int k = -5; k <= 10; k++) {
      float y0 = k * 10 + off;
      if (backslash) g.draw(new Line2D.Float(2, y0, 34, y0 + 20));
      else g.draw(new Line2D.Float(2, y0 + 20, 34, y0));
    }
    g.dispose();
  }

  /** Figma "Arrow FL (reverse)" geometry; flipped vertically for forward. */
  private void arrow(Graphics2D g0, float x, float y, boolean forward, Color c) {
    Graphics2D g = (Graphics2D) g0.create();
    g.translate(x, y);
    if (forward) {
      g.translate(0, 68);
      g.scale(1, -1);
    }
    g.setColor(c);
    g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.draw(new Line2D.Float(10, 2, 10, 57));
    Path2D.Float head = new Path2D.Float();
    head.moveTo(2, 49);
    head.lineTo(10, 64);
    head.lineTo(18, 49);
    g.draw(head);
    g.dispose();
  }

  private void label(Graphics2D g, int i) {
    SimMotor m = sim.motors[i];
    double p = m.getPower();
    String[] lines = {
        m.name() + " [" + m.getPortNumber() + "]",
        "pwr " + (p == 0 ? "0" : new BigDecimal(String.format("%.2f", p)).stripTrailingZeros().toPlainString()),
        "enc " + m.getCurrentPosition(),
    };
    boolean left = m.isLeftSide();
    Color c = p != 0 ? Color.WHITE : LABEL;
    float top = i < 2 ? 72 : 238;
    Draw.lines(g, lines, Fonts.semibold(13), c, left ? 132 : 468, top, 18, left ? Draw.RIGHT : Draw.LEFT);
  }

  private void directionCircle(Graphics2D g0, Move move) {
    Graphics2D g = (Graphics2D) g0.create();
    g.setColor(Color.WHITE);
    g.fill(new Ellipse2D.Float(261, 143, 78, 78));
    g.setColor(CIRCLE_EDGE);
    g.setStroke(new BasicStroke(2));
    g.draw(new Ellipse2D.Float(261, 143, 78, 78));
    if (move == Move.NONE) {
      g.dispose();
      return; // no clean move: stays empty
    }
    g.translate(300, 182);
    g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    Color dark = CIRCLE_EDGE;
    double s = Math.sqrt(0.5);
    switch (move) {
      case FORWARD: straight(g, 0, -1, dark); break;
      case BACKWARD: straight(g, 0, 1, dark); break;
      case STRAFE_LEFT: straight(g, -1, 0, dark); break;
      case STRAFE_RIGHT: straight(g, 1, 0, dark); break;
      case FORWARD_LEFT: straight(g, -s, -s, BLUE); break;   // FR + BL
      case BACKWARD_RIGHT: straight(g, s, s, BLUE); break;
      case FORWARD_RIGHT: straight(g, s, -s, GREEN); break;  // FL + BR
      case BACKWARD_LEFT: straight(g, -s, s, GREEN); break;
      case TURN_CCW: rotation(g, false, dark); break;
      case TURN_CW: rotation(g, true, dark); break;
      default:
    }
    g.dispose();
  }

  private static void straight(Graphics2D g, double dx, double dy, Color c) {
    g.setColor(c);
    double len = 24;
    float x1 = (float) (-dx * len), y1 = (float) (-dy * len), x2 = (float) (dx * len), y2 = (float) (dy * len);
    g.draw(new Line2D.Float(x1, y1, x2, y2));
    head(g, x2, y2, Math.atan2(dy, dx));
  }

  private static void rotation(Graphics2D g, boolean clockwise, Color c) {
    g.setColor(c);
    float r = 20;
    // Arc with a gap at the top; arrowhead at the end in the turn direction.
    double start = clockwise ? 60 : 120;        // degrees, Java2D: counter-clockwise from 3 o'clock
    double extent = clockwise ? -300 : 300;
    g.draw(new Arc2D.Float(-r, -r, 2 * r, 2 * r, (float) start, (float) extent, Arc2D.OPEN));
    double end = Math.toRadians(start + extent);
    float ex = (float) (r * Math.cos(end)), ey = (float) (-r * Math.sin(end));
    // Tangent direction at the end of the arc (screen coords, y down).
    double tangent = clockwise ? Math.atan2(Math.cos(end), Math.sin(end)) : Math.atan2(-Math.cos(end), -Math.sin(end));
    head(g, ex, ey, tangent);
  }

  private static void head(Graphics2D g, float x, float y, double angle) {
    double a = Math.toRadians(35), len = 12;
    Path2D.Float p = new Path2D.Float();
    p.moveTo(x - len * Math.cos(angle - a), y - len * Math.sin(angle - a));
    p.lineTo(x, y);
    p.lineTo(x - len * Math.cos(angle + a), y - len * Math.sin(angle + a));
    g.draw(p);
  }
}
