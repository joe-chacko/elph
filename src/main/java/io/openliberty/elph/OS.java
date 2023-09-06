package io.openliberty.elph;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum OS {
    LINUX("eclipse"),
    MAC("Contents/MacOS/eclipse"),
    WINDOWS("eclipse.exe");
    final Path pathToExecutable;
    OS(String relativePath) {
        pathToExecutable = Paths.get(relativePath);
    }

    static OS current() {return valueOf(System.getProperty("os.name").toUpperCase().split(" ")[0]);}
    static boolean is(OS os) { return current() == os; }
}
