package jkind.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import jkind.JKindException;
import jkind.api.results.JKindResult;
import jkind.api.xml.XmlParseThread;

import org.eclipse.core.runtime.IProgressMonitor;
import org.xml.sax.SAXException;

/**
 * The primary interface to Kind2.
 */
public class Kind2Api extends CommonKindApi {
	/**
	 * Run Kind2 on a Lustre program
	 * 
	 * @param lustreFile
	 *            File containing Lustre program
	 * @param result
	 *            Place to store results as they come in
	 * @param monitor
	 *            Used to check for cancellation
	 * @throws jkind.JKindException
	 */
	@Override
	public void execute(File lustreFile, JKindResult result, IProgressMonitor monitor) {
		try {
			callKind2(lustreFile, result, monitor);
		} catch (JKindException e) {
			throw e;
		} catch (Throwable t) {
			throw new JKindException(result.getText(), t);
		}
	}

	private void callKind2(File lustreFile, JKindResult result, IProgressMonitor monitor)
			throws IOException, InterruptedException, ParserConfigurationException, SAXException {
		ProcessBuilder builder = getKind2ProcessBuilder(lustreFile);
		Process process = null;
		XmlParseThread parseThread = null;

		try {
			result.start();
			process = builder.start();
			parseThread = new XmlParseThread(process.getInputStream(), result);
			parseThread.start();
		} finally {
			int code = 0;
			if (process != null) {
				process.destroy();
				code = process.waitFor();
			}

			parseThread.join();

			if (monitor.isCanceled()) {
				result.cancel();
			} else {
				result.done();
			}
			monitor.done();

			if (code != 0 && !monitor.isCanceled()) {
				throw new JKindException("Abnormal termination, exit code " + code);
			}
		}

		if (parseThread.getThrowable() != null) {
			throw new JKindException("Error parsing XML", parseThread.getThrowable());
		}
	}

	private ProcessBuilder getKind2ProcessBuilder(File lustreFile) {
		List<String> args = new ArrayList<>();
		args.add("kind2");
		args.add("-xml");
		if (timeout != null) {
			args.add("--timeout_wall");
			args.add(timeout.toString());
		}
		args.add(lustreFile.toString());

		ProcessBuilder builder = new ProcessBuilder(args);
		builder.redirectErrorStream(true);
		return builder;
	}
}
