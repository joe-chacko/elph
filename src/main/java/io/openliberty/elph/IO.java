package io.openliberty.elph;

import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.openliberty.elph.IO.Verbosity.DEBUG;
import static io.openliberty.elph.IO.Verbosity.INFO;
import static io.openliberty.elph.IO.Verbosity.LOG;
import static io.openliberty.elph.IO.Verbosity.OFF;
import static java.util.function.Predicate.not;

class IO {
    private final IO elph = this;

    enum Verbosity {OFF, INFO, LOG, DEBUG}
    /** Use statics so settings are global */
    private static Verbosity verbosity = OFF;
    private static boolean quiet;
    /** Single scanner for the whole process */
    final static Scanner SCANNER = new Scanner(System.in);

    @Option(names = {"-q", "--quiet"}, description = "Suppress output.")
    private void setQuiet(boolean value) { quiet = value; }

    @Option(names = {"-v", "--verbose"}, description = "Show more information about processing.")
    private void setVerbose(boolean[] values) {
        verbosity = switch (values.length) {
            case 0 -> OFF;
            case 1 -> INFO;
            case 2 -> LOG;
            default -> DEBUG;
        };
    }

    private boolean isEnabled(Verbosity v) { return !quiet && v.compareTo(verbosity) <= 0; }

    Path chooseDirectory(String title, Path oldPath) {
        reportf("=== %s ===", title);
        String oldVal = oldPath == null ? "<not specified>" : oldPath.toString();
        reportf("   was: %s", oldVal);
        return inputf(Paths::get, "   new %s (blank to leave unchanged): ", title);
    }

    <T> T input(Function<String, T> converter, String prompt) {
        System.out.print(prompt);
        return Optional.of(SCANNER.nextLine()).filter(not(String::isBlank)).map(converter).orElse(null);
    }
    <T> T inputf(Function<String, T> converter, String prompt, Object... inserts) { return input(converter, String.format(prompt, inserts)); }

    void reportDirectory(String title, Path oldPath, Path newPath) {
        if (null != newPath) {
            reportf("=== %s ===", title);
            reportf("   was: %s", oldPath);
            reportf("    is: %s", newPath);
        } else {
            reportf("=== %s ===", title);
            reportf("        %s", oldPath);
        }
    }

    Error error(String message, String... details) {
        System.err.println("ERROR: " + message);
        for (String detail: details) System.err.println(detail);
        System.exit(1);
        throw new Error();
    }

    void warn(String message, String... details) {
        System.err.println("WARNING: " + message);
        for (String detail : details) System.err.println(detail);
    }

    void info(Supplier<String> msg) { if (isEnabled(INFO)) System.out.println(msg.get()); }
    void infof(String msg, Object...inserts) { if (isEnabled(INFO)) System.out.println(String.format(msg, inserts)); }
    void log(Supplier<String> msg) { if (isEnabled(LOG)) System.out.println(msg.get()); }
    void logf(String msg, Object...inserts) { if (isEnabled(LOG)) System.out.println(String.format(msg, inserts)); }
    void debug(Supplier<String> msg) { if (isEnabled(DEBUG)) System.out.println(msg.get()); }
    void debugf(String msg, Object...inserts) { if (isEnabled(DEBUG)) System.out.println(String.format(msg, inserts)); }
    void report(Object msg) { if (!quiet) System.out.println(msg); }
    void reportf(String msg, Object... inserts) { if (!quiet) System.out.println(String.format(msg, inserts)); }

    Path verifyDir(String desc, Path dir) {
        if (Files.isDirectory(dir)) return dir;
        throw error("Could not locate " + desc + ": " + dir);
    }
}
