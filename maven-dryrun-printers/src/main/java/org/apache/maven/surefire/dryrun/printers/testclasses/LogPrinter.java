package org.apache.maven.surefire.dryrun.printers.testclasses;

import org.apache.maven.surefire.report.ConsoleStream;

public class LogPrinter extends ClassPrinter {

	@Override
	protected void prepareOut() {
		log.println("Test classes found:");
	}

	public LogPrinter(ConsoleStream log) {
		super(log);
	}

	@Override
	protected void printLine(String line, boolean debug) {
		log.println(line);		
	}

}
