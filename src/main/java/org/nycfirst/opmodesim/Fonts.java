package org.nycfirst.opmodesim;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Figma uses Inter + Roboto Mono; fall back to Mac/system fonts when not installed. */
final class Fonts {
  private Fonts() {}

  private static final Set<String> FAMILIES = new HashSet<>(Arrays.asList(
      GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

  private static final String UI = pick("Inter", "Helvetica Neue", "Helvetica", Font.SANS_SERIF);
  private static final String MONO = pick("Roboto Mono", "Menlo", "Consolas", Font.MONOSPACED);

  private static String pick(String... names) {
    for (String n : names) if (FAMILIES.contains(n)) return n;
    return names[names.length - 1];
  }

  static Font ui(int style, int size) { return new Font(UI, style, size); }

  static Font mono(int size) { return new Font(MONO, Font.PLAIN, size); }
}
