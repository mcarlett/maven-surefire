package org.apache.maven.surefire.dryrun.printers;


import org.apache.maven.surefire.dryrun.printers.testclasses.TestPrinter.Params;
import org.apache.maven.surefire.report.ConsoleStream;

public class DryRunPrinter 
{
    public static void print( Iterable<Class<?>> classes, ConsoleStream log, OutputType outputType
    		, boolean printDebugInFile, String printFilePath)
    {
    	PrinterFactory.getPrinter(log, outputType)
			.addParam(Params.PRINT_DUBUG_IN_FILE.getVal(), printDebugInFile)
			.addParam(Params.PRINT_FILE_PATH.getVal(), printFilePath)
			.print(classes);
    }
}
