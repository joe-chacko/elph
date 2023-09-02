package io.openliberty.elph;

public enum Util {
    ;

    static boolean isMacOS() { return "Mac OS X".equals(System.getProperty("os.name")); }
}
