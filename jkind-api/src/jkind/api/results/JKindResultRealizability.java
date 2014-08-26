package jkind.api.results;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import jkind.excel.ExcelFormatter;
import jkind.results.Realizability;
import jkind.results.layout.Layout;
import jkind.results.layout.SingletonLayout;

/**
 * This class holds the results of a run of JKind.
 * 
 * Each realizability is tracked using a {@link RealizabilityResult} class.
 * 
 * Note on renaming: This object can be configured with a {@link Renaming} which
 * changes the names of properties and signals as they arrive. In this case, all
 * properties are added and retrieved using their original names.
 * 
 * @see RealizabilityResult
 */
public class JKindResultRealizability extends AnalysisResult implements PropertyChangeListener {
	private final StringBuilder text = new StringBuilder();
	private final List<RealizabilityResult> realizabilityResults = new ArrayList<>();
	private final MultiStatus multiStatus = new MultiStatus();
	private TickerReal ticker;
	private Renaming renaming;

	/**
	 * Construct an empty JKindResultRealizability to hold the results of a run of jrealizability
	 * 
	 * @param name
	 *            Name of the results
	 */
	public JKindResultRealizability(String name) {
		super(name);
	}

	/**
	 * Construct a JKindResultRealizability to hold the results of a run of jrealizability
	 * 
	 * @param name
	 *            Name of the results
	 * @param realizabilities
	 *            Realizability sets to track
	 */
	public JKindResultRealizability(String name, List<String> realizabilities) {
		super(name);
		addRealizabilities(realizabilities);
	}

	/**
	 * Construct a JKindResultReal to hold the results of a run of JKind
	 * 
	 * @param name
	 *            Name of the results
	 * @param realizabilities
	 *            Realizability sets to track (pre-renaming)
	 * @param renaming
	 *            Renaming to apply realizabilities
	 */
	public JKindResultRealizability(String name, List<String> realizabilities, Renaming renaming) {
		super(name);
		this.renaming = renaming;
		addRealizabilities(realizabilities);
	}

	/**
	 * Construct a JKindResultReal to hold the results of a run of JKind
	 * 
	 * @param name
	 *            Name of the results
	 * @param realizabilities
	 *            realizability names to track (pre-renaming)
	 * @param invertStatus
	 *            True if the status of the realizability of the same index in
	 *            realizabilities should be inverted
	 * @param renaming
	 *            Renaming to apply to apply realizabilities
	 */
	public JKindResultRealizability(String name, List<String> realizabilities, List<Boolean> invertStatus,
			Renaming renaming) {
		super(name);
		this.renaming = renaming;
		addRealizabilities(realizabilities, invertStatus);
	}

	private void addRealizabilities(List<String> realizabilities) {
		if (realizabilities.size()!= 0) {
			addRealizability(realizabilities.get(realizabilities.size()-1));
		} else {
			throw new IllegalArgumentException("There are no inputs defined in the current model.");
		}
	}

	private void addRealizabilities(List<String> realizabilities, List<Boolean> invertStatus) {
		int i = 0;
		if (realizabilities.size() != invertStatus.size()) {
			throw new IllegalArgumentException("Lists have different length");
		}
		for (String realizability : realizabilities) {
			addRealizability(realizability, invertStatus.get(i++));
		}
	}

	/**
	 * Add a new realizability to track
	 * 
	 * @param realizability
	 *            realizability to be tracked (pre-renaming)
	 * @param invertStatus
	 *            True if finding a model means success. Otherwise false.
	 * @return The RealizabilityResult object which will store the results of the
	 *         realizability
	 */
	public RealizabilityResult addRealizability(String realizability, boolean invertStatus) {
		if (renaming != null) {
			realizability = renaming.rename(realizability);
			if (realizability == null) {
				return null;
			}
		}

		RealizabilityResult realizabilityResult = new RealizabilityResult(realizability, renaming, invertStatus);
		realizabilityResults.add(realizabilityResult);
		realizabilityResult.setParent(this);
		pcs.fireIndexedPropertyChange("realizabilityResults", realizabilityResults.size() - 1, null,
				realizabilityResult);
		addStatus(realizabilityResult.getStatus());
		realizabilityResult.addPropertyChangeListener(this);
		return realizabilityResult;
	}

