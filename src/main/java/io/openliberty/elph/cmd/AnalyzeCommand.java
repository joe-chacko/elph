package io.openliberty.elph.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "analyze", description = "Force re-analysis of dependencies using bnd. Slow!")
public class AnalyzeCommand implements Runnable {
    @ParentCommand
    ElphCommand elph;
    @Override
    public void run() {
        elph.getCatalog().reanalyze();
    }
}
