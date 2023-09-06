package io.openliberty.elph.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum OS {
    LINUX("eclipse"),
    MAC("Contents/MacOS/eclipse"),
    WINDOWS("eclipse.exe");
    public final Path pathToExecutable;
    OS(String relativePath) {
        pathToExecutable = Paths.get(relativePath);
    }

    public static OS current() {return valueOf(System.getProperty("os.name").toUpperCase().split(" ")[0]);}
    public static boolean is(OS os) { return current() == os; }
}
