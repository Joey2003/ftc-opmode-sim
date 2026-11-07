package org.nycfirst.opmodesim;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;

/**
 * Minimal SVG renderer for the Figma-exported icons (Swing has no SVG support).
 * Supports &lt;path&gt; (absolute M L H V C Z), &lt;circle&gt;, &lt;ellipse&gt;; fill and stroke.
 * &lt;defs&gt;/&lt;mask&gt; content is skipped; a masked stroke is drawn as an inside stroke.
 */
final class SvgIcon implements Icon {

  private static final Pattern ELEMENT = Pattern.compile("<(path|circle|ellipse)\\b([^>]*)/?>");
  private static final Pattern ATTR = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");
  private static final Pattern TOKEN = Pattern.compile("[MLHVCZmlhvcz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");
  private static final Pattern SIZE = Pattern.compile("viewBox=\"[-\\d.]+ [-\\d.]+ ([\\d.]+) ([\\d.]+)\"");

  private static final class Part {
    Shape shape;
    Color fill;
    Color stroke;
    BasicStroke pen;
  }

  private final List<Part> parts = new ArrayList<>();
  private final float viewW, viewH;
  private final float width, height;

  private SvgIcon(String svg, float width, float height) {
    Matcher vb = SIZE.matcher(svg);
    float vw = width, vh = height;
    if (vb.find()) {
      vw = Float.parseFloat(vb.group(1));
      vh = Float.parseFloat(vb.group(2));
    }
    viewW = vw;
    viewH = vh;
    this.width = width > 0 ? width : vw;
    this.height = height > 0 ? height : vh;

    svg = svg.replaceAll("(?s)<defs>.*?</defs>", "").replaceAll("(?s)<mask\\b.*?</mask>", "");
    Matcher em = ELEMENT.matcher(svg);
    while (em.find()) {
      String tag = em.group(1);
      String d = null, fill = null, stroke = null, cap = "butt", join = "miter";
      float sw = 1, cx = 0, cy = 0, rx = 0, ry = 0;
      boolean masked = false;
      Matcher am = ATTR.matcher(em.group(2));
      while (am.find()) {
        String v = am.group(2);
        switch (am.group(1)) {
          case "d": d = v; break;
          case "fill": fill = v; break;
          case "stroke": stroke = v; break;
          case "stroke-width": sw = Float.parseFloat(v); break;
          case "stroke-linecap": cap = v; break;
          case "stroke-linejoin": join = v; break;
          case "cx": cx = Float.parseFloat(v); break;
          case "cy": cy = Float.parseFloat(v); break;
          case "r": rx = ry = Float.parseFloat(v); break;
          case "rx": rx = Float.parseFloat(v); break;
          case "ry": ry = Float.parseFloat(v); break;
          case "mask": masked = true; break;
          default:
        }
      }
      Part p = new Part();
      if (tag.equals("path")) {
        if (d == null) continue;
        p.shape = parse(d);
      } else {
        if (masked) { // mask = the shape itself: only the inner half of the stroke shows
          rx -= sw / 4;
          ry -= sw / 4;
          sw /= 2;
        }
        p.shape = new Ellipse2D.Float(cx - rx, cy - ry, 2 * rx, 2 * ry);
      }
      // Figma emits fill="black" on degenerate stroke-only paths; those fill nothing.
      p.fill = tag.equals("path") && "black".equals(fill) ? null : color(fill);
      p.stroke = color(stroke);
      p.pen = new BasicStroke(sw,
          cap.equals("round") ? BasicStroke.CAP_ROUND : cap.equals("square") ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT,
          join.equals("round") ? BasicStroke.JOIN_ROUND : join.equals("bevel") ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_MITER);
      if (p.fill != null || p.stroke != null) parts.add(p);
    }
  }

  private static Color color(String c) {
    if (c == null || c.equals("none")) return null;
    if (c.equals("white")) return Color.WHITE;
    if (c.equals("black")) return Color.BLACK;
    try {
      return Color.decode(c);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String read(String resource) {
    try (InputStream in = SvgIcon.class.getResourceAsStream(resource)) {
      return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return "";
    }
  }

  /** Editor toolbar icon: icons/&lt;name&gt;.svg drawn at size x size. */
  static SvgIcon load(String name, int size) {
    return new SvgIcon(read("icons/" + name + ".svg"), size, size);
  }

  /** Sim UI asset: ui/&lt;name&gt;.svg drawn at its natural (Figma) size. */
  static SvgIcon ui(String name) {
    return new SvgIcon(read("ui/" + name + ".svg"), 0, 0);
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

  /** Draws at (x, y) with float precision (Figma positions are fractional). */
  void paint(Graphics2D g, float x, float y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g2.translate(x, y);
    g2.scale(width / viewW, height / viewH);
    for (Part p : parts) {
      if (p.fill != null) {
        g2.setColor(p.fill);
        g2.fill(p.shape);
      }
      if (p.stroke != null) {
        g2.setColor(p.stroke);
        g2.setStroke(p.pen);
        g2.draw(p.shape);
      }
    }
    g2.dispose();
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    paint((Graphics2D) g, x, y);
  }

  @Override public int getIconWidth() { return Math.round(width); }
  @Override public int getIconHeight() { return Math.round(height); }
}
