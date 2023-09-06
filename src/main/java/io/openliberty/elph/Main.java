package io.openliberty.elph;

import io.openliberty.elph.cmd.ElphCommand;
import picocli.CommandLine;

public enum Main {
    ;
    public static void main(String... args) {
        System.exit(new CommandLine(new ElphCommand()).setAbbreviatedSubcommandsAllowed(true).execute(args));
    }
}
