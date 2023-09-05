package io.openliberty.elph;

import io.openliberty.elph.io.IO;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.util.List;

@Command(name = ForgetCommand.SUBCOMMAND_NAME, description = "Remove items from import history.")
public class ForgetCommand implements Runnable {
    static final String SUBCOMMAND_NAME = "forget";

    @ParentCommand
    ElphCommand elph;
    @Spec
    CommandSpec spec;
    @Mixin
    IO io;

    static class Args {
        @Option(names = {"-a", "--all"}, required = true, description = "Remove import history completely.")
        boolean all;
        @Parameters(paramLabel = "Pattern to delete", description = "Remove pattern from import history")
        List<String> patterns;
    }

    @ArgGroup(exclusive = true)
    Args args;

    @Override
    public void run() {
        ImportCommand ic = new ImportCommand();
        ic.elph = elph;
        ic.spec = spec;
        ic.io = io;
        if (null == args) {
            ic.reportHistory();
        } else if (args.all) {
            ic.deleteHistory();
        } else if (null != args.patterns) {
            ic.deleteHistory(args.patterns);
        }
    }

}
