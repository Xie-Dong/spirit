/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
 * CH-4123 Allschwil, Switzerland.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.spiritapp.ui.result;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.SpiritTab;
import com.actelion.research.spiritapp.ui.pivot.PivotPanel;
import com.actelion.research.spiritapp.ui.pivot.graph.GraphPanelWithResults;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;
import com.actelion.research.util.ui.iconbutton.IconType;

public class ResultTab extends SpiritTab {

	private final ResultSearchPane searchPane;

	private final GraphPanelWithResults graphPanel;
	private List<Result> results = new ArrayList<>();

	public ResultTab(SpiritFrame frame) {
		this(frame, null);
	}

	public ResultTab(SpiritFrame frame, Biotype forcedBiotype) {
		super(frame,"Results", IconType.RESULT.getIcon());

		this.searchPane = new ResultSearchPane(this, forcedBiotype);
		this.graphPanel = new GraphPanelWithResults();

		JPanel queryPanel = new JPanel(new BorderLayout());
		queryPanel.add(BorderLayout.CENTER, searchPane);

		JSplitPane contentPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, queryPanel, UIUtils.createBox(graphPanel, createButtonsPanel()));
		contentPane.setDividerLocation(300);


		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, contentPane);

		ResultActions.attachPopup(graphPanel.getPivotTable());

		searchPane.addPropertyChangeListener(evt-> {
			ResultTab.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
		});
	}

	@Override
	public<T> void fireModelChanged(final SpiritChangeType action, final Class<T> what, final Collection<T> details) {
		if(what==Test.class || what==Study.class || what==Result.class) {
			searchPane.getSearchTree().repopulate();
		}
		if(what==Result.class) {
			searchPane.query(searchPane.getSearchTree().getQuery());
		}
	}

	public void setResults(List<Result> results) {
		this.results = results;
		refreshResults();
	}

	public void setErrorText(String errorText) {
		graphPanel.getGraphPanel().setErrorText(errorText);
	}

	private void refreshResults() {
		graphPanel.setResults(results, true);
	}

	public void query(ResultQuery q, int graphIndex) {
		searchPane.setQuery(q).afterDone(() -> {
			if(graphIndex>=0) {
				graphPanel.getGraphPanel().setSelectedIndex(graphIndex);
			}
		});
	}

	public void setCurrentPivotTemplate(PivotTemplate pivotTemplate) {
		graphPanel.setPivotTemplate(pivotTemplate);
	}

	public void setDefaultTemplates(PivotTemplate[] pivotTemplates) {
		graphPanel.setDefaultTemplates(pivotTemplates);
	}

	/**
	 * To be overriden by classes to get a custom button panel
	 * @return
	 */
	protected JPanel createButtonsPanel() {
		return null;
	}

	public PivotPanel getPivotPanel() {
		return graphPanel.getPivotPanel();
	}

	public List<Result> getSelection() {
		return graphPanel.getPivotTable().getSelectedResults();
	}

	public List<Result> getResults() {
		return results;
	}

	@Override
	public void onTabSelect() {
		searchPane.getSearchTree().repopulate();
		if(getRootPane()!=null) {
			getRootPane().setDefaultButton(searchPane.getSearchButton());
		}
	}

	@Override
	public void onStudySelect() {
		if(SpiritFrame.getStudyId()!=null && SpiritFrame.getStudyId().length()>0) {
			ResultQuery q = ResultQuery.createQueryForStudyIds(SpiritFrame.getStudyId());
			q.setMaxResults(10000);
			searchPane.setQuery(q);
			searchPane.getSearchTree().refreshFilters(false);
		} else {
			setResults(new ArrayList<>());
		}
	}

}
