package org.nycfirst.opmodesim;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.*;
import javax.tools.*;

/** Compiles student OpMode source in memory with javax.tools and loads it. */
public final class OpModeCompiler {

  private OpModeCompiler() {}

  public static final class Result {
    public final boolean ok;
    public final List<String> errors;
    public final Class<? extends LinearOpMode> opModeClass;
    public final String opModeName;

    Result(List<String> errors, Class<? extends LinearOpMode> cls, String name) {
      this.ok = cls != null;
      this.errors = Collections.unmodifiableList(errors);
      this.opModeClass = cls;
      this.opModeName = name;
    }
  }

  public static Result compile(String fileName, String source) {
    List<String> errors = new ArrayList<>();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      errors.add("No Java compiler available (running on a JRE?). java.home=" + System.getProperty("java.home"));
      return new Result(errors, null, null);
    }

    Map<String, ByteArrayOutputStream> out = new LinkedHashMap<>();
    DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
    StandardJavaFileManager std = compiler.getStandardFileManager(diags, Locale.US, null);
    JavaFileManager fm = new ForwardingJavaFileManager<StandardJavaFileManager>(std) {
      @Override
      public JavaFileObject getJavaFileForOutput(Location loc, String className,
                                                 JavaFileObject.Kind kind, FileObject sibling) {
        return new SimpleJavaFileObject(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
          @Override public OutputStream openOutputStream() {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            out.put(className, b);
            return b;
          }
        };
      }
    };

    JavaFileObject src = new SimpleJavaFileObject(URI.create("string:///" + fileName), JavaFileObject.Kind.SOURCE) {
      @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
    };

    List<String> opts = Arrays.asList("-classpath", sdkClasspath(), "-proc:none", "-g");
    boolean ok = compiler.getTask(null, fm, diags, opts, null, Collections.singletonList(src)).call();
    for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
      if (d.getKind() == Diagnostic.Kind.ERROR) {
        errors.add(fileName + ":" + d.getLineNumber() + ": error: " + d.getMessage(Locale.US));
      }
    }
    if (!ok) return new Result(errors, null, null);

    Map<String, byte[]> bytes = new HashMap<>();
    out.forEach((k, v) -> bytes.put(k, v.toByteArray()));
    MemoryLoader loader = new MemoryLoader(bytes, LinearOpMode.class.getClassLoader());

    for (String className : out.keySet()) {
      try {
        Class<?> c = loader.loadClass(className);
        if (!LinearOpMode.class.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers())) continue;
        if (c.isAnnotationPresent(Disabled.class)) continue;
        String name = null;
        TeleOp t = c.getAnnotation(TeleOp.class);
        Autonomous a = c.getAnnotation(Autonomous.class);
        if (t != null) name = t.name();
        else if (a != null) name = a.name();
        else continue;
        if (name.isEmpty()) name = c.getSimpleName();
        return new Result(errors, c.asSubclass(LinearOpMode.class), name);
      } catch (Throwable e) {
        errors.add("Could not load " + className + ": " + e);
      }
    }
    errors.add("No OpMode found. Add @TeleOp or @Autonomous to a class that extends LinearOpMode.");
    return new Result(errors, null, null);
  }

  /** Classpath holding the fake SDK: wherever LinearOpMode was loaded from, plus the JVM classpath. */
  static String sdkClasspath() {
    StringBuilder cp = new StringBuilder();
    try {
      cp.append(new File(LinearOpMode.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
    } catch (Exception ignored) {
    }
    String jcp = System.getProperty("java.class.path");
    if (jcp != null && !jcp.isEmpty()) cp.append(File.pathSeparator).append(jcp);
    return cp.toString();
  }

  /**
   * Child-first for the freshly compiled classes. Needed because Processing also
   * compiles the student's .java tab with the sketch, so a stale copy of the same
   * class name exists in the parent loader.
   */
  static final class MemoryLoader extends ClassLoader {
    private final Map<String, byte[]> classes;

    MemoryLoader(Map<String, byte[]> classes, ClassLoader parent) {
      super(parent);
      this.classes = classes;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(name)) {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
          byte[] b = classes.get(name);
          if (b == null) return super.loadClass(name, resolve);
          c = defineClass(name, b, 0, b.length);
        }
        if (resolve) resolveClass(c);
        return c;
      }
    }
  }
}
