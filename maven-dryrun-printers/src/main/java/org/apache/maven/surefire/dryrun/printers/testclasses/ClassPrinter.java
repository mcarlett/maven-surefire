package org.apache.maven.surefire.dryrun.printers.testclasses;

import java.util.Comparator;
import java.util.stream.StreamSupport;

import org.apache.maven.surefire.report.ConsoleStream;

public abstract class ClassPrinter extends TestPrinter {

	protected ClassPrinter(ConsoleStream log) {
		super(log);
	}
	
	protected void prepareOut() {
		//do nothing by default
	}
	
	protected void completeOut() {
		//do nothing by default
	}

	@Override
	public void print(Iterable<Class<?>> classes) {
		prepareOut();
		StreamSupport.stream(classes.spliterator(), false)
		.sorted(Comparator.comparing(Class::getName))
		.forEach(clazz -> {
			printLine(clazz.getName(), false);
		});
		completeOut();
	}
	
	protected abstract void printLine(String line, boolean debug);

}
