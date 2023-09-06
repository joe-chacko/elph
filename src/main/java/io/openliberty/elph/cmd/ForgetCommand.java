package io.openliberty.elph.cmd;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = ForgetCommand.SUBCOMMAND_NAME, description = "Remove items from import history.")
class ForgetCommand extends AbstractImportCommand implements Runnable {
    static final String SUBCOMMAND_NAME = "forget";

    static class Args {
        @Option(names = {"-a", "--all"}, required = true, description = "Remove import history completely.")
        boolean all;
        @Parameters(paramLabel = "Pattern to delete", description = "Remove pattern from import history")
        List<String> patterns;
    }

    @ArgGroup
    Args args;

    @Override
    public void run() {
        if (null == args) {
            var history = getHistoryList();
            if (history.isEmpty()) {
                io.report("No import history recorded.");
                return;
            }
            io.reportf("What would you like to forget? Here is the import history:");
            history.stream().map(s -> "  " + s).forEach(io::report);
        } else if (args.all) {
            deleteHistory();
        } else if (null != args.patterns) {
            if (deleteHistory(args.patterns)) return;
            throw io.error("Nothing deleted.");
        }
    }
}
