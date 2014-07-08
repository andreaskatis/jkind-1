package jkind.api.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jkind.api.JRealizabilityApi;
import jkind.api.results.JKindResultRealizability;
import jkind.api.results.RealizabilityResult;
import jkind.api.results.Status;
import jkind.api.ui.AnalysisResultTableReal;
import jkind.results.Counterexample;
import jkind.results.InvalidRealizability;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * This example illustrates how to dynamically report the results of a JKind API
 * execution.
 */
public class RealizabilityUI {
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Must specify lustre file as argument");
			System.exit(-1);
		}

		File file = new File(args[0]);
		run(file, parseRealizabilities(file));
	}

	public static List<String> parseRealizabilities(File file) throws IOException {


		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			List<String> realizabilities = new ArrayList<>();
			Pattern pattern = Pattern.compile(".*--%REALIZABILITY +(\\{([a-zA-Z_0-9]*)(\\,([a-zA-Z_0-9]*)*)\\}) *;.*");
			String line;

			while ((line = reader.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches()) {
					String placeholder = matcher.group(1).replaceAll("\\{", "\\[").replaceAll("\\}", "\\]").replaceAll(",", ", " );
					realizabilities.add(placeholder);					
				}
			}

			return realizabilities;
		}
	}

	private static void run(File file, List<String> realizabilities) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("jrealizability Run Example");
		createControls(shell, file, realizabilities);

		shell.pack();
		Point size = shell.getSize();
		shell.setSize(size.x, Math.min(size.y, 500));
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();

		System.exit(0);
	}

	private static void createControls(final Shell parent, final File file, List<String> realizabilities) {
		parent.setLayout(new GridLayout(2, true));
		AnalysisResultTableReal viewer = createTable(parent);
		final Button startButton = createButton(parent, "Start");
		final Button cancelButton = createButton(parent, "Cancel");
		cancelButton.setEnabled(false);

		final JKindResultRealizability result = new JKindResultRealizability(file.getName(), realizabilities);
		viewer.setInput(result);
		final IProgressMonitor monitor = new NullProgressMonitor();

		startButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startButton.setEnabled(false);
				cancelButton.setEnabled(true);

				new Thread("Analysis") {
					@Override
					public void run() {
						new JRealizabilityApi().execute(file, result, monitor);
					}
				}.start();
			}
		});

		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancelButton.setEnabled(false);
				monitor.setCanceled(true);
			}
		});

		viewer.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) event.getSelection();
					if (!sel.isEmpty()) {
						click(parent, (RealizabilityResult) sel.getFirstElement());
					}
				}
			}
		});
	}

	public static void click(Shell parent, RealizabilityResult re) {
		if (re.getStatus() == Status.INVALID) {
			InvalidRealizability ir = (InvalidRealizability) re.getRealizability();
			Counterexample cex = ir.getCounterexample();

			try {
				File file = File.createTempFile("cex", ".xls");
				cex.toExcel(file);
				Program.launch(file.toString());
			} catch (Throwable t) {
				MessageDialog.openError(parent, "Error opening Excel file", t.getMessage());
			}
		}
	}

	private static AnalysisResultTableReal createTable(Composite parent) {
		/*
		 * AnalysisResultTable knows how to format itself. The code here is just
		 * to position the table within its parent.
		 */
		AnalysisResultTableReal table = new AnalysisResultTableReal(parent);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 2;
		table.setLayoutData(gridData);
		return table;
	}

	private static Button createButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.None);
		button.setText(text);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.CENTER;
		button.setLayoutData(gridData);
		return button;
	}
}
