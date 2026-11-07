package org.nycfirst.opmodesim;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;

/**
 * OnBot Java-style editor, 680 x 720. Layout and colors from Figma "Editor" (node 3:2).
 * Build Everything saves the file, compiles it at runtime and reports in the Messages pane.
 * Plain Swing: usable with or without Processing.
 */
public class EditorPanel extends JPanel {

  public static final int WIDTH = 680;
  public static final int HEIGHT = 720;

  // ---- Figma colors ----
  static final Color RED = new Color(0xC62828);
  static final Color RED_DARK = new Color(0xB71C1C);
  static final Color RED_LIGHT_TEXT = new Color(0xFFCDD2);
  static final Color PANE_BG = new Color(0xF3F3F3);
  static final Color DIVIDER = new Color(0xD0D0D0);
  static final Color LABEL_GRAY = new Color(0x666666);
  static final Color TEXT = new Color(0x333333);
  static final Color TEXT_DARK = new Color(0x1A1A1A);
  static final Color SELECTED = new Color(0xDCE6F5);
  static final Color TABS_BG = new Color(0xECECEC);
  static final Color GUTTER_BG = new Color(0xF7F7F7);
  static final Color LINE_NUM = new Color(0x9E9E9E);
  static final Color CODE = new Color(0x1F1F1F);
  static final Color MESSAGES_BG = new Color(0xFAFAFA);
  static final Color SUCCESS = new Color(0x2E7D32);
  static final Color ERROR_LINE_BG = new Color(0xFDE0E0);

  static final int CODE_LINE_HEIGHT = 18;

  private final OpModeSim sim;
  private final File file;

  private final CodePane code = new CodePane();
  private final Gutter gutter = new Gutter();
  private final JTextPane messages = new JTextPane();
  private final BuildButton buildButton = new BuildButton();
  private final UndoManager undo = new UndoManager();
  private final Set<Integer> errorLines = new HashSet<>();
  private volatile boolean building = false;

  public EditorPanel(OpModeSim sim, File file) {
    this.sim = sim;
    this.file = file;
    setLayout(null);
    setBackground(Color.WHITE);
    setPreferredSize(new Dimension(WIDTH, HEIGHT));
    setSize(WIDTH, HEIGHT);

    add(topBar());
    add(projectFiles());
    add(editorTabs());
    add(codeArea());
    add(messagePane());

    loadFile();
  }

  // ---------------------------------------------------------------- layout

  private JComponent topBar() {
    JPanel bar = panel(0, 0, WIDTH, 36, RED);
    bar.add(label("Blocks", Fonts.ui(Font.BOLD, 13), RED_LIGHT_TEXT, 14, 10));
    JPanel tab = panel(70, 0, 96, 36, RED_DARK);
    tab.add(label("OnBotJava", Fonts.ui(Font.BOLD, 13), Color.WHITE, 12, 10));
    bar.add(tab);
    return bar;
  }

  private JComponent projectFiles() {
    JPanel p = panel(0, 36, 150, 684, PANE_BG);
    p.add(panel(149, 0, 1, 684, DIVIDER));
    String[][] icons = {
        {"settings", "Settings (not in sim)"},
        {"undo", "Undo"},
        {"addfile", "New file (not in sim)"},
        {"upload", "Upload files (not in sim)"},
        {"collapse", "Collapse (not in sim)"},
    };
    for (int i = 0; i < icons.length; i++) {
      JLabel icon = new JLabel(SvgIcon.load(icons[i][0], 18));
      icon.setBounds(10 + 27 * i, 12, 18, 18);
      icon.setToolTipText(icons[i][1]);
      if (icons[i][0].equals("undo")) {
        icon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        icon.addMouseListener(new MouseAdapter() {
          @Override public void mouseClicked(MouseEvent e) { undo(); }
        });
      }
      p.add(icon);
    }
    p.add(panel(0, 40, 150, 1, DIVIDER));
    p.add(label("Project Files", Fonts.ui(Font.BOLD, 12), LABEL_GRAY, 10, 50));
    p.add(label("▾ org.firstinspires.", Fonts.ui(Font.PLAIN, 11), TEXT, 10, 72));
    p.add(label("    ftc.teamcode", Fonts.ui(Font.PLAIN, 11), TEXT, 10, 88));
    JPanel sel = panel(0, 108, 149, 22, SELECTED);
    sel.add(label(file.getName(), Fonts.ui(Font.BOLD, 11), TEXT_DARK, 24, 4));
    p.add(sel);
    return p;
  }

