// Day 1 spike: compile a Java source string at runtime, load it, run it.
import javax.tools.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

String result = "running...";

void setup() {
  size(800, 300);
  result = runSpike();
  println(result);
}

void draw() {
  background(30);
  fill(255);
  textSize(16);
  text(result, 20, 30, width - 40, height - 40);
}

String runSpike() {
  try {
    String src =
      "package org.firstinspires.ftc.teamcode;\n" +
      "public class Hello implements Runnable {\n" +
      "  public void run() { System.out.println(\"Hello from runtime-compiled code\"); }\n" +
      "  public static String msg() { return \"Compiled + loaded + ran OK\"; }\n" +
      "}\n";

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) return "FAIL: no system Java compiler (javax.tools returned null). java.home=" + System.getProperty("java.home");

    Path dir = Files.createTempDirectory("spike");
    Path srcFile = dir.resolve("org/firstinspires/ftc/teamcode/Hello.java");
    Files.createDirectories(srcFile.getParent());
    Files.write(srcFile, src.getBytes("UTF-8"));

    DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<JavaFileObject>();
    StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, null);
    Iterable<? extends JavaFileObject> units = fm.getJavaFileObjects(srcFile.toFile());
    List<String> opts = Arrays.asList("-d", dir.toString(), "--release", "17");
    boolean ok = compiler.getTask(null, fm, diags, opts, null, units).call();
    fm.close();
    if (!ok) {
      StringBuilder sb = new StringBuilder("FAIL: compile errors\n");
      for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) sb.append(d).append("\n");
      return sb.toString();
    }

    URLClassLoader cl = new URLClassLoader(new URL[]{ dir.toUri().toURL() }, getClass().getClassLoader());
    Class<?> c = cl.loadClass("org.firstinspires.ftc.teamcode.Hello");
    ((Runnable) c.getDeclaredConstructor().newInstance()).run();
    String msg = (String) c.getMethod("msg").invoke(null);
    return "PASS: " + msg + "\njava.home=" + System.getProperty("java.home") + "\njava.version=" + System.getProperty("java.version");
  } catch (Throwable t) {
    return "FAIL: " + t;
  }
}
