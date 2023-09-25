package io.openliberty.elph.cmd;

import picocli.CommandLine.Command;

import static io.openliberty.elph.cmd.ElphCommand.TOOL_NAME;

@Command(name = "eclipse", description = "Start Eclipse with the specified workspace. This is a good test of your " + TOOL_NAME + " configuration, but it does not replace other ways of launching Eclipse.")
class EclipseCommand extends AbstractCommand implements Runnable {
    @Override
    public void run() {
        elph.startEclipse();
    }
}