	/**
	 * Add a new realizability to track
	 * 
	 * @param realizability
	 *            realizability to be tracked (pre-renaming)
	 * @return The RealizabilityResult object which will store the results of the
	 *         realizability
	 */
	public RealizabilityResult addRealizability(String realizability) {
		return addRealizability(realizability, false);
	}

	private void addStatus(Status other) {
		multiStatus.add(other);
		pcs.firePropertyChange("status", null, other);
	}

	/**
	 * Get all RealizabilityResult objects stored in the JKindResultReal
	 */
	public List<RealizabilityResult> getRealizabilityResults() {
		return Collections.unmodifiableList(realizabilityResults);
	}

	/**
	 * Get a specific RealizabilityResult by realizability name
	 * 
	 * @param name
	 *            Name of realizability to retrieve (pre-renaming)
	 * @return Realizability with the given name or <code>null</code> if not found
	 */
	public RealizabilityResult getRealizabilityResult() {
		   return realizabilityResults.get(0);
	}
	
	public void addText(char c) {
		text.append(c);
	}

	public void addText(String string) {
		text.append(string);
	}

	public String getText() {
		return text.toString();
	}

	public MultiStatus getMultiStatus() {
		return multiStatus;
	}

	public void start() {
		for (RealizabilityResult re : realizabilityResults) {
			re.start();
		}
		ticker = new TickerReal(this);
		ticker.start();
	}

	public void tick() {
		for (RealizabilityResult re : realizabilityResults) {
			re.tick();
		}
	}

	public void cancel() {
		for (RealizabilityResult re : realizabilityResults) {
			re.cancel();
		}
		if (ticker != null) {
			ticker.done();
		}
	}

	public void done() {
		for (RealizabilityResult re : realizabilityResults) {
			re.done();
		}
		if (ticker != null) {
			ticker.done();
		}
	}

	/**
	 * Convert results to an Excel spreadsheet
	 * 
	 * Using this requires the jxl.jar file in your classpath
	 * 
	 * @param file
	 *            File to write Excel spreadsheet to
	 * @param layout
	 *            Layout information for counterexamples, defined over renamed
	 *            signals
	 * @see Layout
	 * @throws jkind.JKindException
	 */
	public void toExcel(File file, Layout layout) {
		try (ExcelFormatter formatter = new ExcelFormatter(file, layout)) {
			formatter.writeReal(getRealizabilities());
		}
	}

	private List<Realizability> getRealizabilities() {
		List<Realizability> realizabilities = new ArrayList<>();
		for (RealizabilityResult re : realizabilityResults) {
			if (re.getRealizability() != null) {
				realizabilities.add(re.getRealizability());
			}
		}
		return realizabilities;
	}

	/**
	 * Convert results to an Excel spreadsheet using default layout
	 * 
	 * Using this requires the jxl.jar file in your classpath
	 * 
	 * @param file
	 *            File to write Excel spreadsheet to
	 * @throws jkind.JKindException
	 */
	public void toExcel(File file) {
		toExcel(file, new SingletonLayout("Signals"));
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// Status updates from immediate children are noted and propagated
		if ("status".equals(evt.getPropertyName()) && realizabilityResults.contains(evt.getSource())) {
			multiStatus.remove((Status) evt.getOldValue());
			multiStatus.add((Status) evt.getNewValue());
			pcs.firePropertyChange("status", evt.getOldValue(), evt.getNewValue());
		}

		if ("multiStatus".equals(evt.getPropertyName())
				&& realizabilityResults.contains(evt.getSource())) {
			multiStatus.remove((MultiStatus) evt.getOldValue());
			multiStatus.add((MultiStatus) evt.getNewValue());
			pcs.firePropertyChange("multiStatus", evt.getOldValue(), evt.getNewValue());
		}
	}

	@Override
	public String toString() {
		return name + realizabilityResults;
	}
}
