package org.apache.maven.surefire.dryrun.printers.testclasses;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.surefire.report.ConsoleStream;

public class FilePrinter extends ClassPrinter {

	private Path outFile;
	private PrintWriter printWriter;
	
	public FilePrinter(ConsoleStream log) {
		super(log);
	}

	@Override
	protected void prepareOut() {
		try {
			String pathFile = (String) getParameters().get(Params.PRINT_FILE_PATH.getVal());
			if( pathFile != null && !pathFile.trim().isEmpty()
					&& Paths.get(pathFile.trim()).toFile().getParentFile().exists()
					&& Paths.get(pathFile.trim()).toFile().getParentFile().canWrite()
					&& !Paths.get(pathFile.trim()).toFile().exists() ) {
				outFile = Paths.get(pathFile.trim());
				outFile.toFile().createNewFile();
			} else {
				outFile = Files.createTempFile("printer", ".temp");
			}			
			printWriter = new PrintWriter(outFile.toFile());
		} catch (IOException e) {
			log.println(e.toString());
		}
	}

	@Override
	protected void completeOut() {
		printWriter.flush();
		printWriter.close();
		log.println(String.format("File completed : %s", outFile.toAbsolutePath()));
	}

	@Override
	protected void printLine(String line) {
		printWriter.println(line);
	}

}
