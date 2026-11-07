package org.nycfirst.opmodesim;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import processing.awt.PSurfaceAWT;
import processing.core.PApplet;

/**
 * Puts the OnBot Java-style editor on the left 680 px of a 1280x720 sketch.
 * The editor is a Swing layer above the Processing canvas (JAVA2D renderer only).
 * The sketch should draw the sim in x >= 680.
 *
 * <pre>
 *   sim = new OpModeSim();
 *   editor = OnBotEditor.attach(this, sim, new File(dataPath("FourMotorTest.java")));
 * </pre>
 */
public class OnBotEditor {

  private final PApplet app;
  private final OpModeSim sim;
  private final File file;
  private volatile EditorPanel panel;
  private boolean attached = false;

  private OnBotEditor(PApplet app, OpModeSim sim, File file) {
    this.app = app;
    this.sim = sim;
    this.file = file;
  }

  /** Call from setup(). The editor appears after the first frame and runs a first build. */
  public static OnBotEditor attach(PApplet app, OpModeSim sim, File javaFile) {
    OnBotEditor e = new OnBotEditor(app, sim, javaFile);
    app.registerMethod("pre", e);
    return e;
  }

  /** Processing hook. Public only because Processing requires it. */
  public void pre() {
    // Processing sizes the window after setup(), so wait for the first frame.
    if (attached || app.frameCount < 2) return;
    attached = true;
    app.unregisterMethod("pre", this);
    Object nativeSurface = app.getSurface().getNative();
    if (!(nativeSurface instanceof PSurfaceAWT.SmoothCanvas)) {
      PApplet.println("OnBotEditor needs the default renderer (size(1280, 720) with no P2D/P3D).");
      return;
    }
    final PSurfaceAWT.SmoothCanvas canvas = (PSurfaceAWT.SmoothCanvas) nativeSurface;
    final JFrame frame = (JFrame) canvas.getFrame();
    SwingUtilities.invokeLater(() -> {
      EditorPanel p = new EditorPanel(sim, file);
      p.setBounds(0, 0, EditorPanel.WIDTH, EditorPanel.HEIGHT);
      frame.getLayeredPane().add(p, JLayeredPane.PALETTE_LAYER);
      frame.getLayeredPane().revalidate();
      frame.getLayeredPane().repaint();
      // Clicking the sim side gives keys back to the sketch.
      canvas.addMouseListener(new MouseAdapter() {
        @Override public void mousePressed(MouseEvent e) { ((Component) e.getSource()).requestFocusInWindow(); }
      });
      panel = p;
      p.buildEverything();
    });
  }

  /** The editor panel, or null until it has appeared. */
  public EditorPanel panel() {
    return panel;
  }
}
