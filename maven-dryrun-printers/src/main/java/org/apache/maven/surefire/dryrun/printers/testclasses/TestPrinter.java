package org.apache.maven.surefire.dryrun.printers.testclasses;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.surefire.report.ConsoleStream;

public abstract class TestPrinter {
	
	public enum Params {
		
		PRINT_FILE_PATH("printFilePath"),
		EXLUDED_CLASSES("excludedClasses"),
		EXTRA_CONTENT("extraContent");
		
		private final String val;
		
		Params(String val) {
			this.val = val;
		}

		public String getVal() {
			return val;
		}
		
	}
	
	protected final ConsoleStream log;
	
	private final Map<String, Object> parameters = new HashMap<String, Object>();
	
	protected TestPrinter(ConsoleStream log) {
		this.log = log;
	}
	
	protected Map<String, Object> getParameters() {
		return parameters;
	}
	

	public TestPrinter addParam(String name, Object value) {
		parameters.put(name, value);
		return this;
	}

	public abstract void print(Iterable<Class<?>> classes);

}
