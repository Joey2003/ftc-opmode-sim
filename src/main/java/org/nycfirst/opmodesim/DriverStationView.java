package org.nycfirst.opmodesim;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

/**
 * Driver Station panel, 600 x 360: the REV Driver Station screen at 0.75 scale.
 * Layout from Figma "Driver Station" (node 4:27).
 * Working: OpMode bar, INIT -> PLAY -> STOP button, Status line, telemetry, errors.
 * Static: timer, battery, network, User 1/2, menu.
 */
public class DriverStationView {

  public static final int WIDTH = 600;
  public static final int HEIGHT = 360;

  static final Color BG = Draw.rgb(0x3C0007);
  static final Color TOP_BAR = Draw.rgb(0x2A2A2A);
  static final Color BORDER = Draw.rgb(0xC8101E);
  static final Color MENU_BG = Draw.rgb(0x3A2A2A);
  static final Color BATTERY_GREEN = Draw.rgb(0x3FA535);
  static final Color BUTTON = Draw.rgb(0xD32F2F);
  static final Color BUTTON_HOVER = Draw.rgb(0xE53935);
  static final Color HINT = Draw.rgb(0xE8B4B8);
  static final Color ERROR = Draw.rgb(0xF2C94C);
  static final float BW = 1.579f; // border width

  // Big button (Figma "STOP button")
  static final float BTN_X = 81.32f, BTN_Y = 214.74f, BTN_D = 105f;

  // Telemetry text area
  static final float TEL_X = 306.32f, TEL_Y = 100.26f, TEL_LINE = 14.211f, TEL_BOTTOM = 356f;

  private final OpModeSim sim;
  private final SvgIcon connectedBg = SvgIcon.ui("connected_bg");
  private final SvgIcon signal = SvgIcon.ui("signal");
  private final SvgIcon menuRing = SvgIcon.ui("menu_ring");
  private final SvgIcon menuDot = SvgIcon.ui("menu_dot");
  private final SvgIcon playCircle = SvgIcon.ui("play_circle");
  private final SvgIcon playTri = SvgIcon.ui("play_tri");
  private final SvgIcon configIcon = SvgIcon.ui("config_icon");
  private final SvgIcon dsBatt = SvgIcon.ui("ds_batt");
  private final SvgIcon robotBatt = SvgIcon.ui("robot_batt");
  private final SvgIcon bigCircle = SvgIcon.ui("big_bg_circle");
  private final SvgIcon tri = SvgIcon.ui("tri");
  private final SvgIcon robotIcon = SvgIcon.ui("robot_icon");

  private boolean hoverButton = false;
  private int scroll = 0;          // telemetry scroll, in lines

  public DriverStationView(OpModeSim sim) {
    this.sim = sim;
  }

  // ---------------------------------------------------------------- input (panel coordinates)

