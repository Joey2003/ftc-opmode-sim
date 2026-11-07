package org.nycfirst.opmodesim;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Console driver: java -cp ftc-opmode-sim.jar org.nycfirst.opmodesim.ConsoleMain Foo.java
 * Commands on stdin: i = INIT, p = PLAY, s = STOP, q = quit.
 */
public class ConsoleMain {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("usage: ConsoleMain <OpMode.java>");
      System.exit(2);
    }
    OpModeSim sim = new OpModeSim();
    if (!sim.load(new File(args[0]))) System.exit(1);
    System.out.println("Commands: i=INIT  p=PLAY  s=STOP  q=quit");

    final boolean[] quit = {false};
    Thread reader = new Thread(() -> {
      try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
        for (String line; (line = in.readLine()) != null; ) {
          switch (line.trim()) {
            case "i": sim.init(); break;
            case "p": sim.play(); break;
            case "s": sim.stop(); break;
            case "q": quit[0] = true; return;
            default: System.out.println("?  i=INIT  p=PLAY  s=STOP  q=quit");
          }
        }
      } catch (Exception ignored) {
      }
      quit[0] = true;
    });
    reader.setDaemon(true);
    reader.start();

    while (!quit[0]) {
      sim.update();
      Thread.sleep(50);
    }
    sim.stop();
    Thread.sleep(200);
    sim.update();
  }
}
