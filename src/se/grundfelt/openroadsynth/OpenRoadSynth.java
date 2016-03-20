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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.math3.util.FastMath;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.dial.DialBackground;
import org.jfree.chart.plot.dial.DialCap;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialTextAnnotation;
import org.jfree.chart.plot.dial.DialValueIndicator;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialRange;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.CompositeTitle;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.StandardGradientPaintTransformer;
import org.jfree.util.ShapeUtilities;

/**
 *
 * @author Gustav Grundfelt
 */
public class OpenRoadSynth extends javax.swing.JFrame {


    double[] fCenter = {25, 32, 40, 50, 63, 80, 100, 125, 163, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000};

    final int SAMPLE_MEM_SIZE = 2;                          // byte per sample 2 bytes=2^16 = short interger type
    final int NO_OF_CHANNELS = 2;                           // stereomode
    final double GAIN = 10;                                 // gain
    final int N_BUFFER = 6;                                 //Number of buffers
    final int N_BAND = fCenter.length;                     //Number of buffer

    //Creating buffers that contains a maximum of 90 seconds of noise.
    //MATRIX CONTAINING ONLY WHITE NOISE
    float[][] noiseComponentsArray = new float[fCenter.length][Globals.SAMPLING_RATE * Globals.PCN_DUR];

    //GET NUMBER OF CORES
    final int PROCESSOR_CORES = Runtime.getRuntime().availableProcessors();

    int ongoingAnalyserThreads = 0;

    float[] samplesForAnalyserArray = new float[5513 + 1];         //temporary array to store the data to be analysed

    float[] resultsFromAnalyserAweighted = new float[N_BAND + 2];            //temporary array to store the data to be analysed

    //Stores all the soundfiles
    SoundBuffer[] soundBufferArray = new SoundBuffer[N_BUFFER];

    ExportFrame exportDialogFrame = new ExportFrame(this, true);

    //current projekt file
    File filePathToThisProject = null;

    //Flag to keep track of the stored file
    boolean projectIsSaved = true;

    //GUI for the analyser graph
    AnalyserFrame analyserFrame = new AnalyserFrame();

    InfoFrame infoFrame = new InfoFrame();

    //FileChoosers
    JFileChooser openFileChooser = new JFileChooser();
    JFileChooser exportFileChooser = new JFileChooser();
    JFileChooser saveFileChooser = new JFileChooser();

    CpxFrame cpxFrame = new CpxFrame();

    //ChartFrame CHART_FRAME = new ChartFrame(); 
    boolean canCalulate = false;                          //True if PCN-file exists

    CalibratorSignalBuffer calibratorSignal = new CalibratorSignalBuffer();    //

    DefaultCategoryDataset groundAbsCategoryDataset = new DefaultCategoryDataset(); //Dataset for the groundabsorption

    //True if second layer is enabled
    private boolean secondLayerPavementIsEnabled = false;

    //Holds a boolean for each soundbuffer that detects if the canceled the calculation
    public boolean[] userHasCanceledCalculation = new boolean[6];

    //Global datasets for the charts
    DefaultValueDataset datasetSPLmaxFast = new DefaultValueDataset(0D);
    DefaultValueDataset datasetSPLmaxSample = new DefaultValueDataset(0D);

