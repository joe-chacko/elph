package io.openliberty.elph;

public enum Util {
    ;

    static boolean isMacOS() { return "Mac OS X".equals(System.getProperty("os.name")); }
}

enum OS {
    LINUX,
    MAC,
    WINDOWS;
    static OS current() {return valueOf(System.getProperty("os.name").toUpperCase().split(" ")[0]);}
    static boolean is(OS os) { return current() == os; }
}