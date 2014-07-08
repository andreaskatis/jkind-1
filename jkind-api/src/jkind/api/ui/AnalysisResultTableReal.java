package jkind.api.ui;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class AnalysisResultTableReal extends AnalysisResultColumnViewer {
	private TableViewer tableViewer;

	public AnalysisResultTableReal(Composite parent) {
		super(parent);
	}

	@Override
	protected ColumnViewer createViewer() {
		tableViewer = new TableViewer(composite, SWT.FULL_SELECTION);
		tableViewer.getTable().setHeaderVisible(true);
		createColumns();
		return tableViewer;
	}

	private void createColumns() {
		TableViewerColumn realzabilityColumn = new TableViewerColumn(tableViewer, SWT.None);
		realzabilityColumn.getColumn().setText("Realizability");
		realzabilityColumn.getColumn().setWidth(400);
		realzabilityColumn.setLabelProvider(new AnalysisResultLabelProvider(Column.REALIZABILITY, tableViewer));

		TableViewerColumn resultColumn = new TableViewerColumn(tableViewer, SWT.None);
		resultColumn.getColumn().setText("Result");
		resultColumn.setLabelProvider(new AnalysisResultLabelProvider(Column.RESULT));

		TableColumnLayout layout = new TableColumnLayout();
		composite.setLayout(layout);
		layout.setColumnData(realzabilityColumn.getColumn(), new ColumnWeightData(2));
		layout.setColumnData(resultColumn.getColumn(), new ColumnWeightData(1));
	}
	
	@Override
	public TableViewer getViewer() {
		return tableViewer;
	}
}
