package org.apache.maven.surefire.dryrun.printers;

import java.lang.reflect.InvocationTargetException;

import org.apache.maven.surefire.dryrun.printers.testclasses.TestPrinter;
import org.apache.maven.surefire.report.ConsoleStream;

public final class PrinterFactory {

	public static TestPrinter getPrinter(ConsoleStream log, OutputType outputType) {
		TestPrinter printer = null;
		try {
			printer = outputType.getImplementationClass().getDeclaredConstructor(ConsoleStream.class).newInstance(log);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			log.println(e.toString());
		}
		return printer;
	}
	
}
