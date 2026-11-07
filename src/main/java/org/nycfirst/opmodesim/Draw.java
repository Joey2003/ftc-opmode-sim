package org.nycfirst.opmodesim;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.ArrayList;
import java.util.List;

/** Java2D text helpers placed like Figma text boxes (y = top of the line box). */
final class Draw {
  private Draw() {}

  static final int LEFT = -1, CENTER = 0, RIGHT = 1;

  static Color rgb(int hex) {
    return new Color(hex);
  }

  /** Font for one character: the bundled font, or a system font if it lacks the glyph (e.g. ▲). */
  private static Font fontFor(Font f, char c) {
    return f.canDisplay(c) ? f : new Font(Font.SANS_SERIF, Font.PLAIN, 1).deriveFont(f.getSize2D());
  }

  static float width(Graphics2D g, String s, Font f) {
    float w = 0;
    for (int i = 0; i < s.length(); ) {
      Font rf = fontFor(f, s.charAt(i));
      int j = i + 1;
      while (j < s.length() && fontFor(f, s.charAt(j)).equals(rf)) j++;
      w += g.getFontMetrics(rf).stringWidth(s.substring(i, j));
      i = j;
    }
    return w;
  }

  /** One line, "normal" line height. */
  static void text(Graphics2D g, String s, Font f, Color c, float x, float yTop, int align) {
    FontRenderContext frc = g.getFontRenderContext();
    LineMetrics lm = f.getLineMetrics("Hg", frc);
    lines(g, new String[] {s}, f, c, x, yTop, lm.getAscent() + lm.getDescent(), align);
  }

  /** Several lines with a fixed line height (Figma "leading"). */
  static void lines(Graphics2D g, String[] lines, Font f, Color c, float x, float yTop, float lineHeight, int align) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    g2.setColor(c);
    LineMetrics lm = f.getLineMetrics("Hg", g2.getFontRenderContext());
    float glyphBox = lm.getAscent() + lm.getDescent();
    for (int n = 0; n < lines.length; n++) {
      String s = lines[n];
      float baseline = yTop + n * lineHeight + (lineHeight - glyphBox) / 2 + lm.getAscent();
      float w = width(g2, s, f);
      float px = align == LEFT ? x : align == CENTER ? x - w / 2 : x - w;
      for (int i = 0; i < s.length(); ) {
        Font rf = fontFor(f, s.charAt(i));
        int j = i + 1;
        while (j < s.length() && fontFor(f, s.charAt(j)).equals(rf)) j++;
        String run = s.substring(i, j);
        g2.setFont(rf);
        g2.drawString(run, px, baseline);
        px += g2.getFontMetrics(rf).stringWidth(run);
        i = j;
      }
    }
    g2.dispose();
  }

  /** Greedy word wrap to a pixel width. */
  static List<String> wrap(Graphics2D g, String text, Font f, float maxWidth) {
    List<String> out = new ArrayList<>();
    for (String para : text.split("\n", -1)) {
      StringBuilder line = new StringBuilder();
      for (String word : para.split(" ")) {
        String trial = line.length() == 0 ? word : line + " " + word;
        if (width(g, trial, f) <= maxWidth || line.length() == 0) {
          line.setLength(0);
          line.append(trial);
        } else {
          out.add(line.toString());
          line.setLength(0);
          line.append(word);
        }
      }
      out.add(line.toString());
    }
    return out;
  }

  /** CSS-style border: stroke drawn inside the box. */
  static void box(Graphics2D g, float x, float y, float w, float h, Color fill, Color border, float bw) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    if (fill != null) {
      g2.setColor(fill);
      g2.fill(new java.awt.geom.Rectangle2D.Float(x, y, w, h));
    }
    if (border != null) {
      g2.setColor(border);
      g2.setStroke(new BasicStroke(bw));
      g2.draw(new java.awt.geom.Rectangle2D.Float(x + bw / 2, y + bw / 2, w - bw, h - bw));
    }
    g2.dispose();
  }
}
