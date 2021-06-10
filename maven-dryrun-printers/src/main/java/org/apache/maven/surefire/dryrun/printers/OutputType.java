package org.apache.maven.surefire.dryrun.printers;

import org.apache.maven.surefire.dryrun.printers.testclasses.FilePrinter;
import org.apache.maven.surefire.dryrun.printers.testclasses.LogPrinter;
import org.apache.maven.surefire.dryrun.printers.testclasses.TestPrinter;

public enum OutputType {
	
	LOG(LogPrinter.class),
	FILE(FilePrinter.class);
	
	private Class<? extends TestPrinter> implementationClass;
	
	OutputType(Class<? extends TestPrinter> implementationClass){
		this.implementationClass = implementationClass;
	}

	public Class<? extends TestPrinter> getImplementationClass() {
		return implementationClass;
	}
}
