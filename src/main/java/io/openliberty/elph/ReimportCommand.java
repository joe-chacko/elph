package io.openliberty.elph;

import picocli.CommandLine.Command;

@Command(name = ReimportCommand.SUBCOMMAND_NAME, description = "Complete previous import actions, or re-interpret them after changes to OpenLiberty. ")
public class ReimportCommand extends AbstractImportCommand implements Runnable {
    static final String SUBCOMMAND_NAME = "reimport";

    @Override
    public void run() {
        var projects = getProjectsFromHistory();
        importDepsInBatches(projects);
    }
}
