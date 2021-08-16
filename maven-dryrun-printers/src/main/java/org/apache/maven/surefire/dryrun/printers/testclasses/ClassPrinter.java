package org.apache.maven.surefire.dryrun.printers.testclasses;

import java.util.Comparator;
import java.util.List;
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

	@SuppressWarnings("unchecked")
	@Override
	public void print(Iterable<Class<?>> classes) {
		prepareOut();
		StreamSupport.stream(classes.spliterator(), false)
		.filter(cls -> !((List<String>)getParameters().get(Params.EXLUDED_CLASSES.getVal()))
				.contains(cls.getName()))
		.sorted(Comparator.comparing(Class::getName))
		.forEach(clazz -> {
			printLine(clazz.getName(), false);
		});
		for(String extra : (List<String>)getParameters().get(Params.EXTRA_CONTENT.getVal())) {
			printLine(extra, false);
		}
		completeOut();
	}
	
	protected abstract void printLine(String line, boolean debug);

}