    /**
     * Constructor
     *
     * @throws UnsupportedAudioFileException
     */
    public OpenRoadSynth() throws UnsupportedAudioFileException {

        this.initLookAndFeel();
        this.initVariables();
        this.initComponents(); //Netbeans editor
        this.initMyClassesAndGUI(); //Start threads aswell
        this.initMyCharts();
        this.initDialogs();
        this.writeStatus(false);
        this.makePavementMenus();

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelBuffers = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        jLabelBuffer5Spl2 = new javax.swing.JLabel();
        jLabelBufferOverallSPL = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jButtonClearBuffer0 = new javax.swing.JButton();
        jButtonExportBuffer0 = new javax.swing.JButton();
        jButtonPlayOnlyBuffer0 = new javax.swing.JButton();
        jToggleButtonPlayBuffer0 = new javax.swing.JToggleButton();
        jSliderBuffer0 = new javax.swing.JSlider();
        jLabelBuffer0 = new javax.swing.JLabel();
        jLabelBuffer0Spl = new javax.swing.JLabel();
        jToggleButtonWriteToBuffer0 = new javax.swing.JToggleButton();
        jPanel13 = new javax.swing.JPanel();
        jButtonClearBuffer1 = new javax.swing.JButton();
        jButtonExportBuffer1 = new javax.swing.JButton();
        jButtonPlayOnlyBuffer1 = new javax.swing.JButton();
        jToggleButtonPlayBuffer1 = new javax.swing.JToggleButton();
        jSliderBuffer1 = new javax.swing.JSlider();
        jLabelBuffer1 = new javax.swing.JLabel();
        jLabelBuffer1Spl = new javax.swing.JLabel();
        jToggleButtonWriteToBuffer1 = new javax.swing.JToggleButton();
        jPanel14 = new javax.swing.JPanel();
        jButtonExportBuffer2 = new javax.swing.JButton();
        jToggleButtonPlayBuffer2 = new javax.swing.JToggleButton();
        jLabelBuffer2 = new javax.swing.JLabel();
        jButtonClearBuffer2 = new javax.swing.JButton();
        jSliderBuffer2 = new javax.swing.JSlider();
        jButtonPlayOnlyBuffer2 = new javax.swing.JButton();
        jLabelBuffer2Spl = new javax.swing.JLabel();
        jToggleButtonWriteToBuffer2 = new javax.swing.JToggleButton();
        jPanel15 = new javax.swing.JPanel();
        jButtonClearBuffer3 = new javax.swing.JButton();
        jButtonExportBuffer3 = new javax.swing.JButton();
        jButtonPlayOnlyBuffer3 = new javax.swing.JButton();
        jToggleButtonPlayBuffer3 = new javax.swing.JToggleButton();
        jSliderBuffer3 = new javax.swing.JSlider();
        jLabelBuffer3 = new javax.swing.JLabel();
        jLabelBuffer3Spl = new javax.swing.JLabel();
        jToggleButtonWriteToBuffer3 = new javax.swing.JToggleButton();
        jPanel16 = new javax.swing.JPanel();
        jButtonClearBuffer4 = new javax.swing.JButton();
        jButtonExportBuffer4 = new javax.swing.JButton();
        jButtonPlayOnlyBuffer4 = new javax.swing.JButton();
        jToggleButtonPlayBuffer4 = new javax.swing.JToggleButton();
        jSliderBuffer4 = new javax.swing.JSlider();
        jLabelBuffer4 = new javax.swing.JLabel();
        jLabelBuffer4Spl = new javax.swing.JLabel();
        jToggleButtonWriteToBuffer4 = new javax.swing.JToggleButton();
        jPanel17 = new javax.swing.JPanel();
        jButtonClearBuffer5 = new javax.swing.JButton();
        jButtonExportBuffer5 = new javax.swing.JButton();
        jButtonPlayOnlyBuffer5 = new javax.swing.JButton();
        jToggleButtonPlayBuffer5 = new javax.swing.JToggleButton();
        jSliderBuffer5 = new javax.swing.JSlider();
        jLabelBuffer5 = new javax.swing.JLabel();
        jLabelBuffer5Spl = new javax.swing.JLabel();
        jToggleButtonWriteToBuffer5 = new javax.swing.JToggleButton();
        jLabel19 = new javax.swing.JLabel();
        jPanelInfoBar = new javax.swing.JPanel();
        jLabelStatus = new javax.swing.JLabel();
        jProgressBarHidden = new javax.swing.JProgressBar();
        jLabelOverload = new javax.swing.JLabel();
        jPanelToolBar = new javax.swing.JPanel();
        jButtonToolBarNewFile = new javax.swing.JButton();
        jButtonToolBarOpenProject = new javax.swing.JButton();
        jButtonToolBarSaveProject = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButtonToolBarExport = new javax.swing.JButton();
        jButtonToolBarChart = new javax.swing.JButton();
        jButtonToolBarAddCPXData = new javax.swing.JButton();
        jButtonToolBarCheckHardWare = new javax.swing.JButton();
        jLabelVersionInfo = new javax.swing.JLabel();
        jButtonToolBarExit = new javax.swing.JButton();
        jButtonStopAll = new javax.swing.JButton();
        jButtonToolBarExplainPicture = new javax.swing.JButton();
        jTabbedPanel = new javax.swing.JTabbedPane();
        jPanelListener = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldRoadDist = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jComboBoxEarDist = new javax.swing.JComboBox();
        jComboBoxPlaybackMode = new javax.swing.JComboBox();
        jButtonDecreaseDistanceToRoad = new javax.swing.JButton();
        jButtonIncreaseDistanceToRoad = new javax.swing.JButton();
        jLabel42 = new javax.swing.JLabel();
        jLabelTrafficInformation1 = new javax.swing.JLabel();
        jPanelVehicle = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jComboBoxSpeed = new javax.swing.JComboBox();
        jLabel15 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jComboBoxDirection = new javax.swing.JComboBox();
        jComboBoxEngine = new javax.swing.JComboBox();
        jLabel25 = new javax.swing.JLabel();
        jComboBoxSeparation = new javax.swing.JComboBox();
        jLabel32 = new javax.swing.JLabel();
        jLabelTrafficInformation2 = new javax.swing.JLabel();
        jPanelVehicleRoad = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jTextFieldEndPos = new javax.swing.JTextField();
        jTextFieldStartPos = new javax.swing.JTextField();
        jLabelStartStopSecondLayer = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabelSecondLayer = new javax.swing.JLabel();
        jCheckBoxActivateSecondLayer = new javax.swing.JCheckBox();
        jLabelStartStop2 = new javax.swing.JLabel();
        jTextFieldStartPosSecondLayer = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jTextFieldEndPosSecondLayer = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jComboBoxSetPassByDist = new javax.swing.JComboBox();
        jComboBoxSetPassByDistSecondLayer = new javax.swing.JComboBox();
        jLabel24 = new javax.swing.JLabel();
        jComboBoxPavement = new javax.swing.JComboBox();
        jComboBoxPavementSecondLayer = new javax.swing.JComboBox();
        jLabelTrafficInformation3 = new javax.swing.JLabel();
        jPanelBarrier = new javax.swing.JPanel();
        jCheckBoxNoiseBarrier1Active = new javax.swing.JCheckBox();
        jComboBoxNoiseBarrier1RoadDist = new javax.swing.JComboBox();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jTextFieldNoiseBarrier1Start = new javax.swing.JTextField();
        jTextFieldNoiseBarrier1End = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jComboBoxNoiseBarrier1Height = new javax.swing.JComboBox();
        jCheckBoxNoiseBarrier2Active = new javax.swing.JCheckBox();
        jComboBoxNoiseBarrier2RoadDist = new javax.swing.JComboBox();
        jTextFieldNoiseBarrier2Start = new javax.swing.JTextField();
        jTextFieldNoiseBarrier2End = new javax.swing.JTextField();
        jComboBoxNoiseBarrier2Height = new javax.swing.JComboBox();
        jComboBoxNoiseBarrier3Height = new javax.swing.JComboBox();
        jTextFieldNoiseBarrier3Start = new javax.swing.JTextField();
        jComboBoxNoiseBarrier3RoadDist = new javax.swing.JComboBox();
        jCheckBoxNoiseBarrier3Active = new javax.swing.JCheckBox();
        jTextFieldNoiseBarrier3End = new javax.swing.JTextField();
        jLabel31 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jLabel53 = new javax.swing.JLabel();
        jLabel54 = new javax.swing.JLabel();
        jPanelGroundAbs = new javax.swing.JPanel();
        jComboBoxGroundAbs = new javax.swing.JComboBox();
        jLabel26 = new javax.swing.JLabel();
        jLabel33 = new javax.swing.JLabel();
        jPanelGroundAbsChart = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jTextFieldGroundResistivety = new javax.swing.JTextField();
        jTextFieldGroundThickness = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jButtonIncreaseRange = new javax.swing.JButton();
        jButtonDecreaseRange = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jComboBoxDynamicRange = new javax.swing.JComboBox();
        jLabel41 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel49 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jComboBoxLpFilterCutOff = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        jCheckBoxLpOn = new javax.swing.JCheckBox();
        jLabel45 = new javax.swing.JLabel();
        jComboBoxHpFilterCutOff = new javax.swing.JComboBox();
        jLabel46 = new javax.swing.JLabel();
        jCheckBoxHpOn = new javax.swing.JCheckBox();
        jLabel57 = new javax.swing.JLabel();
        jCheckBoxLeaveOneCore = new javax.swing.JCheckBox();
        jCheckBoxUseSpline = new javax.swing.JCheckBox();
        jPanel7 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel50 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jComboBoxDither = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        jPanelCalibration = new javax.swing.JPanel();
        jComboBoxCalibrationSignalAmplitude = new javax.swing.JComboBox();
        jCheckBoxCalibrationSignal = new javax.swing.JCheckBox();
        jLabel23 = new javax.swing.JLabel();
        jButtonGenerateCalibrationFile = new javax.swing.JButton();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jPanelDialChart = new javax.swing.JPanel();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemNewFile = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemOpenFile = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemSaveAs = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExport = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuDSP = new javax.swing.JMenu();
        jMenuItemGeneratePCN = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuBufferControl = new javax.swing.JMenu();
        jMenuItemPlay01 = new javax.swing.JMenuItem();
        jMenuItemPlay23 = new javax.swing.JMenuItem();
        jMenuItemPlay45 = new javax.swing.JMenuItem();
        jMenuItemPlay012 = new javax.swing.JMenuItem();
        jMenuItemPlay345 = new javax.swing.JMenuItem();
        jMenuItemPlayAll = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenuItemStopAll = new javax.swing.JMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuItemManual = new javax.swing.JMenuItem();
        jMenuAbout = new javax.swing.JMenu();
        jMenuItemThirdPartyLicenses = new javax.swing.JMenuItem();
        jMenuIcons = new javax.swing.JMenuItem();
        jMenuItemEula = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("RoadSynth");
        setBackground(new java.awt.Color(153, 153, 153));
        setFont(new java.awt.Font("Microsoft JhengHei", 0, 12)); // NOI18N
        setForeground(new java.awt.Color(51, 51, 255));
        setLocationByPlatform(true);
        setMinimumSize(null);
        setName("RoadSynthFrame"); // NOI18N
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanelBuffers.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 0, new java.awt.Color(255, 153, 0)));
        jPanelBuffers.setMinimumSize(new java.awt.Dimension(876, 100));
        jPanelBuffers.setPreferredSize(new java.awt.Dimension(567, 271));

        jLabelBuffer5Spl2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelBuffer5Spl2.setText("Overall LAeq and LAmax (playing buffers) ≈");
        jLabelBuffer5Spl2.setToolTipText("Logarithmic sum of all playing buffers");
        jLabelBuffer5Spl2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabelBuffer5Spl2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabelBufferOverallSPL.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelBufferOverallSPL.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBufferOverallSPL.setText("00.0/00.0");
        jLabelBufferOverallSPL.setToolTipText("LAeq / LAFmax [dB(A)]");
        jLabelBufferOverallSPL.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabelBufferOverallSPL.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelBuffer5Spl2, javax.swing.GroupLayout.PREFERRED_SIZE, 413, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                .addComponent(jLabelBufferOverallSPL, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelBuffer5Spl2)
                    .addComponent(jLabelBufferOverallSPL))
                .addGap(4, 4, 4))
        );

        jPanel11.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(255, 153, 0)));

        jButtonClearBuffer0.setBackground(new java.awt.Color(102, 102, 102));
        jButtonClearBuffer0.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/delete-26.png"))); // NOI18N
        jButtonClearBuffer0.setToolTipText("Clears this sound buffer");
        jButtonClearBuffer0.setBorder(null);
        jButtonClearBuffer0.setContentAreaFilled(false);
        jButtonClearBuffer0.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonClearBuffer0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearBuffer0ActionPerformed(evt);
            }
        });

        jButtonExportBuffer0.setBackground(new java.awt.Color(102, 102, 102));
        jButtonExportBuffer0.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/audio_file-26.png"))); // NOI18N
        jButtonExportBuffer0.setToolTipText("Exports the buffer into a wav-file");
        jButtonExportBuffer0.setBorder(null);
        jButtonExportBuffer0.setContentAreaFilled(false);
        jButtonExportBuffer0.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonExportBuffer0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportBuffer0ActionPerformed(evt);
            }
        });

        jButtonPlayOnlyBuffer0.setBackground(new java.awt.Color(102, 102, 102));
        jButtonPlayOnlyBuffer0.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/next-26.png"))); // NOI18N
        jButtonPlayOnlyBuffer0.setToolTipText("Play only this buffer");
        jButtonPlayOnlyBuffer0.setBorder(null);
        jButtonPlayOnlyBuffer0.setContentAreaFilled(false);
        jButtonPlayOnlyBuffer0.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonPlayOnlyBuffer0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayOnlyBuffer0ActionPerformed(evt);
            }
        });

        jToggleButtonPlayBuffer0.setBackground(new java.awt.Color(102, 102, 102));
        jToggleButtonPlayBuffer0.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jToggleButtonPlayBuffer0.setToolTipText("Play this buffer on/off");
        jToggleButtonPlayBuffer0.setBorder(null);
        jToggleButtonPlayBuffer0.setContentAreaFilled(false);
        jToggleButtonPlayBuffer0.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonPlayBuffer0.setName(""); // NOI18N
        jToggleButtonPlayBuffer0.setRolloverEnabled(false);
        jToggleButtonPlayBuffer0.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/pause-26.png"))); // NOI18N
        jToggleButtonPlayBuffer0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonPlayBuffer0ActionPerformed(evt);
            }
        });

        jSliderBuffer0.setValue(0);
        jSliderBuffer0.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jSliderBuffer0.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jSliderBuffer0MouseDragged(evt);
            }
        });

        jLabelBuffer0.setText("No sound loaded");

        jLabelBuffer0Spl.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelBuffer0Spl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBuffer0Spl.setText("00.0/00.0");
        jLabelBuffer0Spl.setToolTipText("LAeq / LAFmax [dB(A)]");
        jLabelBuffer0Spl.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabelBuffer0Spl.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jToggleButtonWriteToBuffer0.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/calculator-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer0.setBorder(null);
        jToggleButtonWriteToBuffer0.setBorderPainted(false);
        jToggleButtonWriteToBuffer0.setContentAreaFilled(false);
        jToggleButtonWriteToBuffer0.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonWriteToBuffer0.setRolloverEnabled(false);
        jToggleButtonWriteToBuffer0.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/cancel-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonWriteToBuffer0ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToggleButtonWriteToBuffer0, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClearBuffer0, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExportBuffer0, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonPlayOnlyBuffer0, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButtonPlayBuffer0, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSliderBuffer0, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelBuffer0)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelBuffer0Spl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabelBuffer0Spl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSliderBuffer0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonPlayBuffer0, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                    .addComponent(jButtonPlayOnlyBuffer0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonExportBuffer0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonClearBuffer0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonWriteToBuffer0, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4))
        );

        jPanel13.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(255, 153, 0)));

        jButtonClearBuffer1.setBackground(new java.awt.Color(153, 153, 153));
        jButtonClearBuffer1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/delete-26.png"))); // NOI18N
        jButtonClearBuffer1.setToolTipText("Clears this sound buffer");
        jButtonClearBuffer1.setBorder(null);
        jButtonClearBuffer1.setContentAreaFilled(false);
        jButtonClearBuffer1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonClearBuffer1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearBuffer1ActionPerformed(evt);
            }
        });

        jButtonExportBuffer1.setBackground(new java.awt.Color(153, 153, 153));
        jButtonExportBuffer1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/audio_file-26.png"))); // NOI18N
        jButtonExportBuffer1.setToolTipText("Exports the buffer into a wav-file");
        jButtonExportBuffer1.setBorder(null);
        jButtonExportBuffer1.setContentAreaFilled(false);
        jButtonExportBuffer1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonExportBuffer1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportBuffer1ActionPerformed(evt);
            }
        });

        jButtonPlayOnlyBuffer1.setBackground(new java.awt.Color(153, 153, 153));
        jButtonPlayOnlyBuffer1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/next-26.png"))); // NOI18N
        jButtonPlayOnlyBuffer1.setToolTipText("Play only this buffer");
        jButtonPlayOnlyBuffer1.setBorder(null);
        jButtonPlayOnlyBuffer1.setContentAreaFilled(false);
        jButtonPlayOnlyBuffer1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonPlayOnlyBuffer1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayOnlyBuffer1ActionPerformed(evt);
            }
        });

        jToggleButtonPlayBuffer1.setBackground(new java.awt.Color(153, 153, 153));
        jToggleButtonPlayBuffer1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jToggleButtonPlayBuffer1.setToolTipText("Play this buffer on/off");
        jToggleButtonPlayBuffer1.setBorder(null);
        jToggleButtonPlayBuffer1.setContentAreaFilled(false);
        jToggleButtonPlayBuffer1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonPlayBuffer1.setRolloverEnabled(false);
        jToggleButtonPlayBuffer1.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/pause-26.png"))); // NOI18N
        jToggleButtonPlayBuffer1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonPlayBuffer1ActionPerformed(evt);
            }
        });

        jSliderBuffer1.setValue(0);
        jSliderBuffer1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jSliderBuffer1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jSliderBuffer1MouseDragged(evt);
            }
        });

        jLabelBuffer1.setBackground(new java.awt.Color(153, 153, 153));
        jLabelBuffer1.setText("No sound loaded");

        jLabelBuffer1Spl.setBackground(new java.awt.Color(153, 153, 153));
        jLabelBuffer1Spl.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelBuffer1Spl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBuffer1Spl.setText("00.0/00.0");
        jLabelBuffer1Spl.setToolTipText("LAeq / LAFmax [dB(A)]");
        jLabelBuffer1Spl.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabelBuffer1Spl.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jToggleButtonWriteToBuffer1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/calculator-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer1.setBorder(null);
        jToggleButtonWriteToBuffer1.setBorderPainted(false);
        jToggleButtonWriteToBuffer1.setContentAreaFilled(false);
        jToggleButtonWriteToBuffer1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonWriteToBuffer1.setRolloverEnabled(false);
        jToggleButtonWriteToBuffer1.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/cancel-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonWriteToBuffer1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToggleButtonWriteToBuffer1, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClearBuffer1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExportBuffer1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonPlayOnlyBuffer1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButtonPlayBuffer1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSliderBuffer1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelBuffer1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelBuffer1Spl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabelBuffer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSliderBuffer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonExportBuffer1, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                    .addComponent(jButtonPlayOnlyBuffer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonPlayBuffer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer1Spl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonClearBuffer1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonWriteToBuffer1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4))
        );

        jPanel14.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(255, 153, 0)));

        jButtonExportBuffer2.setBackground(new java.awt.Color(102, 102, 102));
        jButtonExportBuffer2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/audio_file-26.png"))); // NOI18N
        jButtonExportBuffer2.setToolTipText("Exports the buffer into a wav-file");
        jButtonExportBuffer2.setBorder(null);
        jButtonExportBuffer2.setContentAreaFilled(false);
        jButtonExportBuffer2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonExportBuffer2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportBuffer2ActionPerformed(evt);
            }
        });

        jToggleButtonPlayBuffer2.setBackground(new java.awt.Color(102, 102, 102));
        jToggleButtonPlayBuffer2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jToggleButtonPlayBuffer2.setToolTipText("Play this buffer on/off");
        jToggleButtonPlayBuffer2.setBorder(null);
        jToggleButtonPlayBuffer2.setContentAreaFilled(false);
        jToggleButtonPlayBuffer2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonPlayBuffer2.setRolloverEnabled(false);
        jToggleButtonPlayBuffer2.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/pause-26.png"))); // NOI18N
        jToggleButtonPlayBuffer2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonPlayBuffer2ActionPerformed(evt);
            }
        });

        jLabelBuffer2.setText("No sound loaded");

        jButtonClearBuffer2.setBackground(new java.awt.Color(102, 102, 102));
        jButtonClearBuffer2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/delete-26.png"))); // NOI18N
        jButtonClearBuffer2.setToolTipText("Clears this sound buffer");
        jButtonClearBuffer2.setBorder(null);
        jButtonClearBuffer2.setContentAreaFilled(false);
        jButtonClearBuffer2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonClearBuffer2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearBuffer2ActionPerformed(evt);
            }
        });

        jSliderBuffer2.setValue(0);
        jSliderBuffer2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jSliderBuffer2.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jSliderBuffer2MouseDragged(evt);
            }
        });

        jButtonPlayOnlyBuffer2.setBackground(new java.awt.Color(102, 102, 102));
        jButtonPlayOnlyBuffer2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/next-26.png"))); // NOI18N
        jButtonPlayOnlyBuffer2.setToolTipText("Play only this buffer");
        jButtonPlayOnlyBuffer2.setBorder(null);
        jButtonPlayOnlyBuffer2.setContentAreaFilled(false);
        jButtonPlayOnlyBuffer2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonPlayOnlyBuffer2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayOnlyBuffer2ActionPerformed(evt);
            }
        });

        jLabelBuffer2Spl.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelBuffer2Spl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBuffer2Spl.setText("00.0/00.0");
        jLabelBuffer2Spl.setToolTipText("LAeq / LAFmax [dB(A)]");
        jLabelBuffer2Spl.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabelBuffer2Spl.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jToggleButtonWriteToBuffer2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/calculator-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer2.setBorder(null);
        jToggleButtonWriteToBuffer2.setBorderPainted(false);
        jToggleButtonWriteToBuffer2.setContentAreaFilled(false);
        jToggleButtonWriteToBuffer2.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonWriteToBuffer2.setRolloverEnabled(false);
        jToggleButtonWriteToBuffer2.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/cancel-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonWriteToBuffer2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToggleButtonWriteToBuffer2, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClearBuffer2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExportBuffer2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonPlayOnlyBuffer2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButtonPlayBuffer2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSliderBuffer2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelBuffer2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelBuffer2Spl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonClearBuffer2, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                    .addComponent(jButtonExportBuffer2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonPlayOnlyBuffer2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonPlayBuffer2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSliderBuffer2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonWriteToBuffer2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer2Spl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4))
        );

        jPanel15.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(255, 153, 0)));

        jButtonClearBuffer3.setBackground(new java.awt.Color(153, 153, 153));
        jButtonClearBuffer3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/delete-26.png"))); // NOI18N
        jButtonClearBuffer3.setToolTipText("Clears this sound buffer");
        jButtonClearBuffer3.setBorder(null);
        jButtonClearBuffer3.setContentAreaFilled(false);
        jButtonClearBuffer3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonClearBuffer3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearBuffer3ActionPerformed(evt);
            }
        });

        jButtonExportBuffer3.setBackground(new java.awt.Color(153, 153, 153));
        jButtonExportBuffer3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/audio_file-26.png"))); // NOI18N
        jButtonExportBuffer3.setToolTipText("Exports the buffer into a wav-file");
        jButtonExportBuffer3.setBorder(null);
        jButtonExportBuffer3.setContentAreaFilled(false);
        jButtonExportBuffer3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonExportBuffer3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportBuffer3ActionPerformed(evt);
            }
        });

        jButtonPlayOnlyBuffer3.setBackground(new java.awt.Color(153, 153, 153));
        jButtonPlayOnlyBuffer3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/next-26.png"))); // NOI18N
        jButtonPlayOnlyBuffer3.setToolTipText("Play only this buffer");
        jButtonPlayOnlyBuffer3.setBorder(null);
        jButtonPlayOnlyBuffer3.setContentAreaFilled(false);
        jButtonPlayOnlyBuffer3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonPlayOnlyBuffer3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayOnlyBuffer3ActionPerformed(evt);
            }
        });

        jToggleButtonPlayBuffer3.setBackground(new java.awt.Color(153, 153, 153));
        jToggleButtonPlayBuffer3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jToggleButtonPlayBuffer3.setToolTipText("Play this buffer on/off");
        jToggleButtonPlayBuffer3.setBorder(null);
        jToggleButtonPlayBuffer3.setContentAreaFilled(false);
        jToggleButtonPlayBuffer3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonPlayBuffer3.setRolloverEnabled(false);
        jToggleButtonPlayBuffer3.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/pause-26.png"))); // NOI18N
        jToggleButtonPlayBuffer3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonPlayBuffer3ActionPerformed(evt);
            }
        });

        jSliderBuffer3.setValue(0);
        jSliderBuffer3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jSliderBuffer3.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jSliderBuffer3MouseDragged(evt);
            }
        });

        jLabelBuffer3.setBackground(new java.awt.Color(153, 153, 153));
        jLabelBuffer3.setText("No sound loaded");

        jLabelBuffer3Spl.setBackground(new java.awt.Color(153, 153, 153));
        jLabelBuffer3Spl.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelBuffer3Spl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBuffer3Spl.setText("00.0/00.0");
        jLabelBuffer3Spl.setToolTipText("LAeq / LAFmax [dB(A)]");
        jLabelBuffer3Spl.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabelBuffer3Spl.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jToggleButtonWriteToBuffer3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/calculator-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer3.setBorder(null);
        jToggleButtonWriteToBuffer3.setBorderPainted(false);
        jToggleButtonWriteToBuffer3.setContentAreaFilled(false);
        jToggleButtonWriteToBuffer3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonWriteToBuffer3.setRolloverEnabled(false);
        jToggleButtonWriteToBuffer3.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/cancel-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonWriteToBuffer3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToggleButtonWriteToBuffer3, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClearBuffer3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExportBuffer3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonPlayOnlyBuffer3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButtonPlayBuffer3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSliderBuffer3, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelBuffer3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelBuffer3Spl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonPlayOnlyBuffer3, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                    .addComponent(jToggleButtonPlayBuffer3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonClearBuffer3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonExportBuffer3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonWriteToBuffer3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSliderBuffer3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer3Spl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4))
        );

        jPanel16.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(255, 153, 0)));

        jButtonClearBuffer4.setBackground(new java.awt.Color(102, 102, 102));
        jButtonClearBuffer4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/delete-26.png"))); // NOI18N
        jButtonClearBuffer4.setToolTipText("Clears this sound buffer");
        jButtonClearBuffer4.setBorder(null);
        jButtonClearBuffer4.setContentAreaFilled(false);
        jButtonClearBuffer4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonClearBuffer4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearBuffer4ActionPerformed(evt);
            }
        });

        jButtonExportBuffer4.setBackground(new java.awt.Color(102, 102, 102));
        jButtonExportBuffer4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/audio_file-26.png"))); // NOI18N
        jButtonExportBuffer4.setToolTipText("Exports the buffer into a wav-file");
        jButtonExportBuffer4.setBorder(null);
        jButtonExportBuffer4.setContentAreaFilled(false);
        jButtonExportBuffer4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonExportBuffer4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportBuffer4ActionPerformed(evt);
            }
        });

        jButtonPlayOnlyBuffer4.setBackground(new java.awt.Color(102, 102, 102));
        jButtonPlayOnlyBuffer4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/next-26.png"))); // NOI18N
        jButtonPlayOnlyBuffer4.setToolTipText("Play only this buffer");
        jButtonPlayOnlyBuffer4.setBorder(null);
        jButtonPlayOnlyBuffer4.setContentAreaFilled(false);
        jButtonPlayOnlyBuffer4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonPlayOnlyBuffer4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayOnlyBuffer4ActionPerformed(evt);
            }
        });

        jToggleButtonPlayBuffer4.setBackground(new java.awt.Color(102, 102, 102));
        jToggleButtonPlayBuffer4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jToggleButtonPlayBuffer4.setToolTipText("Play this buffer on/off");
        jToggleButtonPlayBuffer4.setBorder(null);
        jToggleButtonPlayBuffer4.setContentAreaFilled(false);
        jToggleButtonPlayBuffer4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonPlayBuffer4.setRolloverEnabled(false);
        jToggleButtonPlayBuffer4.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/pause-26.png"))); // NOI18N
        jToggleButtonPlayBuffer4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonPlayBuffer4ActionPerformed(evt);
            }
        });

        jSliderBuffer4.setValue(0);
        jSliderBuffer4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jSliderBuffer4.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jSliderBuffer4MouseDragged(evt);
            }
        });

        jLabelBuffer4.setText("No sound loaded");

        jLabelBuffer4Spl.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelBuffer4Spl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBuffer4Spl.setText("00.0/00.0");
        jLabelBuffer4Spl.setToolTipText("LAeq / LAFmax [dB(A)]");
        jLabelBuffer4Spl.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabelBuffer4Spl.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jToggleButtonWriteToBuffer4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/calculator-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer4.setBorder(null);
        jToggleButtonWriteToBuffer4.setBorderPainted(false);
        jToggleButtonWriteToBuffer4.setContentAreaFilled(false);
        jToggleButtonWriteToBuffer4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonWriteToBuffer4.setRolloverEnabled(false);
        jToggleButtonWriteToBuffer4.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/cancel-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonWriteToBuffer4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToggleButtonWriteToBuffer4, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClearBuffer4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExportBuffer4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonPlayOnlyBuffer4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButtonPlayBuffer4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSliderBuffer4, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelBuffer4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelBuffer4Spl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSliderBuffer4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonPlayBuffer4, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                    .addComponent(jButtonPlayOnlyBuffer4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonExportBuffer4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonClearBuffer4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonWriteToBuffer4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer4Spl, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4))
        );

        jPanel17.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(255, 153, 0)));

        jButtonClearBuffer5.setBackground(new java.awt.Color(153, 153, 153));
        jButtonClearBuffer5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/delete-26.png"))); // NOI18N
        jButtonClearBuffer5.setToolTipText("Clears this sound buffer");
        jButtonClearBuffer5.setBorder(null);
        jButtonClearBuffer5.setContentAreaFilled(false);
        jButtonClearBuffer5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonClearBuffer5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearBuffer5ActionPerformed(evt);
            }
        });

        jButtonExportBuffer5.setBackground(new java.awt.Color(153, 153, 153));
        jButtonExportBuffer5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/audio_file-26.png"))); // NOI18N
        jButtonExportBuffer5.setToolTipText("Exports the buffer into a wav-file");
        jButtonExportBuffer5.setBorder(null);
        jButtonExportBuffer5.setContentAreaFilled(false);
        jButtonExportBuffer5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonExportBuffer5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportBuffer5ActionPerformed(evt);
            }
        });

        jButtonPlayOnlyBuffer5.setBackground(new java.awt.Color(153, 153, 153));
        jButtonPlayOnlyBuffer5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/next-26.png"))); // NOI18N
        jButtonPlayOnlyBuffer5.setToolTipText("Play only this buffer");
        jButtonPlayOnlyBuffer5.setBorder(null);
        jButtonPlayOnlyBuffer5.setContentAreaFilled(false);
        jButtonPlayOnlyBuffer5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonPlayOnlyBuffer5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayOnlyBuffer5ActionPerformed(evt);
            }
        });

        jToggleButtonPlayBuffer5.setBackground(new java.awt.Color(153, 153, 153));
        jToggleButtonPlayBuffer5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jToggleButtonPlayBuffer5.setToolTipText("Play this buffer on/off");
        jToggleButtonPlayBuffer5.setBorder(null);
        jToggleButtonPlayBuffer5.setContentAreaFilled(false);
        jToggleButtonPlayBuffer5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonPlayBuffer5.setRolloverEnabled(false);
        jToggleButtonPlayBuffer5.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/pause-26.png"))); // NOI18N
        jToggleButtonPlayBuffer5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonPlayBuffer5ActionPerformed(evt);
            }
        });

        jSliderBuffer5.setValue(0);
        jSliderBuffer5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jSliderBuffer5.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jSliderBuffer5MouseDragged(evt);
            }
        });

        jLabelBuffer5.setBackground(new java.awt.Color(153, 153, 153));
        jLabelBuffer5.setText("No sound loaded");

        jLabelBuffer5Spl.setBackground(new java.awt.Color(153, 153, 153));
        jLabelBuffer5Spl.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelBuffer5Spl.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBuffer5Spl.setText("00.0/00.0");
        jLabelBuffer5Spl.setToolTipText("LAeq / LAFmax [dB(A)]");
        jLabelBuffer5Spl.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabelBuffer5Spl.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jToggleButtonWriteToBuffer5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/calculator-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer5.setBorder(null);
        jToggleButtonWriteToBuffer5.setBorderPainted(false);
        jToggleButtonWriteToBuffer5.setContentAreaFilled(false);
        jToggleButtonWriteToBuffer5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jToggleButtonWriteToBuffer5.setRolloverEnabled(false);
        jToggleButtonWriteToBuffer5.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/cancel-26.png"))); // NOI18N
        jToggleButtonWriteToBuffer5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonWriteToBuffer5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jToggleButtonWriteToBuffer5, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClearBuffer5, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExportBuffer5, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonPlayOnlyBuffer5, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButtonPlayBuffer5, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSliderBuffer5, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelBuffer5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelBuffer5Spl, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel17Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jToggleButtonWriteToBuffer5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                    .addComponent(jButtonClearBuffer5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonExportBuffer5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonPlayOnlyBuffer5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jToggleButtonPlayBuffer5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSliderBuffer5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelBuffer5Spl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(4, 4, 4))
        );

        jLabel19.setBackground(new java.awt.Color(102, 102, 102));
        jLabel19.setFont(new java.awt.Font("Meiryo UI", 1, 12)); // NOI18N
        jLabel19.setText("Buffer controls");
        jLabel19.setToolTipText("");

        javax.swing.GroupLayout jPanelBuffersLayout = new javax.swing.GroupLayout(jPanelBuffers);
        jPanelBuffers.setLayout(jPanelBuffersLayout);
        jPanelBuffersLayout.setHorizontalGroup(
            jPanelBuffersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBuffersLayout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addGroup(jPanelBuffersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelBuffersLayout.createSequentialGroup()
                        .addGroup(jPanelBuffersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel19))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel17, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelBuffersLayout.setVerticalGroup(
            jPanelBuffersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBuffersLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(jLabel19)
                .addGap(3, 3, 3)
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );

        jPanelInfoBar.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(255, 153, 0)));

        jLabelStatus.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelStatus.setText("Welcome to RoadSynth");

        jProgressBarHidden.setStringPainted(true);

        jLabelOverload.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelOverload.setForeground(new java.awt.Color(255, 0, 0));
        jLabelOverload.setText("Overload Label");

        javax.swing.GroupLayout jPanelInfoBarLayout = new javax.swing.GroupLayout(jPanelInfoBar);
        jPanelInfoBar.setLayout(jPanelInfoBarLayout);
        jPanelInfoBarLayout.setHorizontalGroup(
            jPanelInfoBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoBarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelStatus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelOverload)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jProgressBarHidden, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelInfoBarLayout.setVerticalGroup(
            jPanelInfoBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInfoBarLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanelInfoBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabelOverload)
                    .addComponent(jProgressBarHidden, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelStatus, javax.swing.GroupLayout.Alignment.LEADING))
                .addGap(5, 5, 5))
        );

        jPanelToolBar.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(255, 153, 0)));

        jButtonToolBarNewFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/add_file-32.png"))); // NOI18N
        jButtonToolBarNewFile.setToolTipText("Creates a new project");
        jButtonToolBarNewFile.setBorder(null);
        jButtonToolBarNewFile.setContentAreaFilled(false);
        jButtonToolBarNewFile.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarNewFile.setFocusable(false);
        jButtonToolBarNewFile.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarNewFile.setSelected(true);
        jButtonToolBarNewFile.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarNewFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarNewFileActionPerformed(evt);
            }
        });

        jButtonToolBarOpenProject.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/opened_folder-32.png"))); // NOI18N
        jButtonToolBarOpenProject.setToolTipText("Opens up an earlier saved project");
        jButtonToolBarOpenProject.setBorder(null);
        jButtonToolBarOpenProject.setBorderPainted(false);
        jButtonToolBarOpenProject.setContentAreaFilled(false);
        jButtonToolBarOpenProject.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarOpenProject.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarOpenProject.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarOpenProject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarOpenProjectActionPerformed(evt);
            }
        });

        jButtonToolBarSaveProject.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/save-32.png"))); // NOI18N
        jButtonToolBarSaveProject.setToolTipText("Saves the project");
        jButtonToolBarSaveProject.setBorder(null);
        jButtonToolBarSaveProject.setBorderPainted(false);
        jButtonToolBarSaveProject.setContentAreaFilled(false);
        jButtonToolBarSaveProject.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarSaveProject.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarSaveProject.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarSaveProject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarSaveProjectActionPerformed(evt);
            }
        });

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/save_as-32.png"))); // NOI18N
        jButton4.setToolTipText("Saves the project as a new file");
        jButton4.setBorder(null);
        jButton4.setBorderPainted(false);
        jButton4.setContentAreaFilled(false);
        jButton4.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButtonToolBarExport.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/audio_file-32.png"))); // NOI18N
        jButtonToolBarExport.setToolTipText("Exports soundbuffers to wav");
        jButtonToolBarExport.setBorder(null);
        jButtonToolBarExport.setBorderPainted(false);
        jButtonToolBarExport.setContentAreaFilled(false);
        jButtonToolBarExport.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarExport.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarExport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarExportActionPerformed(evt);
            }
        });

        jButtonToolBarChart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/bar_chart-32.png"))); // NOI18N
        jButtonToolBarChart.setToolTipText("Displays the 1/3 octave band analyser");
        jButtonToolBarChart.setBorder(null);
        jButtonToolBarChart.setBorderPainted(false);
        jButtonToolBarChart.setContentAreaFilled(false);
        jButtonToolBarChart.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarChart.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarChart.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarChart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarChartActionPerformed(evt);
            }
        });

        jButtonToolBarAddCPXData.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/grid-32.png"))); // NOI18N
        jButtonToolBarAddCPXData.setToolTipText("Opens up the CPX database editor");
        jButtonToolBarAddCPXData.setBorder(null);
        jButtonToolBarAddCPXData.setBorderPainted(false);
        jButtonToolBarAddCPXData.setContentAreaFilled(false);
        jButtonToolBarAddCPXData.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarAddCPXData.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarAddCPXData.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarAddCPXData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarAddCPXDataActionPerformed(evt);
            }
        });

        jButtonToolBarCheckHardWare.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/system_information-32.png"))); // NOI18N
        jButtonToolBarCheckHardWare.setToolTipText("Displays your hardware");
        jButtonToolBarCheckHardWare.setBorderPainted(false);
        jButtonToolBarCheckHardWare.setContentAreaFilled(false);
        jButtonToolBarCheckHardWare.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarCheckHardWare.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarCheckHardWare.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarCheckHardWare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarCheckHardWareActionPerformed(evt);
            }
        });

        jLabelVersionInfo.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        jLabelVersionInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelVersionInfo.setText("VERSION INFO");
        jLabelVersionInfo.setToolTipText("Code and design by Gustav Grundfelt");

        jButtonToolBarExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/shutdown-32.png"))); // NOI18N
        jButtonToolBarExit.setToolTipText("Exits RoadSynth");
        jButtonToolBarExit.setBorder(null);
        jButtonToolBarExit.setBorderPainted(false);
        jButtonToolBarExit.setContentAreaFilled(false);
        jButtonToolBarExit.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarExit.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarExit.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarExitActionPerformed(evt);
            }
        });

        jButtonStopAll.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jButtonStopAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/stop-32.png"))); // NOI18N
        jButtonStopAll.setToolTipText("Stops all buffers");
        jButtonStopAll.setBorder(null);
        jButtonStopAll.setBorderPainted(false);
        jButtonStopAll.setContentAreaFilled(false);
        jButtonStopAll.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonStopAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStopAllActionPerformed(evt);
            }
        });

        jButtonToolBarExplainPicture.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/info-32.png"))); // NOI18N
        jButtonToolBarExplainPicture.setToolTipText("Displays the RoadSynth flat model");
        jButtonToolBarExplainPicture.setBorderPainted(false);
        jButtonToolBarExplainPicture.setContentAreaFilled(false);
        jButtonToolBarExplainPicture.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonToolBarExplainPicture.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonToolBarExplainPicture.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonToolBarExplainPicture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToolBarExplainPictureActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelToolBarLayout = new javax.swing.GroupLayout(jPanelToolBar);
        jPanelToolBar.setLayout(jPanelToolBarLayout);
        jPanelToolBarLayout.setHorizontalGroup(
            jPanelToolBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolBarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonToolBarNewFile, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jButtonToolBarOpenProject, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jButtonToolBarSaveProject, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonToolBarExport, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonToolBarChart, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonToolBarAddCPXData, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonToolBarCheckHardWare, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelVersionInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(88, 88, 88)
                .addComponent(jButtonToolBarExplainPicture, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonStopAll, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonToolBarExit, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelToolBarLayout.setVerticalGroup(
            jPanelToolBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelToolBarLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelToolBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonToolBarCheckHardWare, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonToolBarAddCPXData, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonStopAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonToolBarNewFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonToolBarOpenProject, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonToolBarSaveProject, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonToolBarExport, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonToolBarChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonToolBarExit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonToolBarExplainPicture, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jLabelVersionInfo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 1, 0, new java.awt.Color(255, 153, 0)));
        jTabbedPanel.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPanel.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        jTabbedPanel.setToolTipText("");
        jTabbedPanel.setFocusTraversalPolicyProvider(true);
        jTabbedPanel.setFont(new java.awt.Font("Meiryo", 1, 12)); // NOI18N

        jPanelListener.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new java.awt.Color(255, 153, 0)));

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("Distance from road:");
        jLabel5.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        jTextFieldRoadDist.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldRoadDist.setText("10");
        jTextFieldRoadDist.setToolTipText("Set the distance between the listener and the vehicle");
        jTextFieldRoadDist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldRoadDistActionPerformed(evt);
            }
        });

        jLabel16.setText("[m]");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Head diameter:");
        jLabel9.setToolTipText("");
        jLabel9.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel20.setText("Use HRTF:");
        jLabel20.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        jComboBoxEarDist.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22" }));
        jComboBoxEarDist.setSelectedIndex(5);

        jComboBoxPlaybackMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No (Mono)" }));
        jComboBoxPlaybackMode.setToolTipText("Use headphones in order to get the 3D-sound");
        jComboBoxPlaybackMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPlaybackModeActionPerformed(evt);
            }
        });

        jButtonDecreaseDistanceToRoad.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/expand_arrow-32.png"))); // NOI18N
        jButtonDecreaseDistanceToRoad.setToolTipText("Get further from road");
        jButtonDecreaseDistanceToRoad.setBorderPainted(false);
        jButtonDecreaseDistanceToRoad.setContentAreaFilled(false);
        jButtonDecreaseDistanceToRoad.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonDecreaseDistanceToRoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDecreaseDistanceToRoadActionPerformed(evt);
            }
        });

        jButtonIncreaseDistanceToRoad.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/collapse_arrow-32.png"))); // NOI18N
        jButtonIncreaseDistanceToRoad.setToolTipText("Increase distance to the road");
        jButtonIncreaseDistanceToRoad.setBorderPainted(false);
        jButtonIncreaseDistanceToRoad.setContentAreaFilled(false);
        jButtonIncreaseDistanceToRoad.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonIncreaseDistanceToRoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonIncreaseDistanceToRoadActionPerformed(evt);
            }
        });

        jLabel42.setText("[cm]");

        jLabelTrafficInformation1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelTrafficInformation1.setText("Traffic Information");
        jLabelTrafficInformation1.setToolTipText("This text indicates what your vehicle and road inputs are going to sound like when the sound is repeated.");

        javax.swing.GroupLayout jPanelListenerLayout = new javax.swing.GroupLayout(jPanelListener);
        jPanelListener.setLayout(jPanelListenerLayout);
        jPanelListenerLayout.setHorizontalGroup(
            jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelListenerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelListenerLayout.createSequentialGroup()
                        .addGroup(jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel20, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelListenerLayout.createSequentialGroup()
                                .addGroup(jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButtonDecreaseDistanceToRoad, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                                    .addComponent(jButtonIncreaseDistanceToRoad, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jTextFieldRoadDist)
                                    .addComponent(jComboBoxEarDist, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel16)
                                    .addComponent(jLabel42)))
                            .addComponent(jComboBoxPlaybackMode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(505, 505, 505))
                    .addGroup(jPanelListenerLayout.createSequentialGroup()
                        .addComponent(jLabelTrafficInformation1)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanelListenerLayout.setVerticalGroup(
            jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelListenerLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jButtonIncreaseDistanceToRoad, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextFieldRoadDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonDecreaseDistanceToRoad, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jComboBoxEarDist, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel42))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelListenerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(jComboBoxPlaybackMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 53, Short.MAX_VALUE)
                .addComponent(jLabelTrafficInformation1)
                .addContainerGap())
        );

        jTabbedPanel.addTab("Listener", jPanelListener);

        jPanelVehicle.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new java.awt.Color(255, 153, 0)));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText("Speed:");

        jComboBoxSpeed.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "30", "40", "50", "60", "70", "80", "90", "100", "110" }));
        jComboBoxSpeed.setSelectedIndex(8);
        jComboBoxSpeed.setToolTipText("Set the velocity of the vehicle");
        jComboBoxSpeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSpeedActionPerformed(evt);
            }
        });

        jLabel15.setText("[km/h]");

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Direction:");

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Engine:");

        jComboBoxDirection.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Left to Right", "Right to Left" }));
        jComboBoxDirection.setToolTipText("Select the direction of the car");
        jComboBoxDirection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDirectionActionPerformed(evt);
            }
        });

        jComboBoxEngine.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Gasoline 4-stroke with random rmp", "No Engine" }));
        jComboBoxEngine.setToolTipText("Select engine type");

        jLabel25.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel25.setText("Time separation:");
        jLabel25.setToolTipText("");

        jComboBoxSeparation.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", " " }));
        jComboBoxSeparation.setSelectedIndex(3);
        jComboBoxSeparation.setToolTipText("Seconds for the next car in the buffer to wait before \"entering\" sound buffer.");
        jComboBoxSeparation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSeparationActionPerformed(evt);
            }
        });

        jLabel32.setText("[s]");

        jLabelTrafficInformation2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelTrafficInformation2.setText("Traffic Information");
        jLabelTrafficInformation2.setToolTipText("This text indicates what your vehicle and road inputs are going to sound like when the sound is repeated.");

        javax.swing.GroupLayout jPanelVehicleLayout = new javax.swing.GroupLayout(jPanelVehicle);
        jPanelVehicle.setLayout(jPanelVehicleLayout);
        jPanelVehicleLayout.setHorizontalGroup(
            jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelVehicleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelTrafficInformation2)
                    .addGroup(jPanelVehicleLayout.createSequentialGroup()
                        .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel25, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelVehicleLayout.createSequentialGroup()
                                .addComponent(jComboBoxSeparation, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel32))
                            .addGroup(jPanelVehicleLayout.createSequentialGroup()
                                .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jComboBoxDirection, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jComboBoxSpeed, javax.swing.GroupLayout.Alignment.LEADING, 0, 99, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jComboBoxEngine, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(453, Short.MAX_VALUE))
        );
        jPanelVehicleLayout.setVerticalGroup(
            jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelVehicleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jComboBoxSpeed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jComboBoxDirection)
                    .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jComboBoxEngine)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelVehicleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jComboBoxSeparation)
                        .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel25, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 79, Short.MAX_VALUE)
                .addComponent(jLabelTrafficInformation2)
                .addContainerGap())
        );

        jTabbedPanel.addTab("Vehicle", jPanelVehicle);

        jPanelVehicleRoad.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new java.awt.Color(255, 153, 0)));
        jPanelVehicleRoad.setToolTipText("");

        jPanel2.setToolTipText("");

        jLabel14.setText("Start at:");
        jLabel14.setToolTipText("Left side of listener");

        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel21.setText("Stop at:");
        jLabel21.setToolTipText("Right side of listener");

        jTextFieldEndPos.setText("100");
        jTextFieldEndPos.setToolTipText("Right side of listener");
        jTextFieldEndPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldEndPosActionPerformed(evt);
            }
        });

        jTextFieldStartPos.setText("-100");
        jTextFieldStartPos.setToolTipText("Left side of listener");
        jTextFieldStartPos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldStartPosActionPerformed(evt);
            }
        });

        jLabelStartStopSecondLayer.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelStartStopSecondLayer.setText("Set passby length [m]:");

        jLabel38.setText("[m]");

        jLabel39.setText("[m]");

        jLabel22.setBackground(new java.awt.Color(102, 102, 102));
        jLabel22.setFont(new java.awt.Font("Meiryo UI", 1, 12)); // NOI18N
        jLabel22.setText("Pavement 1:");

        jLabelSecondLayer.setBackground(new java.awt.Color(102, 102, 102));
        jLabelSecondLayer.setFont(new java.awt.Font("Meiryo UI", 1, 12)); // NOI18N
        jLabelSecondLayer.setText("Pavement 2:");

        jCheckBoxActivateSecondLayer.setText("Activate a second pavement");
        jCheckBoxActivateSecondLayer.setToolTipText("Select if you want more then one type of pavement.");
        jCheckBoxActivateSecondLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxActivateSecondLayerActionPerformed(evt);
            }
        });

        jLabelStartStop2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelStartStop2.setText("Set passby length [m]:");

        jTextFieldStartPosSecondLayer.setText("-100");
        jTextFieldStartPosSecondLayer.setToolTipText("Left side of listener");
        jTextFieldStartPosSecondLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldStartPosSecondLayerActionPerformed(evt);
            }
        });

        jLabel17.setText("[m]");

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("Stop at:");
        jLabel12.setToolTipText("Right side of listener");

        jTextFieldEndPosSecondLayer.setText("100");
        jTextFieldEndPosSecondLayer.setToolTipText("Right side of listener");
        jTextFieldEndPosSecondLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldEndPosSecondLayerActionPerformed(evt);
            }
        });

        jLabel10.setText("Start at:");
        jLabel10.setToolTipText("Left side of listener");

        jComboBoxSetPassByDist.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000" }));
        jComboBoxSetPassByDist.setSelectedIndex(1);
        jComboBoxSetPassByDist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSetPassByDistActionPerformed(evt);
            }
        });

        jComboBoxSetPassByDistSecondLayer.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000" }));
        jComboBoxSetPassByDistSecondLayer.setSelectedIndex(1);
        jComboBoxSetPassByDistSecondLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSetPassByDistSecondLayerActionPerformed(evt);
            }
        });

        jLabel24.setText("[m]");

        jComboBoxPavement.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ABS 16 (build in, 30 - 110 km/h)\t 0 dB", "VIADODRÄN (build in, 30-110 km/h)\t-6 dB" }));
        jComboBoxPavement.setToolTipText("Select pavement from the menu");
        jComboBoxPavement.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxPavementItemStateChanged(evt);
            }
        });
        jComboBoxPavement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPavementActionPerformed(evt);
            }
        });

        jComboBoxPavementSecondLayer.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ABS 16 (build in, 30 - 110 km/h)\t 0 dB", "VIADODRÄN (build in, 30-110 km/h)\t-6 dB" }));
        jComboBoxPavementSecondLayer.setToolTipText("Select pavement from the menu");
        jComboBoxPavementSecondLayer.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jComboBoxPavementSecondLayerMouseClicked(evt);
            }
        });
        jComboBoxPavementSecondLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPavementSecondLayerActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jComboBoxPavement, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jCheckBoxActivateSecondLayer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jLabelSecondLayer, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(22, 22, 22)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextFieldStartPosSecondLayer, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                            .addComponent(jTextFieldStartPos))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17)
                            .addComponent(jLabel39))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)
                            .addComponent(jLabel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextFieldEndPosSecondLayer, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                            .addComponent(jTextFieldEndPos))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel38, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabelStartStopSecondLayer, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                            .addComponent(jLabelStartStop2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBoxSetPassByDist, 0, 119, Short.MAX_VALUE)
                            .addComponent(jComboBoxSetPassByDistSecondLayer, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(10, 10, 10))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jComboBoxPavementSecondLayer, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(jLabel14)
                    .addComponent(jTextFieldStartPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel39)
                    .addComponent(jLabel21)
                    .addComponent(jLabel38)
                    .addComponent(jTextFieldEndPos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelStartStop2)
                    .addComponent(jComboBoxSetPassByDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxPavement, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jCheckBoxActivateSecondLayer)
                .addGap(1, 1, 1)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelSecondLayer)
                    .addComponent(jLabel10)
                    .addComponent(jTextFieldStartPosSecondLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(jLabel12)
                    .addComponent(jTextFieldEndPosSecondLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelStartStopSecondLayer)
                    .addComponent(jComboBoxSetPassByDistSecondLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel24))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxPavementSecondLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jLabelTrafficInformation3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelTrafficInformation3.setText("Traffic Information");
        jLabelTrafficInformation3.setToolTipText("This text indicates what your vehicle and road inputs are going to sound like when the sound is repeated.");

        javax.swing.GroupLayout jPanelVehicleRoadLayout = new javax.swing.GroupLayout(jPanelVehicleRoad);
        jPanelVehicleRoad.setLayout(jPanelVehicleRoadLayout);
        jPanelVehicleRoadLayout.setHorizontalGroup(
            jPanelVehicleRoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelVehicleRoadLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelVehicleRoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelVehicleRoadLayout.createSequentialGroup()
                        .addComponent(jLabelTrafficInformation3)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelVehicleRoadLayout.setVerticalGroup(
            jPanelVehicleRoadLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelVehicleRoadLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                .addComponent(jLabelTrafficInformation3)
                .addContainerGap())
        );

        jTabbedPanel.addTab("Road", null, jPanelVehicleRoad, "");

        jPanelBarrier.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new java.awt.Color(255, 153, 0)));
        jPanelBarrier.setForeground(new java.awt.Color(212, 208, 200));
        jPanelBarrier.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N

        jCheckBoxNoiseBarrier1Active.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        jCheckBoxNoiseBarrier1Active.setToolTipText("Turns the barriers on and off");
        jCheckBoxNoiseBarrier1Active.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxNoiseBarrier1ActiveActionPerformed(evt);
            }
        });

        jComboBoxNoiseBarrier1RoadDist.setMaximumRowCount(5);
        jComboBoxNoiseBarrier1RoadDist.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2", "3", "4", "5", "6", "8", "10", "12", "15" }));
        jComboBoxNoiseBarrier1RoadDist.setSelectedIndex(3);
        jComboBoxNoiseBarrier1RoadDist.setToolTipText("Horizontal distance between road and barrier egde");

        jLabel27.setText("Dist [m]");
        jLabel27.setToolTipText("");

        jLabel28.setText("Start");

        jTextFieldNoiseBarrier1Start.setText("-100");
        jTextFieldNoiseBarrier1Start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldNoiseBarrier1StartActionPerformed(evt);
            }
        });

        jTextFieldNoiseBarrier1End.setText("0");

        jLabel29.setText("End");

        jLabel30.setText("Height [m]");

        jComboBoxNoiseBarrier1Height.setMaximumRowCount(5);
        jComboBoxNoiseBarrier1Height.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0" }));
        jComboBoxNoiseBarrier1Height.setSelectedIndex(3);
        jComboBoxNoiseBarrier1Height.setToolTipText("Height of the barrier egde rel. road surface");

        jCheckBoxNoiseBarrier2Active.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        jCheckBoxNoiseBarrier2Active.setToolTipText("Turns the barriers on and off");
        jCheckBoxNoiseBarrier2Active.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxNoiseBarrier2ActiveActionPerformed(evt);
            }
        });

        jComboBoxNoiseBarrier2RoadDist.setMaximumRowCount(5);
        jComboBoxNoiseBarrier2RoadDist.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2", "3", "4", "5", "6", "8", "10", "12", "15" }));
        jComboBoxNoiseBarrier2RoadDist.setSelectedIndex(3);
        jComboBoxNoiseBarrier2RoadDist.setToolTipText("Horizontal distance between road and barrier egde");

        jTextFieldNoiseBarrier2Start.setText("0");

        jTextFieldNoiseBarrier2End.setText("100");

        jComboBoxNoiseBarrier2Height.setMaximumRowCount(5);
        jComboBoxNoiseBarrier2Height.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0" }));
        jComboBoxNoiseBarrier2Height.setSelectedIndex(3);
        jComboBoxNoiseBarrier2Height.setToolTipText("Height of the barrier egde rel. road surface");

        jComboBoxNoiseBarrier3Height.setMaximumRowCount(5);
        jComboBoxNoiseBarrier3Height.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0" }));
        jComboBoxNoiseBarrier3Height.setSelectedIndex(3);
        jComboBoxNoiseBarrier3Height.setToolTipText("Height of the barrier egde rel. road surface");

        jTextFieldNoiseBarrier3Start.setText("0");

        jComboBoxNoiseBarrier3RoadDist.setMaximumRowCount(5);
        jComboBoxNoiseBarrier3RoadDist.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2", "3", "4", "5", "6", "8", "10", "12", "15" }));
        jComboBoxNoiseBarrier3RoadDist.setSelectedIndex(3);
        jComboBoxNoiseBarrier3RoadDist.setToolTipText("Horizontal distance between road and barrier egde");
        jComboBoxNoiseBarrier3RoadDist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxNoiseBarrier3RoadDistActionPerformed(evt);
            }
        });

        jCheckBoxNoiseBarrier3Active.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        jCheckBoxNoiseBarrier3Active.setToolTipText("Turns the barriers on and off");
        jCheckBoxNoiseBarrier3Active.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxNoiseBarrier3ActiveActionPerformed(evt);
            }
        });

        jTextFieldNoiseBarrier3End.setText("100");

        jLabel31.setText("Active");

        jLabel51.setText("Barrier 1:");

        jLabel52.setText("Barrier 2:");

        jLabel53.setText("Barrier 3:");

        jLabel54.setText("Warning! Barriers are implemented as \"stupid\" barriers and do not know each others existance.");

        javax.swing.GroupLayout jPanelBarrierLayout = new javax.swing.GroupLayout(jPanelBarrier);
        jPanelBarrier.setLayout(jPanelBarrierLayout);
        jPanelBarrierLayout.setHorizontalGroup(
            jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBarrierLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelBarrierLayout.createSequentialGroup()
                        .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel51)
                            .addComponent(jLabel52)
                            .addComponent(jLabel53))
                        .addGap(18, 18, 18)
                        .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel31, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelBarrierLayout.createSequentialGroup()
                                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jCheckBoxNoiseBarrier3Active, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jCheckBoxNoiseBarrier2Active, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jCheckBoxNoiseBarrier1Active, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jComboBoxNoiseBarrier3RoadDist, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jComboBoxNoiseBarrier1RoadDist, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jComboBoxNoiseBarrier2RoadDist, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(20, 20, 20)
                        .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanelBarrierLayout.createSequentialGroup()
                                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                                    .addComponent(jTextFieldNoiseBarrier1Start))
                                .addGap(18, 18, 18)
                                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel29, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                                    .addGroup(jPanelBarrierLayout.createSequentialGroup()
                                        .addComponent(jTextFieldNoiseBarrier1End)
                                        .addGap(1, 1, 1))))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelBarrierLayout.createSequentialGroup()
                                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelBarrierLayout.createSequentialGroup()
                                        .addComponent(jTextFieldNoiseBarrier2Start)
                                        .addGap(18, 18, 18))
                                    .addGroup(jPanelBarrierLayout.createSequentialGroup()
                                        .addComponent(jTextFieldNoiseBarrier3Start)
                                        .addGap(18, 18, 18)))
                                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jTextFieldNoiseBarrier2End, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                                    .addComponent(jTextFieldNoiseBarrier3End))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel30, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jComboBoxNoiseBarrier3Height, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jComboBoxNoiseBarrier2Height, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jComboBoxNoiseBarrier1Height, javax.swing.GroupLayout.Alignment.LEADING, 0, 65, Short.MAX_VALUE))))
                    .addGroup(jPanelBarrierLayout.createSequentialGroup()
                        .addComponent(jLabel54)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanelBarrierLayout.setVerticalGroup(
            jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBarrierLayout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel29)
                        .addComponent(jLabel28)
                        .addComponent(jLabel27)
                        .addComponent(jLabel31))
                    .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(7, 7, 7)
                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jCheckBoxNoiseBarrier1Active, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jComboBoxNoiseBarrier1Height, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBoxNoiseBarrier1RoadDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldNoiseBarrier1Start, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldNoiseBarrier1End, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel51, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jCheckBoxNoiseBarrier2Active, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldNoiseBarrier2Start, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextFieldNoiseBarrier2End, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jComboBoxNoiseBarrier2Height, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jComboBoxNoiseBarrier2RoadDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel52, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelBarrierLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldNoiseBarrier3Start, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextFieldNoiseBarrier3End, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jComboBoxNoiseBarrier3Height, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCheckBoxNoiseBarrier3Active, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jComboBoxNoiseBarrier3RoadDist, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel53, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(34, 34, 34)
                .addComponent(jLabel54)
                .addContainerGap(51, Short.MAX_VALUE))
        );

        jTabbedPanel.addTab("Barrier", jPanelBarrier);
        jPanelBarrier.getAccessibleContext().setAccessibleName("NOISE PROTECTION BARRIER ISO9613");

        jPanelGroundAbs.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new java.awt.Color(255, 153, 0)));
        jPanelGroundAbs.setForeground(new java.awt.Color(255, 255, 255));
        jPanelGroundAbs.setToolTipText("");

        jComboBoxGroundAbs.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Snow - [ 13 k rayls/m , t = 25 cm ]", "Thick moss - [ 30 k rayls/m , t = 10 cm ]", "Loose ground (grass, turf) - [  80 k rayls/m , t = 15 cm ]", "Pasture, Forest floors - [ 200 k rayls/m , t = 15 cm ]", "Compact lawns - [ 500 k rayls/m , t = 10 cm ]", "Dense ground (gravel, parking lot) - [ 2000 k rayls/m , t =10 cm ]", "Normal asphalt - [ 20000 k rayls/m , t = 10 cm ]", "Dense surface (Water, Concrete) - [ 200000 k rayls/m , t = 1 cm ]" }));
        jComboBoxGroundAbs.setToolTipText("Select the ground type");
        jComboBoxGroundAbs.setAutoscrolls(true);
        jComboBoxGroundAbs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxGroundAbsActionPerformed(evt);
            }
        });

        jLabel26.setText("Ground type:");

        jLabel33.setText("[k rayls / m ]");
        jLabel33.setToolTipText("Select the ground type");

        jPanelGroundAbsChart.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                jPanelGroundAbsChartComponentResized(evt);
            }
        });

        javax.swing.GroupLayout jPanelGroundAbsChartLayout = new javax.swing.GroupLayout(jPanelGroundAbsChart);
        jPanelGroundAbsChart.setLayout(jPanelGroundAbsChartLayout);
        jPanelGroundAbsChartLayout.setHorizontalGroup(
            jPanelGroundAbsChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanelGroundAbsChartLayout.setVerticalGroup(
            jPanelGroundAbsChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 188, Short.MAX_VALUE)
        );

        jLabel34.setText("Resistivety");

        jLabel35.setText("Thickness");

        jLabel36.setText("[cm]");
        jLabel36.setToolTipText("Select the ground type");

        jTextFieldGroundResistivety.setText("13");
        jTextFieldGroundResistivety.setToolTipText("Ground resistivety k rayls/m");
        jTextFieldGroundResistivety.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldGroundResistivetyActionPerformed(evt);
            }
        });

        jTextFieldGroundThickness.setText("25");
        jTextFieldGroundThickness.setToolTipText("Ground layer thickness");
        jTextFieldGroundThickness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldGroundThicknessActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelGroundAbsLayout = new javax.swing.GroupLayout(jPanelGroundAbs);
        jPanelGroundAbs.setLayout(jPanelGroundAbsLayout);
        jPanelGroundAbsLayout.setHorizontalGroup(
            jPanelGroundAbsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGroundAbsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelGroundAbsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelGroundAbsLayout.createSequentialGroup()
                        .addComponent(jPanelGroundAbsChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanelGroundAbsLayout.createSequentialGroup()
                        .addGroup(jPanelGroundAbsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBoxGroundAbs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(25, 25, 25)
                        .addGroup(jPanelGroundAbsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanelGroundAbsLayout.createSequentialGroup()
                                .addComponent(jTextFieldGroundResistivety, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel33)))
                        .addGap(21, 21, 21)
                        .addGroup(jPanelGroundAbsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel35, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanelGroundAbsLayout.createSequentialGroup()
                                .addComponent(jTextFieldGroundThickness, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel36)))
                        .addGap(53, 87, Short.MAX_VALUE))))
        );
        jPanelGroundAbsLayout.setVerticalGroup(
            jPanelGroundAbsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGroundAbsLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanelGroundAbsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel35, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addGroup(jPanelGroundAbsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxGroundAbs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel33, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldGroundResistivety, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldGroundThickness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel36, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(2, 2, 2)
                .addComponent(jPanelGroundAbsChart, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(2, 2, 2))
        );

        jLabel33.getAccessibleContext().setAccessibleName("[k Pa s / m2]");

        jTabbedPanel.addTab("Ground", jPanelGroundAbs);

        jButtonIncreaseRange.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/collapse_arrow-32.png"))); // NOI18N
        jButtonIncreaseRange.setToolTipText("Increase distance to the road");
        jButtonIncreaseRange.setBorderPainted(false);
        jButtonIncreaseRange.setContentAreaFilled(false);
        jButtonIncreaseRange.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonIncreaseRange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonIncreaseRangeActionPerformed(evt);
            }
        });

        jButtonDecreaseRange.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/expand_arrow-32.png"))); // NOI18N
        jButtonDecreaseRange.setToolTipText("Get further from road");
        jButtonDecreaseRange.setBorderPainted(false);
        jButtonDecreaseRange.setContentAreaFilled(false);
        jButtonDecreaseRange.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButtonDecreaseRange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDecreaseRangeActionPerformed(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Dynamic range:");

        jComboBoxDynamicRange.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0  -  80", "0  -  85", "0  -  90 ", "0  -  95", "0  -  100", "0  -  105", "0  -  110", "0  -  115" }));
        jComboBoxDynamicRange.setSelectedIndex(4);
        jComboBoxDynamicRange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDynamicRangeActionPerformed(evt);
            }
        });

        jLabel43.setText("dB @ 16 bit sound");

        jLabel8.setText("Note! Dynamic range should be set as low as possible without causing overload.");

        jLabel44.setText("High dynamic range increases the quantization distortion/error ");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel41))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jComboBoxDynamicRange, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jButtonDecreaseRange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jButtonIncreaseRange, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel43)
                                .addGap(0, 77, Short.MAX_VALUE)))
                        .addGap(414, 414, 414))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel44)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jButtonIncreaseRange, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxDynamicRange)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel43))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonDecreaseRange, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel44)
                .addGap(71, 71, 71)
                .addComponent(jLabel41)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPanel.addTab("Range", jPanel1);

        jPanel4.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new java.awt.Color(255, 153, 0)));

        jPanel3.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(255, 153, 0)));

        jLabel49.setBackground(new java.awt.Color(102, 102, 102));
        jLabel49.setFont(new java.awt.Font("Meiryo UI", 1, 12)); // NOI18N
        jLabel49.setText("Filters");

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel13.setText("Low pass corner frequency:");

        jComboBoxLpFilterCutOff.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "4000", "4500", "5000", "5500", "6000", "6500", "7000", "7500", "8000", "8500", "9000", "9500", "10000", "10500", "11000" }));
        jComboBoxLpFilterCutOff.setSelectedIndex(10);
        jComboBoxLpFilterCutOff.setToolTipText("To remove some of the resampling distortion.");
        jComboBoxLpFilterCutOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxLpFilterCutOffActionPerformed(evt);
            }
        });

        jLabel6.setText("Hz");

        jCheckBoxLpOn.setSelected(true);
        jCheckBoxLpOn.setText("On");
        jCheckBoxLpOn.setToolTipText("Toggles high pass filters on and off");
        jCheckBoxLpOn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxLpOnActionPerformed(evt);
            }
        });

        jLabel45.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel45.setText("High pass corner frequency:");

        jComboBoxHpFilterCutOff.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "40", "50", "60", "70", "80", "90", "100", "125", "150", "175", "200", "250", "300", "350", "400" }));
        jComboBoxHpFilterCutOff.setToolTipText("To avoid low frequency sound due to turbulence from measurements with the CPX method");

        jLabel46.setText("Hz");

        jCheckBoxHpOn.setSelected(true);
        jCheckBoxHpOn.setText("On");
        jCheckBoxHpOn.setToolTipText("Toggles low pass filters on and off");

        jLabel57.setBackground(new java.awt.Color(102, 102, 102));
        jLabel57.setFont(new java.awt.Font("Meiryo UI", 1, 12)); // NOI18N
        jLabel57.setText("Calculation");

        jCheckBoxLeaveOneCore.setText("Leave one processor core free");

        jCheckBoxUseSpline.setText("Use spline interpolation for doppler resampling");
        jCheckBoxUseSpline.setToolTipText("Otherwise linear interpolation will be used.");
        jCheckBoxUseSpline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxUseSplineActionPerformed(evt);
            }
        });

        jPanel7.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, new java.awt.Color(255, 153, 0)));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 11, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 74, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel49)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel45, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jComboBoxHpFilterCutOff, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBoxLpFilterCutOff, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel46, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jCheckBoxLpOn)
                            .addComponent(jCheckBoxHpOn))
                        .addGap(21, 21, 21)))
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel57)
                    .addComponent(jCheckBoxLeaveOneCore)
                    .addComponent(jCheckBoxUseSpline))
                .addContainerGap(156, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel49)
                            .addComponent(jLabel57))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jComboBoxLpFilterCutOff)
                                .addComponent(jLabel6)
                                .addComponent(jCheckBoxLpOn, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(jCheckBoxLeaveOneCore))
                            .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jComboBoxHpFilterCutOff)
                                .addComponent(jLabel46)
                                .addComponent(jCheckBoxHpOn, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jCheckBoxUseSpline))
                            .addComponent(jLabel45, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jLabel50.setBackground(new java.awt.Color(102, 102, 102));
        jLabel50.setFont(new java.awt.Font("Meiryo UI", 1, 12)); // NOI18N
        jLabel50.setText("Dithering at playback");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Dithering:");

        jComboBoxDither.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "0 [off]", "1", "2", "3 [default]", "4", "5" }));
        jComboBoxDither.setSelectedIndex(3);

        jLabel7.setText("samples");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel50)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxDither, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel50)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jComboBoxDither, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(47, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        jTabbedPanel.addTab("Miscellaneous", jPanel4);

        jPanelCalibration.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, new java.awt.Color(255, 153, 0)));

        jComboBoxCalibrationSignalAmplitude.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Sine 1 kHz - 40", "Sine 1 kHz - 50", "Sine 1 kHz - 60", "Sine 1 kHz - 70", "Sine 1 kHz - 80", "Noise 250 - 2500 Hz - 50", "Noise 250 - 2500 Hz - 60", "Noise 250 - 2500 Hz - 70", "Noise 250 - 2500 Hz - 80", "Noise 250 - 2500 Hz - 90" }));
        jComboBoxCalibrationSignalAmplitude.setToolTipText("Select a sine or noise signal");
        jComboBoxCalibrationSignalAmplitude.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxCalibrationSignalAmplitudeActionPerformed(evt);
            }
        });

        jCheckBoxCalibrationSignal.setText("Turn on");
        jCheckBoxCalibrationSignal.setToolTipText("Turns on the calibrator signal");
        jCheckBoxCalibrationSignal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxCalibrationSignalActionPerformed(evt);
            }
        });

        jLabel23.setText("dB(A) rel 20E-6 Pa");

        jButtonGenerateCalibrationFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/video_file-24.png"))); // NOI18N
        jButtonGenerateCalibrationFile.setText("Export 30 sec calibration signal");
        jButtonGenerateCalibrationFile.setToolTipText("Exports the calibration signal to wav file");
        jButtonGenerateCalibrationFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGenerateCalibrationFileActionPerformed(evt);
            }
        });

        jLabel47.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel47.setText("Signal rms:");

        jLabel48.setBackground(new java.awt.Color(102, 102, 102));
        jLabel48.setFont(new java.awt.Font("Meiryo UI", 1, 12)); // NOI18N
        jLabel48.setText("Calibrator");

        javax.swing.GroupLayout jPanelCalibrationLayout = new javax.swing.GroupLayout(jPanelCalibration);
        jPanelCalibration.setLayout(jPanelCalibrationLayout);
        jPanelCalibrationLayout.setHorizontalGroup(
            jPanelCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCalibrationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel48, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCheckBoxCalibrationSignal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCalibrationLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanelCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCalibrationLayout.createSequentialGroup()
                                .addComponent(jLabel47)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBoxCalibrationSignalAmplitude, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(153, 153, 153))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCalibrationLayout.createSequentialGroup()
                                .addComponent(jButtonGenerateCalibrationFile)
                                .addGap(19, 19, 19)))))
                .addContainerGap())
        );
        jPanelCalibrationLayout.setVerticalGroup(
            jPanelCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCalibrationLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel48)
                .addGap(8, 8, 8)
                .addComponent(jCheckBoxCalibrationSignal)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCalibrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel47)
                    .addComponent(jComboBoxCalibrationSignalAmplitude, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 106, Short.MAX_VALUE)
                .addComponent(jButtonGenerateCalibrationFile)
                .addContainerGap())
        );

        jTabbedPanel.addTab("Calibration", jPanelCalibration);

        javax.swing.GroupLayout jPanelDialChartLayout = new javax.swing.GroupLayout(jPanelDialChart);
        jPanelDialChart.setLayout(jPanelDialChartLayout);
        jPanelDialChartLayout.setHorizontalGroup(
            jPanelDialChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 285, Short.MAX_VALUE)
        );
        jPanelDialChartLayout.setVerticalGroup(
            jPanelDialChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 316, Short.MAX_VALUE)
        );

        jMenuFile.setText("File");

        jMenuItemNewFile.setText("New project");
        jMenuItemNewFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemNewFileActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemNewFile);
        jMenuFile.add(jSeparator1);

        jMenuItemOpenFile.setText("Open project");
        jMenuItemOpenFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenFileActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemOpenFile);
        jMenuFile.add(jSeparator2);

        jMenuItemSave.setText("Save");
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSave);

        jMenuItemSaveAs.setText("Save As");
        jMenuItemSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveAsActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveAs);
        jMenuFile.add(jSeparator3);

        jMenuItemExport.setText("Export project to wav");
        jMenuItemExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExportActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExport);
        jMenuFile.add(jSeparator4);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExit);

        jMenuBar.add(jMenuFile);

        jMenuDSP.setText("DSP");
        jMenuDSP.setToolTipText("Digital Signal Processing");

        jMenuItemGeneratePCN.setText("Calculate Noise Components");
        jMenuItemGeneratePCN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGeneratePCNActionPerformed(evt);
            }
        });
        jMenuDSP.add(jMenuItemGeneratePCN);
        jMenuDSP.add(jSeparator9);

        jMenuItem4.setText("Listen to Noise Components");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenuDSP.add(jMenuItem4);

        jMenuBar.add(jMenuDSP);

        jMenuBufferControl.setText("Buffer");

        jMenuItemPlay01.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jMenuItemPlay01.setText("Play Buffer 0 1");
        jMenuItemPlay01.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPlay01ActionPerformed(evt);
            }
        });
        jMenuBufferControl.add(jMenuItemPlay01);

        jMenuItemPlay23.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jMenuItemPlay23.setText("Play Buffer 2 3");
        jMenuItemPlay23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPlay23ActionPerformed(evt);
            }
        });
        jMenuBufferControl.add(jMenuItemPlay23);

        jMenuItemPlay45.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jMenuItemPlay45.setText("Play Buffer 4 5");
        jMenuItemPlay45.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPlay45ActionPerformed(evt);
            }
        });
        jMenuBufferControl.add(jMenuItemPlay45);

        jMenuItemPlay012.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jMenuItemPlay012.setText("Play Buffer 0 1 2");
        jMenuItemPlay012.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPlay012ActionPerformed(evt);
            }
        });
        jMenuBufferControl.add(jMenuItemPlay012);

        jMenuItemPlay345.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-26.png"))); // NOI18N
        jMenuItemPlay345.setText("Play Buffer 3 4 5");
        jMenuItemPlay345.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPlay345ActionPerformed(evt);
            }
        });
        jMenuBufferControl.add(jMenuItemPlay345);

        jMenuItemPlayAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/play-32.png"))); // NOI18N
        jMenuItemPlayAll.setText("Play All Buffers");
        jMenuItemPlayAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPlayAllActionPerformed(evt);
            }
        });
        jMenuBufferControl.add(jMenuItemPlayAll);
        jMenuBufferControl.add(jSeparator5);

        jMenuItemStopAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/se/grundfelt/openroadsynth/stop-32.png"))); // NOI18N
        jMenuItemStopAll.setText("Stop All Buffers");
        jMenuItemStopAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStopAllActionPerformed(evt);
            }
        });
        jMenuBufferControl.add(jMenuItemStopAll);

        jMenuBar.add(jMenuBufferControl);

        jMenuHelp.setText("Help");

        jMenuItemManual.setText("Manual");
        jMenuItemManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemManualActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemManual);

        jMenuBar.add(jMenuHelp);

        jMenuAbout.setText("About");
        jMenuAbout.setToolTipText("");

        jMenuItemThirdPartyLicenses.setText("Third party licenses");
        jMenuItemThirdPartyLicenses.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemThirdPartyLicensesActionPerformed(evt);
            }
        });
        jMenuAbout.add(jMenuItemThirdPartyLicenses);

        jMenuIcons.setText("Icons from icons8.com");
        jMenuIcons.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuIconsActionPerformed(evt);
            }
        });
        jMenuAbout.add(jMenuIcons);

        jMenuItemEula.setText("License");
        jMenuItemEula.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemEulaActionPerformed(evt);
            }
        });
        jMenuAbout.add(jMenuItemEula);

        jMenuBar.add(jMenuAbout);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTabbedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanelInfoBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelBuffers, javax.swing.GroupLayout.PREFERRED_SIZE, 557, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                        .addComponent(jPanelDialChart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(5, 5, 5))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelDialChart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelBuffers, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelInfoBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );

        jTabbedPanel.getAccessibleContext().setAccessibleName("Vehicle");

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void jTextFieldRoadDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldRoadDistActionPerformed
        jTextFieldRoadDist.setText(NumericHelper.makeToDouble(jTextFieldRoadDist.getText()));
    }//GEN-LAST:event_jTextFieldRoadDistActionPerformed

    private void jTextFieldStartPosSecondLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldStartPosSecondLayerActionPerformed
        jTextFieldStartPosSecondLayer.setText(NumericHelper.makeToDouble(jTextFieldStartPosSecondLayer.getText()));
        this.estimateTrafficFromInput();
    }//GEN-LAST:event_jTextFieldStartPosSecondLayerActionPerformed

    private void jComboBoxSpeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSpeedActionPerformed
        this.estimateTrafficFromInput();
    }//GEN-LAST:event_jComboBoxSpeedActionPerformed

    private void jComboBoxCalibrationSignalAmplitudeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxCalibrationSignalAmplitudeActionPerformed
        jCheckBoxCalibrationSignal.setSelected(false);
        calibratorSignal.isPlaying = false;
        calibratorSignal.selectedAmplitude = jComboBoxCalibrationSignalAmplitude.getSelectedIndex();
    }//GEN-LAST:event_jComboBoxCalibrationSignalAmplitudeActionPerformed

    private void jComboBoxSetPassByDistSecondLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSetPassByDistSecondLayerActionPerformed
        double distance = Double.parseDouble(jComboBoxSetPassByDistSecondLayer.getItemAt(jComboBoxSetPassByDistSecondLayer.getSelectedIndex()).toString());
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        jTextFieldStartPosSecondLayer.setText(String.valueOf(decimalFormat.format(-distance / 2)));
        jTextFieldEndPosSecondLayer.setText(String.valueOf(decimalFormat.format(distance / 2)));
        this.estimateTrafficFromInput();

    }//GEN-LAST:event_jComboBoxSetPassByDistSecondLayerActionPerformed

    private void jComboBoxDirectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDirectionActionPerformed

    }//GEN-LAST:event_jComboBoxDirectionActionPerformed

    private void jComboBoxPlaybackModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPlaybackModeActionPerformed
        //System.out.printf("NR: %d\n",jComboBoxPlaybackMode.getSelectedIndex());
    }//GEN-LAST:event_jComboBoxPlaybackModeActionPerformed

    private void jComboBoxSeparationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSeparationActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxSeparationActionPerformed

    private void jCheckBoxCalibrationSignalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxCalibrationSignalActionPerformed

            calibratorSignal.isPlaying = jCheckBoxCalibrationSignal.isSelected();
    }//GEN-LAST:event_jCheckBoxCalibrationSignalActionPerformed

    private void jCheckBoxNoiseBarrier3ActiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxNoiseBarrier3ActiveActionPerformed

    }//GEN-LAST:event_jCheckBoxNoiseBarrier3ActiveActionPerformed

    private void jCheckBoxNoiseBarrier2ActiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxNoiseBarrier2ActiveActionPerformed
      
    }//GEN-LAST:event_jCheckBoxNoiseBarrier2ActiveActionPerformed

    private void jCheckBoxNoiseBarrier1ActiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxNoiseBarrier1ActiveActionPerformed
     
    }//GEN-LAST:event_jCheckBoxNoiseBarrier1ActiveActionPerformed

    private void jButtonStopAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStopAllActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jButtonStopAllActionPerformed

    private void jComboBoxNoiseBarrier3RoadDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxNoiseBarrier3RoadDistActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxNoiseBarrier3RoadDistActionPerformed

    private void jButtonToolBarOpenProjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarOpenProjectActionPerformed
        openRoadSynthDataFile();
    }//GEN-LAST:event_jButtonToolBarOpenProjectActionPerformed

    private void jButtonToolBarSaveProjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarSaveProjectActionPerformed
        saveRoadSynthDataFile(false);
    }//GEN-LAST:event_jButtonToolBarSaveProjectActionPerformed


    private void jButtonToolBarExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarExitActionPerformed

        exitRoadSynth();
    }//GEN-LAST:event_jButtonToolBarExitActionPerformed

    private void jButtonToolBarExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarExportActionPerformed
        exportMixedBufferWavfile();
    }//GEN-LAST:event_jButtonToolBarExportActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        saveRoadSynthDataFile(true);
    }//GEN-LAST:event_jButton4ActionPerformed


    private void jButtonToolBarNewFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarNewFileActionPerformed
        this.createNewProject();
    }//GEN-LAST:event_jButtonToolBarNewFileActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        this.exitRoadSynth();
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.exitRoadSynth();
    }//GEN-LAST:event_formWindowClosing

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        this.exitRoadSynth();
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemOpenFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenFileActionPerformed
        this.openRoadSynthDataFile();
    }//GEN-LAST:event_jMenuItemOpenFileActionPerformed

    private void jMenuItemSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveAsActionPerformed
        this.saveRoadSynthDataFile(true);
    }//GEN-LAST:event_jMenuItemSaveAsActionPerformed

    private void jMenuItemNewFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemNewFileActionPerformed
        this.createNewProject();
    }//GEN-LAST:event_jMenuItemNewFileActionPerformed

    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveActionPerformed
        this.saveRoadSynthDataFile(false);
    }//GEN-LAST:event_jMenuItemSaveActionPerformed

    private void jMenuItemExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExportActionPerformed
        this.exportMixedBufferWavfile();
    }//GEN-LAST:event_jMenuItemExportActionPerformed

    private void jMenuItemGeneratePCNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGeneratePCNActionPerformed
        new PCNGeneratorThread().start();
    }//GEN-LAST:event_jMenuItemGeneratePCNActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        new PCNPlayer().start();
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jButtonToolBarChartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarChartActionPerformed
        analyserFrame.setVisible(true);
    }//GEN-LAST:event_jButtonToolBarChartActionPerformed

    private void jButtonToolBarAddCPXDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarAddCPXDataActionPerformed

            cpxFrame.loadCpxDataFile();
            cpxFrame.setVisible(true);
    }//GEN-LAST:event_jButtonToolBarAddCPXDataActionPerformed

    private void jComboBoxPavementSecondLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPavementSecondLayerActionPerformed

    }//GEN-LAST:event_jComboBoxPavementSecondLayerActionPerformed

    private void jComboBoxPavementSecondLayerMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jComboBoxPavementSecondLayerMouseClicked

    }//GEN-LAST:event_jComboBoxPavementSecondLayerMouseClicked

    private void jButtonToolBarCheckHardWareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarCheckHardWareActionPerformed
        new SystemInfoFrame().setVisible(true);
    }//GEN-LAST:event_jButtonToolBarCheckHardWareActionPerformed

    private void jMenuItemPlay01ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPlay01ActionPerformed
        soundBufferArray[0].isPlaying = true;
        soundBufferArray[1].isPlaying = true;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(true);
        jToggleButtonPlayBuffer1.setSelected(true);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jMenuItemPlay01ActionPerformed

    private void jMenuItemPlay23ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPlay23ActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = true;
        soundBufferArray[3].isPlaying = true;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(true);
        jToggleButtonPlayBuffer3.setSelected(true);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jMenuItemPlay23ActionPerformed

    private void jMenuItemPlay45ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPlay45ActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = true;
        soundBufferArray[5].isPlaying = true;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(true);
        jToggleButtonPlayBuffer5.setSelected(true);
        this.writeStatus(true);
    }//GEN-LAST:event_jMenuItemPlay45ActionPerformed

    private void jMenuItemPlay012ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPlay012ActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = true;
        soundBufferArray[1].isPlaying = true;
        soundBufferArray[2].isPlaying = true;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(true);
        jToggleButtonPlayBuffer1.setSelected(true);
        jToggleButtonPlayBuffer2.setSelected(true);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jMenuItemPlay012ActionPerformed

    private void jMenuItemPlay345ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPlay345ActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = true;
        soundBufferArray[4].isPlaying = true;
        soundBufferArray[5].isPlaying = true;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(true);
        jToggleButtonPlayBuffer4.setSelected(true);
        jToggleButtonPlayBuffer5.setSelected(true);
        this.writeStatus(true);
    }//GEN-LAST:event_jMenuItemPlay345ActionPerformed

    private void jMenuItemPlayAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemPlayAllActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = true;
        soundBufferArray[1].isPlaying = true;
        soundBufferArray[2].isPlaying = true;
        soundBufferArray[3].isPlaying = true;
        soundBufferArray[4].isPlaying = true;
        soundBufferArray[5].isPlaying = true;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(true);
        jToggleButtonPlayBuffer1.setSelected(true);
        jToggleButtonPlayBuffer2.setSelected(true);
        jToggleButtonPlayBuffer3.setSelected(true);
        jToggleButtonPlayBuffer4.setSelected(true);
        jToggleButtonPlayBuffer5.setSelected(true);
        this.writeStatus(true);
    }//GEN-LAST:event_jMenuItemPlayAllActionPerformed

    private void jMenuItemStopAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStopAllActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jMenuItemStopAllActionPerformed

    private void jTextFieldNoiseBarrier1StartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldNoiseBarrier1StartActionPerformed

    }//GEN-LAST:event_jTextFieldNoiseBarrier1StartActionPerformed

    private void jComboBoxGroundAbsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxGroundAbsActionPerformed
        /*
         Snow - [ 13 k rayls/m , t = 25 cm ]
         Thick moss - [ 30 k rayls/m , t = 10 cm ]
         Loose ground (grass, turf) -[  80 k rayls/m , t = 15 cm ]
         Pasture, Forest floors - [ 200 k rayls/m , t = 15 cm ]
         Compact lawns - [ 500 k rayls/m , t = 10 cm ]
         Dense ground (gravel, parking lot) - [ 2000 k rayls/m , t =10 cm ]
         Normal asphalt - [ 20000 k rayls/m , t = 10 cm ]
         Dense surface (Water, Concrete) - [ 200000 k rayls/m , t = 1 cm ]
         */
        switch (jComboBoxGroundAbs.getSelectedIndex()) {
            case 0: {
                jTextFieldGroundResistivety.setText("13");
                jTextFieldGroundThickness.setText("25");
                break;
            }
            case 1: {
                jTextFieldGroundResistivety.setText("30");
                jTextFieldGroundThickness.setText("10");
                break;
            }
            case 2: {
                jTextFieldGroundResistivety.setText("80");
                jTextFieldGroundThickness.setText("15");
                break;
            }
            case 3: {
                jTextFieldGroundResistivety.setText("200");
                jTextFieldGroundThickness.setText("15");
                break;
            }
            case 4: {
                jTextFieldGroundResistivety.setText("500");
                jTextFieldGroundThickness.setText("10");
                break;
            }
            case 5: {
                jTextFieldGroundResistivety.setText("2000");
                jTextFieldGroundThickness.setText("10");
                break;
            }
            case 6: {
                jTextFieldGroundResistivety.setText("20000");
                jTextFieldGroundThickness.setText("10");
                break;
            }
            case 7: {
                jTextFieldGroundResistivety.setText("200000");
                jTextFieldGroundThickness.setText("1");
                break;
            }
        }
        this.calculateGroundAbsorption();
    }//GEN-LAST:event_jComboBoxGroundAbsActionPerformed

    private void jPanelGroundAbsChartComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelGroundAbsChartComponentResized
        jPanelGroundAbsChart.removeAll();
        addGroundAbsChartToPanel();
    }//GEN-LAST:event_jPanelGroundAbsChartComponentResized

    private void jTextFieldGroundThicknessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldGroundThicknessActionPerformed
        //Uses the NumericHelper class in order to correct the entered strings
        jTextFieldGroundThickness.setText(NumericHelper.makeToDouble(jTextFieldGroundThickness.getText()));

        //Thickness can not be smaller than 1.
        Double thickness = Double.parseDouble(jTextFieldGroundThickness.getText());
        if (thickness < 1D) {
            thickness = 1D;
            jTextFieldGroundThickness.setText("" + thickness);
        }

        //Agains to remove any unneccesary decimals
        jTextFieldGroundThickness.setText(NumericHelper.makeToDouble(jTextFieldGroundThickness.getText()));

        calculateGroundAbsorption();
    }//GEN-LAST:event_jTextFieldGroundThicknessActionPerformed

    private void jTextFieldGroundResistivetyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldGroundResistivetyActionPerformed
        //Uses the NumericHelper class in order to correct the entered strings
        jTextFieldGroundResistivety.setText(NumericHelper.makeToDouble(jTextFieldGroundResistivety.getText()));

        Double resistance = Double.parseDouble(jTextFieldGroundResistivety.getText());
        if (resistance < 0.01D) {
            resistance = 0.01D;
            jTextFieldGroundResistivety.setText("" + resistance);
        }
        jTextFieldGroundResistivety.setText(NumericHelper.makeToDouble(jTextFieldGroundResistivety.getText()));

        calculateGroundAbsorption();
    }//GEN-LAST:event_jTextFieldGroundResistivetyActionPerformed

    private void jMenuItemThirdPartyLicensesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemThirdPartyLicensesActionPerformed
        new LicenseFrame("TPL").setVisible(true);
    }//GEN-LAST:event_jMenuItemThirdPartyLicensesActionPerformed

    private void jMenuItemEulaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemEulaActionPerformed
        new LicenseFrame("EULA").setVisible(true);
    }//GEN-LAST:event_jMenuItemEulaActionPerformed

    private void jMenuItemManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemManualActionPerformed
        JOptionPane.showMessageDialog(this,
                "There is no manual...yet!",
                "Not avaliable yet",
                JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_jMenuItemManualActionPerformed

    private void jMenuIconsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuIconsActionPerformed
        if (JOptionPane.showConfirmDialog(null, "All the icons are provided by icons8.com\n"
                + "\n"
                + "Do you want to continue to their website?",
                "About the icons",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                open(new URI("http://www.icons8.com"));
            } catch (URISyntaxException ex) {
                Logger.getLogger(OpenRoadSynth.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
        }
    }//GEN-LAST:event_jMenuIconsActionPerformed

    private void jComboBoxPavementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPavementActionPerformed
        
        int selItem = jComboBoxPavement.getSelectedIndex();
        if (selItem > 1) {
            jComboBoxSpeed.setEnabled(false);
            jComboBoxSpeed.setToolTipText("Speed is taken from the CPX database");
        } else {
            jComboBoxSpeed.setEnabled(true);
            jComboBoxSpeed.setToolTipText("Set the velocity of the vehicle");
        }
    }//GEN-LAST:event_jComboBoxPavementActionPerformed

    private void jTextFieldStartPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldStartPosActionPerformed
        jTextFieldStartPos.setText(NumericHelper.makeToDouble(jTextFieldStartPos.getText()));
        this.estimateTrafficFromInput();
    }//GEN-LAST:event_jTextFieldStartPosActionPerformed

    private void jComboBoxSetPassByDistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSetPassByDistActionPerformed
        double distance = Double.parseDouble(jComboBoxSetPassByDist.getItemAt(jComboBoxSetPassByDist.getSelectedIndex()).toString());
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalSeparatorAlwaysShown(false);
        jTextFieldStartPos.setText(String.valueOf(decimalFormat.format(-distance / 2)));
        jTextFieldEndPos.setText(String.valueOf(decimalFormat.format(distance / 2)));
        this.estimateTrafficFromInput();
    }//GEN-LAST:event_jComboBoxSetPassByDistActionPerformed

    private void jCheckBoxActivateSecondLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxActivateSecondLayerActionPerformed

     
            this.setSecondRoadLayerEnabled(this.jCheckBoxActivateSecondLayer.isSelected());
        
    }//GEN-LAST:event_jCheckBoxActivateSecondLayerActionPerformed

    private void jTextFieldEndPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldEndPosActionPerformed
        jTextFieldEndPos.setText(NumericHelper.makeToDouble(jTextFieldEndPos.getText()));
        this.estimateTrafficFromInput();
    }//GEN-LAST:event_jTextFieldEndPosActionPerformed

    private void jTextFieldEndPosSecondLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldEndPosSecondLayerActionPerformed
        jTextFieldEndPosSecondLayer.setText(NumericHelper.makeToDouble(jTextFieldEndPosSecondLayer.getText()));
        this.estimateTrafficFromInput();
    }//GEN-LAST:event_jTextFieldEndPosSecondLayerActionPerformed

    private void jComboBoxDynamicRangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDynamicRangeActionPerformed
        /*-10 <-> 80
         -5   <-> 85
         0    <-> 90
         5    <-> 95
         10  <-> 100
         15  <-> 105
         20  <-> 110
         25  <-> 115*/

        int selValue = jComboBoxDynamicRange.getSelectedIndex();
        switch (selValue) {
            case 0: {
                Globals.DYN_RANGE_LIMIT_LOW = -10D;
                break;
            }
            case 1: {
                Globals.DYN_RANGE_LIMIT_LOW = -5D;
                break;
            }
            case 2: {
                Globals.DYN_RANGE_LIMIT_LOW = 0D;
                break;
            }
            case 3: {
                Globals.DYN_RANGE_LIMIT_LOW = 5D;
                break;
            }
            case 4: {
                Globals.DYN_RANGE_LIMIT_LOW = 10D;
                break;
            }
            case 5: {
                Globals.DYN_RANGE_LIMIT_LOW = 15D;
                break;
            }
            case 6: {
                Globals.DYN_RANGE_LIMIT_LOW = 20D;
                break;
            }
            case 7: {
                Globals.DYN_RANGE_LIMIT_LOW = 25D;
                break;
            }
        }
        Globals.DYN_RANGE_GAIN = (float) Math.pow(10d, -Globals.DYN_RANGE_LIMIT_LOW / 20d);
        jLabelOverload.setText("");
        createDialChart();
    }//GEN-LAST:event_jComboBoxDynamicRangeActionPerformed

    private void jToggleButtonWriteToBuffer5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonWriteToBuffer5ActionPerformed
        startSoundCalculation(5);
    }//GEN-LAST:event_jToggleButtonWriteToBuffer5ActionPerformed

    private void jSliderBuffer5MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSliderBuffer5MouseDragged
        double slider_position = jSliderBuffer5.getValue();
        soundBufferArray[5].position = (int) (0.01 * slider_position * soundBufferArray[5].n_samples);
    }//GEN-LAST:event_jSliderBuffer5MouseDragged

    private void jToggleButtonPlayBuffer5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonPlayBuffer5ActionPerformed
        soundBufferArray[5].isPlaying = jToggleButtonPlayBuffer5.isSelected();
        writeStatus(true);
    }//GEN-LAST:event_jToggleButtonPlayBuffer5ActionPerformed

    private void jButtonPlayOnlyBuffer5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayOnlyBuffer5ActionPerformed
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = true;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(true);
        this.writeStatus(true);
    }//GEN-LAST:event_jButtonPlayOnlyBuffer5ActionPerformed

    private void jButtonExportBuffer5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportBuffer5ActionPerformed
        exportBufferToWav(5);
    }//GEN-LAST:event_jButtonExportBuffer5ActionPerformed

    private void jButtonClearBuffer5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearBuffer5ActionPerformed
        resetBuffer(5);
    }//GEN-LAST:event_jButtonClearBuffer5ActionPerformed

    private void jToggleButtonWriteToBuffer4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonWriteToBuffer4ActionPerformed
        startSoundCalculation(4);
    }//GEN-LAST:event_jToggleButtonWriteToBuffer4ActionPerformed

    private void jSliderBuffer4MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSliderBuffer4MouseDragged
        double slider_position = jSliderBuffer4.getValue();
        soundBufferArray[4].position = (int) (0.01 * slider_position * soundBufferArray[4].n_samples);
    }//GEN-LAST:event_jSliderBuffer4MouseDragged

    private void jToggleButtonPlayBuffer4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonPlayBuffer4ActionPerformed
        soundBufferArray[4].isPlaying = jToggleButtonPlayBuffer4.isSelected();
        writeStatus(true);
    }//GEN-LAST:event_jToggleButtonPlayBuffer4ActionPerformed

    private void jButtonPlayOnlyBuffer4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayOnlyBuffer4ActionPerformed
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = true;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(true);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jButtonPlayOnlyBuffer4ActionPerformed

    private void jButtonExportBuffer4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportBuffer4ActionPerformed
        exportBufferToWav(4);
    }//GEN-LAST:event_jButtonExportBuffer4ActionPerformed

    private void jButtonClearBuffer4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearBuffer4ActionPerformed
        resetBuffer(4);
    }//GEN-LAST:event_jButtonClearBuffer4ActionPerformed

    private void jToggleButtonWriteToBuffer3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonWriteToBuffer3ActionPerformed
        startSoundCalculation(3);
    }//GEN-LAST:event_jToggleButtonWriteToBuffer3ActionPerformed

    private void jSliderBuffer3MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSliderBuffer3MouseDragged
        double slider_position = jSliderBuffer3.getValue();
        soundBufferArray[3].position = (int) (0.01 * slider_position * soundBufferArray[3].n_samples);
    }//GEN-LAST:event_jSliderBuffer3MouseDragged

    private void jToggleButtonPlayBuffer3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonPlayBuffer3ActionPerformed
        soundBufferArray[3].isPlaying = jToggleButtonPlayBuffer3.isSelected();
        writeStatus(true);
    }//GEN-LAST:event_jToggleButtonPlayBuffer3ActionPerformed

    private void jButtonPlayOnlyBuffer3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayOnlyBuffer3ActionPerformed
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = true;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(true);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jButtonPlayOnlyBuffer3ActionPerformed

    private void jButtonExportBuffer3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportBuffer3ActionPerformed
        exportBufferToWav(3);
    }//GEN-LAST:event_jButtonExportBuffer3ActionPerformed

    private void jButtonClearBuffer3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearBuffer3ActionPerformed
        resetBuffer(3);
    }//GEN-LAST:event_jButtonClearBuffer3ActionPerformed

    private void jToggleButtonWriteToBuffer2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonWriteToBuffer2ActionPerformed
        startSoundCalculation(2);
    }//GEN-LAST:event_jToggleButtonWriteToBuffer2ActionPerformed

    private void jButtonPlayOnlyBuffer2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayOnlyBuffer2ActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = true;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(true);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jButtonPlayOnlyBuffer2ActionPerformed

    private void jSliderBuffer2MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSliderBuffer2MouseDragged
        double slider_position = jSliderBuffer2.getValue();
        soundBufferArray[2].position = (int) (0.01 * slider_position * soundBufferArray[2].n_samples);
    }//GEN-LAST:event_jSliderBuffer2MouseDragged

    private void jButtonClearBuffer2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearBuffer2ActionPerformed
        resetBuffer(2);
    }//GEN-LAST:event_jButtonClearBuffer2ActionPerformed

    private void jToggleButtonPlayBuffer2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonPlayBuffer2ActionPerformed
        soundBufferArray[2].isPlaying = jToggleButtonPlayBuffer2.isSelected();
        writeStatus(true);
    }//GEN-LAST:event_jToggleButtonPlayBuffer2ActionPerformed

    private void jButtonExportBuffer2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportBuffer2ActionPerformed
        exportBufferToWav(2);
    }//GEN-LAST:event_jButtonExportBuffer2ActionPerformed

    private void jToggleButtonWriteToBuffer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonWriteToBuffer1ActionPerformed
        startSoundCalculation(1);
    }//GEN-LAST:event_jToggleButtonWriteToBuffer1ActionPerformed

    private void jSliderBuffer1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSliderBuffer1MouseDragged
        double slider_position = jSliderBuffer1.getValue();
        soundBufferArray[1].position = (int) (0.01 * slider_position * soundBufferArray[1].n_samples);
    }//GEN-LAST:event_jSliderBuffer1MouseDragged

    private void jToggleButtonPlayBuffer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonPlayBuffer1ActionPerformed
        soundBufferArray[1].isPlaying = jToggleButtonPlayBuffer1.isSelected();
        writeStatus(true);
    }//GEN-LAST:event_jToggleButtonPlayBuffer1ActionPerformed

    private void jButtonPlayOnlyBuffer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayOnlyBuffer1ActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = false;
        soundBufferArray[1].isPlaying = true;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(false);
        jToggleButtonPlayBuffer1.setSelected(true);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jButtonPlayOnlyBuffer1ActionPerformed

    private void jButtonExportBuffer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportBuffer1ActionPerformed
        exportBufferToWav(1);
    }//GEN-LAST:event_jButtonExportBuffer1ActionPerformed

    private void jButtonClearBuffer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearBuffer1ActionPerformed
        resetBuffer(1);
    }//GEN-LAST:event_jButtonClearBuffer1ActionPerformed

    private void jToggleButtonWriteToBuffer0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonWriteToBuffer0ActionPerformed
        startSoundCalculation(0);
    }//GEN-LAST:event_jToggleButtonWriteToBuffer0ActionPerformed

    private void jSliderBuffer0MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jSliderBuffer0MouseDragged
        double slider_position = jSliderBuffer0.getValue();
        soundBufferArray[0].position = (int) (0.01 * slider_position * soundBufferArray[0].n_samples);
    }//GEN-LAST:event_jSliderBuffer0MouseDragged

    private void jToggleButtonPlayBuffer0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonPlayBuffer0ActionPerformed
        soundBufferArray[0].isPlaying = jToggleButtonPlayBuffer0.isSelected();
        writeStatus(true);
    }//GEN-LAST:event_jToggleButtonPlayBuffer0ActionPerformed

    private void jButtonPlayOnlyBuffer0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayOnlyBuffer0ActionPerformed
        //Pauses all the other bufferthreads
        soundBufferArray[0].isPlaying = true;
        soundBufferArray[1].isPlaying = false;
        soundBufferArray[2].isPlaying = false;
        soundBufferArray[3].isPlaying = false;
        soundBufferArray[4].isPlaying = false;
        soundBufferArray[5].isPlaying = false;

        //Update the GUI
        jToggleButtonPlayBuffer0.setSelected(true);
        jToggleButtonPlayBuffer1.setSelected(false);
        jToggleButtonPlayBuffer2.setSelected(false);
        jToggleButtonPlayBuffer3.setSelected(false);
        jToggleButtonPlayBuffer4.setSelected(false);
        jToggleButtonPlayBuffer5.setSelected(false);
        this.writeStatus(true);
    }//GEN-LAST:event_jButtonPlayOnlyBuffer0ActionPerformed

    private void jButtonExportBuffer0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportBuffer0ActionPerformed
        exportBufferToWav(0);
    }//GEN-LAST:event_jButtonExportBuffer0ActionPerformed

    private void jButtonClearBuffer0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearBuffer0ActionPerformed
        resetBuffer(0);
    }//GEN-LAST:event_jButtonClearBuffer0ActionPerformed

    private void jButtonIncreaseDistanceToRoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonIncreaseDistanceToRoadActionPerformed
        String str = NumericHelper.makeToDouble(jTextFieldRoadDist.getText());
        double dbl = Double.parseDouble(str);
        dbl *= 1.1;
        Integer value = (int) (dbl + 0.5);
        jTextFieldRoadDist.setText(value.toString());
    }//GEN-LAST:event_jButtonIncreaseDistanceToRoadActionPerformed

    private void jButtonDecreaseDistanceToRoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDecreaseDistanceToRoadActionPerformed
        String str = NumericHelper.makeToDouble(jTextFieldRoadDist.getText());
        double dbl = Double.parseDouble(str);
        dbl *= 0.9;
        Integer value = (int) dbl;
        if (value >= 5) {
            jTextFieldRoadDist.setText(value.toString());
        }
    }//GEN-LAST:event_jButtonDecreaseDistanceToRoadActionPerformed

    private void jButtonIncreaseRangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonIncreaseRangeActionPerformed
        int val = jComboBoxDynamicRange.getSelectedIndex();
        int numberOfSelections = jComboBoxDynamicRange.getItemCount();

        if (val < numberOfSelections - 1) {
            val++;
            jComboBoxDynamicRange.setSelectedIndex(val);
        }
    }//GEN-LAST:event_jButtonIncreaseRangeActionPerformed

    private void jButtonDecreaseRangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDecreaseRangeActionPerformed
        int val = jComboBoxDynamicRange.getSelectedIndex();

        if (val > 0) {
            val--;
            jComboBoxDynamicRange.setSelectedIndex(val);
        }
    }//GEN-LAST:event_jButtonDecreaseRangeActionPerformed

    private void jComboBoxLpFilterCutOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxLpFilterCutOffActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxLpFilterCutOffActionPerformed

    private void jCheckBoxLpOnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxLpOnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxLpOnActionPerformed

    private void jButtonGenerateCalibrationFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGenerateCalibrationFileActionPerformed

       
        int buttons = JOptionPane.YES_NO_OPTION;
        int numberOfSamplesToWrite = 0;
        int actionDialog = 0;
        boolean exportFile;

        numberOfSamplesToWrite = Globals.SAMPLING_RATE * 20;

        String dynRange = this.jComboBoxDynamicRange.getSelectedItem().toString();
        String selRMS = this.jComboBoxCalibrationSignalAmplitude.getSelectedItem().toString();

        //File chooser
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Wave files", "wav");
        exportFileChooser.addChoosableFileFilter(filter);
        exportFileChooser.setFileFilter(filter);
        exportFileChooser.setSelectedFile(new File(exportFileChooser.getCurrentDirectory() + "\\RS Calibration Signal " + selRMS + " dBA at range " + dynRange + "dB.wav"));

        int selIndex = this.jComboBoxCalibrationSignalAmplitude.getSelectedIndex();

        int returnVal = exportFileChooser.showSaveDialog(OpenRoadSynth.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            //exportFileChooser.setFileFilter(JFileChooser.);
            File wavFile = exportFileChooser.getSelectedFile();
            exportFile = true;
            if (wavFile.exists()) {
                actionDialog = JOptionPane.showConfirmDialog(null, "Replace existing file?", "File exists!", buttons);
            }
            if (actionDialog != JOptionPane.YES_OPTION) {
                exportFile = false;
            }
            if (exportFile) {
                //Get the number of samples to write.

                float[] signalLeft = new float[numberOfSamplesToWrite];
                float[] signalRight = new float[numberOfSamplesToWrite];

                for (int n = 0; n < numberOfSamplesToWrite; n++) { //<---------
                    signalLeft[n] = calibratorSignal.signalArray[selIndex][n] * (float) Globals.DYN_RANGE_GAIN;
                    signalRight[n] = calibratorSignal.signalArray[selIndex][n] * (float) Globals.DYN_RANGE_GAIN;
                }

                SignalToolbox.exportSignalToWav(signalLeft, signalRight, wavFile);

            }
        }
    }//GEN-LAST:event_jButtonGenerateCalibrationFileActionPerformed

    private void jCheckBoxUseSplineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxUseSplineActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCheckBoxUseSplineActionPerformed

    private void jButtonToolBarExplainPictureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToolBarExplainPictureActionPerformed
        infoFrame.setVisible(true);
    }//GEN-LAST:event_jButtonToolBarExplainPictureActionPerformed

    private void jComboBoxPavementItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxPavementItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxPavementItemStateChanged

    private static void open(URI uri) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (Exception e) { /* TODO: error handling */ }
        } else { /* TODO: error handling */ }
    }

    private void makePavementMenus() {
        jComboBoxPavement.removeAllItems();
        jComboBoxPavement.addItem("ABS 16    (build in, 30 - 110 km/h)\t 0 dB");
        jComboBoxPavement.addItem("VIADODRÄN (build in, 30 - 110 km/h)\t-6 dB");

        jComboBoxPavementSecondLayer.removeAllItems();
        jComboBoxPavementSecondLayer.addItem("ABS 16    (build in, 30 - 110 km/h)\t 0 dB");
        jComboBoxPavementSecondLayer.addItem("VIADODRÄN (build in, 30 - 110 km/h)\t-6 dB");

        String[] menuItems = cpxFrame.getStringsForMenu();
        for (String menuItem : menuItems) {
            jComboBoxPavement.addItem(menuItem);
            jComboBoxPavementSecondLayer.addItem(menuItem);
        }
    }

    private void initMyCharts() {
        //Dial Chart LAFmax display
        this.createDialChart();

        //Ground absorption chart
        this.createGroundAbsDataset();
        this.addGroundAbsChartToPanel();

        this.calculateGroundAbsorption();
    }

    private void createNewProject() {
        int buttons = JOptionPane.YES_NO_OPTION;
        int actionDialog = 0;
        boolean OkayToClear = false;

        if (projectIsSaved) {
            OkayToClear = true;
        } else {
            actionDialog = JOptionPane.showConfirmDialog(null, "Project is not saved. Do you want discard the sounddata?", "Please confirm!", buttons);
            if (actionDialog == JOptionPane.YES_OPTION) {
                OkayToClear = true;
            }
        }
        if (OkayToClear) {
            filePathToThisProject = null;
            resetBuffer(0);
            resetBuffer(1);
            resetBuffer(2);
            resetBuffer(3);
            resetBuffer(4);
            resetBuffer(5);
            writeStatus(false);
            jLabelStatus.setText("New file...");
        }
    }

    public void updateDatasetSPLmaxFast(double newValue) {
        this.datasetSPLmaxFast.setValue(newValue);
    }

    public void updateDatasetSPLmaxSample(double newValue) {
        double currentValue = this.datasetSPLmaxSample.getValue().doubleValue();
        if (newValue > currentValue) {
            this.datasetSPLmaxSample.setValue(newValue);
        }
    }

    public void setDatasetSPLmaxSampleValue(double value) {
        this.datasetSPLmaxSample.setValue(value);
    }

    private void createDialChart() {

        final Color myTextColor = Globals.TEXT_COLOR;//new Color(255,153,0);
        final Color myBarColor = new Color(255, 200, 100);
        final Color myDRColor = Color.GREEN;//new Color(32, 132, 55);
        final Color myPointerColor = new Color(0, 191, 255);

        jPanelDialChart.removeAll();

        DialPlot localDialPlot = new DialPlot();
        localDialPlot.setView(0.0D, 0.0D, 1.0D, 1.0D);
        //localDialPlot.set
        localDialPlot.setDataset(0, this.datasetSPLmaxFast);
        localDialPlot.setDataset(1, this.datasetSPLmaxSample);

        StandardDialFrame localStandardDialFrame = new StandardDialFrame();
        localStandardDialFrame.setBackgroundPaint(Color.BLACK);
        localStandardDialFrame.setForegroundPaint(Color.DARK_GRAY);
        localDialPlot.setDialFrame(localStandardDialFrame);

        GradientPaint localGradientPaint = new GradientPaint(new Point(), myBarColor, new Point(), Color.WHITE);
        DialBackground localDialBackground = new DialBackground(localGradientPaint);
        localDialBackground.setGradientPaintTransformer(new StandardGradientPaintTransformer(GradientPaintTransformType.CENTER_VERTICAL));

        //localDialPlot.setBackground(localDialBackground);
        localDialPlot.setBackground(localDialBackground);

        DialTextAnnotation localDialTextAnnotation = new DialTextAnnotation("SPL METER");
        localDialTextAnnotation.setFont(new Font("Dialog", 1, 12));
        //localDialTextAnnotation.setPaint(myTextColor);
        localDialTextAnnotation.setRadius(0.7D);
        localDialPlot.addLayer(localDialTextAnnotation);

        DialValueIndicator localDialValueIndicator1 = new DialValueIndicator(0);
        localDialValueIndicator1.setFont(new Font("Dialog", 0, 10));
        localDialValueIndicator1.setPaint(Color.BLACK);
        localDialValueIndicator1.setBackgroundPaint(myPointerColor);
        localDialValueIndicator1.setOutlinePaint(myPointerColor);
        localDialValueIndicator1.setRadius(0.6D);
        localDialValueIndicator1.setAngle(-103.0D);
        localDialPlot.addLayer(localDialValueIndicator1);

        DialValueIndicator localDialValueIndicator2 = new DialValueIndicator(1);
        localDialValueIndicator2.setFont(new Font("Dialog", 1, 10));
        localDialValueIndicator2.setPaint(Color.WHITE);
        localDialValueIndicator2.setBackgroundPaint(Color.RED);
        localDialValueIndicator2.setOutlinePaint(Color.RED);
        localDialValueIndicator2.setRadius(0.6D);
        localDialValueIndicator2.setAngle(-77.0D);
        localDialPlot.addLayer(localDialValueIndicator2);

        StandardDialScale localStandardDialScale1 = new StandardDialScale(0D, 120D, -120D, -300D, 10D, 4);
        localStandardDialScale1.setTickRadius(0.88D);
        localStandardDialScale1.setTickLabelOffset(0.15D);
        localStandardDialScale1.setTickLabelFont(new Font("Dialog", 0, 11));
        localStandardDialScale1.setTickLabelPaint(Color.BLUE);
        localStandardDialScale1.setMajorTickPaint(Color.BLUE);
        localStandardDialScale1.setMinorTickPaint(Color.BLUE);
        localStandardDialScale1.setTickLabelFormatter(new DecimalFormat("###"));
        localDialPlot.addScale(0, localStandardDialScale1);

        StandardDialScale localStandardDialScale2 = new StandardDialScale(0D, 120D, -120D, -300D, 10D, 4);
        localStandardDialScale2.setTickRadius(0.5D);
        localStandardDialScale2.setTickLabelOffset(0.15D);
        localStandardDialScale2.setTickLabelFont(new Font("Dialog", 0, 10));
        localStandardDialScale2.setTickLabelPaint(Color.red);
        localStandardDialScale2.setMajorTickPaint(Color.red);
        localStandardDialScale2.setMinorTickPaint(Color.red);
        localStandardDialScale2.setTickLabelFormatter(new DecimalFormat("###"));
        localDialPlot.addScale(1, localStandardDialScale2);
        localDialPlot.mapDatasetToScale(1, 1);

        StandardDialRange localStandardDialRange = new StandardDialRange(0D, Globals.DYN_RANGE_LIMIT_LOW + 90D, myDRColor);
        localStandardDialRange.setScaleIndex(1);
        localStandardDialRange.setInnerRadius(0.59D);
        localStandardDialRange.setOuterRadius(0.59D);
        localDialPlot.addLayer(localStandardDialRange);

        DialPointer.Pin localPin = new DialPointer.Pin(1);
        localPin.setRadius(0.55D);
        localPin.setPaint(Color.RED);
        localDialPlot.addPointer(localPin);

        DialPointer.Pointer localPointer = new DialPointer.Pointer(0);
        localPointer.setFillPaint(myPointerColor);
        localPointer.setOutlinePaint(Color.BLUE);
        localDialPlot.addPointer(localPointer);

        DialCap localDialCap = new DialCap();
        localDialCap.setRadius(0.1D);
        localDialPlot.setCap(localDialCap);

        JFreeChart localJFreeChart = new JFreeChart(localDialPlot);

        //from http://www.jfree.org/phpBB2/viewtopic.php?f=3&t=27968
        TextTitle tt = new TextTitle("Mouse click on chart to reset");
        tt.setPaint(myTextColor);
        BorderArrangement arrangement = new BorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);
        container.add(tt, RectangleEdge.BOTTOM);
        CompositeTitle main = new CompositeTitle(container);
        localJFreeChart.addSubtitle(0, main);
        main.setPosition(RectangleEdge.BOTTOM);

        LegendTitle legend = new LegendTitle(new LegendItemSource() {
            @Override
            public LegendItemCollection getLegendItems() {
                LegendItemCollection result = new LegendItemCollection();
                result.add(new LegendItem("SPL [FAST] dB(A)", myPointerColor));
                result.add(new LegendItem("Loudest sample dB", Color.red));
                result.add(new LegendItem("Range dB", myDRColor));
                return result;
            }
        });
        //legend.setBackgroundPaint(myPanelColor);
        legend.setItemPaint(myTextColor);
        legend.setPosition(RectangleEdge.BOTTOM);
        localJFreeChart.addLegend(legend);

        //Add chart to jPanelDialChart
        ChartPanel chartPanel = new ChartPanel(localJFreeChart);
        chartPanel.setToolTipText("Click to reset");
        int width = this.jPanelDialChart.getWidth();
        int height = this.jPanelDialChart.getHeight();

        /*jPanel1.setLayout(new java.awt.BorderLayout());
         ........
         ChartPanel CP = new ChartPanel(chart);
         .....
         jPanel1.add(CP,BorderLayout.CENTER);
         jPanel1.validate();*/
        //this.jPanelDialChart.setLayout(new java.awt.BorderLayout());
        chartPanel.setSize(width, height);
        chartPanel.setVisible(true);
        //jPanelDialChart.setLayout(new java.awt.BorderLayout());
        //jPanelDialChart.setHorizontalAlignment(JPanel.);
        //label.setVerticalAlignment(JLabel.CENTER);
        this.jPanelDialChart.add(chartPanel, BorderLayout.CENTER);
        this.jPanelDialChart.validate();
        double currentValue = this.datasetSPLmaxSample.getValue().doubleValue();
        setDatasetSPLmaxSampleValue(currentValue);

        //set mouse listener
        //http://stackoverflow.com/questions/8218853/how-to-listen-for-clicks-in-java-jfreechart-using-events
        chartPanel.addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseClicked(ChartMouseEvent e) {
                setDatasetSPLmaxSampleValue(0d);

            }

            @Override
            public void chartMouseMoved(ChartMouseEvent e) {
            }

        });

    }

    private void createGroundAbsDataset() {

        groundAbsCategoryDataset = new DefaultCategoryDataset();
        for (int i_band = 0; i_band < this.fCenter.length; i_band++) {
            groundAbsCategoryDataset.addValue(0.2d, "Band", String.valueOf((int) fCenter[i_band]));
        }
    }

    private JFreeChart createGroundAbsChart(CategoryDataset paramCategoryDataset) {

        Color myPanelColor = Globals.PANEL_COLOR;
        Color myBackgroundColor = new Color(18, 30, 49);
        Color myLineColor = Color.WHITE;

        /*CategoryAxis localCategoryAxis = new CategoryAxis("Frequency 1/3 octave bands [Hz]");
         NumberAxis localNumberAxis = new NumberAxis("Sound Pressure Level - LAmaxF [dB(A)]");
         */
        JFreeChart localJFreeChart = ChartFactory.createLineChart("", "Frequency 1/3 octave bands [Hz]", "Absorption", paramCategoryDataset);

        CategoryPlot localCategoryPlot = (CategoryPlot) localJFreeChart.getPlot();
        localCategoryPlot.setDomainGridlinesVisible(true);
        localCategoryPlot.setRangeGridlinePaint(Color.gray);
        //Set line color
        localCategoryPlot.getRenderer().setSeriesPaint(0, myLineColor);

        //Set Background color //gradient
        //localJFreeChart.setBackgroundPaint(new GradientPaint(0.0F, 0.0F, myPanelColor, 350.0F, 0.0F,myBackgroundColor , true));
        localJFreeChart.setBackgroundPaint(myPanelColor);
        localJFreeChart.getPlot().setBackgroundPaint(myBackgroundColor);
        //localJFreeChart.setBorderPaint(Color.lightGray);
        localJFreeChart.removeLegend();

        //My text color
        Color myTextColor = Globals.TEXT_COLOR;

        //Y-axis
        NumberAxis localNumberAxis = (NumberAxis) localCategoryPlot.getRangeAxis();
        localNumberAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        localNumberAxis.setAxisLinePaint(myTextColor);
        localNumberAxis.setTickLabelPaint(myTextColor);
        localNumberAxis.setLabelPaint(myTextColor);
        localNumberAxis.setTickMarkPaint(myTextColor);
        //Grid size
        localNumberAxis.setTickUnit(new NumberTickUnit(0.2));
        //Y-axis range
        localNumberAxis.setRange(0d, 1.3d);

        //Line
        LineAndShapeRenderer localLineAndShapeRenderer = (LineAndShapeRenderer) localCategoryPlot.getRenderer();
        localLineAndShapeRenderer.setBaseShapesVisible(true);
        localLineAndShapeRenderer.setDrawOutlines(true);
        localLineAndShapeRenderer.setUseFillPaint(true);
        localLineAndShapeRenderer.setBaseFillPaint(Color.white);
        localLineAndShapeRenderer.setSeriesStroke(0, new BasicStroke(1.0F));
        localLineAndShapeRenderer.setSeriesOutlineStroke(0, new BasicStroke(1.0F));
        localLineAndShapeRenderer.setSeriesShape(0, ShapeUtilities.createDiamond(4.0F));

        //X-axis
        CategoryAxis localCategoryAxis = localCategoryPlot.getDomainAxis();
        localCategoryAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 4));
        localCategoryAxis.setAxisLinePaint(myTextColor);
        localCategoryAxis.setTickLabelPaint(myTextColor);
        localCategoryAxis.setLabelPaint(myTextColor);
        localCategoryAxis.setTickMarkPaint(myTextColor);

        return localJFreeChart;
    }

    private void addGroundAbsChartToPanel() {
        JFreeChart localJFreeChart = createGroundAbsChart(groundAbsCategoryDataset);
        ChartPanel chartPanel = new ChartPanel(localJFreeChart);
        jPanelGroundAbsChart.add(chartPanel);
        chartPanel.setSize(jPanelGroundAbsChart.getWidth(), jPanelGroundAbsChart.getHeight());
        chartPanel.setVisible(true);

    }

    public void updateDatasetGroundAbs(double[] y_values) {
        int i_band;
        int row = 0;
        Comparable rowKey = this.groundAbsCategoryDataset.getRowKey(row);
        Comparable columnKey;
        //sprectraCategoryDataset = new DefaultCategoryDataset();
        for (i_band = 0; i_band < this.fCenter.length; i_band++) {
            int col = i_band;
            columnKey = this.groundAbsCategoryDataset.getColumnKey(col);
            this.groundAbsCategoryDataset.setValue((y_values[i_band]), rowKey, columnKey);
        }
    }

    public double[] getGroundAbs() {
        int i_band;
        int row = 0;

        double[] groundAbs = new double[this.N_BAND];

        Comparable rowKey = this.groundAbsCategoryDataset.getRowKey(row);
        Comparable columnKey;
        //sprectraCategoryDataset = new DefaultCategoryDataset();
        for (i_band = 0; i_band < this.fCenter.length; i_band++) {
            int col = i_band;
            columnKey = this.groundAbsCategoryDataset.getColumnKey(col);
            groundAbs[i_band] = groundAbsCategoryDataset.getValue(rowKey, columnKey).doubleValue();
        }
        return groundAbs;
    }

    private void calculateGroundAbsorption() {

        double groundResistance = Double.parseDouble(this.jTextFieldGroundResistivety.getText());
        double thiknessInCm = Double.parseDouble(this.jTextFieldGroundThickness.getText());

        double[] abs = new double[this.N_BAND];

        double waveNumberReal;
        double waveNumberImag;
        double absImpedanceReal;
        double absImpedanceImag;

        double rho0 = 1.21D;
        double c = 343D;
        double omega;

        for (int i_band = 0; i_band < this.N_BAND; i_band++) {
            double E = 1.21D * fCenter[i_band] / (1000D * groundResistance);
            omega = 2D * Math.PI * fCenter[i_band];

            waveNumberReal = 1d + 0.109D * Math.pow(E, -0.618D);
            waveNumberImag = 0.16D * Math.pow(E, -0.618D);
            Complex waveNumber = new Complex(waveNumberReal, waveNumberImag);//.multiply(omega/c);

            absImpedanceReal = 1D + 0.07D * Math.pow(E, -0.632D);
            absImpedanceImag = -0.107D * Math.pow(E, -0.632D);
            Complex absImpedance = new Complex(absImpedanceReal, absImpedanceImag);//.multiply(rho0*c);

            Complex tanhFactor = waveNumber.multiply(omega / c).multiply(thiknessInCm / 100D);

            tanhFactor = tanhFactor.tanh();
            Complex Zw = absImpedance.divide(tanhFactor);

            Complex resNom = Zw.subtract(1);
            Complex resDen = Zw.add(1);
            Complex res = resNom.divide(resDen);
            abs[i_band] = 1 - res.multiply(res).abs();
        }
        updateDatasetGroundAbs(abs);
    }

    private void saveRoadSynthDataFile(boolean SaveAs) {

        int buttons = JOptionPane.YES_NO_OPTION;
        int actionDialog = 0;
        boolean OkayToSaveFile = false;

        File outputFile = null;

        //Get length of each buffer
        int len0 = soundBufferArray[0].n_samples;
        int len1 = soundBufferArray[1].n_samples;
        int len2 = soundBufferArray[2].n_samples;
        int len3 = soundBufferArray[3].n_samples;
        int len4 = soundBufferArray[4].n_samples;
        int len5 = soundBufferArray[5].n_samples;

        //this is an instance of the class which all the data is stored
        RoadSynthFileData data = new RoadSynthFileData(len0, len1, len2, len3, len4, len5);

        //manual setting of som other data
        data.carsInBuffer0 = soundBufferArray[0].n_cars;
        data.carsInBuffer1 = soundBufferArray[1].n_cars;
        data.carsInBuffer2 = soundBufferArray[2].n_cars;
        data.carsInBuffer3 = soundBufferArray[3].n_cars;
        data.carsInBuffer4 = soundBufferArray[4].n_cars;
        data.carsInBuffer5 = soundBufferArray[5].n_cars;

        data.bufferPosition0 = soundBufferArray[0].position;
        data.bufferPosition1 = soundBufferArray[1].position;
        data.bufferPosition2 = soundBufferArray[2].position;
        data.bufferPosition3 = soundBufferArray[3].position;
        data.bufferPosition4 = soundBufferArray[4].position;
        data.bufferPosition5 = soundBufferArray[5].position;

        data.bufferOffset0 = soundBufferArray[0].offset;
        data.bufferOffset1 = soundBufferArray[1].offset;
        data.bufferOffset2 = soundBufferArray[2].offset;
        data.bufferOffset3 = soundBufferArray[3].offset;
        data.bufferOffset4 = soundBufferArray[4].offset;
        data.bufferOffset5 = soundBufferArray[5].offset;

        data.bufferLAFmax0 = soundBufferArray[0].LAFmax;
        data.bufferLAFmax1 = soundBufferArray[1].LAFmax;
        data.bufferLAFmax2 = soundBufferArray[2].LAFmax;
        data.bufferLAFmax3 = soundBufferArray[3].LAFmax;
        data.bufferLAFmax4 = soundBufferArray[4].LAFmax;
        data.bufferLAFmax5 = soundBufferArray[5].LAFmax;

        data.bufferLAeq0 = soundBufferArray[0].LAeq;
        data.bufferLAeq1 = soundBufferArray[1].LAeq;
        data.bufferLAeq2 = soundBufferArray[2].LAeq;
        data.bufferLAeq3 = soundBufferArray[3].LAeq;
        data.bufferLAeq4 = soundBufferArray[4].LAeq;
        data.bufferLAeq5 = soundBufferArray[5].LAeq;

        data.contentLength0 = len0;
        data.contentLength1 = len1;
        data.contentLength2 = len2;
        data.contentLength3 = len3;
        data.contentLength4 = len4;
        data.contentLength5 = len5;

        if (soundBufferArray[0].n_cars > 0) {
            for (int i = 0; i < soundBufferArray[0].n_samples; i++) {
                data.signalArray0_L[i] = soundBufferArray[0].signalArrayL[i];
                data.signalArray0_R[i] = soundBufferArray[0].signalArrayR[i];
            }
        }
        if (soundBufferArray[1].n_cars > 0) {
            for (int i = 0; i < soundBufferArray[1].n_samples; i++) {
                data.signalArray1_L[i] = soundBufferArray[1].signalArrayL[i];
                data.signalArray1_R[i] = soundBufferArray[1].signalArrayR[i];
            }
        }
        if (soundBufferArray[2].n_cars > 0) {
            for (int i = 0; i < soundBufferArray[2].n_samples; i++) {
                data.signalArray2_L[i] = soundBufferArray[2].signalArrayL[i];
                data.signalArray2_R[i] = soundBufferArray[2].signalArrayR[i];
            }
        }
        if (soundBufferArray[3].n_cars > 0) {
            for (int i = 0; i < soundBufferArray[3].n_samples; i++) {
                data.signalArray3_L[i] = soundBufferArray[3].signalArrayL[i];
                data.signalArray3_R[i] = soundBufferArray[3].signalArrayR[i];
            }
        }
        if (soundBufferArray[4].n_cars > 0) {
            for (int i = 0; i < soundBufferArray[4].n_samples; i++) {
                data.signalArray4_L[i] = soundBufferArray[4].signalArrayL[i];
                data.signalArray4_L[i] = soundBufferArray[4].signalArrayL[i];
            }
        }
        if (soundBufferArray[5].n_cars > 0) {
            for (int i = 0; i < soundBufferArray[5].n_samples; i++) {
                data.signalArray5_L[i] = soundBufferArray[5].signalArrayL[i];
                data.signalArray5_R[i] = soundBufferArray[5].signalArrayR[i];
            }
        }

        //If save as or if no file was loaded
        if (SaveAs || filePathToThisProject == null) {
            saveFileChooser.setSelectedFile(new File(saveFileChooser.getCurrentDirectory() + "\\My_Auralizaton_Project.rsd"));
            int returnVal = saveFileChooser.showSaveDialog(OpenRoadSynth.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                outputFile = saveFileChooser.getSelectedFile();
                OkayToSaveFile = true;
                if (outputFile.exists()) {
                    OkayToSaveFile = false;
                    actionDialog = JOptionPane.showConfirmDialog(null, "Replace existing file?", "File exists!", buttons);
                }

                if (actionDialog == JOptionPane.YES_OPTION) {
                    OkayToSaveFile = true;
                }
            }
        } else if (filePathToThisProject != null) {
            outputFile = filePathToThisProject;
            OkayToSaveFile = true;
        }

        if (OkayToSaveFile) {
            try {
                FileOutputStream fout = new FileOutputStream(outputFile);
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(data);
                oos.close();
                filePathToThisProject = outputFile;
                projectIsSaved = true;
                writeStatus(false);
                jLabelStatus.setText("File was saved: " + filePathToThisProject.toString());

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "I could not write the file...");
            }
        }  //*/
    }

    private void openRoadSynthDataFile() {

        int buttons = JOptionPane.YES_NO_OPTION;
        int actionDialog = 0;
        boolean OkayToReadFile = false;
        boolean FileReadSucces = false;

        if (!this.projectIsSaved) {
            actionDialog = JOptionPane.showConfirmDialog(null, "All unsaved data will be lost. Do you want to proceed?", "Unsaved data found", buttons);
            if (actionDialog == JOptionPane.YES_OPTION) {
                OkayToReadFile = true;
            }
        } else {
            OkayToReadFile = true;
        }

        if (OkayToReadFile) {
            int len = Globals.SAMPLING_RATE * Globals.SIGNAL_DUR;
            RoadSynthFileData inputdata = new RoadSynthFileData(len, len, len, len, len, len);

            //File to be loaded;
            File inputFile = null;

            //Fileselector
            int returnVal = openFileChooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    inputFile = openFileChooser.getSelectedFile();
                    FileInputStream fis = new FileInputStream(inputFile);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    inputdata = (RoadSynthFileData) ois.readObject();
                    fis.close();
                    FileReadSucces = true;
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(this, "I could not find this file. ");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "I could not read this file.");
                } catch (ClassNotFoundException ex) {
                    JOptionPane.showMessageDialog(this, "I could not find all the data in this file. \nPossible version missmatch");
                }

            }
            if (FileReadSucces) {

                soundBufferArray[0].n_samples = inputdata.contentLength0;
                soundBufferArray[0].n_cars = inputdata.carsInBuffer0;
                soundBufferArray[0].position = inputdata.bufferPosition0;
                soundBufferArray[0].offset = inputdata.bufferOffset0;
                soundBufferArray[0].LAFmax = inputdata.bufferLAFmax0;
                soundBufferArray[0].LAeq = inputdata.bufferLAeq0;
                for (int i = 0; i < soundBufferArray[0].n_samples; i++) {
                    soundBufferArray[0].signalArrayL[i] = inputdata.signalArray0_L[i];
                    soundBufferArray[0].signalArrayR[i] = inputdata.signalArray0_R[i];
                }

                soundBufferArray[1].n_samples = inputdata.contentLength1;
                soundBufferArray[1].n_cars = inputdata.carsInBuffer1;
                soundBufferArray[1].position = inputdata.bufferPosition1;
                soundBufferArray[1].offset = inputdata.bufferOffset1;
                soundBufferArray[1].LAFmax = inputdata.bufferLAFmax1;
                soundBufferArray[1].LAeq = inputdata.bufferLAeq1;
                for (int i = 0; i < soundBufferArray[1].n_samples; i++) {
                    soundBufferArray[1].signalArrayL[i] = inputdata.signalArray1_L[i];
                    soundBufferArray[1].signalArrayR[i] = inputdata.signalArray1_R[i];
                }

                soundBufferArray[2].n_samples = inputdata.contentLength2;
                soundBufferArray[2].n_cars = inputdata.carsInBuffer2;
                soundBufferArray[2].position = inputdata.bufferPosition2;
                soundBufferArray[2].offset = inputdata.bufferOffset2;
                soundBufferArray[2].LAFmax = inputdata.bufferLAFmax2;
                soundBufferArray[2].LAeq = inputdata.bufferLAeq2;
                for (int i = 0; i < soundBufferArray[2].n_samples; i++) {
                    soundBufferArray[2].signalArrayL[i] = inputdata.signalArray2_L[i];
                    soundBufferArray[2].signalArrayR[i] = inputdata.signalArray2_R[i];
                }

                soundBufferArray[3].n_samples = inputdata.contentLength3;
                soundBufferArray[3].n_cars = inputdata.carsInBuffer3;
                soundBufferArray[3].position = inputdata.bufferPosition3;
                soundBufferArray[3].offset = inputdata.bufferOffset3;
                soundBufferArray[3].LAFmax = inputdata.bufferLAFmax3;
                soundBufferArray[3].LAeq = inputdata.bufferLAeq3;
                for (int i = 0; i < soundBufferArray[3].n_samples; i++) {
                    soundBufferArray[3].signalArrayL[i] = inputdata.signalArray3_L[i];
                    soundBufferArray[3].signalArrayR[i] = inputdata.signalArray3_R[i];
                }

                soundBufferArray[4].n_samples = inputdata.contentLength4;
                soundBufferArray[4].n_cars = inputdata.carsInBuffer4;
                soundBufferArray[4].position = inputdata.bufferPosition4;
                soundBufferArray[4].offset = inputdata.bufferOffset4;
                soundBufferArray[4].LAFmax = inputdata.bufferLAFmax4;
                soundBufferArray[4].LAeq = inputdata.bufferLAeq4;
                for (int i = 0; i < soundBufferArray[4].n_samples; i++) {
                    soundBufferArray[4].signalArrayL[i] = inputdata.signalArray4_L[i];
                    soundBufferArray[4].signalArrayR[i] = inputdata.signalArray4_R[i];
                }

                soundBufferArray[5].n_samples = inputdata.contentLength5;
                soundBufferArray[5].n_cars = inputdata.carsInBuffer5;
                soundBufferArray[5].position = inputdata.bufferPosition5;
                soundBufferArray[5].offset = inputdata.bufferOffset5;
                soundBufferArray[5].LAFmax = inputdata.bufferLAFmax5;
                soundBufferArray[5].LAeq = inputdata.bufferLAeq5;
                for (int i = 0; i < soundBufferArray[5].n_samples; i++) {
                    soundBufferArray[5].signalArrayL[i] = inputdata.signalArray5_L[i];
                    soundBufferArray[5].signalArrayR[i] = inputdata.signalArray5_R[i];
                }

                //Done reading
                filePathToThisProject = inputFile;
                projectIsSaved = true;
                //now update the status.
                writeStatus(false);
                jLabelStatus.setText("File was loaded: " + filePathToThisProject.toString());
            }

        }
    }

    private void exitRoadSynth() {
        int buttons = JOptionPane.YES_NO_OPTION;
        int actionDialog = 0;
        boolean OkayToExit = false;

        if (projectIsSaved) {
            OkayToExit = true;
        } else {
            actionDialog = JOptionPane.showConfirmDialog(null, "Project not saved. Discard unsaved sounddata?", "Warning - unsaved data.", buttons);
            if (actionDialog == JOptionPane.YES_OPTION) {
                OkayToExit = true;
            }
        }
        if (OkayToExit) {
            System.exit(0);
        }
    }

    private void exportMixedBufferWavfile() {


        int buttons = JOptionPane.YES_NO_OPTION;
        int actionDialog = 0;
        boolean exportFile = false;
        File wavFile = null;

        String dynRange = this.jComboBoxDynamicRange.getSelectedItem().toString();

        exportDialogFrame.setVisible(true);
        if (exportDialogFrame.DoExport) {

            exportFileChooser.setSelectedFile(new File(exportFileChooser.getCurrentDirectory() + "\\Exported from RS at " + dynRange + "dB.wav"));

            int returnVal = exportFileChooser.showSaveDialog(OpenRoadSynth.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                //exportFileChooser.setFileFilter(JFileChooser.);
                wavFile = exportFileChooser.getSelectedFile();
                exportFile = true;
                if (wavFile.exists()) {
                    actionDialog = JOptionPane.showConfirmDialog(null, "Replace existing file?", "File exists!", buttons);
                }
                if (actionDialog != JOptionPane.YES_OPTION) {
                    exportFile = false;
                }
            }
        }
        //Create the buffer
        if (exportFile) {

            int numberOfsamplesToWrite = Globals.SAMPLING_RATE * exportDialogFrame.getTimeinSeconds();

            float[] signalLeft = new float[numberOfsamplesToWrite];
            float[] signalRight = new float[numberOfsamplesToWrite];

            int[] bufferPosition = new int[6];

            for (int i_buffer = 0; i_buffer < 6; i_buffer++) {
                bufferPosition[i_buffer] = (int) soundBufferArray[i_buffer].position;
            }

            int i_sample = 0;
            float tmp_buffer_R, tmp_buffer_L;

            while (i_sample < numberOfsamplesToWrite) {
                tmp_buffer_L = 0;
                tmp_buffer_R = 0;

                //Adds samples from each buffer
                for (int i_buffer = 0; i_buffer < 6; i_buffer++) {
                    if (soundBufferArray[i_buffer].n_cars > 0 && exportDialogFrame.exportIsSelectedforBuffer[i_buffer]) {
                        tmp_buffer_L += soundBufferArray[i_buffer].signalArrayL[bufferPosition[i_buffer]];
                        tmp_buffer_R += soundBufferArray[i_buffer].signalArrayR[bufferPosition[i_buffer]];
                        bufferPosition[i_buffer]++;
                        if (bufferPosition[i_buffer] > soundBufferArray[i_buffer].n_samples) {
                            bufferPosition[i_buffer] = 0;
                        }
                    }
                }

                signalLeft[i_sample] = tmp_buffer_L * Globals.DYN_RANGE_GAIN;
                signalRight[i_sample] = tmp_buffer_R * Globals.DYN_RANGE_GAIN;
                i_sample++;
            }

            if (exportDialogFrame.fadeInAndOut) {
                SignalToolbox.fadeInAndOut(signalLeft, 1.5);
                SignalToolbox.fadeInAndOut(signalRight, 1.5);
            }

            SignalToolbox.exportSignalToWav(signalLeft, signalRight, wavFile);
            this.jLabelStatus.setText("File was exported: " + wavFile.toString());
        }

    }

    private void initMyClassesAndGUI() {

        //Resets the GUI
        for (int i_buffer = 0; i_buffer < soundBufferArray.length; i_buffer++) {
            this.setCalcButtonToCalcMode(i_buffer);
        }

        //Start the PCN calculator
        new PCNGeneratorThread().start();

        //Puts zeros in all of the buffers
        resetBuffer(0);
        resetBuffer(1);
        resetBuffer(2);
        resetBuffer(3);
        resetBuffer(4);
        resetBuffer(5);

        jSliderBuffer0.setValue(0);
        jSliderBuffer1.setValue(0);
        jSliderBuffer2.setValue(0);
        jSliderBuffer3.setValue(0);
        jSliderBuffer4.setValue(0);
        jSliderBuffer5.setValue(0);

        //Init the CalibrationSignal Thread
        new CalibrationSignalMakerThread().start();

        //Init the soundplayer
        new SoundPlayer().start();

        //Init the Ambient Sound Thread
        AmbientSoundPlayer AmbSound = new AmbientSoundPlayer();
        AmbSound.start();

        //        
        jLabelOverload.setText("");

        //Start the listener for change in database
        new CpxFrameListener().start();

        //Set icon
        URL iconURL = getClass().getResource("icon_24x24.png");
        ImageIcon icon = new ImageIcon(iconURL);
        this.setIconImage(icon.getImage());

        //CENTER THE JFRAME
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);

        //Write the version info
        jLabelVersionInfo.setText("VERSION " + Globals.VERSION);

        //
        setSecondRoadLayerEnabled(false);

        //Calculates the trafic for the default inputs
        this.estimateTrafficFromInput();

    }

    public void setBufferText(int bufferIndex, String str) {
        switch (bufferIndex) {
            case 0: {
                jLabelBuffer0.setText(str);
                break;
            }
            case 1: {
                jLabelBuffer1.setText(str);
                break;
            }
            case 2: {
                jLabelBuffer2.setText(str);
                break;
            }
            case 3: {
                jLabelBuffer3.setText(str);
                break;
            }
            case 4: {
                jLabelBuffer4.setText(str);
                break;
            }
            case 5: {
                jLabelBuffer5.setText(str);
                break;
            }
        }
    }

    private void checkAndCorrectInput() {

        //Use the class NumericHelper to set and remove any non numerical chars
        jTextFieldStartPosSecondLayer.setText(NumericHelper.makeToDouble(jTextFieldStartPosSecondLayer.getText()));
        jTextFieldEndPosSecondLayer.setText(NumericHelper.makeToDouble(jTextFieldEndPosSecondLayer.getText()));

        double carStart = Double.parseDouble(jTextFieldStartPosSecondLayer.getText());
        double carEnd = Double.parseDouble(jTextFieldEndPosSecondLayer.getText());

        //Car passby must be at least 50 meters.
        if (carEnd < carStart) {
            carEnd = carStart + 50;
            jTextFieldEndPosSecondLayer.setText(String.valueOf(carEnd));
        }

        //Road dist
        jTextFieldRoadDist.setText(NumericHelper.makeToDouble(jTextFieldRoadDist.getText()));

        //Roaddist cannot be less than 5 meters
        double tmpVar = Double.parseDouble(jTextFieldRoadDist.getText());
        if (tmpVar < 5) {
            jTextFieldRoadDist.setText("5");
        }

        //Barrier 1
        jTextFieldNoiseBarrier1Start.setText(NumericHelper.makeToDouble(jTextFieldNoiseBarrier1Start.getText()));
        jTextFieldNoiseBarrier1End.setText(NumericHelper.makeToDouble(jTextFieldNoiseBarrier1End.getText()));
        double bar_start = Double.parseDouble(jTextFieldNoiseBarrier1Start.getText());
        double bar_end = Double.parseDouble(jTextFieldNoiseBarrier1End.getText());

        //"start" cannot be less then "end"
        if (bar_end < bar_start) {
            bar_end = bar_start + 10;
            jTextFieldNoiseBarrier1End.setText(String.valueOf(bar_end));
        }

        //Barrier 2
        jTextFieldNoiseBarrier2Start.setText(NumericHelper.makeToDouble(jTextFieldNoiseBarrier2Start.getText()));
        jTextFieldNoiseBarrier2End.setText(NumericHelper.makeToDouble(jTextFieldNoiseBarrier2End.getText()));
        bar_start = Double.parseDouble(jTextFieldNoiseBarrier2Start.getText());
        bar_end = Double.parseDouble(jTextFieldNoiseBarrier2End.getText());

        //"start" cannot be less then "end"
        if (bar_end < bar_start) {
            bar_end = bar_start + 10;
            jTextFieldNoiseBarrier2End.setText(String.valueOf(bar_end));
        }

        //Barrier 3
        jTextFieldNoiseBarrier3Start.setText(NumericHelper.makeToDouble(jTextFieldNoiseBarrier3Start.getText()));
        jTextFieldNoiseBarrier3End.setText(NumericHelper.makeToDouble(jTextFieldNoiseBarrier3End.getText()));
        bar_start = Double.parseDouble(jTextFieldNoiseBarrier3Start.getText());
        bar_end = Double.parseDouble(jTextFieldNoiseBarrier3End.getText());

        if (bar_end < bar_start) {
            bar_end = bar_start + 10;
            jTextFieldNoiseBarrier3End.setText(String.valueOf(bar_end));
        }
    }

    private void startSoundCalculation(int bufferIndex) {

        boolean leaveOneCore = this.jCheckBoxLeaveOneCore.isSelected();

        if (!this.canCalulate) {
            setCalcButtonToCalcMode(bufferIndex);
            return;
        }

        int coresAvaliable = this.PROCESSOR_CORES;

        if (coresAvaliable > 1 && leaveOneCore) {
            coresAvaliable--;
        }
        
        System.out.println(""+coresAvaliable);

        checkAndCorrectInput();

        if (!this.soundBufferArray[bufferIndex].isCalculating) {
            CalcSoundThread runner = new CalcSoundThread(bufferIndex);
            for (int core = 0; core < coresAvaliable; core++) {
                new Thread(runner).start();
            }
            this.soundBufferArray[bufferIndex].isCalculating = true;
            this.setCalcButtonToCancelMode(bufferIndex);

        } else {
            this.userHasCanceledCalculation[bufferIndex] = true;
        }
    }

    //Exports a buffer to a file
    private void exportBufferToWav(int index) {

        int buttons = JOptionPane.YES_NO_OPTION;
        int numberOfSamplesToWrite = 0;
        int actionDialog = 0;
        boolean exportFile;

        String dynRange = this.jComboBoxDynamicRange.getSelectedItem().toString();

        //File chooser
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Wave files", "wav");
        exportFileChooser.addChoosableFileFilter(filter);
        exportFileChooser.setFileFilter(filter);
        exportFileChooser.setSelectedFile(new File(exportFileChooser.getCurrentDirectory() + "\\Exported from RS at " + dynRange + "dB.wav"));

        int returnVal = exportFileChooser.showSaveDialog(OpenRoadSynth.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            //exportFileChooser.setFileFilter(JFileChooser.);
            File wavFile = exportFileChooser.getSelectedFile();
            exportFile = true;
            if (wavFile.exists()) {
                actionDialog = JOptionPane.showConfirmDialog(null, "Replace existing file?", "File exists!", buttons);
            }
            if (actionDialog != JOptionPane.YES_OPTION) {
                exportFile = false;
            }
            if (exportFile) {
                //Get the number of samples to write.

                numberOfSamplesToWrite = soundBufferArray[index].n_samples;

                float[] signalLeft = new float[numberOfSamplesToWrite];
                float[] signalRight = new float[numberOfSamplesToWrite];

                for (int n = 0; n < numberOfSamplesToWrite; n++) { //<---------
                    signalLeft[n] = soundBufferArray[index].signalArrayL[n] * (float) Globals.DYN_RANGE_GAIN;
                    signalRight[n] = soundBufferArray[index].signalArrayR[n] * (float) Globals.DYN_RANGE_GAIN;
                }

                SignalToolbox.exportSignalToWav(signalLeft, signalRight, wavFile);

            }
        }
    }

    /**
     * Writes the little status text at the bottom of the main frame. Set
     * WriteTrafficStats = true and traffic stats will be written.
     *
     * License check is also called from here.
     *
     * @param WriteTrafficStats
     */
    private void writeStatus(boolean WriteTrafficStats) {

 
            if (soundBufferArray[0].n_cars > 0) {
                jLabelBuffer0.setText("Cars in Buffer: " + soundBufferArray[0].n_cars);
                jLabelBuffer0Spl.setText(soundBufferArray[0].LAeq + " / " + soundBufferArray[0].LAFmax);
            } else {
                jLabelBuffer0.setText("No data in Buffer");
                jLabelBuffer0Spl.setText("- / -");
            }

            if (soundBufferArray[1].n_cars > 0) {
                jLabelBuffer1.setText("Cars in Buffer: " + Integer.toString(soundBufferArray[1].n_cars));
                jLabelBuffer1Spl.setText(soundBufferArray[1].LAeq + " / " + soundBufferArray[1].LAFmax);
            } else {
                jLabelBuffer1.setText("No data in Buffer");
                jLabelBuffer1Spl.setText("- / -");
            }

            if (soundBufferArray[2].n_cars > 0) {
                jLabelBuffer2.setText("Cars in Buffer: " + Integer.toString(soundBufferArray[2].n_cars));
                jLabelBuffer2Spl.setText(soundBufferArray[2].LAeq + " / " + soundBufferArray[2].LAFmax);
            } else {
                jLabelBuffer2.setText("No data in Buffer");
                jLabelBuffer2Spl.setText("- / -");
            }

            if (soundBufferArray[3].n_cars > 0) {
                jLabelBuffer3.setText("Cars in Buffer: " + Integer.toString(soundBufferArray[3].n_cars));
                jLabelBuffer3Spl.setText(soundBufferArray[3].LAeq + " / " + soundBufferArray[3].LAFmax);
            } else {
                jLabelBuffer3.setText("No data in Buffer");
                jLabelBuffer3Spl.setText("- / -");
            }

            if (soundBufferArray[4].n_cars > 0) {
                jLabelBuffer4.setText("Cars in Buffer: " + Integer.toString(soundBufferArray[4].n_cars));
                jLabelBuffer4Spl.setText(soundBufferArray[4].LAeq + " / " + soundBufferArray[4].LAFmax);
            } else {
                jLabelBuffer4.setText("No data in Buffer");
                jLabelBuffer4Spl.setText("- / -");
            }

            if (soundBufferArray[5].n_cars > 0) {
                jLabelBuffer5.setText("Cars in Buffer: " + Integer.toString(soundBufferArray[5].n_cars));
                jLabelBuffer5Spl.setText(soundBufferArray[5].LAeq + " / " + soundBufferArray[5].LAFmax);
            } else {
                jLabelBuffer5.setText("No data in Buffer");
                jLabelBuffer5Spl.setText("- / -");
            }

            //If something changed then make it possible to save the file
            if (!projectIsSaved) {
                jButtonToolBarSaveProject.setEnabled(true);
            } else {
                jButtonToolBarSaveProject.setEnabled(false);
            }//*/

            String frameTitle = "OpenRoadSynth " + Globals.VERSION;

            if (filePathToThisProject == null) {
                this.setTitle(frameTitle.concat(" - New Project"));
            } else {
                this.setTitle(frameTitle.concat(" - ").concat(filePathToThisProject.getPath()));
            }

            jLabelOverload.setText("");

            if (WriteTrafficStats) {
                int trafficperhour = getVehiclesPerHour();
                int trafficper24h = 24 * trafficperhour;
                this.jLabelStatus.setText("Now listening to: " + trafficperhour + " veh/h ( " + trafficper24h + " veh/24h )");
            }

            double[] overallSPL = calcOverallSoundLevels();
            if (overallSPL[0] == 0 && overallSPL[1] == 0) {
                jLabelBufferOverallSPL.setText("- / -");
            } else {
                jLabelBufferOverallSPL.setText(overallSPL[0] + " / " + overallSPL[1]);
            }

            //Add signal time in the buffers to jSliderBuffers as Tooltip
            jSliderBuffer0.setToolTipText("Length of buffer: " + Math.round(10d * (double) (soundBufferArray[0].n_samples) / (double) (Globals.SAMPLING_RATE)) / 10d + " s");
            jSliderBuffer1.setToolTipText("Length of buffer: " + Math.round(10d * (double) (soundBufferArray[1].n_samples) / (double) (Globals.SAMPLING_RATE)) / 10d + " s");
            jSliderBuffer2.setToolTipText("Length of buffer: " + Math.round(10d * (double) (soundBufferArray[2].n_samples) / (double) (Globals.SAMPLING_RATE)) / 10d + " s");
            jSliderBuffer3.setToolTipText("Length of buffer: " + Math.round(10d * (double) (soundBufferArray[3].n_samples) / (double) (Globals.SAMPLING_RATE)) / 10d + " s");
            jSliderBuffer4.setToolTipText("Length of buffer: " + Math.round(10d * (double) (soundBufferArray[4].n_samples) / (double) (Globals.SAMPLING_RATE)) / 10d + " s");
            jSliderBuffer5.setToolTipText("Length of buffer: " + Math.round(10d * (double) (soundBufferArray[5].n_samples) / (double) (Globals.SAMPLING_RATE)) / 10d + " s");

     
    }

    public double[] calcOverallSoundLevels() {
        double overallLAeq = 0;
        double overallLAFmax = 0;

        int shortestPlayingBufferSamples = Integer.MAX_VALUE;

        for (SoundBuffer soundBufferArray1 : soundBufferArray) {
            if (soundBufferArray1.isPlaying && soundBufferArray1.n_cars > 0) {

                //Get LAeq integration time=min time of the buffers
                if (soundBufferArray1.n_samples < shortestPlayingBufferSamples) {
                    shortestPlayingBufferSamples = soundBufferArray1.n_samples;
                }
                //Calculate overall LAFmax
                if (soundBufferArray1.LAFmax > overallLAFmax) {
                    overallLAFmax = soundBufferArray1.LAFmax;
                }
            }
        }

        //Calculare the LAeq of all playing buffers
        for (SoundBuffer soundBufferArray1 : soundBufferArray) {
            double factorOfMinBufferTime;
            double bufferLAeqWithMaxTimeIntegration;
            if (soundBufferArray1.isPlaying && soundBufferArray1.n_cars > 0) {
                factorOfMinBufferTime = (double) soundBufferArray1.n_samples / (double) shortestPlayingBufferSamples;
                bufferLAeqWithMaxTimeIntegration = SignalToolbox.multiplyDecibels(1D / factorOfMinBufferTime, soundBufferArray1.LAeq);
                overallLAeq = SignalToolbox.addDecibels(overallLAeq, bufferLAeqWithMaxTimeIntegration);
            }
        }

        //Rounding
        overallLAeq = Math.round(10d * overallLAeq) / 10d;
        overallLAFmax = Math.round(10d * overallLAFmax) / 10d;

        return new double[]{overallLAeq, overallLAFmax};
    }

    public int getVehiclesPerHour() {
        double time = 0;
        int vehicles = 0;

        double vps = 0;

        for (int i_buffer = 0; i_buffer < soundBufferArray.length; i_buffer++) {
            if (this.soundBufferArray[i_buffer].isPlaying && this.soundBufferArray[i_buffer].n_cars > 0) {
                time = (double) this.soundBufferArray[i_buffer].n_samples / (double) Globals.SAMPLING_RATE;
                vehicles = this.soundBufferArray[i_buffer].n_cars;
                vps += vehicles / time;
            }
        }

        //System.out.printf("time %f -- vehicles: %d\n", time, vehicles);
        if (vehicles > 0) {
            return (int) (vps * 3600d);
        } else {
            return 0;
        }
    }

    //test if a string is nummeric, is ugly... iknow! :)
    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private void setCalcButtonToCancelMode(int bufferIndex) {
        String toolTipText = "Cancels ongoing calculation";
        switch (bufferIndex) {
            case 0: {
                this.jToggleButtonWriteToBuffer0.setSelected(true);
                this.jToggleButtonWriteToBuffer0.setToolTipText(toolTipText);
                break;
            }
            case 1: {
                this.jToggleButtonWriteToBuffer1.setSelected(true);
                this.jToggleButtonWriteToBuffer1.setToolTipText(toolTipText);
                break;
            }
            case 2: {
                this.jToggleButtonWriteToBuffer2.setSelected(true);
                this.jToggleButtonWriteToBuffer2.setToolTipText(toolTipText);
                break;
            }
            case 3: {
                this.jToggleButtonWriteToBuffer3.setSelected(true);
                this.jToggleButtonWriteToBuffer3.setToolTipText(toolTipText);
                break;
            }
            case 4: {
                this.jToggleButtonWriteToBuffer4.setSelected(true);
                this.jToggleButtonWriteToBuffer4.setToolTipText(toolTipText);
                break;
            }
            case 5: {
                this.jToggleButtonWriteToBuffer5.setSelected(true);
                this.jToggleButtonWriteToBuffer5.setToolTipText(toolTipText);
                break;
            }
        }
    }

    private void setCalcButtonToCalcMode(int bufferIndex) {
        String toolTipText = "Starts a passby calculation";
        switch (bufferIndex) {
            case 0: {
                this.jToggleButtonWriteToBuffer0.setSelected(false);
                this.jToggleButtonWriteToBuffer0.setToolTipText(toolTipText);
                break;
            }
            case 1: {
                this.jToggleButtonWriteToBuffer1.setSelected(false);
                this.jToggleButtonWriteToBuffer1.setToolTipText(toolTipText);
                break;
            }
            case 2: {
                this.jToggleButtonWriteToBuffer2.setSelected(false);
                this.jToggleButtonWriteToBuffer2.setToolTipText(toolTipText);
                break;
            }
            case 3: {
                this.jToggleButtonWriteToBuffer3.setSelected(false);
                this.jToggleButtonWriteToBuffer3.setToolTipText(toolTipText);
                break;
            }
            case 4: {
                this.jToggleButtonWriteToBuffer4.setSelected(false);
                this.jToggleButtonWriteToBuffer4.setToolTipText(toolTipText);
                break;
            }
            case 5: {
                this.jToggleButtonWriteToBuffer5.setSelected(false);
                this.jToggleButtonWriteToBuffer5.setToolTipText(toolTipText);
                break;
            }
        }
    }

    //Clears the buffers
    private void resetBuffer(int bufferIndex) {

        switch (bufferIndex) {
            case 0: {
                this.jToggleButtonPlayBuffer0.setSelected(false);
                break;
            }
            case 1: {
                this.jToggleButtonPlayBuffer1.setSelected(false);
                break;
            }
            case 2: {
                this.jToggleButtonPlayBuffer2.setSelected(false);
                break;
            }
            case 3: {
                this.jToggleButtonPlayBuffer3.setSelected(false);
                break;
            }
            case 4: {
                this.jToggleButtonPlayBuffer4.setSelected(false);
                break;
            }
            case 5: {
                this.jToggleButtonPlayBuffer5.setSelected(false);
                break;
            }
        }

        soundBufferArray[bufferIndex].isPlaying = false;
        soundBufferArray[bufferIndex].position = 0;
        soundBufferArray[bufferIndex].n_samples = 1 * Globals.SAMPLING_RATE;
        soundBufferArray[bufferIndex].offset = 0;

        for (int i_sample = 0; i_sample < soundBufferArray[bufferIndex].signalArrayL.length; i_sample++) {
            soundBufferArray[bufferIndex].signalArrayL[i_sample] = 0f;
            soundBufferArray[bufferIndex].signalArrayR[i_sample] = 0f;
        }
        soundBufferArray[bufferIndex].n_cars = 0;

        writeStatus(false);
    }

    private void initLookAndFeel() {
        //From http://hg.netbeans.org/main/contrib/file/25a8acfe62f4/nimbus.theme/src/org/netbeans/modules/nimbus/theme/Installer.java

        Globals.PANEL_COLOR = new Color(170, 170, 170);
        Globals.TEXT_COLOR = new Color(255, 255, 255);
        UIManager.put("control", Globals.PANEL_COLOR);
        UIManager.put("info", Globals.PANEL_COLOR);
        UIManager.put("nimbusBase", new Color(18, 30, 49));
        UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
        UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
        UIManager.put("nimbusFocus", new Color(115, 164, 209));
        UIManager.put("nimbusGreen", new Color(176, 179, 50));
        UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
        UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
        UIManager.put("nimbusOrange", new Color(191, 98, 4));
        UIManager.put("nimbusRed", new Color(169, 46, 34));
        UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
        UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
        UIManager.put("text", Globals.TEXT_COLOR);//*/
        System.setProperty("nb.useSwingHtmlRendering", "true");
    }

    /**
     * Inits the dialogs for the open, save and the export fileChooserDialogs
     */
    private void initDialogs() {;

        //Create wav file filter for the exportFileChooser
        FileNameExtensionFilter expFilter = new FileNameExtensionFilter("Wave files", "wav");

        //Set file filter for the exportFileChooser
        exportFileChooser.addChoosableFileFilter(expFilter);
        exportFileChooser.setFileFilter(expFilter);

        //Create rsd file filter for the open and save file fileChoosers
        FileNameExtensionFilter fileFilter = new FileNameExtensionFilter("RSD-files", "rsd");

        saveFileChooser.addChoosableFileFilter(fileFilter);
        saveFileChooser.setFileFilter(fileFilter);

        openFileChooser.addChoosableFileFilter(fileFilter);
        openFileChooser.setFileFilter(fileFilter);
        openFileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "\\Documents"));

    }

    private void setSecondRoadLayerEnabled(boolean isActive) {
        jComboBoxPavementSecondLayer.setEnabled(isActive);
        jLabelStartStopSecondLayer.setEnabled(isActive);
        jComboBoxSetPassByDistSecondLayer.setEnabled(isActive);
        jLabelSecondLayer.setEnabled(isActive);
        jLabel24.setEnabled(isActive);
        jLabel10.setEnabled(isActive);
        jTextFieldStartPosSecondLayer.setEnabled(isActive);
        jLabel17.setEnabled(isActive);
        jLabel12.setEnabled(isActive);
        jTextFieldEndPosSecondLayer.setEnabled(isActive);
        secondLayerPavementIsEnabled = isActive;
    }

    private void estimateTrafficFromInput() {
        double startPosLayer1 = Double.parseDouble(jTextFieldStartPos.getText());
        double startPos = 0d;

        if (this.jCheckBoxActivateSecondLayer.isEnabled()) {
            double startPosLayer2 = Double.parseDouble(jTextFieldStartPosSecondLayer.getText());
            startPos = FastMath.min(startPosLayer1, startPosLayer2);
        } else {
            startPos = startPosLayer1;
        }

        double endPosLayer1 = Double.parseDouble(jTextFieldEndPos.getText());
        double endPos = 0d;

        if (this.jCheckBoxActivateSecondLayer.isEnabled()) {
            double endPosLayer2 = Double.parseDouble(jTextFieldEndPosSecondLayer.getText());
            endPos = FastMath.max(endPosLayer1, endPosLayer2);
        } else {
            endPos = endPosLayer1;
        }

        double speed = Double.parseDouble(jComboBoxSpeed.getSelectedItem().toString()) / 3.6;
        double length = endPos - startPos;
        double time = length / speed;
        double vehPerHour = 60d * 60d / time;
        double vehPer24Hour = 24d * 60d * 60d / time;

        String outStr = "Current input: Passby length " + (int) length + " m - Passby every " + Math.round(10d * time) / 10d + " sec - " + "Vehicles/h: " + (int) vehPerHour + " Vehicles/24h: " + (int) vehPer24Hour;
        jLabelTrafficInformation1.setText(outStr);
        jLabelTrafficInformation2.setText(outStr);
        jLabelTrafficInformation3.setText(outStr);

    }

    private void initVariables() {
        for (int i = 0; i < userHasCanceledCalculation.length; i++) {
            userHasCanceledCalculation[i] = false;
        }
        for (int i_buffer = 0; i_buffer < soundBufferArray.length; i_buffer++) {
            soundBufferArray[i_buffer] = new SoundBuffer();
        }

        Globals.DYN_RANGE_LIMIT_LOW = 10;
        Globals.DYN_RANGE_GAIN = (float) Math.pow(10d, -Globals.DYN_RANGE_LIMIT_LOW / 20d);

        //test
    }

    class PCNPlayer extends Thread {

        @Override
        public void run() {
            int n_samples = 1 * Globals.SAMPLING_RATE;
            int n_band = fCenter.length;
            short[] samples = new short[n_samples];
            for (int i_band = 0; i_band < n_band; i_band++) {
                for (int i_sample = 0; i_sample < n_samples; i_sample++) {
                    samples[i_sample] = (short) noiseComponentsArray[i_band][i_sample];

                }
                jLabelStatus.setText("Now listening to band: "
                        + String.valueOf((int) fCenter[i_band])
                        + " Hz  ("
                        + String.valueOf(i_band + 1)
                        + " of "
                        + String.valueOf(n_band) + ")");
                try {
                    playNoise(samples);
                } catch (LineUnavailableException | InterruptedException ex) {
                    Logger.getLogger(OpenRoadSynth.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            jLabelStatus.setText("Done");

        }
    }

    /**
     * Plays samples. Use for listening to the PCN
     *
     * @param samples
     * @throws LineUnavailableException
     * @throws InterruptedException
     */
    private void playNoise(short[] samples) throws LineUnavailableException, InterruptedException {
        int i_sample = 0;
        int n_samples = samples.length;

        SourceDataLine line;

        //Open up audio output, using 44100hz sampling rate, 16 bit samples, mono, and big 
        // endian byte ordering
        AudioFormat format = new AudioFormat(Globals.SAMPLING_RATE, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line matching " + info + " is not supported.");
            throw new LineUnavailableException();
        }

        line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(format);

        line.start();

        // Make our buffer size match audio system's buffer
        ByteBuffer cBuf = ByteBuffer.allocate(line.getBufferSize());

        //CODE FOR CLICK REMOVAL
        //FADE IN AND FADE OUT SOUND TO AVOID "CLICKS"
        double FadeGAIN = 0;
        int FadeSAMP = 2000;
        for (int i1 = 0;
                i1 < FadeSAMP;
                i1++) {
            FadeGAIN = (double) i1 / (double) FadeSAMP;

            //Fade in
            samples[i1] = (short) (FadeGAIN * (double) samples[i1]);
            samples[i1] = (short) (FadeGAIN * (double) samples[i1]);

            //Fade out
            samples[n_samples - i1 - 1] = (short) (FadeGAIN * (double) samples[n_samples - i1 - 1]);
            samples[n_samples - i1 - 1] = (short) (FadeGAIN * (double) samples[n_samples - i1 - 1]);
        }
        //END OF FADING

        //On each pass main loop fills the available free space in the audio buffer
        //Main loop creates audio samples for sine wave, runs until we tell the thread to exit
        //Each sample is spaced 1/RoadSynthConstants.SAMPLING_RATE apart in time
        while (i_sample < n_samples) {

            cBuf.clear();                            // Discard samples from previous pass

            // Figure out how many samples we can add
            int ctSamplesThisPass = line.available() / 2;
            for (int i = 0; i < ctSamplesThisPass; i++) {
                if (i_sample < n_samples) {
                    cBuf.putShort(samples[i_sample]);
                    i_sample++;
                }

            }

            //Write sine samples to the line buffer.  If the audio buffer is full, this will 
            // block until there is room (we never write more samples than buffer will hold)
            line.write(cBuf.array(), 0, cBuf.position());

            //Wait until the buffer is at least half empty  before we add more
            while (line.getBufferSize() / 2 < line.available()) {
                Thread.sleep(10);
            }
        }

        //Done playing the whole waveform, now wait until the queued samples finish 
        //playing, then clean up and exit
        line.drain();

        line.close();
    }

//INNER CLASS FOR GENERATING PCN (Pre Calculated Noise)
    class PCNGeneratorThread extends Thread {

        @Override
        public void run() {
            try {
                generate();
            } catch (InterruptedException ex) {

            }
        }

        public void generate() throws InterruptedException {

            canCalulate = false;
            jLabelStatus.setText("Generating noise components...");

            //show the hidden progress bar
            jProgressBarHidden.setVisible(true);

            int n_band = fCenter.length;
            int n_samples = Globals.SAMPLING_RATE * Globals.PCN_DUR;
            long calc_samples = 0;
            long tot_samples = n_samples * n_band;

            float[] tmp_samples = new float[n_samples];

            double progres;

            Random rand = new Random();

            double MAX_SHORT = Math.pow(2d, 15d) - 1d;

            for (int i_band = 1; i_band < n_band; i_band++) {

                //RANDOMISE SOME NOISE
                for (int i_sample = 0; i_sample < n_samples; i_sample++) {
                    tmp_samples[i_sample] = (short) (2d * MAX_SHORT * rand.nextDouble() - MAX_SHORT);
                    calc_samples++;
                }

                //Apply bandpass fiter;
                tmp_samples = new FilterOneThirdOctaveBand().applyBandPassFilter(tmp_samples, i_band, true);

                System.arraycopy(tmp_samples, 0, noiseComponentsArray[i_band], 0, n_samples);

                progres = 100 * (double) calc_samples / (double) tot_samples;
                jProgressBarHidden.setString(String.valueOf((int) progres) + " %");
                jProgressBarHidden.setValue((int) (progres));
            }
            jProgressBarHidden.setString("");
            jProgressBarHidden.setVisible(false);

            jLabelStatus.setText("Ready to start. Cores found on your system: " + String.valueOf(PROCESSOR_CORES));
            canCalulate = true;
        }

    }
    //END OF INNER CLASS FOR GENERATING PCN (Pre Calculated Noise)

// INNER CLASS SO THAT THE GUI IS ACCECIBLE FROM THE CALCBUFFERCLASS
    class CalcSoundThread implements Runnable {
        /*
         * In this class one passby is calculated and thwn looped so meny times as the global buffers will allow.
         * Calc
         */

        private int bufferIndex = 0; //Tells the prog to which of the three buffers that gets the current sound
        private boolean reportTaskIstaken;

        long startTime;
        long endTime;

        /**
         * Length of signal in seconds
         */
        int signalLength;
        int iterChunk;
        int randomSampleOffset = 200;
        double maxLinearSPL = 0;
        double maxAWeightedSPL = 0;

        //time for one chuck of sound
        final int SAMPLES_PER_CHUNK = 10;
        final int SHORT_MAX = (int) (Math.pow(2, 16) / 2);

        //Number of Calculated Samples
        int numberOfCalculatedSamples = 0;

        /**
         * Samples to be analyzed are temporary stored here
         */
        float[] sampleArrayForAnalysor = new float[SAMPLES_PER_CHUNK];

        //Declaration of engine properties here so that we have same engine in all threads
        Engine engine1;

        float[] SIG_L = new float[Globals.SAMPLING_RATE * 120];
        float[] SIG_R = new float[Globals.SAMPLING_RATE * 120];

        //global math constants implm.
        private final double pi2 = 2 * Math.PI;

        //constructor
        CalcSoundThread(int BufferNr) {
            this.bufferIndex = BufferNr;
            this.reportTaskIstaken = false;
            this.iterChunk = 0;
            this.signalLength = 0;
            this.startTime = System.currentTimeMillis();

            //Declaration of engine properties here so that we have same engine in all threads
            int Eng_Sel = jComboBoxEngine.getSelectedIndex();
            engine1 = new Engine(Eng_Sel);

            //Will randomize a location in the PCN-file (Noise sound files) to avaid echo effect when having several sources
            Random randdbl = new Random();
            randomSampleOffset = (int) (10d * Globals.SAMPLING_RATE * randdbl.nextDouble() + 250);

        }

        @Override
        public void run() {
            calcSound();
        }

        void calcSound() {

            // Variabled conserning playing sound
            int n;
            int samplesInThisSignal;

            boolean useSplineInterpolation = jCheckBoxUseSpline.isSelected();

            //Read all the values from the GUI
            int carDelay = Integer.parseInt(jComboBoxSeparation.getItemAt(jComboBoxSeparation.getSelectedIndex()).toString());
            int carSpeedIndex = jComboBoxSpeed.getSelectedIndex();

            double listenerEarDistance = Double.parseDouble(jComboBoxEarDist.getItemAt(jComboBoxEarDist.getSelectedIndex()).toString());
            listenerEarDistance = listenerEarDistance / 100; //from cm -> m

            double startPosLayer1 = Double.parseDouble(jTextFieldStartPos.getText());
            double startPosLayer2 = Double.parseDouble(jTextFieldStartPosSecondLayer.getText());
            double carStart = 0;
            if (jCheckBoxActivateSecondLayer.isSelected()) {
                carStart = Math.min(startPosLayer1, startPosLayer2);
            } else {
                carStart = startPosLayer1;
            }

            double endPosLayer1 = Double.parseDouble(jTextFieldEndPos.getText());
            double endPosLayer2 = Double.parseDouble(jTextFieldEndPosSecondLayer.getText());
            double carEnd = 0;
            if (jCheckBoxActivateSecondLayer.isSelected()) {
                carEnd = Math.max(endPosLayer1, endPosLayer2);
            } else {
                carEnd = endPosLayer1;
            }

            boolean applyLowPassFilter = jCheckBoxLpOn.isSelected();
            boolean applyHighPassFilter = jCheckBoxHpOn.isSelected();

            double lowPassFilterCutOff = Double.parseDouble(jComboBoxLpFilterCutOff.getSelectedItem().toString());
            double highPassFilterCutOn = Double.parseDouble(jComboBoxHpFilterCutOff.getSelectedItem().toString());
            double listenerDistFromRoad = Double.parseDouble(jTextFieldRoadDist.getText());
            int signalPlaybackMode = jComboBoxPlaybackMode.getSelectedIndex();

            boolean carDirectionLeftToRight = false;
            if (jComboBoxDirection.getSelectedIndex() == 0) {
                carDirectionLeftToRight = true;
            }
            //Done reading all the values from the GUI

            // Number of sines per 1/3 octave band
            // 1/3-octave band variables
            //Centerfrekvenser för tersband
            double[] FREQ_CENTER = {25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500d, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000};

            //double[] FREQ_RMS=new double[FREQ_CENTER.length];
            //double[] FREQ_dB=new double[FREQ_CENTER.length];
            double[] layer1CpxSpectrumArray = new double[FREQ_CENTER.length];
            double[] layer2CpxSpectrumArray = new double[FREQ_CENTER.length];
            double[] currentCpxSpectrumArray = new double[FREQ_CENTER.length];

            //CPX MEASUREMENTS FOR ABS16
            //Recalculated to correspond to the soundpressure at 10 m. 25-20kHz
            double[][] AMP_ROAD_ABS16
                    = {{19, 22, 24, 27, 30, 33, 35, 40, 44, 48, 50, 53, 57, 60, 62, 64, 66, 65, 63, 58, 55, 51, 47, 44, 41, 38, 35, 31, 33},
                    {19, 22, 24, 27, 30, 33, 35, 41, 44, 48, 53, 53, 57, 60, 60, 63, 66, 65, 63, 58, 55, 51, 47, 44, 41, 38, 35, 31, 29},
                    {25, 28, 31, 33, 35, 37, 38, 42, 45, 50, 54, 55, 60, 64, 63, 67, 69, 69, 66, 62, 58, 55, 51, 47, 44, 41, 38, 34, 33},
                    {29, 33, 36, 38, 39, 40, 40, 43, 47, 52, 55, 57, 62, 66, 66, 70, 72, 71, 68, 64, 61, 57, 53, 49, 46, 43, 40, 36, 34},
                    {29, 33, 37, 39, 40, 41, 41, 44, 48, 53, 56, 58, 63, 67, 67, 71, 73, 73, 70, 66, 63, 59, 55, 51, 47, 44, 41, 37, 36},
                    {30, 34, 37, 39, 41, 42, 42, 45, 49, 54, 57, 59, 64, 68, 68, 72, 75, 74, 72, 68, 64, 60, 56, 52, 48, 45, 42, 39, 37},
                    {30, 34, 38, 40, 41, 42, 43, 46, 50, 55, 58, 60, 65, 69, 69, 74, 76, 76, 73, 69, 66, 61, 57, 53, 50, 46, 43, 40, 38},
                    {31, 35, 38, 41, 42, 43, 43, 46, 50, 55, 59, 61, 66, 70, 70, 75, 78, 77, 75, 71, 67, 63, 59, 54, 51, 47, 43, 41, 39},
                    {31, 36, 39, 41, 43, 44, 45, 47, 51, 56, 60, 62, 67, 72, 72, 76, 79, 79, 76, 72, 69, 64, 60, 55, 52, 48, 44, 43, 41}};

            //CPX MEASUREMENTS FOR ENKELDRÄN
            //Recalculated to correspond to the soundpressure at 10 m. 25-20kHz
            double[][] AMP_ROAD_ENKELDRAN
                    = {{17, 20, 22, 25, 27, 29, 32, 37, 39, 44, 48, 48, 52, 55, 54, 58, 60, 59, 57, 53, 49, 46, 42, 38, 35, 33, 30, 29, 27},
                    {18, 21, 23, 25, 28, 30, 33, 38, 40, 45, 49, 49, 53, 56, 55, 59, 61, 61, 58, 54, 51, 47, 43, 39, 36, 34, 31, 27, 25},
                    {23, 26, 29, 31, 33, 34, 35, 39, 42, 46, 50, 51, 55, 59, 58, 62, 65, 64, 61, 57, 54, 50, 46, 42, 39, 36, 33, 28, 26},
                    {28, 31, 34, 36, 37, 37, 37, 39, 43, 47, 50, 52, 57, 61, 61, 65, 67, 66, 64, 60, 56, 52, 48, 45, 41, 38, 35, 28, 26},
                    {28, 31, 34, 36, 37, 37, 37, 40, 43, 48, 51, 53, 58, 62, 62, 66, 68, 68, 65, 61, 58, 54, 50, 46, 42, 39, 36, 30, 28},
                    {28, 32, 35, 37, 38, 38, 38, 40, 44, 49, 52, 54, 59, 63, 63, 67, 70, 69, 67, 63, 59, 55, 51, 47, 43, 40, 37, 31, 29},
                    {29, 32, 35, 37, 38, 38, 38, 41, 44, 49, 52, 54, 59, 64, 64, 68, 71, 70, 68, 64, 60, 56, 52, 48, 44, 41, 37, 32, 30},
                    {29, 33, 36, 38, 38, 39, 39, 41, 44, 49, 53, 55, 60, 65, 64, 69, 72, 71, 69, 65, 61, 57, 53, 48, 45, 41, 37, 33, 31},
                    {30, 33, 36, 38, 39, 39, 39, 41, 45, 50, 53, 55, 61, 65, 65, 70, 73, 72, 70, 66, 62, 58, 53, 49, 45, 42, 38, 35, 33}};
            //Airdamping according to ISO-9613 70 rel humidity 20 degree celcius dB/km
            double[] AIR_DAMP = {0d, 0d, 0d, 0.057d, 0.089d, 0.014d, 0.22d, 0.34d, 0.52d, 0.78d, 1.13d, 1.6d, 2.16d, 2.8d, 3.5d, 4.19d, 4.98d, 5.92d, 7.18d, 9.02d, 11.8d, 16.1d, 22.9d, 33.6d, 50.4d, 76.6d, 118d, 159.4d, 200.8d, 242.2d, 400d, 400d, 400, 400, 400};

            //A-weighting
            double[] A_WEIGHT = {-44.7D, -39.4D, -34.6D, -30.2D, -26.2D, -22.5D, -19.1D, -16.1D, -13.4D, -10.9D, -8.6D, -6.6D, -4.8D, -3.2D, -1.9D, -0.8D, 0.0D, 0.6D, 1.0D, 1.2D, 1.3D, 1.2D, 1.0D, 0.5D, -0.1D, -1.1D, -2.5D, -4.3D, -6.6D, -9.3D};

            //double[] MIC_CORR = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 8, 9, 9};
            double[] BAR_GAIN = new double[FREQ_CENTER.length]; // gain barrier screening.
            double[] HRTF_GAIN_L = new double[FREQ_CENTER.length]; //gain HRTF left ear
            double[] HRTF_GAIN_R = new double[FREQ_CENTER.length]; //gain HRTF right ear
            double[] AIR_ABS_GAIN = new double[FREQ_CENTER.length]; //gain air absorbtion
            double[] TOT_GAIN_ENGINE_L = new double[FREQ_CENTER.length]; //all gains added left channel
            double[] TOT_GAIN_ENGINE_R = new double[FREQ_CENTER.length]; //all gains added right channel
            double[] TOT_GAIN_TYRE_L = new double[FREQ_CENTER.length]; //all gains added left channel
            double[] TOT_GAIN_TYRE_R = new double[FREQ_CENTER.length]; //all gains added right channel

            final double c = 343;     //sound of speed;

            //Tidsvariabel
            double tmp_k, tmp_m;
            int i1, i_chunk, i_band, i_tone; //iteration variables

            double currentTime = 0;
            int currentSample;
            double currentDist;
            double currentDistReflection;            //path length of reflected wave  
            double currentAngle;             //angle between the car,rec - car direction
            double currentTheta;             //angle between road normal direction and the car position
            double currentRms;
            double timeStep = 1 / (double) Globals.SAMPLING_RATE;  //timestep
            double currentPosition, currentHrtfDeltaR, currentHrtfDeltaL;
            double currentLeftSampleValue;
            double MaxLeftSampleValue = 0;
            double currentRightSampleValue;
            double MaxRightSampleValue = 0;
            double currentRmsDecibels;
            double currentRmsDecibelsA;
            double currentLeftEngineSignalValue;
            double currentRightEngineSignalValue;

            //Barrier variables
            Barrier barrier1 = new Barrier(Double.parseDouble(jTextFieldNoiseBarrier1Start.getText()), //Start
                    Double.parseDouble(jTextFieldNoiseBarrier1End.getText()), //End
                    Double.parseDouble(jComboBoxNoiseBarrier1RoadDist.getItemAt(jComboBoxNoiseBarrier1RoadDist.getSelectedIndex()).toString()), //Distance from road source
                    Double.parseDouble(jComboBoxNoiseBarrier1Height.getItemAt(jComboBoxNoiseBarrier1Height.getSelectedIndex()).toString()), //Height
                    listenerDistFromRoad);
            barrier1.isActive = jCheckBoxNoiseBarrier1Active.isSelected();

            Barrier barrier2 = new Barrier(Double.parseDouble(jTextFieldNoiseBarrier2Start.getText()), //Start
                    Double.parseDouble(jTextFieldNoiseBarrier2End.getText()), //End
                    Double.parseDouble(jComboBoxNoiseBarrier2RoadDist.getItemAt(jComboBoxNoiseBarrier2RoadDist.getSelectedIndex()).toString()), //Distance from road source
                    Double.parseDouble(jComboBoxNoiseBarrier2Height.getItemAt(jComboBoxNoiseBarrier2Height.getSelectedIndex()).toString()), //Height
                    listenerDistFromRoad);
            barrier2.isActive = jCheckBoxNoiseBarrier2Active.isSelected();

            Barrier barrier3 = new Barrier(Double.parseDouble(jTextFieldNoiseBarrier3Start.getText()), //Start
                    Double.parseDouble(jTextFieldNoiseBarrier3End.getText()), //End
                    Double.parseDouble(jComboBoxNoiseBarrier3RoadDist.getItemAt(jComboBoxNoiseBarrier3RoadDist.getSelectedIndex()).toString()), //Distance from road source
                    Double.parseDouble(jComboBoxNoiseBarrier3Height.getItemAt(jComboBoxNoiseBarrier3Height.getSelectedIndex()).toString()), //Height
                    listenerDistFromRoad);
            barrier3.isActive = jCheckBoxNoiseBarrier3Active.isSelected();

            double currentBarrierCalculatedGain;
            double currentVelocityInMps = 70 / 3.6;
            double currentGeometricalAttenuation;
            double tmp_Amp_L = 0, tmp_Amp_R = 0; //Amplitude storage for the engine tones
            double tmp_fgain = 0;

            //Variables for spline interpolation
            double x0 = 0, x1 = 0, x2 = 0, x3 = 0, y0 = 0, y1 = 0, y2 = 0, y3 = 0, yp1 = 0, yp2 = 0;
            double spl_a, spl_b, spl_c, spl_d;
            double tmp_interp_y;
            double y_tmp;
            int n_guess;
            boolean look_for_samples;

            int i_case, i_sampl_interp, tmp_upspeeder1, tmp_upspeeder2, n_case = 0, i_sample;
            double tmp_t;

            int n_filter = FREQ_CENTER.length;

            double[] x_interp, y_interp, sig_time, tmp_sig;
            y_interp = new double[4];
            x_interp = new double[4];
            sig_time = new double[4];
            tmp_sig = new double[4];

            //Variables conserning randomisation of the tyre road noise
            //double CarPitch = 1+0.2*(int_rand.nextDouble()-0.5);
            double CarLFO = 1;
            //System.out.printf("%f \t", CarPitch);

            //Variables concerning screening. See ISO-9613
            //double maxBarrierGainChange = 0.001; //Maximum change per sample of the barrier reduction/gain.
            //Variables concerning ground reflections See ISO-9613 part 2
            double[] currentGroundEffectGain = new double[FREQ_CENTER.length];
            double hight_s;
            double hight_r;
            double Agr = 0, Am = 1, As = 1, Ar = 1;
            double q = 0;
            double a_h_s;
            double b_h_s;
            double c_h_s;
            double d_h_s;
            double a_h_r;
            double b_h_r;
            double c_h_r;
            double d_h_r;
            double Gs, Gm, Gr;
            double[] groundAbs = getGroundAbs();

            //Variables concerning HRTF
            double omega;
            double beta;
            double alphaL;
            double alphaR;
            double tmpDBL1, tmpDBL2;
            double currentLeftHrtfGain;
            double currentRightHrtfGain;

            int currentProgress = 0;

            boolean continueCalculation = true;

            //Calc speed in m/s
            switch (carSpeedIndex) {
                case 0:
                    currentVelocityInMps = 30 / 3.6;
                    break;
                case 1:
                    currentVelocityInMps = 40 / 3.6;
                    break;
                case 2:
                    currentVelocityInMps = 50 / 3.6;
                    break;
                case 3:
                    currentVelocityInMps = 60 / 3.6;
                    break;
                case 4:
                    currentVelocityInMps = 70 / 3.6;
                    break;
                case 5:
                    currentVelocityInMps = 80 / 3.6;
                    break;
                case 6:
                    currentVelocityInMps = 90 / 3.6;
                    break;
                case 7:
                    currentVelocityInMps = 100 / 3.6;
                    break;
                case 8:
                    currentVelocityInMps = 110 / 3.6;
                    break;
            }

            //Update GUI
            setBufferText(bufferIndex, "Initializing calculation...");

            //To be able to acces the clipboard
            //Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            //String ClipboardString="";
            //A-weighting the road spektrum and sets the spectra
            int layer1PavingSel = jComboBoxPavement.getSelectedIndex();
            int layer2PavingSel = jComboBoxPavementSecondLayer.getSelectedIndex();
            //System.out.printf("Val nummer: %d \n", intsel);

            //Set the CPX spekctrum for the first layer
            if (layer1PavingSel < 2) {
                switch (layer1PavingSel) {
                    case 0: //Vanlig ABS 16
                    {
                        for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                            layer1CpxSpectrumArray[i_band] = (double) (AMP_ROAD_ABS16[carSpeedIndex][i_band]) - A_WEIGHT[i_band];
                        }
                        break;
                    }
                    case 1: //Enkeldrän Tyst asfalt
                    {
                        for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                            layer1CpxSpectrumArray[i_band] = (double) (AMP_ROAD_ENKELDRAN[carSpeedIndex][i_band]) - A_WEIGHT[i_band];
                        }
                        break;
                    }
                }
            } else {
                layer1CpxSpectrumArray = cpxFrame.getLinearSpectrum(layer1PavingSel - 2);

                //Get the speed from the data base and convert into m/s
                currentVelocityInMps = cpxFrame.getSpeedInKmph(layer1PavingSel - 2) / 3.6;
            }

            //Set the CPX spekctrum for the second layer
            if (layer2PavingSel < 2) {
                switch (layer2PavingSel) {
                    case 0: //Vanlig ABS 16
                    {
                        for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                            layer2CpxSpectrumArray[i_band] = (double) (AMP_ROAD_ABS16[carSpeedIndex][i_band]) - A_WEIGHT[i_band];
                        }
                        break;
                    }
                    case 1: //Enkeldrän Tyst asfalt
                    {
                        for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                            layer2CpxSpectrumArray[i_band] = (double) (AMP_ROAD_ENKELDRAN[carSpeedIndex][i_band]) - A_WEIGHT[i_band];
                        }
                        break;
                    }
                }
            } else {
                layer2CpxSpectrumArray = cpxFrame.getLinearSpectrum(layer2PavingSel - 2);
            }

            //Adding corrections of 79 dB to correspond to fast
            for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                layer1CpxSpectrumArray[i_band] -= 53D;
                layer2CpxSpectrumArray[i_band] -= 53D;
            }

            //Signallength in sec
            signalLength = (int) ((carEnd - carStart) / (currentVelocityInMps));

            //loop until car reaches the passby length
            while (continueCalculation) //one round for each chunk = 0.125 sek.
            {
                currentRms = 0;

                i_chunk = this.iterChunk++;

                //FUNDAMENTAL CALCULATIONS
                //update time
                currentTime = this.SAMPLES_PER_CHUNK * i_chunk * timeStep;

                //move the car in the desired direction
                currentPosition = carStart + currentVelocityInMps * currentTime;

                //calculates the diffrance between source receiver
                currentDist = Math.sqrt(Math.pow(listenerDistFromRoad, 2d) + Math.pow(currentPosition, 2d) + (1.8 - 0.2) * (1.8 - 0.2));//(2+0.2)=(list_Height+car_Height)

                //Calculates the path length of the ground reflected soundwave
                //calculates the angle of car velocity vector and the light between car and receiver
                currentAngle = Math.acos(currentPosition / -currentDist);

                //geometrical gain calcularted using ISO 9613
                currentGeometricalAttenuation = -20d * Math.log10(currentDist);
                currentGeometricalAttenuation = Math.pow(10d, currentGeometricalAttenuation / 20d);

                //calculates the angle of the road normal and the line car-listener
                currentTheta = Math.PI / 2 - currentAngle;

                //BINAURAL TIME DIFFERENCE from left to right based on eardist/head diameter. BinAuralPlayback ONLY!
                if (signalPlaybackMode == 0) {
                    if (currentPosition > 0) {
                        currentHrtfDeltaL = (listenerEarDistance + listenerEarDistance * currentTheta) / c;
                        currentHrtfDeltaR = (listenerEarDistance - listenerEarDistance * Math.sin(currentTheta)) / c;
                    } else {
                        currentHrtfDeltaL = (listenerEarDistance + listenerEarDistance * Math.sin(currentTheta)) / c;
                        currentHrtfDeltaR = (listenerEarDistance - listenerEarDistance * currentTheta) / c;
                    }
                } else {
                    currentHrtfDeltaL = 0;
                    currentHrtfDeltaR = 0;
                }
                    //END OF BINAURAL TIME DIFFERENCE

                //GROUND EFFECT ISO 9613 - To understand this se the standard. Same variable names as standard.
                hight_s = 0.5;
                hight_r = 2;

                a_h_s = 1.5d + 3d * FastMath.exp(-0.12 * FastMath.pow(hight_s - 5, 2d)) * (1d - FastMath.exp(-currentDist / 50d)) + 5.7 * FastMath.exp(-0.09 * FastMath.pow(hight_s, 2)) * (1 - FastMath.exp(-2.8 * FastMath.pow(10, -6) * FastMath.pow(currentDist, 2)));
                b_h_s = 1.5d + 8.6 * FastMath.exp(-0.09 * FastMath.pow(hight_s, 2d)) * (1d - FastMath.exp(-currentDist / 50d));
                c_h_s = 1.5d + 14d * FastMath.exp(-0.46 * FastMath.pow(hight_s, 2d)) * (1d - FastMath.exp(-currentDist / 50d));
                d_h_s = 1.5d + 5d * FastMath.exp(-0.9 * FastMath.pow(hight_s, 2d)) * (1d - FastMath.exp(-currentDist / 50d));

                a_h_r = 1.5d + 3d * FastMath.exp(-0.12 * FastMath.pow(hight_r - 5d, 2d)) * (1d - FastMath.exp(-currentDist / 50d)) + 5.7 * FastMath.exp(-0.09 * FastMath.pow(hight_r, 2)) * (1 - FastMath.exp(-2.8 * FastMath.pow(10, -6) * FastMath.pow(currentDist, 2)));
                b_h_r = 1.5d + 8.6 * FastMath.exp(-0.09 * FastMath.pow(hight_r, 2d)) * (1d - FastMath.exp(-currentDist / 50d));
                c_h_r = 1.5d + 14d * FastMath.exp(-0.46 * FastMath.pow(hight_r, 2d)) * (1d - FastMath.exp(-currentDist / 50d));
                d_h_r = 1.5d + 5d * FastMath.exp(-0.9 * FastMath.pow(hight_r, 2d)) * (1d - FastMath.exp(-currentDist / 50d));

                if (currentDist <= (30d * (hight_s + hight_r))) {
                    q = 0;
                } else {
                    q = 1d - (30d * (hight_s + hight_r) / currentDist);
                }

                //double[] FREQ_CENTER = {25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500d, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000};
                for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                    Gs = groundAbs[i_band];
                    Gm = groundAbs[i_band];
                    Gr = groundAbs[i_band];
                    switch (i_band) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5: { //63 Hz
                            As = -1.5d;
                            Am = -3d * q;
                            Ar = -1.5d;
                            break;
                        }

                        case 6:
                        case 7:
                        case 8: {//125 Hz
                            As = -1.5 + Gs * a_h_s;
                            Am = -3d * q * (1d - Gm);
                            Ar = -1.5 + Gr * a_h_r;
                            break;
                        }
                        case 9:
                        case 10:
                        case 11: {//250 Hz
                            As = -1.5 + Gs * b_h_s;
                            Am = -3d * q * (1d - Gm);
                            Ar = -1.5 + Gr * b_h_r;
                            break;
                        }
                        case 12:
                        case 13:
                        case 14: {//500 Hz
                            As = -1.5 + Gs * c_h_s;
                            Am = -3d * q * (1d - Gm);
                            Ar = -1.5 + Gr * c_h_r;
                            break;
                        }
                        case 15:
                        case 16:
                        case 17: {//1000 Hz
                            As = -1.5 + Gs * d_h_s;
                            Am = -3d * q * (1d - Gm);
                            Ar = -1.5 + Gr * d_h_r;
                            break;
                        }
                        case 18:
                        case 19:
                        case 20: {//2000 Hz
                            As = -1.5 * (1d - Gs);
                            Am = -3d * q * (1d - Gm);
                            Ar = -1.5 * (1d - Gr);
                            break;
                        }
                        case 21:
                        case 22:
                        case 23: {//4000 Hz
                            As = -1.5 * (1d - Gs);
                            Am = -3d * q * (1d - Gm);
                            Ar = -1.5 * (1d - Gr);
                            break;
                        }
                        case 24:
                        case 25:
                        case 26:
                        case 27:
                        case 28: {//8000 Hz
                            As = -1.5 * (1d - Gs);
                            Am = -3d * q * (1d - Gm);
                            Ar = -1.5 * (1d - Gr);
                            break;
                        }
                    }
                    Agr = As + Am + Ar;
                    currentGroundEffectGain[i_band] = FastMath.pow(10, (-Agr) / 20d);
                    //System.out.printf("A: %f \t %f \t %f\t %f\n",Agr,As , Am , Ar);
                }
                    //END OF GROUND REFLEX CALCULATION

                //Resets the signal_variables
                currentLeftSampleValue = 0;
                currentRightSampleValue = 0;

                //SCREENING BY NOISE BARRIER EXCL LATERAL DIFFRACTION See ISO9613 formulas.
                //For each band calculate the corresponding GAIN due to a barrier
                for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                    currentBarrierCalculatedGain
                            = Math.min(Math.min(
                                            barrier1.getReduction(FREQ_CENTER[i_band], currentPosition, listenerDistFromRoad, currentDist),
                                            barrier2.getReduction(FREQ_CENTER[i_band], currentPosition, listenerDistFromRoad, currentDist)),
                                    barrier3.getReduction(FREQ_CENTER[i_band], currentPosition, listenerDistFromRoad, currentDist));

                    BAR_GAIN[i_band] = currentBarrierCalculatedGain;
                    /*if (i_band == 19) {
                     System.out.printf("%f  %f  \n", currentPosition, currentBarrierCalculatedGain);
                     }*/
                }
                    //System.out.printf("\n");

                //END OF BARRIER CODE
                //START OF HRTF SCREENING CODE
                //For each band calculate the corresponding GAIN due to head screening
                if (signalPlaybackMode == 0) //IF BINAURAL THEN
                {
                    for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                        beta = 2 * c / (0.5 * listenerEarDistance);
                        omega = pi2 * FREQ_CENTER[i_band];
                        tmpDBL1 = 1 / (beta * beta + omega * omega);

                        alphaL = 1 + Math.sin(currentTheta);
                        alphaR = 1 - Math.sin(currentTheta);
                        HRTF_GAIN_L[i_band] = tmpDBL1 * FastMath.sqrt(FastMath.pow(beta * beta + alphaL * omega * omega, 2d) + FastMath.pow(alphaL * beta * omega - beta * omega, 2d));
                        HRTF_GAIN_R[i_band] = tmpDBL1 * FastMath.sqrt(FastMath.pow(beta * beta + alphaR * omega * omega, 2d) + FastMath.pow(alphaR * beta * omega - beta * omega, 2d));

                    }
                } else {  //IF MONO THEN
                    for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                        HRTF_GAIN_L[i_band] = 1;
                        HRTF_GAIN_R[i_band] = 1;
                    }
                }
                    //HRTF ENDS

                //START OF AIR ABSORPTION CODE (ISO 9613)
                for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                    AIR_ABS_GAIN[i_band] = FastMath.pow(10d, (-AIR_DAMP[i_band] * currentDist * 0.001) / 20d);
                }
                    //END OF AIR ABSORPTION CODE

                //DEPENDING ON THE POSITION OF THE CAR SELECT PAVEMENT CPX
                if (secondLayerPavementIsEnabled && currentPosition > startPosLayer2 && currentPosition < endPosLayer2) {

                    currentCpxSpectrumArray = layer2CpxSpectrumArray;
                } else {
                    currentCpxSpectrumArray = layer1CpxSpectrumArray;
                }

                //"ADD" ALL THE FREQENCY DEPENDANT GAINS TOGETHER
                for (i_band = 0; i_band < FREQ_CENTER.length; i_band++) {
                    //All the physicall stuff that is affecting the sound from the car exept the CPX-spectrum which is not affeting the sound from engine.
                    TOT_GAIN_ENGINE_L[i_band] = CarLFO * currentGeometricalAttenuation * BAR_GAIN[i_band] * HRTF_GAIN_L[i_band] * AIR_ABS_GAIN[i_band] * currentGroundEffectGain[i_band];
                    TOT_GAIN_ENGINE_R[i_band] = CarLFO * currentGeometricalAttenuation * BAR_GAIN[i_band] * HRTF_GAIN_R[i_band] * AIR_ABS_GAIN[i_band] * currentGroundEffectGain[i_band];

                    //Same as above just adding the CPX-spektrum which not affects the sound from the engine 
                    TOT_GAIN_TYRE_L[i_band] = TOT_GAIN_ENGINE_L[i_band] * FastMath.pow(10d, currentCpxSpectrumArray[i_band] / 20d);
                    TOT_GAIN_TYRE_R[i_band] = TOT_GAIN_ENGINE_R[i_band] * FastMath.pow(10d, currentCpxSpectrumArray[i_band] / 20d);

                }
                //END "ADDING"

                for (i_sample = 0; i_sample < SAMPLES_PER_CHUNK; i_sample++) //for each sample until the buffer is full
                {
                    currentSample = (this.SAMPLES_PER_CHUNK * i_chunk + i_sample);
                    currentTime = (double) currentSample * timeStep;
                    currentDist = Math.sqrt(Math.pow(listenerDistFromRoad, 2d) + Math.pow(currentPosition, 2d) + (1.8 - 0.2) * (1.8 - 0.2));
                    currentPosition = carStart + currentVelocityInMps * currentTime;
                    currentDistReflection = Math.sqrt(listenerDistFromRoad * listenerDistFromRoad + currentPosition * currentPosition + (1.8 + 0.2) * (1.8 + 0.2));//(2+0.2)=(list_Height+car_Height)

                    sig_time[0] = 3 + currentTime - currentDist / c + currentHrtfDeltaL;            //Left            
                    sig_time[1] = 3 + currentTime - currentDistReflection / c + currentHrtfDeltaL;   //Left + Reflection

                    n_case = 2;
                    // IF BINAURAL DO ALSO ADD
                    if (signalPlaybackMode == 0) {

                        sig_time[2] = 3 + currentTime - currentDist / c + currentHrtfDeltaR;            //Right
                        sig_time[3] = 3 + currentTime - currentDistReflection / c + currentHrtfDeltaR;   //Right + Reflection
                        n_case = 4;
                    }

                    //ADDING TONES CORRESPONDING TO ENGINE IGNITION
                    currentLeftEngineSignalValue = 0;
                    currentRightEngineSignalValue = 0;
                    if (this.engine1.isActive()) {
                        tmp_upspeeder1 = 0;
                        for (i_tone = tmp_upspeeder1; i_tone < this.engine1.gfex.length; i_tone++) {
                            //LEFT CHANNEL
                            for (i_band = 0; i_band < FREQ_CENTER.length - 1; i_band++) {
                                if ((this.engine1.fex[i_tone] > FREQ_CENTER[i_band]) && (this.engine1.fex[i_tone] < FREQ_CENTER[i_band + 1])) {
                                    tmp_Amp_L = TOT_GAIN_ENGINE_L[i_band] * this.engine1.gfex[i_tone];
                                    tmp_Amp_R = TOT_GAIN_ENGINE_R[i_band] * this.engine1.gfex[i_tone];
                                    break;
                                }
                            }
                            currentLeftEngineSignalValue += tmp_Amp_L * Math.sin(pi2 * this.engine1.getPitch() * this.engine1.fex[i_tone] * sig_time[0]);
                            currentLeftEngineSignalValue += tmp_Amp_L * Math.sin(pi2 * this.engine1.getPitch() * this.engine1.fex[i_tone] * sig_time[0] + 0.5 / this.engine1.fex[0]);
                            currentRightEngineSignalValue += tmp_Amp_R * Math.sin(pi2 * this.engine1.getPitch() * this.engine1.fex[i_tone] * sig_time[2]);
                            currentRightEngineSignalValue += tmp_Amp_R * Math.sin(pi2 * this.engine1.getPitch() * this.engine1.fex[i_tone] * sig_time[2] + 0.5 / this.engine1.fex[0]);

                            tmp_upspeeder1 = i_band;
                        }

                        //ADDING TONES CORRESPONDING TO ENGINE RPM
                        tmp_upspeeder2 = 0;
                        for (i_tone = tmp_upspeeder2; i_tone < this.engine1.gfen.length - 1; i_tone++) {
                            //LEFT
                            for (i_band = 0; i_band < FREQ_CENTER.length - 1; i_band++) {
                                if ((this.engine1.fen[i_tone] > FREQ_CENTER[i_band]) && (this.engine1.fen[i_tone] < FREQ_CENTER[i_band + 1])) {
                                    tmp_Amp_L = TOT_GAIN_ENGINE_L[i_band] * this.engine1.gfen[i_tone];
                                    tmp_Amp_R = TOT_GAIN_ENGINE_R[i_band] * this.engine1.gfen[i_tone];
                                    break;
                                }
                            }
                            currentLeftEngineSignalValue += tmp_Amp_L * Math.sin(pi2 * this.engine1.getPitch() * this.engine1.fen[i_tone] * sig_time[0]);
                            currentLeftEngineSignalValue += tmp_Amp_L * Math.sin(pi2 * this.engine1.getPitch() * this.engine1.fen[i_tone] * sig_time[0] + 0.5 / this.engine1.fen[0]);
                            currentRightEngineSignalValue += tmp_Amp_R * Math.sin(pi2 * this.engine1.getPitch() * this.engine1.fen[i_tone] * sig_time[2]);
                            currentRightEngineSignalValue += tmp_Amp_R * Math.sin(pi2 * this.engine1.getPitch() * this.engine1.fen[i_tone] * sig_time[2] + 0.5 / this.engine1.fen[0]);
                            tmp_upspeeder2 = i_band;
                        }
                    }//*/

                    //System.out.printf("currentLeftEngineSignalValue: %f \n",currentLeftEngineSignalValue);
                    //ADD ROAD/TYRE NOISE
                    //FOR EACH CHANNEL AND EACH REFLECTION
                    for (i_case = 0; i_case < n_case; i_case += 2) {

                        look_for_samples = true;
                        //calculetate where to look in the NOISE to find the samples where to
                        //interpolate
                        n_guess = (int) (((sig_time[i_case] - currentTime) / timeStep - 1) + 0.5) + currentSample - 2;

                        while (look_for_samples) {
                            if ((sig_time[i_case] >= n_guess * timeStep) && (sig_time[i_case] < (n_guess + 1) * timeStep)) {
                                for (i_sampl_interp = 0; i_sampl_interp < 4; i_sampl_interp++) {
                                    tmp_interp_y = 0;
                                    for (i_band = 0; i_band < n_filter; i_band++) {

                                        switch (i_case) {
                                            //if i_case = 0 or 1 then
                                            case 0:
                                            case 1: {
                                                //do this...
                                                tmp_fgain = TOT_GAIN_TYRE_L[i_band];
                                                break;
                                            }
                                            //if i_case = 0 or 1 then
                                            case 2:
                                            case 3: {
                                                //do this...
                                                tmp_fgain = TOT_GAIN_TYRE_R[i_band];
                                                break;
                                            }
                                        }
                                        tmp_interp_y = tmp_interp_y + tmp_fgain * (double) (noiseComponentsArray[i_band][n_guess + i_sampl_interp + randomSampleOffset]);
                                    }
                                    y_interp[i_sampl_interp] = tmp_interp_y;
                                    x_interp[i_sampl_interp] = (i_sampl_interp - 1) * timeStep;
                                }
                                y0 = y_interp[0];
                                y1 = y_interp[1];
                                y2 = y_interp[2];
                                y3 = y_interp[3];

                                x0 = x_interp[0];
                                x1 = x_interp[1];
                                x2 = x_interp[2];
                                x3 = x_interp[3];

                                yp1 = (y2 - y0) / (x2 - x0);
                                yp2 = (y3 - y1) / (x3 - x1);

                                look_for_samples = false;//%stop search
                            } else {
                                n_guess++;
                            }
                        }

                        tmp_t = timeStep + (sig_time[i_case] % timeStep);

                        if (useSplineInterpolation) {
                            //cubic spline interpolation
                            //y=spl_a*t^3+spl_b*t^2+spl_c*t+spl_d; third order polynome
                            //see matlab file

                            spl_a = -(2 * y1 - 2 * y2 - x1 * yp1 - x1 * yp2 + x2 * yp1 + x2 * yp2) / ((x1 - x2) * (x1 - x2) * (x1 - x2));
                            spl_b = (3 * x1 * y1 - 3 * x1 * y2 + 3 * x2 * y1 - 3 * x2 * y2 - x1 * x1 * yp1 - 2 * x1 * x1 * yp2 + 2 * x2 * x2 * yp1 + x2 * x2 * yp2 - x1 * x2 * yp1 + x1 * x2 * yp2) / ((x1 - x2) * (x1 - x2) * (x1 - x2));
                            spl_c = (x1 * x1 * x1 * yp2 - x2 * x2 * x2 * yp1 - x1 * x2 * x2 * yp1 + 2 * x1 * x1 * x2 * yp1 - 2 * x1 * x2 * x2 * yp2 + x1 * x1 * x2 * yp2 - 6 * x1 * x2 * y1 + 6 * x1 * x2 * y2) / ((x1 - x2) * (x1 - x2) * (x1 - x2));
                            spl_d = (x1 * x1 * x1 * y2 - x2 * x2 * x2 * y1 + 3 * x1 * x2 * x2 * y1 - 3 * x1 * x1 * x2 * y2 + x1 * x2 * x2 * x2 * yp1 - x1 * x1 * x1 * x2 * yp2 - x1 * x1 * x2 * x2 * yp1 + x1 * x1 * x2 * x2 * yp2) / ((x1 - x2) * (x1 * x1 - 2 * x1 * x2 + x2 * x2));

                            y_tmp = spl_a * tmp_t * tmp_t * tmp_t + spl_b * tmp_t * tmp_t + spl_c * tmp_t + spl_d;
                        } else {
                            //straight line interploation y=kx+m
                            tmp_k = (y2 - y1) / timeStep;
                            tmp_m = y1 - tmp_k * x1;
                            y_tmp = (tmp_k * tmp_t + tmp_m);
                        }
                        tmp_sig[i_case] = y_tmp;
                    }//*/

                    //System.out.printf("tmp_sig[0]: %f \n",tmp_sig[0]);
                    if (carDirectionLeftToRight) {
                        currentLeftSampleValue = tmp_sig[0] + 0.7 * tmp_sig[1] + currentLeftEngineSignalValue;
                        currentRightSampleValue = tmp_sig[2] + 0.7 * tmp_sig[3] + currentRightEngineSignalValue;
                    } else {
                        currentRightSampleValue = tmp_sig[0] + 0.7 * tmp_sig[1] + currentLeftEngineSignalValue;
                        currentLeftSampleValue = tmp_sig[2] + 0.7 * tmp_sig[3] + currentRightEngineSignalValue;
                    }

                    //Look and set max sample value
                    if (currentLeftSampleValue > MaxLeftSampleValue) {
                        MaxLeftSampleValue = currentLeftSampleValue;
                    }
                    if (currentRightSampleValue > MaxRightSampleValue) {
                        MaxRightSampleValue = currentRightSampleValue;
                    }

                    currentRms += 0.25 * (currentLeftSampleValue + currentRightSampleValue) * (currentLeftSampleValue + currentRightSampleValue); //SQUARE
                    this.SIG_L[currentSample] = (float) currentLeftSampleValue;
                    this.SIG_R[currentSample] = (float) currentRightSampleValue;
                    //System.out.printf("this.SIG_L[i_sample]: %f\n",this.SIG_L[i_sample]);

                    this.numberOfCalculatedSamples++;

                }//END OF LOOP GENERATING ONE SAMPLE

                currentRmsDecibels = 20d * Math.log10(Math.sqrt(currentRms / (double) SAMPLES_PER_CHUNK));
                currentRmsDecibelsA = currentRmsDecibels; // For road noise peaking at 1kHz it is almost the same

                //Stores the maximum values
                if (currentRmsDecibelsA > maxAWeightedSPL) {
                    maxAWeightedSPL = currentRmsDecibelsA;
                }
                if (currentRmsDecibels > maxLinearSPL) {
                    maxLinearSPL = currentRmsDecibels;
                }

                //Calculate calculation progress
                currentProgress = (int) (100 * currentTime / this.signalLength);

                //Do not show more than 100% progress to the user. This can happen withot the fallowing code
                if (currentProgress > 100) {
                    currentProgress = 100;
                }

                //Update status text
                if (!this.reportTaskIstaken) {
                    setBufferText(bufferIndex, "Calculating... " + currentProgress + " %");
                }

                //If the time of the calculated sound is reatched stop calculating
                if ((currentTime > this.signalLength) || (userHasCanceledCalculation[bufferIndex])) {
                    continueCalculation = false;
                }
                //System.out.printf("Block 5b: %d \n",System.nanoTime()-timer);
                //timer = System.nanoTime();

            }//END OF LOOP GENRATING SOUND////////////////////////////////////

            //Delivery of the calculated data, only done by master thread!
            if (!reportTaskIstaken) {
                this.reportTaskIstaken = true;
                if (userHasCanceledCalculation[bufferIndex]) {
                    setBufferText(bufferIndex, "Aborted...");
                    userHasCanceledCalculation[bufferIndex] = false;
                    soundBufferArray[bufferIndex].isCalculating = false;
                    setCalcButtonToCalcMode(bufferIndex);
                } else {
                    setBufferText(bufferIndex, "Analyzing signal...");

                    this.endTime = System.currentTimeMillis();
                    SystemInfoFrame.SAMPLES_PER_SECOND_TIME = numberOfCalculatedSamples / ((this.endTime - this.startTime) / 1000);

                    //Calc the maximul level according to the NPM 1996, soft ground.
                    double LAFmax5p10m_light = 69d + 30d * Math.log10(currentVelocityInMps * 3.6 / 50d) + 9d * Math.exp(-0.7 * currentVelocityInMps * 3.6 / 50d);
                    double L2max = -20d * Math.log10(Math.sqrt(Math.pow(listenerDistFromRoad, 2) + Math.pow(2d - 0.5, 2d)) / 10d);
                    double LAFmax_light = LAFmax5p10m_light + L2max;

                    double LAFmax_diff_sig_calc = LAFmax_light - maxAWeightedSPL;
                    //System.out.printf("LAFmax_light: %f\n", LAFmax_light);
                    //System.out.printf("maxAWeightedSPL: %f\n\n", maxAWeightedSPL);

                    samplesInThisSignal = this.numberOfCalculatedSamples - 1;

                    // LOW PASS FILTER
                    if (applyLowPassFilter) {
                        BiquadButterworthFilter lp;
                        lp = new BiquadButterworthFilter(Globals.SAMPLING_RATE, (int) lowPassFilterCutOff, "lp");
                        this.SIG_L = lp.apply(this.SIG_L);
                        this.SIG_R = lp.apply(this.SIG_R);
                    }

                    //HIGH PASS FILTER
                    if (applyHighPassFilter) {
                        BiquadButterworthFilter hp;
                        hp = new BiquadButterworthFilter(Globals.SAMPLING_RATE, (int) highPassFilterCutOn, "hp");
                        this.SIG_L = hp.apply(this.SIG_L);
                        this.SIG_R = hp.apply(this.SIG_R);
                    }

                    // END LOW PASS FILTER
                    //FADE IN AND FADE OUT SOUND TO AVOID "CLICKS"
                    this.SIG_L = SignalToolbox.fadeInAndOut(SIG_L, 1);
                    this.SIG_R = SignalToolbox.fadeInAndOut(SIG_R, 1);

                    //Sets the offsets between cars in sec.
                    if (soundBufferArray[bufferIndex].n_cars > 0) {
                        soundBufferArray[bufferIndex].offset += carDelay;
                    }

                    //ADDING SIGNAL TO PLAYBUFFER
                    n = 0;
                    int sample_number = 0; //Position which sample in the array to write to.
                    for (i1 = 0; i1 < samplesInThisSignal; i1++) {
                        //if(n>=samplesInThisSignal-1) n=0;                            //If n exeeds the samplesize in SIG_L then reset the counter n so that we will fill upp the buffer from start.
                        //if(i1-BUFFER[0]_L.length>SIG_L.length) break;         //Ends the loop if there no more space for another passby in buffer

                        sample_number = i1 + soundBufferArray[bufferIndex].offset * Globals.SAMPLING_RATE;
                        soundBufferArray[bufferIndex].signalArrayL[sample_number] += SIG_L[n];
                        soundBufferArray[bufferIndex].signalArrayR[sample_number] += SIG_R[n];
                        n++;

                    }
                //END ADDING SIGNAL TO PLAYBUFFER COMPLETE

                    //Update data in the soundBufferArray
                    soundBufferArray[bufferIndex].n_cars += 1;
                    soundBufferArray[bufferIndex].isCalculating = false;
                    soundBufferArray[bufferIndex].n_samples = sample_number;

                    //Resets the calc button
                    setCalcButtonToCalcMode(bufferIndex);

                    //Afilter for the LAFmax calculation
                    FilterFrequencyWeighting Afilter = new FilterFrequencyWeighting('A');

                    float[] tmpSignal = new float[soundBufferArray[bufferIndex].n_samples];

                    System.arraycopy(soundBufferArray[bufferIndex].signalArrayL, 0, tmpSignal, 0, tmpSignal.length);

                    float[] signalAweightedL = Afilter.apply(tmpSignal);

                    //TimeWeighting filter;
                    FilterTimeWeighting Tfilter = new FilterTimeWeighting();
                    soundBufferArray[bufferIndex].LAFmax = Math.round(10D * 20D * Math.log10(SignalToolbox.max(Tfilter.applyLmaxFilter(signalAweightedL)))) / 10D;

                    soundBufferArray[bufferIndex].LAeq = Math.round(10D * 20D * Math.log10(SignalToolbox.rms(signalAweightedL))) / 10D;

                    //END OF UPDATE DATA INsoundBufferArray
                    projectIsSaved = false;
                    writeStatus(false);
                }
            }
        }
    }//END OF CLASS

