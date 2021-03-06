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

package com.actelion.research.spiritapp.ui.pivot.graph;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.pivot.PivotColumn;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer.Sort;
import com.actelion.research.spiritcore.business.pivot.analyzer.ColumnAnalyser;
import com.actelion.research.spiritcore.business.pivot.analyzer.ColumnAnalyser.Distribution;
import com.actelion.research.spiritcore.business.pivot.analyzer.SimpleResult;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

/**
 * The GraphPanel is the UI component used to give an overview of some of the graphs that could be obtained from the given results
 *
 * @author Joel Freyss
 *
 */
public class GraphPanel extends JScrollPane {

	private Analyzer analyzer;


	private String errorText = null;
	private ListPane<BoxPlot> listPane = new ListPane<>();
	private List<PivotColumn> sortedCols = new ArrayList<>();
	private JFrame popupFrame = null;

	public GraphPanel() {
		super(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		setViewportView(listPane);
		getViewport().setBackground(Color.WHITE);

		//Open Graph on doubleClick in the popupFrame
		listPane.addPropertyChangeListener(ListPane.PROPERTY_DOUBLECLICK, e-> {
			if(getSelectedIndex()<0 || getSelectedIndex()>=listPane.getItems().size()) return;
			BoxPlot boxPlot = listPane.getItems().get(getSelectedIndex());
			boxPlot = boxPlot.clone();

			if(popupFrame!=null && popupFrame.isVisible()) {
				popupFrame.dispose();
			}
			popupFrame = new JFrame(boxPlot.getTitle1());
			popupFrame.setContentPane(boxPlot);
			UIUtils.adaptSize(popupFrame, 1000, 1000*3/4);
			popupFrame.setLocationRelativeTo(GraphPanel.this);
			popupFrame.setVisible(true);
		});

	}

	public void setListSelectionListener(ListSelectionListener listener) {
		listPane.setListSelectionListener(listener);
	}

	/**
	 * Analyzes the results based on a standard analyzer, and initialized the graphes
	 * Can be slow. Returns a worker
	 * @param results
	 */
	public SwingWorkerExtended setResults(List<Result> results) {
		this.errorText = null;
		return new SwingWorkerExtended("Analyzing graphs", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
			@Override
			protected void doInBackground() throws Exception {
				analyzer = new Analyzer(JPAUtil.reattach(results), SpiritFrame.getUser());
			}
			@Override
			protected void done() {
				refreshGraphs();
			}
		};
	}

	/**
	 * Sets an error message (red) on top of the panel
	 * @param errorText
	 */
	public void setErrorText(String errorText) {
		this.errorText = errorText;
		this.analyzer = null;
		refreshGraphs();
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		//Paint the error on top
		if(errorText!=null && errorText.length()>0) {
			g.setFont(FastFont.BIGGER);
			g.setColor(Color.RED);
			UIUtils.drawString(g, errorText, 6, FastFont.BIGGER.getSize()+6, getWidth()-12, getHeight()-(FastFont.BIGGER.getSize()+6));
		}
	}

	/**
	 * Refresh the graph layout.
	 * To be called on the EventDispatcherThread
	 */
	public void refreshGraphs() {
		if(analyzer==null) {
			return;
		}

		sortedCols.clear();
		List<BoxPlot> panels = new ArrayList<>();
		for (PivotColumn col : analyzer.getSortedColumns(Sort.KW)) {
			ColumnAnalyser ca = analyzer.getColumn(col);
			if(ca==null) continue;
			sortedCols.add(col);

			String[] titles = col.getTitle().split("\n+");
			String title = titles.length<3? MiscUtils.flatten(Arrays.asList(titles)): MiscUtils.flatten(Arrays.asList(titles).subList(0, 2));
			String yLabel = titles.length<3? "": MiscUtils.flatten(Arrays.asList(titles).subList(2, titles.length));

			List<String> xLabels = SimpleResult.getPhaseStrings(ca.getSimpleResults());
			Map<Group, List<SimpleResult>> map = SimpleResult.groupingPerGroup(ca.getSimpleResults());
			List<Group> groups = new ArrayList<>(map.keySet());
			Collections.sort(groups, CompareUtils.OBJECT_COMPARATOR);
			List<Series> allSeries = new ArrayList<>();
			for (Group group : groups) {
				allSeries.add(new Series(
						(group==null? "": group.getName()),
						(group==null? Color.GRAY: group.getColor()),
						map.get(group)));
			}

			final BoxPlot boxPlot = new BoxPlot();
			boxPlot.setTitle1(MiscUtils.removeHtmlAndNewLines(title));
			boxPlot.setTitle2(MiscUtils.removeHtmlAndNewLines(yLabel));
			boxPlot.setSeries(allSeries);
			boxPlot.setxLabels(xLabels);
			boxPlot.setLogScale(ca.getDistribution()==Distribution.LOGNORMAL);

			Double kw = ca.getKruskalWallis();
			if(kw!=null) {
				boxPlot.setKruskalWallis(kw);
				boxPlot.setBackground(UIUtils.WHITESMOKE);
			}

			panels.add(boxPlot);
		}
		listPane.setItems(panels);
	}

	public List<BoxPlot> getBoxPlots() {
		return listPane.getItems();
	}

	public void selectAll() {
		listPane.setSelectedItems(listPane.getItems());
	}

	public int getSelectedIndex() {
		Set<Integer> sel = listPane.getSelectedIndexes();
		return sel.isEmpty()? -1: sel.iterator().next();
	}

	public void setSelectedIndex(int index) {
		if(index>=0 && index<listPane.getItems().size()) {
			listPane.setSelectedItem(listPane.getItems().get(index));
		}
	}

	public List<Result> getSelectedResults() {
		Set<Result> results = new LinkedHashSet<>();
		for (int index : listPane.getSelectedIndexes()) {
			results.addAll(sortedCols.get(index).getResults());
		}
		return new ArrayList<>(results);
	}

}
