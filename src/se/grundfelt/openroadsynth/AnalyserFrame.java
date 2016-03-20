/* OpenRoadSynth - The free road noise synthisizer
 Copyright (C) 2016  Gustav Grundfelt

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package se.grundfelt.openroadsynth;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Toolkit;
import java.net.URL;
import javax.swing.ImageIcon;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 *
 * @author Gustav Grundfelt 
 */
public class AnalyserFrame extends javax.swing.JFrame {

    double[] fCenter = {25, 32, 40, 50, 63, 80, 100, 125, 163, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000};

    DefaultCategoryDataset sprectraCategoryDataset = new DefaultCategoryDataset();

    /**
     * Constructor
     */
    public AnalyserFrame() {
        initComponents();
        createDataset();
        addChartToPanel();

        //Set icon
        URL iconURL = getClass().getResource("icon_24x24.png");
        ImageIcon icon = new ImageIcon(iconURL);
        this.setIconImage(icon.getImage());

        //CENTER THE JFRAME
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);

        //Set color
        //this.getContentPane().setBackground(new Color(213,233,249));
        //setContentPane(jPanelChart);
    }


    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelChart = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        javax.swing.GroupLayout jPanelChartLayout = new javax.swing.GroupLayout(jPanelChart);
        jPanelChart.setLayout(jPanelChartLayout);
        jPanelChartLayout.setHorizontalGroup(
            jPanelChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 725, Short.MAX_VALUE)
        );
        jPanelChartLayout.setVerticalGroup(
            jPanelChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 448, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void createDataset() {

        sprectraCategoryDataset = new DefaultCategoryDataset();
        for (int i_band = 0; i_band < this.fCenter.length; i_band++) {
            sprectraCategoryDataset.addValue(0d, "Band", String.valueOf((int) fCenter[i_band]));
        }
        sprectraCategoryDataset.addValue(00d, "Band", "Sum dB(A)");

    }

    public void updateDataset(float[] y_values) {
        int i_band;
        int row = 0;
        Comparable rowKey = this.sprectraCategoryDataset.getRowKey(row);
        Comparable columnKey;
        //sprectraCategoryDataset = new DefaultCategoryDataset();
        for (i_band = 0; i_band < this.fCenter.length; i_band++) {

            int col = i_band;
            columnKey = this.sprectraCategoryDataset.getColumnKey(col);
            this.sprectraCategoryDataset.setValue((double) (y_values[i_band]), rowKey, columnKey);
        }

        rowKey = this.sprectraCategoryDataset.getRowKey(row);
        int col = this.fCenter.length;
        columnKey = this.sprectraCategoryDataset.getColumnKey(col);
        this.sprectraCategoryDataset.setValue((double) (y_values[i_band + 1]), rowKey, columnKey);
    }

    private JFreeChart createChart(CategoryDataset paramCategoryDataset) {

        Color myPanelColor = Globals.PANEL_COLOR;
        Color myBackgroundColor = new Color(18, 30, 49);

        /*CategoryAxis localCategoryAxis = new CategoryAxis("Frequency 1/3 octave bands [Hz]");
         NumberAxis localNumberAxis = new NumberAxis("Sound Pressure Level - LAmaxF [dB(A)]");
         */
        JFreeChart localJFreeChart = ChartFactory.createBarChart("", "Frequency 1/3 octave bands [Hz]", "Sound Pressure Level - LAmaxF [dB(A)]", paramCategoryDataset);

        CategoryPlot localCategoryPlot = (CategoryPlot) localJFreeChart.getPlot();
        localCategoryPlot.setDomainGridlinesVisible(true);
        localCategoryPlot.setRangeGridlinePaint(Color.gray);

        //Set Background color //gradient
        //localJFreeChart.setBackgroundPaint(new GradientPaint(0.0F, 0.0F, myPanelColor, 350.0F, 0.0F,myBackgroundColor , true));
        localJFreeChart.setBackgroundPaint(myPanelColor);
        localJFreeChart.getPlot().setBackgroundPaint(myBackgroundColor);
        //localJFreeChart.setBorderPaint(Color.lightGray);
        localJFreeChart.removeLegend();

        //My text color
        Color myTextColor = Globals.TEXT_COLOR;
        Color myBarColor = new Color(255, 153, 0);

        //Y-axis
        NumberAxis localNumberAxis = (NumberAxis) localCategoryPlot.getRangeAxis();
        localNumberAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        localNumberAxis.setAxisLinePaint(myTextColor);
        localNumberAxis.setTickLabelPaint(myTextColor);
        localNumberAxis.setLabelPaint(myTextColor);
        localNumberAxis.setTickMarkPaint(myTextColor);

        localNumberAxis.setRange(10, 100);

        //Bar
        BarRenderer localBarRenderer = (BarRenderer) localCategoryPlot.getRenderer();
        localBarRenderer.setDrawBarOutline(false);
        GradientPaint localGradientPaint1 = new GradientPaint(0.0F, 0.0F, Color.ORANGE, 0.0F, 0.0F, myBarColor);
        GradientPaint localGradientPaint2 = new GradientPaint(0.0F, 0.0F, Color.GREEN, 0.0F, 0.0F, new Color(0, 64, 0));
        GradientPaint localGradientPaint3 = new GradientPaint(0.0F, 0.0F, Color.RED, 0.0F, 0.0F, new Color(64, 0, 0));
        //localBarRenderer.setBaseFillPaint(myBarColor);
        localBarRenderer.setSeriesPaint(0, localGradientPaint1);
        localBarRenderer.setSeriesPaint(1, localGradientPaint2);
        localBarRenderer.setSeriesPaint(2, localGradientPaint3);

        //X-axis
        CategoryAxis localCategoryAxis = localCategoryPlot.getDomainAxis();
        localCategoryAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4));
        localCategoryAxis.setAxisLinePaint(myTextColor);
        localCategoryAxis.setTickLabelPaint(myTextColor);
        localCategoryAxis.setLabelPaint(myTextColor);
        localCategoryAxis.setTickMarkPaint(myTextColor);

        return localJFreeChart;
    }

    private void addChartToPanel() {
        JFreeChart localJFreeChart = createChart(sprectraCategoryDataset);
        //Animator localAnimator = new Animator(sprectraCategoryDataset);
        ChartPanel chartPanel = new ChartPanel(localJFreeChart);
        jPanelChart.add(chartPanel);
        chartPanel.setSize(jPanelChart.getWidth(), jPanelChart.getHeight());
        chartPanel.setVisible(true);

    }


    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.setVisible(false);
    }//GEN-LAST:event_formWindowClosing

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        jPanelChart.removeAll();
        addChartToPanel();
    }//GEN-LAST:event_formComponentResized


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanelChart;
    // End of variables declaration//GEN-END:variables
}
