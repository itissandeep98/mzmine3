/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleEdge;

/**
 * 
 */
class TwoDPlot extends ChartPanel {

    // crosshair (selection) color
    private static final Color crossHairColor = Color.gray;

    // crosshair stroke
    private static final BasicStroke crossHairStroke = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {
                    5, 3 }, 0);

    private RawDataFile rawDataFile;
    private Range rtRange, mzRange;

    private JFreeChart chart;

    private TwoDXYPlot plot;

    private PeakDataRenderer peakDataRenderer;

    // title font
    private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
    private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN,
            11);
    private TextTitle chartTitle, chartSubTitle;

    private NumberAxis xAxis, yAxis;

    private NumberFormat rtFormat = MZmineCore.getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getMZFormat();

    // private TwoDItemRenderer renderer;

    TwoDPlot(RawDataFile rawDataFile, TwoDVisualizerWindow visualizer,
            TwoDDataSet dataset, Range rtRange, Range mzRange) {

        super(null, true);

        this.rawDataFile = rawDataFile;
        this.rtRange = rtRange;
        this.mzRange = mzRange;

        setBackground(Color.white);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        NumberFormat rtFormat = MZmineCore.getRTFormat();
        NumberFormat mzFormat = MZmineCore.getMZFormat();

        // set the X axis (retention time) properties
        xAxis = new NumberAxis("Retention time");
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setNumberFormatOverride(rtFormat);
        xAxis.setUpperMargin(0);
        xAxis.setLowerMargin(0);

        // set the Y axis (intensity) properties
        yAxis = new NumberAxis("m/z");
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setNumberFormatOverride(mzFormat);
        yAxis.setUpperMargin(0);
        yAxis.setLowerMargin(0);

        // set the plot properties
        plot = new TwoDXYPlot(dataset, rtRange, mzRange, xAxis, yAxis);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        // chart properties
        chart = new JFreeChart("", titleFont, plot, false);
        chart.setBackgroundPaint(Color.white);

        setChart(chart);

        // title
        chartTitle = chart.getTitle();
        chartTitle.setMargin(5, 0, 0, 0);
        chartTitle.setFont(titleFont);

        chartSubTitle = new TextTitle();
        chartSubTitle.setFont(subTitleFont);
        chartSubTitle.setMargin(5, 0, 0, 0);
        chart.addSubtitle(chartSubTitle);

        // disable maximum size (we don't want scaling)
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);

        // set crosshair (selection) properties
        plot.setRangeCrosshairVisible(false);
        plot.setDomainCrosshairVisible(true);
        plot.setDomainCrosshairPaint(crossHairColor);
        plot.setDomainCrosshairStroke(crossHairStroke);

        // set rendering order
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        peakDataRenderer = new PeakDataRenderer();

        // add items to popup menu
        JMenuItem annotationsMenuItem, dataPointsMenuItem, plotTypeMenuItem;
        // TODO
        plotTypeMenuItem = new JMenuItem("Toggle centroid/continuous mode");
        plotTypeMenuItem.addActionListener(visualizer);
        plotTypeMenuItem.setActionCommand("TOGGLE_PLOT_MODE");
        add(plotTypeMenuItem);

        JPopupMenu popupMenu = getPopupMenu();
        popupMenu.addSeparator();
        popupMenu.add(plotTypeMenuItem);

    }

    TwoDXYPlot getXYPlot() {
        return plot;
    }

    void setTitle(String title) {
        chartTitle.setText(title);
    }

    void switchDataPointsVisible() {

        boolean dataPointsVisible = peakDataRenderer.getBaseShapesVisible();
        peakDataRenderer.setBaseShapesVisible(!dataPointsVisible);

    }
    
    void switchContinuousCentroid() {
    	plot.switchContinuousCentroid();
    }

    void loadPeakList(PeakList peakList) {

        PeakDataSet peaksDataSet = new PeakDataSet(rawDataFile, peakList,
                rtRange, mzRange);

        plot.setDataset(1, peaksDataSet);
        plot.setRenderer(1, peakDataRenderer);
    }

    public String getToolTipText(MouseEvent event) {

        String tooltip = super.getToolTipText(event);

        if (tooltip == null) {
            int mouseX = event.getX();
            int mouseY = event.getY();
            Rectangle2D plotArea = getScreenDataArea();
            RectangleEdge xAxisEdge = plot.getDomainAxisEdge();
            RectangleEdge yAxisEdge = plot.getRangeAxisEdge();
            float rt = (float) xAxis.java2DToValue(mouseX, plotArea, xAxisEdge);
            float mz = (float) yAxis.java2DToValue(mouseY, plotArea, yAxisEdge);

            tooltip = "<html>Retention time: " + rtFormat.format(rt)
                    + "<br>m/z: " + mzFormat.format(mz) + "</html>";
        }

        return tooltip;

    }

}