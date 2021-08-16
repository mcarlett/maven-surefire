package org.apache.maven.surefire.dryrun.printers;


import java.util.List;

import org.apache.maven.surefire.dryrun.printers.testclasses.TestPrinter.Params;
import org.apache.maven.surefire.report.ConsoleStream;

public class DryRunPrinter 
{
    public static void print( Iterable<Class<?>> classes, List<String> ignoredClasses, List<String> extraContent, ConsoleStream log, OutputType outputType
    		, boolean printDebugInFile, String printFilePath)
    {
    	PrinterFactory.getPrinter(log, outputType)
			.addParam(Params.PRINT_FILE_PATH.getVal(), printFilePath)
			.addParam(Params.EXLUDED_CLASSES.getVal(), ignoredClasses)
			.addParam(Params.EXTRA_CONTENT.getVal(), extraContent)
			.print(classes);
    }
}
