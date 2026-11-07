package org.nycfirst.opmodesim;

import java.awt.*;
import java.awt.geom.Path2D;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;

/**
 * Minimal SVG icon: stroked &lt;path&gt; elements with absolute M L H V C Z commands
 * (all the Figma editor icons use). Swing has no SVG support.
 */
final class SvgIcon implements Icon {

  private static final Pattern PATH = Pattern.compile("<path\\b([^>]*)/?>");
  private static final Pattern ATTR = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");
  private static final Pattern TOKEN = Pattern.compile("[MLHVCZmlhvcz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");

  private static final class Stroke {
    Shape shape;
    Color color;
    BasicStroke stroke;
  }

  private final List<Stroke> strokes = new ArrayList<>();
  private final int viewBox;
  private final int size;

  private SvgIcon(String svg, int viewBox, int size) {
    this.viewBox = viewBox;
    this.size = size;
    Matcher pm = PATH.matcher(svg);
    while (pm.find()) {
      String d = null, color = "#000000", cap = "butt", join = "miter";
      float width = 1;
      Matcher am = ATTR.matcher(pm.group(1));
      while (am.find()) {
        switch (am.group(1)) {
          case "d": d = am.group(2); break;
          case "stroke": color = am.group(2); break;
          case "stroke-width": width = Float.parseFloat(am.group(2)); break;
          case "stroke-linecap": cap = am.group(2); break;
          case "stroke-linejoin": join = am.group(2); break;
          default:
        }
      }
      if (d == null || color.equals("none")) continue;
      Stroke s = new Stroke();
      s.shape = parse(d);
      s.color = color.equals("white") ? Color.WHITE : Color.decode(color);
      s.stroke = new BasicStroke(width,
          cap.equals("round") ? BasicStroke.CAP_ROUND : cap.equals("square") ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT,
          join.equals("round") ? BasicStroke.JOIN_ROUND : join.equals("bevel") ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER);
      strokes.add(s);
    }
  }

  /** Loads icons/&lt;name&gt;.svg from the library jar. Missing icon = blank. */
  static SvgIcon load(String name, int size) {
    try (InputStream in = SvgIcon.class.getResourceAsStream("icons/" + name + ".svg")) {
      String svg = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
      return new SvgIcon(svg, 18, size);
    } catch (Exception e) {
      return new SvgIcon("", 18, size);
    }
  }

  private static Shape parse(String d) {
    Path2D.Float p = new Path2D.Float();
    Matcher m = TOKEN.matcher(d);
    List<String> t = new ArrayList<>();
    while (m.find()) t.add(m.group());
    char cmd = 'M';
    float x = 0, y = 0;
    int i = 0;
    while (i < t.size()) {
      String tok = t.get(i);
      if (Character.isLetter(tok.charAt(0))) {
        cmd = Character.toUpperCase(tok.charAt(0));
        i++;
        if (cmd == 'Z') {
          p.closePath();
          continue;
        }
      }
      switch (cmd) {
        case 'M':
          x = f(t, i++); y = f(t, i++);
          p.moveTo(x, y);
          cmd = 'L'; // extra pairs after M are line-tos
          break;
        case 'L':
          x = f(t, i++); y = f(t, i++);
          p.lineTo(x, y);
          break;
        case 'H':
          x = f(t, i++);
          p.lineTo(x, y);
          break;
        case 'V':
          y = f(t, i++);
          p.lineTo(x, y);
          break;
        case 'C': {
          float x1 = f(t, i++), y1 = f(t, i++), x2 = f(t, i++), y2 = f(t, i++);
          x = f(t, i++); y = f(t, i++);
          p.curveTo(x1, y1, x2, y2, x, y);
          break;
        }
        default:
          i++; // unsupported command: skip token
      }
    }
    return p;
  }

  private static float f(List<String> t, int i) {
    return i < t.size() ? Float.parseFloat(t.get(i)) : 0;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g2.translate(x, y);
    g2.scale(size / (double) viewBox, size / (double) viewBox);
    for (Stroke s : strokes) {
      g2.setColor(s.color);
      g2.setStroke(s.stroke);
      g2.draw(s.shape);
    }
    g2.dispose();
  }

  @Override public int getIconWidth() { return size; }
  @Override public int getIconHeight() { return size; }
}