  /** True when (x, y) is on the big INIT/PLAY/STOP button and it is active. */
  public boolean overButton(float x, float y) {
    if (sim.runner.opModeName() == null) return false;
    float r = BTN_D / 2, cx = BTN_X + r, cy = BTN_Y + r;
    return (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r;
  }

  public void mouseMoved(float x, float y) {
    hoverButton = overButton(x, y);
  }

  /** INIT -> PLAY -> STOP, like the real Driver Station. */
  public void click(float x, float y) {
    if (!overButton(x, y)) return;
    switch (sim.runner.state()) {
      case IDLE:
      case STOPPED: sim.init(); break;
      case INIT: sim.play(); break;
      case RUNNING: sim.stop(); break;
      default:
    }
  }

  /** Mouse wheel over the telemetry pane scrolls it. */
  public void wheel(float x, float y, int lines) {
    if (x >= 300 && y >= 68) scroll = Math.max(0, scroll + lines);
  }

  // ---------------------------------------------------------------- paint

  public void paint(Graphics2D g0) {
    Graphics2D g = (Graphics2D) g0.create();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    OpModeRunner r = sim.runner;
    OpModeRunner.State state = r.state();
    String name = r.opModeName();

    g.setColor(BG);
    g.fillRect(0, 0, WIDTH, HEIGHT);

    // Top bar (static)
    g.setColor(TOP_BAR);
    g.fill(new Rectangle2D.Float(0, 0, 600, 30));
    connectedBg.paint(g, 52.89f, 0);
    Draw.text(g, "Robot Connected", Fonts.bold(12.632f), Color.WHITE, 88.42f, 7.11f, Draw.LEFT);
    Draw.lines(g, new String[] {"Network: SIM-RC", "Ping: 0ms - sim"}, Fonts.semibold(9.474f), Color.WHITE,
        363.16f, 1.58f, 14.211f, Draw.LEFT);
    signal.paint(g, 463.42f, 7.89f);
    Draw.text(g, "User 1   User 2", Fonts.semibold(7.895f), Color.WHITE, 488.68f, 15f, Draw.LEFT);
    g.setColor(MENU_BG);
    g.fill(new Rectangle2D.Float(561.32f, 0, 38.684f, 68.684f));
    menuRing.paint(g, 570f, 13.42f);
    menuDot.paint(g, 577.89f, 22.89f);
    menuDot.paint(g, 577.89f, 31.58f);
    menuDot.paint(g, 577.89f, 40.26f);

    // Timer + config (static)
    Draw.box(g, 0, 30, 150, 112.895f, BG, BORDER, BW);
    Draw.text(g, "Practice Timer", Fonts.semibold(9.474f), Color.WHITE, 63.95f, 67.89f, Draw.CENTER);
    Draw.text(g, "2:30", Fonts.semibold(17.368f), Color.WHITE, 34.74f, 83.68f, Draw.LEFT);
    playCircle.paint(g, 91.58f, 85.26f);
    playTri.paint(g, 97.89f, 90f);
    Draw.box(g, 150, 30, 150, 112.895f, BG, BORDER, BW);
    configIcon.paint(g, 193.42f, 73.42f);
    Draw.text(g, "SimRobot", Fonts.semibold(8.684f), Color.WHITE, 217.89f, 80.53f, Draw.LEFT);

    // Batteries (static)
    Draw.box(g, 300, 30, 82.895f, 38.684f, BG, BORDER, BW);
    Draw.text(g, "DS: 97%", Fonts.semibold(8.684f), Color.WHITE, 341.45f, 36.32f, Draw.CENTER);
    dsBatt.paint(g, 328.42f, 51.32f);
    Draw.box(g, 382.89f, 30, 178.421f, 38.684f, BATTERY_GREEN, BORDER, BW);
    robotBatt.paint(g, 424.74f, 39.47f);
    Draw.text(g, "13.11 V", Fonts.regular(13.421f), Color.WHITE, 476.84f, 34.74f, Draw.LEFT);
    Draw.text(g, "( 13.11 V )", Fonts.regular(7.895f), Color.WHITE, 484.74f, 53.68f, Draw.LEFT);

    // Button area
    g.setColor(BG);
    g.fill(new Rectangle2D.Float(0, 180, 300, 180));
    bigCircle.paint(g, 10.26f, 133.42f);
    if (name != null) bigButton(g, state);
    Draw.text(g, "INIT → PLAY ▶ → STOP ■", Fonts.semibold(9.474f), HINT, 150, 337.89f, Draw.CENTER);

    // Telemetry pane
    Draw.box(g, 300, 68.68f, 300, 291.316f, BG, BORDER, BW);
    Draw.text(g, "Status : " + status(state, name), Fonts.regular(11.053f), Color.WHITE, TEL_X, 78.16f, Draw.LEFT);
    telemetry(g);

    // OpMode bar (on top of the big circle)
    g.setColor(BORDER);
    g.fill(new Rectangle2D.Float(0, 142.89f, 300, 37.895f));
    tri.paint(g, 19.74f, 146.84f);
    tri.paint(g, 250.26f, 146.84f);
    String[] bar = name == null ? new String[] {"", "No Op Mode"} : new String[] {r.opModeKind(), name};
    Draw.lines(g, bar, Fonts.semibold(11.053f), Color.WHITE, 150, 144.47f, 16.579f, Draw.CENTER);

    robotIcon.paint(g, 14.21f, 4.74f);
    g.dispose();
  }

  private String status(OpModeRunner.State state, String name) {
    if (name == null) return "No Op Mode selected";
    switch (state) {
      case INIT: return "Initialized " + name;
      case RUNNING: return "Running " + name;
      case STOPPED: return sim.runner.error() != null ? "Stopped (error)" : "Stopped " + name;
      default: return name + " selected";
    }
  }

  /** INIT text, PLAY triangle or STOP square on the red circle. */
  private void bigButton(Graphics2D g, OpModeRunner.State state) {
    g.setColor(hoverButton ? BUTTON_HOVER : BUTTON);
    g.fill(new Ellipse2D.Float(BTN_X, BTN_Y, BTN_D, BTN_D));
    float cx = BTN_X + BTN_D / 2, cy = BTN_Y + BTN_D / 2;
    g.setColor(Color.WHITE);
    switch (state) {
      case INIT: {
        Path2D.Float play = new Path2D.Float();
        play.moveTo(cx - 13, cy - 20);
        play.lineTo(cx + 21, cy);
        play.lineTo(cx - 13, cy + 20);
        play.closePath();
        g.fill(play);
        break;
      }
      case RUNNING:
        g.fill(new RoundRectangle2D.Float(116.84f, 250.26f, 34.737f, 34.737f, 4.736f, 4.736f));
        break;
      default:
        Draw.text(g, "INIT", Fonts.bold(22f), Color.WHITE, cx, cy - 13.5f, Draw.CENTER);
    }
  }

  private void telemetry(Graphics2D g0) {
    Graphics2D g = (Graphics2D) g0.create();
    g.clip(new Rectangle2D.Float(301.6f, TEL_Y - 2, 296.8f, TEL_BOTTOM - TEL_Y + 2));
    String error = sim.runner.error();
    if (error != null) {
      List<String> lines = Draw.wrap(g, "Error: " + error, Fonts.regular(11.053f), 284f);
      Draw.lines(g, lines.toArray(new String[0]), Fonts.regular(11.053f), ERROR, TEL_X, TEL_Y, 15f, Draw.LEFT);
      g.dispose();
      return;
    }
    List<String> lines = sim.telemetry.lines();
    int visible = (int) ((TEL_BOTTOM - TEL_Y) / TEL_LINE);
    scroll = Math.min(scroll, Math.max(0, lines.size() - visible));
    int end = Math.min(lines.size(), scroll + visible + 1);
    String[] shown = lines.subList(Math.min(scroll, end), end).toArray(new String[0]);
    Draw.lines(g, shown, Fonts.mono(9.474f), Color.WHITE, TEL_X, TEL_Y, TEL_LINE, Draw.LEFT);
    if (lines.size() > visible) { // scroll hint
      g.setColor(new Color(255, 255, 255, 90));
      float trackH = TEL_BOTTOM - TEL_Y;
      float thumbH = Math.max(12, trackH * visible / lines.size());
      float thumbY = TEL_Y + (trackH - thumbH) * scroll / Math.max(1, lines.size() - visible);
      g.fill(new RoundRectangle2D.Float(593, thumbY, 3, thumbH, 3, 3));
    }
    g.dispose();
  }
}
