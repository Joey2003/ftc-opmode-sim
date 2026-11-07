package org.nycfirst.opmodesim;

import java.awt.Graphics2D;
import processing.awt.PGraphicsJava2D;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.event.MouseEvent;

/**
 * Draws the robot view (top-right 600x360) and Driver Station (bottom-right 600x360)
 * every frame, and handles Driver Station clicks. Needs the default renderer.
 *
 * <pre>
 *   SimView.attach(this, sim);   // in setup(); keep an empty draw() in the sketch
 * </pre>
 */
public class SimView {

  public static final int X = 680;           // left edge of the sim area
  public static final int DS_Y = 360;        // top edge of the Driver Station

  private final PApplet app;
  private final OpModeSim sim;
  private final RobotView robot;
  private final DriverStationView ds;
  private boolean warned = false;

  private SimView(PApplet app, OpModeSim sim) {
    this.app = app;
    this.sim = sim;
    this.robot = new RobotView(sim);
    this.ds = new DriverStationView(sim);
  }

  public static SimView attach(PApplet app, OpModeSim sim) {
    SimView v = new SimView(app, sim);
    app.registerMethod("draw", v);
    app.registerMethod("mouseEvent", v);
    return v;
  }

  /** Processing hook (runs after the sketch's draw()). Public only because Processing requires it. */
  public void draw() {
    sim.update(); // console log of events, telemetry, motors
    if (!(app.g instanceof PGraphicsJava2D)) {
      if (!warned) PApplet.println("SimView needs the default renderer: size(1280, 720) with no P2D/P3D.");
      warned = true;
      return;
    }
    Graphics2D g = (Graphics2D) ((PGraphicsJava2D) app.g).g2.create();
    g.translate(X, 0);
    robot.paint(g);
    g.translate(0, DS_Y);
    ds.paint(g);
    g.dispose();
  }

  /** Processing hook. Public only because Processing requires it. */
  public void mouseEvent(MouseEvent e) {
    float x = e.getX() - X, y = e.getY() - DS_Y;
    switch (e.getAction()) {
      case MouseEvent.MOVE:
        ds.mouseMoved(x, y);
        app.cursor(ds.overButton(x, y) ? PConstants.HAND : PConstants.ARROW);
        break;
      case MouseEvent.CLICK:
        ds.click(x, y);
        break;
      case MouseEvent.WHEEL:
        ds.wheel(x, y, e.getCount());
        break;
      default:
    }
  }

  public RobotView robotView() { return robot; }

  public DriverStationView driverStation() { return ds; }
}
