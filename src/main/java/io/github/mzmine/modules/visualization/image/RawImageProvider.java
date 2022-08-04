/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.image;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingFrame;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MathUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * provides dataset for an imaging file with set ranges for m/z and mobility
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class RawImageProvider implements PlotXYZDataProvider {

  private static final Logger logger = Logger.getLogger(RawImageProvider.class.getName());
  protected final NumberFormat mzFormat;
  protected final NumberFormat mobilityFormat;
  protected final NumberFormat intensityFormat;
  protected final UnitFormat unitFormat;

  // input
  private final ImagingRawDataFile raw;
  private final ScanSelection scanSelection;
  private final Range<Double> mzRange;
  private final Range<Double> mobilityRange;
  private final boolean useMobility;
  private final boolean normalize;
  private final double width;
  private final double height;
  protected PaintScale paintScale;
  // output
  private IonTimeSeries<Scan> series;
  private double finishedPercentage;

  public RawImageProvider(ImagingRawDataFile raw, ParameterSet parameters) {
    this.raw = raw;
    ImagingParameters imagingParam = raw.getImagingParam();
    height = imagingParam.getLateralHeight() / imagingParam.getMaxNumberOfPixelY();
    width = imagingParam.getLateralWidth() / imagingParam.getMaxNumberOfPixelX();

    this.normalize = parameters.getValue(ImageVisualizerParameters.normalize);
    this.scanSelection = parameters.getValue(ImageVisualizerParameters.scanSelection);
    this.mzRange = parameters.getValue(ImageVisualizerParameters.mzRange);
    this.useMobility = parameters.getValue(ImageVisualizerParameters.mobilityRange);
    this.mobilityRange = parameters.getParameter(ImageVisualizerParameters.mobilityRange)
        .getEmbeddedParameter().getValue();

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    finishedPercentage = 0d;
  }

  @NotNull
  @Override
  public Color getAWTColor() {
    return raw.getColorAWT();
  }

  @NotNull
  @Override
  public javafx.scene.paint.Color getFXColor() {
    return raw.getColor();
  }

  @Nullable
  @Override
  public String getLabel(int index) {
    return null;
  }

  @Nullable
  @Override
  public PaintScale getPaintScale() {
    return paintScale;
  }

  @NotNull
  @Override
  public Comparable<?> getSeriesKey() {
    return raw.getName() + mzRange.toString();
  }

  @Nullable
  @Override
  public String getToolTipText(int index) {
    return intensityFormat.format(getZValue(index));
  }

  @Override
  public double getDomainValue(int index) {
    // already filtered for ImagingScan
    return ((ImagingScan) series.getSpectrum(index)).getCoordinates().getX() * width;
  }

  @Override
  public double getRangeValue(int index) {
    return ((ImagingScan) series.getSpectrum(index)).getCoordinates().getY() * height;
  }

  @Override
  public int getValueCount() {
    return series.getSpectra().size();
  }

  @Override
  public double getComputationFinishedPercentage() {
    return finishedPercentage;
  }

  @Override
  public double getZValue(int index) {
    return series.getIntensity(index);
  }

  @Nullable
  @Override
  public Double getBoxHeight() {
    return height;
  }

  @Nullable
  @Override
  public Double getBoxWidth() {
    return width;
  }

  @Override
  public void computeValues(SimpleObjectProperty<TaskStatus> status) {
    series = extractIonTimeSeries();

    if (normalize) {
      series = IonTimeSeriesUtils.normalizeToAvgTic(series, null);
    }

    double[] intensities = series.getIntensityValues(new double[series.getNumberOfValues()]);
    final double[] quantiles = MathUtils.calcQuantile(intensities, new double[]{0.50, 0.98});
    paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(PaintScaleTransform.LINEAR, Range.closed(quantiles[0], quantiles[1]));
  }


  @NotNull
  private SimpleIonTimeSeries extractIonTimeSeries() {
    if (useMobility && raw instanceof IMSRawDataFile imsRaw) {
      return extractFromMobilityScans(imsRaw);
    } else {
      return extractFromScans();
    }
  }

  @NotNull
  private SimpleIonTimeSeries extractFromScans() {
    double minMz = mzRange.lowerEndpoint();
    double maxMz = mzRange.upperEndpoint();

    logger.info("ImageViewer: Start data point extraction");
    ScanDataAccess scanAccess = EfficientDataAccess.of(raw, ScanDataType.RAW, scanSelection);
    int numberOfScans = scanAccess.getNumberOfScans();
    DoubleArrayList mzs = new DoubleArrayList(numberOfScans);
    DoubleArrayList intensities = new DoubleArrayList(numberOfScans);
    List<Scan> scans = new ArrayList<>(numberOfScans);
    int finished = 0;
    while (scanAccess.hasNextScan()) {
      finishedPercentage = finished / numberOfScans;
      Scan scan = scanAccess.nextScan();
      if (!(scan instanceof ImagingScan)) {
        continue;
      }

      double maxIntensity = 0;
      double bestMz = 0;
      double sum = 0;
      for (int i = 0; i < scanAccess.getNumberOfDataPoints(); i++) {
        double mz = scanAccess.getMzValue(i);
        if (mz > maxMz) {
          break;
        }
        if (mz < minMz) {
          continue;
        }

        // sum intensity
        double intensity = scanAccess.getIntensityValue(i);
        sum += intensity;
        // find best signal
        if (intensity > maxIntensity) {
          maxIntensity = intensity;
          bestMz = mz;
        }
      }

      mzs.add(bestMz);
      intensities.add(sum);
      scans.add(scan);
    }
    finished = 1;
    return new SimpleIonTimeSeries(null, mzs.toDoubleArray(), intensities.toDoubleArray(), scans);
  }

  @NotNull
  private SimpleIonTimeSeries extractFromMobilityScans(IMSRawDataFile imsRaw) {
    double minMz = mzRange.lowerEndpoint();
    double maxMz = mzRange.upperEndpoint();

    logger.info("ImageViewer: Start data point extraction");
    MobilityScanDataAccess scanAccess = EfficientDataAccess.of(imsRaw,
        MobilityScanDataType.CENTROID, scanSelection);
    int numberOfFrames = scanAccess.getNumberOfScans();
    DoubleArrayList mzs = new DoubleArrayList(numberOfFrames);
    DoubleArrayList intensities = new DoubleArrayList(numberOfFrames);
    List<Scan> scans = new ArrayList<>(numberOfFrames);
    double finished = 0;
    while (scanAccess.hasNextFrame()) {
      finishedPercentage = finished / numberOfFrames;
      Frame scan = scanAccess.nextFrame();
      if (!(scan instanceof ImagingFrame)) {
        continue;
      }

      double maxIntensity = 0;
      double bestMz = 0;
      double sum = 0;
      // loop through mobility scans
      while (scanAccess.nextMobilityScan() != null) {
        // only for user selected mobility range (might not be filtered in scan access)
        if (!mobilityRange.contains(scanAccess.getMobility())) {
          continue;
        }

        for (int i = 0; i < scanAccess.getNumberOfDataPoints(); i++) {
          double mz = scanAccess.getMzValue(i);
          if (mz > maxMz) {
            break;
          }
          if (mz < minMz) {
            continue;
          }

          // sum intensity
          double intensity = scanAccess.getIntensityValue(i);
          sum += intensity;
          // find best signal
          if (intensity > maxIntensity) {
            maxIntensity = intensity;
            bestMz = mz;
          }
        }
      }

      mzs.add(bestMz);
      intensities.add(sum);
      scans.add(scan);
    }
    finishedPercentage = 1;
    return new SimpleIonTimeSeries(null, mzs.toDoubleArray(), intensities.toDoubleArray(), scans);
  }


}