// Day 2 check: can a Swing text editor sit next to the Processing canvas in one window?
// Layout v2: editor 680 wide on the left, sim drawn on the right 600x720.
// Sketch is 1280x720; the editor is layered above the left 680 px of the canvas.
import javax.swing.*;
import java.awt.*;
import processing.awt.PSurfaceAWT;

final int EDITOR_W = 680;
JTextArea editor;

void setup() {
  size(1280, 720);
}

// Processing sizes the window after setup(), so add the editor after the first frame.
void addEditor() {
  final JFrame frame = (JFrame) ((PSurfaceAWT.SmoothCanvas) surface.getNative()).getFrame();
  SwingUtilities.invokeLater(new Runnable() {
    public void run() {
      editor = new JTextArea("package org.firstinspires.ftc.teamcode;\n\n// Type here.\n");
      editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
      JScrollPane scroll = new JScrollPane(editor);
      scroll.setBounds(0, 0, EDITOR_W, 720);
      frame.getLayeredPane().add(scroll, JLayeredPane.PALETTE_LAYER);
      frame.getLayeredPane().revalidate();
      frame.getLayeredPane().repaint();
    }
  });
}

void draw() {
  if (frameCount == 2) addEditor();
  background(40, 60, 90);
  fill(255);
  textSize(18);
  int x = EDITOR_W + 20;
  text("Processing canvas (sim area)", x, 40);
  text("frame " + frameCount + "  mouse " + mouseX + "," + mouseY, x, 70);
  text("editor chars: " + (editor != null ? editor.getText().length() : 0), x, 100);
  if (frameCount == 90) report();
}

void report() {
  PSurfaceAWT.SmoothCanvas canvas = (PSurfaceAWT.SmoothCanvas) surface.getNative();
  JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, editor);
  println("SWINGCHECK canvas bounds=" + canvas.getBounds() + " showing=" + canvas.isShowing());
  println("SWINGCHECK editor bounds=" + scroll.getBounds() + " showing=" + editor.isShowing());
  println("SWINGCHECK frame size=" + canvas.getFrame().getSize());
}
