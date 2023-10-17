/*
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package io.openliberty.elph.util;

import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.openliberty.elph.util.IO.Verbosity.DEBUG;
import static io.openliberty.elph.util.IO.Verbosity.INFO;
import static io.openliberty.elph.util.IO.Verbosity.LOG;
import static io.openliberty.elph.util.IO.Verbosity.OFF;
import static io.openliberty.elph.util.Objects.stringEquals;
import static java.util.function.Predicate.not;
import static picocli.CommandLine.Help.Ansi.Style.bg_blue;
import static picocli.CommandLine.Help.Ansi.Style.bg_red;
import static picocli.CommandLine.Help.Ansi.Style.bg_yellow;
import static picocli.CommandLine.Help.Ansi.Style.blink;
import static picocli.CommandLine.Help.Ansi.Style.bold;
import static picocli.CommandLine.Help.Ansi.Style.faint;
import static picocli.CommandLine.Help.Ansi.Style.fg_black;
import static picocli.CommandLine.Help.Ansi.Style.fg_white;
import static picocli.CommandLine.Help.Ansi.Style.reset;

public class IO {
    public enum Verbosity {OFF, INFO, LOG, DEBUG}
    /** Use statics so settings are global */
    private static Verbosity verbosity = OFF;
    private static boolean quiet;
    /** Single scanner for the whole process */
    final static Scanner SCANNER = new Scanner(System.in);

    public static FileTime getLastModified(Path file) {
        if (Files.isRegularFile(file)) {
            try {
                return Files.getLastModifiedTime(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return FileTime.from(Instant.EPOCH);
    }

    @Option(names = {"-q", "--quiet"}, description = "Suppress output.")
    private void setQuiet(boolean value) { quiet = value; }

    @Option(names = {"-v", "--verbose"}, description = "Show more information while processing. Can be added multiple times, e.g. -vvv")
    private void setVerbose(boolean[] values) {
        verbosity = switch (values.length) {
            case 0 -> OFF;
            case 1 -> INFO;
            case 2 -> LOG;
            default -> DEBUG;
        };
    }

    public boolean isEnabled(Verbosity v) { return !quiet && v.compareTo(verbosity) <= 0; }

    public Path chooseDirectory(String title, Object oldPath) {
        String oldVal = stringify(oldPath);
        return inputf(Paths::get,
                "%s%40s%s %s(%s)%s: ",
                bold.on(), title, reset.on(),
                faint.on(), oldVal, reset.on());
    }

    public String input(String prompt) { return input(s -> s, prompt); }
    public String inputf(String prompt, Object... inserts) { return input(s -> s, prompt.formatted(inserts)); }

    public <T> T input(Function<String, T> converter, String prompt) {
        System.out.print(prompt);
        return Optional.of(SCANNER.nextLine()).filter(not(String::isBlank)).map(converter).orElse(null);
    }
    public <T> T inputf(Function<String, T> converter, String prompt, Object... inserts) { return input(converter, prompt.formatted(inserts)); }

    public void reportDirectory(String title, Object oldPath, Object newPath) {
        if (null == newPath || stringEquals(oldPath, newPath)) {
            reportf("%s%40s:%s %s", bold.on(), title, reset.on(), stringify(oldPath));
        } else {
            reportf("%s%40s:%s %s %s(was: %s)%s", bold.on(), title, reset.on(),
                    stringify(newPath),
                    faint.on(), stringify(oldPath), faint.off());
        }
    }

    public Error error(String message, Object... details) {
        System.err.println(blink.on() + bg_red.on() + fg_black.on() + bold.on() + "ERROR: " + message + reset.on());
        for (Object detail: details) System.err.println(detail);
        System.exit(1);
        throw new Error();
    }

    public void warn(String message, Object... details) {
        System.err.println(blink.on() + bg_yellow.on() + fg_black.on() + bold.on() + "WARNING: " + message + reset.on());
        for (Object detail : details) System.err.println(detail);
    }

    public void info(Supplier<String> msg) { if (isEnabled(INFO)) System.out.println(msg.get()); }
    public void infof(String msg, Object...inserts) { if (isEnabled(INFO)) System.out.printf((msg) + "%n", inserts); }
    public void log(Supplier<String> msg) { if (isEnabled(LOG)) System.out.println(msg.get()); }
    public void logf(String msg, Object...inserts) { if (isEnabled(LOG)) System.out.printf((msg) + "%n", inserts); }
    public void debug(Supplier<String> msg) { if (isEnabled(DEBUG)) System.out.println(msg.get()); }
    public void debugf(String msg, Object...inserts) { if (isEnabled(DEBUG)) System.out.printf((msg) + "%n", inserts); }
    public void report(Object msg) { if (!quiet) System.out.println(msg); }
    public void reportf(String msg, Object... inserts) { if (!quiet) System.out.printf((msg) + "%n", inserts); }

    public void banner(String... lines) {
        var opt = Stream.of(lines)
                .mapToInt(this::asciiLen)
                .max();
        if (opt.isEmpty()) return;
        int effectiveWidth = opt.getAsInt();
        final String prefix = "\t" + bg_blue.on() + fg_white.on() + "  ";
        final String suffix = reset.on() + bg_blue.on() + fg_white.on() + "    " + reset.on();
        final String spacer = prefix + ("%" + (effectiveWidth) + "s").formatted("") + suffix;
        final String metaFormat = prefix + "%%-%ds" + suffix;
        Stream.of(
                Stream.of(spacer),
                Stream.of(lines).map(line -> metaFormat.formatted(fudgedWidth(effectiveWidth, line)).formatted(line)),
                Stream.of(spacer)
        ).flatMap(i -> i).forEach(this::reportf);
    }

    private int fudgedWidth(int effectiveWidth, String line) {
        return effectiveWidth + line.length() - asciiLen(line);
    }

    private int asciiLen(String s) {
        String p = s.replaceAll("\033\\[[0-9;]*[A-Za-z]", "");
        return p.length();
    }

    public Path verifyOrCreateFile(String desc, Path file) {
        verifyOrCreateDir("Parent of " + desc, file.getParent());
        if (Files.exists(file) && !Files.isDirectory(file) && Files.isWritable(file)) return file;
        try {
            return Files.createFile(file);
        } catch (IOException e) {
            throw error("Could not create " + desc + ": " + file);
        }
    }

    public void writeFile(String desc, Path file, String contents) {
        verifyOrCreateDir("Parent of " + desc, file.getParent());
        try {
            Files.writeString(file, contents);
        } catch (IOException e) {
            throw error("Could not write to " + desc + ": " + file, e);
        }
    }

    public void readFile(String desc, Path file, Consumer<String> actionPerLine) {
        try {
            Files.readAllLines(file).forEach(actionPerLine);
        } catch (IOException e) {
            throw error("Could not read from " + desc + ": " + file, e);
        }
    }

    public Path verifyOrCreateDir(String desc, Path dir) {
        if (Files.isDirectory(dir)) return dir;
        if (Files.exists(dir)) throw error("Expected directory but found file for " + desc + ": " + dir);
        try {
            return Files.createDirectory(dir);
        } catch (IOException e) {
            throw error("Could not create " + desc + ": " + dir);
        }
    }

    public Path verifyDir(String desc, Path dir) {
        if (Files.isDirectory(dir)) return dir;
        throw error("Could not locate " + desc + ": " + dir);
    }

    public void pause() {
        if (quiet) return;
        report("Press return to continue, or control-C to quit.");
        new Scanner(System.in).nextLine();
    }

    private static String stringify(Object setting) {
        return setting == null ? "<not specified>" : setting.toString();
    }
}