  private JComponent editorTabs() {
    JPanel tabs = panel(150, 36, 530, 30, TABS_BG);
    JPanel active = panel(0, 0, 150, 30, Color.WHITE);
    active.add(label(file.getName() + "  ×", Fonts.ui(Font.BOLD, 11), TEXT_DARK, 10, 8));
    tabs.add(active);
    return tabs;
  }

  private JComponent codeArea() {
    JScrollPane scroll = new JScrollPane(code);
    scroll.setBounds(150, 66, 530, 474);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setRowHeaderView(gutter);
    scroll.getViewport().setBackground(Color.WHITE);
    scroll.getVerticalScrollBar().setUnitIncrement(CODE_LINE_HEIGHT);
    return scroll;
  }

  private JComponent messagePane() {
    JPanel p = panel(150, 540, 530, 180, MESSAGES_BG);
    p.add(panel(0, 0, 530, 1, DIVIDER));
    p.add(label("Messages", Fonts.ui(Font.BOLD, 12), LABEL_GRAY, 12, 10));

    messages.setEditable(false);
    messages.setOpaque(false);
    messages.setMargin(new Insets(0, 0, 0, 0));
    messages.addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent e) { jumpToErrorAt(messages.viewToModel2D(e.getPoint())); }
    });
    JScrollPane ms = new JScrollPane(messages);
    ms.setBounds(12, 30, 506, 98);
    ms.setBorder(BorderFactory.createEmptyBorder());
    ms.setOpaque(false);
    ms.getViewport().setOpaque(false);
    p.add(ms);

    buildButton.setBounds(382, 134, 136, 32);
    p.add(buildButton);
    return p;
  }

  private static JPanel panel(int x, int y, int w, int h, Color bg) {
    JPanel p = new JPanel(null);
    p.setBounds(x, y, w, h);
    p.setBackground(bg);
    return p;
  }

  /** Label placed like Figma text: (x, y) is the top of the text box. */
  private static JLabel label(String text, Font font, Color color, int x, int y) {
    JLabel l = new JLabel(text);
    l.setFont(font);
    l.setForeground(color);
    Dimension d = l.getPreferredSize();
    l.setBounds(x, y, d.width + 2, d.height);
    return l;
  }

  // ---------------------------------------------------------------- file + build

  private void loadFile() {
    try {
      code.setText(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
      code.setCaretPosition(0);
      undo.discardAllEdits();
    } catch (IOException e) {
      showMessage("Could not open " + file.getName(), RED, e.toString());
    }
  }

  /** Saves the editor text to the file. Returns false (and shows why) on failure. */
  public boolean save() {
    try {
      Files.write(file.toPath(), code.getText().getBytes(StandardCharsets.UTF_8));
      return true;
    } catch (IOException e) {
      showMessage("Could not save " + file.getName(), RED, e.toString());
      return false;
    }
  }

  /** Build Everything: save, compile at runtime, report in Messages. Call on the Swing thread. */
  public void buildEverything() {
    if (building) return;
    if (!save()) return;
    building = true;
    buildButton.repaint();
    showMessage("Building…", LABEL_GRAY, "");
    final String source = code.getText();
    new SwingWorker<OpModeCompiler.Result, Void>() {
      @Override protected OpModeCompiler.Result doInBackground() {
        return sim.build(file.getName(), source);
      }

      @Override protected void done() {
        building = false;
        buildButton.repaint();
        try {
          showResult(get());
        } catch (Exception e) {
          showMessage("Build failed", RED, String.valueOf(e.getCause() != null ? e.getCause() : e));
        }
      }
    }.execute();
  }

  private void showResult(OpModeCompiler.Result r) {
    errorLines.clear();
    if (r.ok) {
      showMessage("Build succeeded!", SUCCESS,
          "OpMode \"" + r.opModeName + "\" is now available on the Driver Station.");
    } else {
      StringBuilder body = new StringBuilder();
      for (OpModeCompiler.Problem p : r.problems) {
        if (p.line > 0) {
          errorLines.add((int) p.line);
          body.append("Line ").append(p.line).append(": ");
        }
        body.append(p.message.replace("\n", "\n    ")).append("\n");
      }
      int n = r.problems.size();
      showMessage("Build failed: " + n + (n == 1 ? " error" : " errors"), RED, body.toString().trim());
    }
    gutter.repaint();
  }

  private void showMessage(String title, Color titleColor, String body) {
    StyledDocument doc = messages.getStyledDocument();
    SimpleAttributeSet t = new SimpleAttributeSet();
    StyleConstants.setFontFamily(t, Fonts.ui(Font.BOLD, 14).getFamily());
    StyleConstants.setFontSize(t, 14);
    StyleConstants.setBold(t, true);
    StyleConstants.setForeground(t, titleColor);
    SimpleAttributeSet b = new SimpleAttributeSet();
    StyleConstants.setFontFamily(b, Fonts.ui(Font.PLAIN, 13).getFamily());
    StyleConstants.setFontSize(b, 13);
    StyleConstants.setForeground(b, TEXT);
    try {
      doc.remove(0, doc.getLength());
      doc.insertString(0, title, t);
      if (!body.isEmpty()) doc.insertString(doc.getLength(), "\n" + body, b);
      SimpleAttributeSet para = new SimpleAttributeSet();
      StyleConstants.setSpaceBelow(para, 4);
      doc.setParagraphAttributes(0, doc.getLength(), para, false);
    } catch (BadLocationException ignored) {
    }
    messages.setCaretPosition(0);
  }

  private static final Pattern LINE_REF = Pattern.compile("^Line (\\d+):");

  /** Click on an error in Messages -> move the cursor to that line. */
  private void jumpToErrorAt(int offset) {
    try {
      Element para = messages.getStyledDocument().getParagraphElement(offset);
      String text = messages.getText(para.getStartOffset(), para.getEndOffset() - para.getStartOffset());
      Matcher m = LINE_REF.matcher(text);
      if (!m.find()) return;
      int line = Integer.parseInt(m.group(1)) - 1;
      Element root = code.getDocument().getDefaultRootElement();
      if (line < 0 || line >= root.getElementCount()) return;
      code.setCaretPosition(root.getElement(line).getStartOffset());
      code.requestFocusInWindow();
      Rectangle2D r = code.modelToView2D(code.getCaretPosition());
      if (r != null) code.scrollRectToVisible(r.getBounds());
    } catch (BadLocationException ignored) {
    }
  }

  private void undo() {
    if (undo.canUndo()) undo.undo();
  }

  private void redo() {
    if (undo.canRedo()) undo.redo();
  }

  // ---------------------------------------------------------------- code pane

  /** Monospace code editor: no soft wrap, 18 px lines, soft tabs, auto-indent, undo. */
  private final class CodePane extends JTextPane {
    CodePane() {
      setMargin(new Insets(8, 8, 8, 6));
      setBackground(Color.WHITE);
      setCaretColor(CODE);
      Font mono = Fonts.mono(12);
      FontMetrics fm = getFontMetrics(mono);
      Style def = getStyledDocument().getStyle(StyleContext.DEFAULT_STYLE);
      StyleConstants.setFontFamily(def, mono.getFamily());
      StyleConstants.setFontSize(def, 12);
      StyleConstants.setForeground(def, CODE);
      StyleConstants.setLineSpacing(def, Math.max(0f, (CODE_LINE_HEIGHT - fm.getHeight()) / (float) fm.getHeight()));
      setFont(mono);

      getDocument().addUndoableEditListener((UndoableEditEvent e) -> {
        if (e.getEdit() instanceof AbstractDocument.DefaultDocumentEvent
            && ((AbstractDocument.DefaultDocumentEvent) e.getEdit()).getType() == DocumentEvent.EventType.CHANGE) {
          return; // style-only change
        }
        undo.addEdit(e.getEdit());
      });
      getDocument().addDocumentListener(new DocumentListener() {
        @Override public void insertUpdate(DocumentEvent e) { edited(); }
        @Override public void removeUpdate(DocumentEvent e) { edited(); }
        @Override public void changedUpdate(DocumentEvent e) {}
      });

      int menu = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menu), "undo", EditorPanel.this::undo);
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menu | InputEvent.SHIFT_DOWN_MASK), "redo", EditorPanel.this::redo);
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_S, menu), "save", EditorPanel.this::save);
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "softTab", () -> replaceSelection("    "));
      bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "autoIndent", this::newlineWithIndent);
    }

    private void bind(KeyStroke ks, String name, Runnable r) {
      getInputMap().put(ks, name);
      getActionMap().put(name, new AbstractAction() {
        @Override public void actionPerformed(ActionEvent e) { r.run(); }
      });
    }

    private void newlineWithIndent() {
      try {
        Element root = getDocument().getDefaultRootElement();
        Element line = root.getElement(root.getElementIndex(getCaretPosition()));
        String text = getDocument().getText(line.getStartOffset(), getCaretPosition() - line.getStartOffset());
        int n = 0;
        while (n < text.length() && text.charAt(n) == ' ') n++;
        replaceSelection("\n" + text.substring(0, n));
      } catch (BadLocationException e) {
        replaceSelection("\n");
      }
    }

    private void edited() {
      if (!errorLines.isEmpty()) {
        errorLines.clear(); // line numbers may have shifted; next build re-marks
      }
      gutter.revalidate();
      gutter.repaint();
    }

    /** No soft wrap: scroll horizontally when a line is wider than the view. */
    @Override public boolean getScrollableTracksViewportWidth() {
      Component p = getParent();
      return p == null || getUI().getPreferredSize(this).width <= p.getSize().width;
    }
  }

  // ---------------------------------------------------------------- gutter

  /** Line numbers, 34 px wide. Lines with build errors are tinted red. */
  private final class Gutter extends JComponent {
    Gutter() {
      setFont(Fonts.mono(12));
    }

    @Override public Dimension getPreferredSize() {
      return new Dimension(34, code.getPreferredSize().height);
    }

    @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      Rectangle clip = g.getClipBounds();
      g2.setColor(GUTTER_BG);
      g2.fillRect(clip.x, clip.y, clip.width, clip.height);
      g2.setFont(getFont());
      FontMetrics fm = g2.getFontMetrics();
      Element root = code.getDocument().getDefaultRootElement();
      int first = root.getElementIndex(code.viewToModel2D(new Point(0, clip.y)));
      int last = root.getElementIndex(code.viewToModel2D(new Point(0, clip.y + clip.height)));
      for (int i = first; i <= last; i++) {
        try {
          Rectangle2D r = code.modelToView2D(root.getElement(i).getStartOffset());
          if (r == null) continue;
          int top = (int) r.getY();
          boolean err = errorLines.contains(i + 1);
          if (err) {
            g2.setColor(ERROR_LINE_BG);
            g2.fillRect(0, top, getWidth(), CODE_LINE_HEIGHT);
          }
          String num = String.valueOf(i + 1);
          g2.setColor(err ? RED : LINE_NUM);
          g2.drawString(num, 30 - fm.stringWidth(num), top + fm.getAscent());
        } catch (BadLocationException ignored) {
        }
      }
    }
  }

  // ---------------------------------------------------------------- button

  /** Red rounded "Build Everything" button with the wrench icon. */
  private final class BuildButton extends JComponent {
    private final Icon wrench = SvgIcon.load("wrench", 18);
    private boolean hover = false;

    BuildButton() {
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      setToolTipText("Save and compile");
      addMouseListener(new MouseAdapter() {
        @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
        @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        @Override public void mouseClicked(MouseEvent e) { buildEverything(); }
      });
    }

    @Override protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setColor(building ? new Color(0xE57373) : hover ? RED_DARK : RED);
      g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
      wrench.paintIcon(this, g2, 10, 7);
      g2.setFont(Fonts.ui(Font.BOLD, 12));
      g2.setColor(Color.WHITE);
      FontMetrics fm = g2.getFontMetrics();
      g2.drawString(building ? "Building…" : "Build Everything", 34, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
      g2.dispose();
    }
  }

  // ---------------------------------------------------------------- for tests

  /** Text of the Messages pane. */
  public String messagesText() {
    return messages.getText();
  }

  public boolean isBuilding() {
    return building;
  }

  public JTextComponent codeComponent() {
    return code;
  }
}
