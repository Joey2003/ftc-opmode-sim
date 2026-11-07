package org.nycfirst.opmodesim;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

/**
 * Inter + Roboto Mono, bundled in the jar (SIL OFL 1.1, licenses next to the .ttf files).
 * Registered with Java so Swing styled text can use the family names too.
 */
final class Fonts {
  private Fonts() {}

  private static final Font REGULAR = load("Inter-Regular.ttf", Font.SANS_SERIF, Font.PLAIN);
  private static final Font SEMIBOLD = load("Inter-SemiBold.ttf", Font.SANS_SERIF, Font.BOLD);
  private static final Font BOLD = load("Inter-Bold.ttf", Font.SANS_SERIF, Font.BOLD);
  private static final Font MONO = load("RobotoMono-Regular.ttf", Font.MONOSPACED, Font.PLAIN);

  private static Font load(String file, String fallback, int fallbackStyle) {
    try (InputStream in = Fonts.class.getResourceAsStream("fonts/" + file)) {
      Font f = Font.createFont(Font.TRUETYPE_FONT, in);
      GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
      return f;
    } catch (Exception e) {
      return new Font(fallback, fallbackStyle, 12);
    }
  }

  static Font regular(float size) { return REGULAR.deriveFont(size); }

  static Font semibold(float size) { return SEMIBOLD.deriveFont(size); }

  static Font bold(float size) { return BOLD.deriveFont(size); }

  static Font mono(float size) { return MONO.deriveFont(size); }

  static String uiFamily() { return REGULAR.getFamily(); }

  static String monoFamily() { return MONO.getFamily(); }
}