//INNER CLASS THAT PLAYS THE CALCULATED VALUES
//Based on http://www.wolinlabs.com/blog/java.sine.wave.html
    class SoundPlayer extends Thread {

        FilterTimeWeighting[] timeWeightngFilter = new FilterTimeWeighting[31];

        //Constructor
        SoundPlayer() {
            initTimeFilters();
        }

        @Override
        public void run() {
            try {
                play();
            } catch (InterruptedException | LineUnavailableException ex) {
                Logger.getLogger(se.grundfelt.openroadsynth.OpenRoadSynth.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        void initTimeFilters() {
            for (int i = 0; i < timeWeightngFilter.length; i++) {
                timeWeightngFilter[i] = new FilterTimeWeighting();
            }
        }

        /**
         * Plays the buffers
         */
        void play() throws InterruptedException, LineUnavailableException {
            int i_sample = 0;

            boolean is_active = false;

            float tmp_sample_L = 0;
            float tmp_sample_R = 0;

            Random rand = new Random();
            float dither = 0;
            float ditherMag;
            long startTime;
            long timeToPassSignalToSoundCard;

            //Create the buffer
            SourceDataLine audioLine = null;
            AudioFormat audioformat = new AudioFormat(Globals.SAMPLING_RATE, 16, 2, true, true);
            DataLine.Info audioInfo = new DataLine.Info(SourceDataLine.class, audioformat);
            int audioSamplesToWrite;

            //Error handeling
            if (!AudioSystem.isLineSupported(audioInfo)) {
                System.out.println("Line matching " + audioInfo + " is not supported.");
                throw new LineUnavailableException();
            }

            audioLine = (SourceDataLine) AudioSystem.getLine(audioInfo);
            audioLine.open(audioformat);
            audioLine.start();

            // Make our buffer size match audio system's buffer
            ByteBuffer audioBuffer = ByteBuffer.allocate(audioLine.getBufferSize());

            boolean MAKE_SOUND = true;

            while (MAKE_SOUND) {

                //Correspomds to FAST with a sampling rate of 44.1 kHz
                audioSamplesToWrite = 5513;

                //On each pass main loop fills the available free space in the audio buffer
                //Main loop creates audio samples for sine wave, runs until we tell the thread to exit
                //Each sample is spaced 1/RoadSynthConstants.SAMPLING_RATE apart in time
                //As long we have more samples to write
                while (!is_active) //If buffer is paused this loop runs
                {
                    Thread.sleep(50);
                    is_active = false;
                    for (int i_buffer = 0; i_buffer < soundBufferArray.length; i_buffer++) {
                        if (soundBufferArray[i_buffer].isPlaying) {
                            is_active = true;
                        }
                    }
                    if (calibratorSignal.isPlaying) {
                        is_active = true;
                    }
                    //System.out.printf("paused on buffer: %d \n",this.BufferIndex);
                }
                startTime = System.nanoTime();
                audioBuffer.clear();                            // Discard samples from previous pass

                // Figure out how many samples we can add in each writecycle to soundcard
                //audiomemory = audioLine.available() / SAMPLE_MEM_SIZE / NO_OF_CHANNELS;
                // Write samples to audio buffer
                for (i_sample = 0; i_sample < audioSamplesToWrite; i_sample++) {

                    tmp_sample_L = 0;
                    tmp_sample_R = 0;

                    is_active = false;

                    //Add signal from each soundbuffer ( if thay are active)
                    for (int i_buffer = 0; i_buffer < soundBufferArray.length; i_buffer++) {

                        if (soundBufferArray[i_buffer].isPlaying) {
                            //Check length of the signals/arrays doesn't end. If so restart them
                            if (soundBufferArray[i_buffer].position > soundBufferArray[i_buffer].n_samples - 1) {
                                soundBufferArray[i_buffer].position = 0;
                            }

                            //System.out.printf("N_SAMPLES: %d \n",BUFFER[i_buffer].n_samples);
                            tmp_sample_L += soundBufferArray[i_buffer].signalArrayL[soundBufferArray[i_buffer].position];
                            tmp_sample_R += soundBufferArray[i_buffer].signalArrayR[soundBufferArray[i_buffer].position];
                            soundBufferArray[i_buffer].position++;
                            is_active = true;
                        }
                    }

                    if (calibratorSignal.isPlaying) {
                        is_active = true;
                        tmp_sample_L += calibratorSignal.signalArray[calibratorSignal.selectedAmplitude][calibratorSignal.position];
                        tmp_sample_R += calibratorSignal.signalArray[calibratorSignal.selectedAmplitude][calibratorSignal.position];
                        calibratorSignal.position++;
                        if (calibratorSignal.position > calibratorSignal.n_samples - 1) {
                            calibratorSignal.isPlaying = false;
                            calibratorSignal.position = 0;
                        }
                    } else {
                        calibratorSignal.position = 0;
                        jCheckBoxCalibrationSignal.setSelected(false);
                    }

                    ditherMag = (float) jComboBoxDither.getSelectedIndex();
                    dither = ditherMag * rand.nextFloat() - ditherMag / 2;

                    //samplesForAnalyserArray[i_sample] = 0.5f * (tmp_sample_L + tmp_sample_R) + dither;
                    samplesForAnalyserArray[i_sample] = tmp_sample_L;

                    //Apply limiter
                    tmp_sample_L *= Globals.DYN_RANGE_GAIN;
                    tmp_sample_R *= Globals.DYN_RANGE_GAIN;

                    //Check if overload
                    double maxShort = (double) Short.MAX_VALUE;
                    if ((tmp_sample_L > maxShort) || (tmp_sample_R > maxShort)) {
                        jLabelOverload.setText("Overload - Change dynamic range");
                    }

                    audioBuffer.putShort((short) (tmp_sample_L + dither));
                    audioBuffer.putShort((short) (tmp_sample_R + dither));
                    //System.out.printf("(short) (tmp_sample_L + dither) : %d\n",(int) (tmp_sample_L + dither));
                    //Write to soundcardbuffer
                }

                //Write sine samples to the line buffer.
                audioLine.write(audioBuffer.array(), 0, audioBuffer.position());
                    //audioSamplesToWrite -= audioSamplesThisPass;     // Update total number of samples written

                //UPDATE GUI PROGRESSBARS
                jSliderBuffer0.setValue((int) (100 * soundBufferArray[0].position / soundBufferArray[0].n_samples));

                jSliderBuffer1.setValue((int) (100 * soundBufferArray[1].position / soundBufferArray[1].n_samples));

                jSliderBuffer2.setValue((int) (100 * soundBufferArray[2].position / soundBufferArray[2].n_samples));

                jSliderBuffer3.setValue((int) (100 * soundBufferArray[3].position / soundBufferArray[3].n_samples));

                jSliderBuffer4.setValue((int) (100 * soundBufferArray[4].position / soundBufferArray[4].n_samples));

                jSliderBuffer5.setValue((int) (100 * soundBufferArray[5].position / soundBufferArray[5].n_samples));

                if (is_active) {
                    //new Analyser().start();
                    //ongoingAnalyserThreads++;
                    analyser();
                }

                timeToPassSignalToSoundCard = System.nanoTime() - startTime;

                SystemInfoFrame.SOUNDCARD_TIME = timeToPassSignalToSoundCard;

                //Wait until the buffer is at least half empty  before we add more. If the audio buffer is full, this will
                // block until there is room (we never write more samples than buffer will hold)
                while (audioLine.available() < audioLine.getBufferSize() * 0.75d) {
                    Thread.sleep(5);
                    //n++;

                }
                //System.out.printf("ongoingAnalyserThreads: %d \n", ongoingAnalyserThreads);
                //System.out.printf("\n");
                //System.out.printf("audioSamplesToWrite: %d -- this.POS: %d -- audioSamplesThisPass: %d -- audioLine.available(): %d -- audioLine.getBufferSize(): %d -- audioBuffer.position(): %d\n",audioSamplesToWrite,this.POS,audioSamplesThisPass,audioLine.available(),audioLine.getBufferSize(),audioBuffer.position());
            }//END OF PLAYBUFFER LOOP
            audioLine.drain();
            audioLine.close();
        }

        private void analyser() {
            long startTime = System.nanoTime();
            long timeToAnalyse;
            double band_max = 0d;
            double tot_max;
            double tmp_dBA;
            double maxSample = 0;

            //A-weighting and time wieghting
            float[] tmpSignalAweighted = new FilterFrequencyWeighting('A').apply(samplesForAnalyserArray);

            //Bandpass analysing and time weighting for each band
            int i_band;
            for (i_band = 0; i_band < N_BAND; i_band++) {
                float[] tmpSignal = new FilterOneThirdOctaveBand().applyBandPassFilter(tmpSignalAweighted, i_band, false);
                band_max = SignalToolbox.max(timeWeightngFilter[i_band].applyLmaxFilter(tmpSignal));
                resultsFromAnalyserAweighted[i_band] = (float) (20d * Math.log10(band_max));
            }

            tot_max = SignalToolbox.max(timeWeightngFilter[i_band + 1].applyLmaxFilter(tmpSignalAweighted));
            tmp_dBA = resultsFromAnalyserAweighted[i_band + 1] = (float) (20d * Math.log10(tot_max));

            //get max sample
            maxSample = SignalToolbox.max(samplesForAnalyserArray);
            maxSample = (20d * Math.log10(maxSample));

            //Draw on the charts
            analyserFrame.updateDataset(resultsFromAnalyserAweighted);
            updateDatasetSPLmaxFast(tmp_dBA);
            updateDatasetSPLmaxSample(maxSample);

            //System.out.print("" + tmp_dBA + "\n");
            //moveResultsInArray();
            //calculateLAFmax();
            timeToAnalyse = System.nanoTime() - startTime;
            SystemInfoFrame.ANALYSE_TIME = timeToAnalyse;

        }

    }//END OF INNER CLASS PLAYING SOUND 

//INNER CLASS THAT GENERATES CALIBRATION SINE SIGNAL
    class CalibrationSignalMakerThread extends Thread {

        @Override
        public void run() {
            try {
                createSignals();
            } catch (InterruptedException ex) {
                Logger.getLogger(OpenRoadSynth.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        void createSignals() throws InterruptedException {
            //int MAX_SHORT = (int) Math.pow(2d, 15d);

            double tstep = 1d / (double) Globals.SAMPLING_RATE;
            double omega = 2d * Math.PI * 1000;
            double omegaT;

            double amp40dB = Math.pow(10d, 40d / 20d) * Math.sqrt(2);
            double amp50dB = Math.pow(10d, 50d / 20d) * Math.sqrt(2);
            double amp60dB = Math.pow(10d, 60d / 20d) * Math.sqrt(2);
            double amp70dB = Math.pow(10d, 70d / 20d) * Math.sqrt(2);
            double amp80dB = Math.pow(10d, 80d / 20d) * Math.sqrt(2);

            for (int i_sample = 0; i_sample < calibratorSignal.n_samples; i_sample++) {
                omegaT = omega * tstep * i_sample;
                calibratorSignal.signalArray[0][i_sample] = (float) (amp40dB * Math.sin(omegaT));
                calibratorSignal.signalArray[1][i_sample] = (float) (amp50dB * Math.sin(omegaT));
                calibratorSignal.signalArray[2][i_sample] = (float) (amp60dB * Math.sin(omegaT));
                calibratorSignal.signalArray[3][i_sample] = (float) (amp70dB * Math.sin(omegaT));
                calibratorSignal.signalArray[4][i_sample] = (float) (amp80dB * Math.sin(omegaT));

            }

            float sample;
            float correction = 0.01f * (float) Math.pow(10d, -14.7d / 20d);

            //Wait until the noise components generation is done!
            //System.out.println("waiting");
            while (!canCalulate) {
                Thread.sleep(100);
            }
            //System.out.println("done_generating");

            for (int i_sample = 0; i_sample < calibratorSignal.n_samples; i_sample++) {
                sample = 0;
                for (int i_band = 10; i_band < 21; i_band++) {
                    sample += noiseComponentsArray[i_band][i_sample] * Math.pow(10, -i_band / 20f);
                }

                calibratorSignal.signalArray[5][i_sample] = (float) (amp40dB * sample * correction);
                calibratorSignal.signalArray[6][i_sample] = (float) (amp50dB * sample * correction);
                calibratorSignal.signalArray[7][i_sample] = (float) (amp60dB * sample * correction);
                calibratorSignal.signalArray[8][i_sample] = (float) (amp70dB * sample * correction);
                calibratorSignal.signalArray[9][i_sample] = (float) (amp80dB * sample * correction);

            }
            //System.out.println("Done");
        }
    }

    class CpxFrameListener extends Thread {

        @Override
        public void run() {
            while (true) {
                if (cpxFrame.getChange()) {
                    makePavementMenus();
                    cpxFrame.changeWasNoticed();
                    jLabelStatus.setText("CPX database was saved");
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(OpenRoadSynth.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

//INNER CLASS THAT PLAYS AMBIENT SOUNDS
    class AmbientSoundPlayer extends Thread {
        /*
         @Override
         public void run() {

         try {
         PlaySound();
         } catch (InterruptedException | LineUnavailableException | IOException | UnsupportedAudioFileException ex) {
         Logger.getLogger(RoadSynth.class.getName()).log(Level.SEVERE, null, ex);
         }

         }

         void PlaySound() throws InterruptedException, LineUnavailableException, IOException, UnsupportedAudioFileException {

         File audiofile = new File("C:\\Users\\gln\\Documents\\_PROG\\05 city skyline.wav");

         AudioInputStream audioStream = AudioSystem.getAudioInputStream(audiofile);

         AudioFormat audFormat = audioStream.getFormat();

         DataLine.Info audInfo = new DataLine.Info(SourceDataLine.class, audFormat);

         SourceDataLine audLine = (SourceDataLine) AudioSystem.getLine(audInfo);

         audLine.open(audFormat);

         boolean AmbientSoundIS_PLAYING = false;

         int audioNumberOfWrittenSamples = 0;
         int audioSamplesToPlay = 0;
         int audioposition = 0;
         int audioSamplesThisPass = 0;
         byte[] audioBuffer = new byte[audLine.getBufferSize()];

         long audioFileLength = audiofile.length();
         int frameSize = audFormat.getFrameSize();
         float frameRate = audFormat.getFrameRate();
         float durationInSeconds = (audioFileLength / (frameSize * frameRate));

         System.out.printf("durationInSeconds: %f s \n", durationInSeconds);

         audLine.start();

         while (true) {

         while (!jCheckBoxAmbientSound.isSelected()) {
         Thread.sleep(100);
         }

         audioSamplesThisPass = audLine.available();

         audioposition = audioStream.read(audioBuffer, 0, audioSamplesThisPass);

         //Write to the soundcard
         audLine.write(audioBuffer, 0, audioposition);

         audioNumberOfWrittenSamples += audioSamplesThisPass;

         //System.out.printf("audioposition: %d -- audioSamplesThisPass: %d -- audioNumberOfWrittenSamples: %d\n ",audioposition,audioSamplesThisPass,audioNumberOfWrittenSamples);
         while (audLine.getBufferSize() / 2 < audLine.available()) {
         //    if(!jCheckBoxAmbientSound.isSelected()) AmbientSoundIS_PLAYING=false;
         Thread.sleep(10);
         }

         if (audioNumberOfWrittenSamples > 60 * frameRate * frameSize) {
         audioStream = AudioSystem.getAudioInputStream(audiofile);
         audioNumberOfWrittenSamples = 0;
         }

         }

         }*/
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButtonClearBuffer0;
    private javax.swing.JButton jButtonClearBuffer1;
    private javax.swing.JButton jButtonClearBuffer2;
    private javax.swing.JButton jButtonClearBuffer3;
    private javax.swing.JButton jButtonClearBuffer4;
    private javax.swing.JButton jButtonClearBuffer5;
    private javax.swing.JButton jButtonDecreaseDistanceToRoad;
    private javax.swing.JButton jButtonDecreaseRange;
    private javax.swing.JButton jButtonExportBuffer0;
    private javax.swing.JButton jButtonExportBuffer1;
    private javax.swing.JButton jButtonExportBuffer2;
    private javax.swing.JButton jButtonExportBuffer3;
    private javax.swing.JButton jButtonExportBuffer4;
    private javax.swing.JButton jButtonExportBuffer5;
    private javax.swing.JButton jButtonGenerateCalibrationFile;
    private javax.swing.JButton jButtonIncreaseDistanceToRoad;
    private javax.swing.JButton jButtonIncreaseRange;
    private javax.swing.JButton jButtonPlayOnlyBuffer0;
    private javax.swing.JButton jButtonPlayOnlyBuffer1;
    private javax.swing.JButton jButtonPlayOnlyBuffer2;
    private javax.swing.JButton jButtonPlayOnlyBuffer3;
    private javax.swing.JButton jButtonPlayOnlyBuffer4;
    private javax.swing.JButton jButtonPlayOnlyBuffer5;
    private javax.swing.JButton jButtonStopAll;
    private javax.swing.JButton jButtonToolBarAddCPXData;
    private javax.swing.JButton jButtonToolBarChart;
    private javax.swing.JButton jButtonToolBarCheckHardWare;
    private javax.swing.JButton jButtonToolBarExit;
    private javax.swing.JButton jButtonToolBarExplainPicture;
    private javax.swing.JButton jButtonToolBarExport;
    private javax.swing.JButton jButtonToolBarNewFile;
    private javax.swing.JButton jButtonToolBarOpenProject;
    private javax.swing.JButton jButtonToolBarSaveProject;
    private javax.swing.JCheckBox jCheckBoxActivateSecondLayer;
    private javax.swing.JCheckBox jCheckBoxCalibrationSignal;
    private javax.swing.JCheckBox jCheckBoxHpOn;
    private javax.swing.JCheckBox jCheckBoxLeaveOneCore;
    private javax.swing.JCheckBox jCheckBoxLpOn;
    public javax.swing.JCheckBox jCheckBoxNoiseBarrier1Active;
    public javax.swing.JCheckBox jCheckBoxNoiseBarrier2Active;
    public javax.swing.JCheckBox jCheckBoxNoiseBarrier3Active;
    private javax.swing.JCheckBox jCheckBoxUseSpline;
    private javax.swing.JComboBox jComboBoxCalibrationSignalAmplitude;
    public javax.swing.JComboBox jComboBoxDirection;
    private javax.swing.JComboBox jComboBoxDither;
    private javax.swing.JComboBox jComboBoxDynamicRange;
    public javax.swing.JComboBox jComboBoxEarDist;
    public javax.swing.JComboBox jComboBoxEngine;
    public javax.swing.JComboBox jComboBoxGroundAbs;
    private javax.swing.JComboBox jComboBoxHpFilterCutOff;
    private javax.swing.JComboBox jComboBoxLpFilterCutOff;
    public javax.swing.JComboBox jComboBoxNoiseBarrier1Height;
    public javax.swing.JComboBox jComboBoxNoiseBarrier1RoadDist;
    public javax.swing.JComboBox jComboBoxNoiseBarrier2Height;
    public javax.swing.JComboBox jComboBoxNoiseBarrier2RoadDist;
    public javax.swing.JComboBox jComboBoxNoiseBarrier3Height;
    public javax.swing.JComboBox jComboBoxNoiseBarrier3RoadDist;
    public javax.swing.JComboBox jComboBoxPavement;
    public javax.swing.JComboBox jComboBoxPavementSecondLayer;
    public javax.swing.JComboBox jComboBoxPlaybackMode;
    public javax.swing.JComboBox jComboBoxSeparation;
    public javax.swing.JComboBox jComboBoxSetPassByDist;
    public javax.swing.JComboBox jComboBoxSetPassByDistSecondLayer;
    public javax.swing.JComboBox jComboBoxSpeed;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    public javax.swing.JLabel jLabelBuffer0;
    private javax.swing.JLabel jLabelBuffer0Spl;
    public javax.swing.JLabel jLabelBuffer1;
    private javax.swing.JLabel jLabelBuffer1Spl;
    public javax.swing.JLabel jLabelBuffer2;
    private javax.swing.JLabel jLabelBuffer2Spl;
    public javax.swing.JLabel jLabelBuffer3;
    private javax.swing.JLabel jLabelBuffer3Spl;
    public javax.swing.JLabel jLabelBuffer4;
    private javax.swing.JLabel jLabelBuffer4Spl;
    public javax.swing.JLabel jLabelBuffer5;
    private javax.swing.JLabel jLabelBuffer5Spl;
    private javax.swing.JLabel jLabelBuffer5Spl2;
    private javax.swing.JLabel jLabelBufferOverallSPL;
    private javax.swing.JLabel jLabelOverload;
    private javax.swing.JLabel jLabelSecondLayer;
    private javax.swing.JLabel jLabelStartStop2;
    private javax.swing.JLabel jLabelStartStopSecondLayer;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JLabel jLabelTrafficInformation1;
    private javax.swing.JLabel jLabelTrafficInformation2;
    private javax.swing.JLabel jLabelTrafficInformation3;
    private javax.swing.JLabel jLabelVersionInfo;
    private javax.swing.JMenu jMenuAbout;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuBufferControl;
    private javax.swing.JMenu jMenuDSP;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenuItem jMenuIcons;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItemEula;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemExport;
    private javax.swing.JMenuItem jMenuItemGeneratePCN;
    private javax.swing.JMenuItem jMenuItemManual;
    private javax.swing.JMenuItem jMenuItemNewFile;
    private javax.swing.JMenuItem jMenuItemOpenFile;
    private javax.swing.JMenuItem jMenuItemPlay01;
    private javax.swing.JMenuItem jMenuItemPlay012;
    private javax.swing.JMenuItem jMenuItemPlay23;
    private javax.swing.JMenuItem jMenuItemPlay345;
    private javax.swing.JMenuItem jMenuItemPlay45;
    private javax.swing.JMenuItem jMenuItemPlayAll;
    private javax.swing.JMenuItem jMenuItemSave;
    private javax.swing.JMenuItem jMenuItemSaveAs;
    private javax.swing.JMenuItem jMenuItemStopAll;
    private javax.swing.JMenuItem jMenuItemThirdPartyLicenses;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanelBarrier;
    private javax.swing.JPanel jPanelBuffers;
    private javax.swing.JPanel jPanelCalibration;
    private javax.swing.JPanel jPanelDialChart;
    private javax.swing.JPanel jPanelGroundAbs;
    private javax.swing.JPanel jPanelGroundAbsChart;
    private javax.swing.JPanel jPanelInfoBar;
    private javax.swing.JPanel jPanelListener;
    private javax.swing.JPanel jPanelToolBar;
    private javax.swing.JPanel jPanelVehicle;
    public javax.swing.JPanel jPanelVehicleRoad;
    private javax.swing.JProgressBar jProgressBarHidden;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JSlider jSliderBuffer0;
    private javax.swing.JSlider jSliderBuffer1;
    private javax.swing.JSlider jSliderBuffer2;
    private javax.swing.JSlider jSliderBuffer3;
    private javax.swing.JSlider jSliderBuffer4;
    private javax.swing.JSlider jSliderBuffer5;
    private javax.swing.JTabbedPane jTabbedPanel;
    public javax.swing.JTextField jTextFieldEndPos;
    public javax.swing.JTextField jTextFieldEndPosSecondLayer;
    private javax.swing.JTextField jTextFieldGroundResistivety;
    private javax.swing.JTextField jTextFieldGroundThickness;
    public javax.swing.JTextField jTextFieldNoiseBarrier1End;
    public javax.swing.JTextField jTextFieldNoiseBarrier1Start;
    public javax.swing.JTextField jTextFieldNoiseBarrier2End;
    public javax.swing.JTextField jTextFieldNoiseBarrier2Start;
    public javax.swing.JTextField jTextFieldNoiseBarrier3End;
    public javax.swing.JTextField jTextFieldNoiseBarrier3Start;
    public javax.swing.JTextField jTextFieldRoadDist;
    public javax.swing.JTextField jTextFieldStartPos;
    public javax.swing.JTextField jTextFieldStartPosSecondLayer;
    private javax.swing.JToggleButton jToggleButtonPlayBuffer0;
    private javax.swing.JToggleButton jToggleButtonPlayBuffer1;
    private javax.swing.JToggleButton jToggleButtonPlayBuffer2;
    private javax.swing.JToggleButton jToggleButtonPlayBuffer3;
    private javax.swing.JToggleButton jToggleButtonPlayBuffer4;
    private javax.swing.JToggleButton jToggleButtonPlayBuffer5;
    private javax.swing.JToggleButton jToggleButtonWriteToBuffer0;
    private javax.swing.JToggleButton jToggleButtonWriteToBuffer1;
    private javax.swing.JToggleButton jToggleButtonWriteToBuffer2;
    private javax.swing.JToggleButton jToggleButtonWriteToBuffer3;
    private javax.swing.JToggleButton jToggleButtonWriteToBuffer4;
    private javax.swing.JToggleButton jToggleButtonWriteToBuffer5;
    // End of variables declaration//GEN-END:variables
}
