package io.openliberty.elph;

import io.openliberty.elph.io.IO;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = ReimportCommand.SUBCOMMAND_NAME, description = "Complete previous import actions, or re-interpret them after changes to OpenLiberty. ")
public class ReimportCommand extends AbstractCommand implements Runnable {
    static final String SUBCOMMAND_NAME = "reimport";

    @Override
    public void run() {
        ImportCommand ic = new ImportCommand();
        ic.elph = elph;
        ic.spec = spec;
        ic.io = io;

        ic.initFromHistory();
        ic.importInBatches();
    }
}
