/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.text.Format;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a manually picked chromatographic feature.
 */
public class ManualFeature {

  private SimpleFeatureInformation featureInfo;

  private RawDataFile dataFile;

  // Raw M/Z, RT, Height and Area
  private double mz, height, area;
  private float rt;
  private Double fwhm = null, tf = null, af = null;

  // Boundaries of the feature
  private Range<Double> mzRange;
  private Range<Float> intensityRange, rtRange;

  // Map of scan number and data point
  private TreeMap<Scan, DataPoint> dataPointMap;

  // Number of most intense fragment scan
  private Scan fragmentScan, representativeScan;

  // Number of all MS2 fragment scans
  private Scan[] allMS2FragmentScanNumbers;

  // Isotope pattern. Null by default but can be set later by deisotoping
  // method.
  private IsotopePattern isotopePattern;
  private int charge = 0;

  /**
   * Initializes empty feature for adding data points
   */
  public ManualFeature(RawDataFile dataFile) {
    this.dataFile = dataFile;
    dataPointMap = new TreeMap<>();
  }

  /**
   * This feature is always a result of manual feature detection, therefore MANUAL
   */
  public @NotNull FeatureStatus getFeatureStatus() {
    return FeatureStatus.MANUAL;
  }

  /**
   * This method returns M/Z value of the feature
   */
  public double getMZ() {
    return mz;
  }

  /**
   * This method returns retention time of the feature
   */
  public float getRT() {
    return rt;
  }

  /**
   * This method returns the raw height of the feature
   */
  public double getHeight() {
    return height;
  }

  /**
   * This method returns the raw area of the feature
   */
  public double getArea() {
    return area;
  }

  /**
   * This method returns numbers of scans that contain this feature
   */
  public @NotNull Scan[] getScanNumbers() {
    return dataPointMap.keySet().toArray(Scan[]::new);
  }

  /**
   * This method returns a representative datapoint of this feature in a given scan
   */
  public DataPoint getDataPoint(Scan scanNumber) {
    return dataPointMap.get(scanNumber);
  }

  public @NotNull Range<Float> getRawDataPointsIntensityRange() {
    return intensityRange;
  }

  public @NotNull Range<Double> getRawDataPointsMZRange() {
    return mzRange;
  }

  public @NotNull Range<Float> getRawDataPointsRTRange() {
    return rtRange;
  }

  /**
   * @see Feature#getRawDataFile()
   */
  public @NotNull RawDataFile getRawDataFile() {
    return dataFile;
  }

  public String getName() {
    StringBuffer buf = new StringBuffer();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
    buf.append("m/z ");
    buf.append(mzFormat.format(getMZ()));
    buf.append(" (");
    buf.append(timeFormat.format(getRT()));
    buf.append(" min) [" + getRawDataFile().getName() + "]");
    return buf.toString();
  }

  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  public void setIsotopePattern(@NotNull IsotopePattern isotopePattern) {
    this.isotopePattern = isotopePattern;
  }

  /**
   * Adds a new data point to this feature
   *
   * @param scanNumber
   * @param dataPoint
   */
  public void addDatapoint(Scan scanNumber, DataPoint dataPoint) {
    float rt = scanNumber.getRetentionTime();

    if (dataPointMap.isEmpty()) {
      rtRange = Range.singleton(rt);
      mzRange = Range.singleton(dataPoint.getMZ());
      intensityRange = Range.singleton((float) dataPoint.getIntensity());
    } else {
      rtRange = rtRange.span(Range.singleton(rt));
      mzRange = mzRange.span(Range.singleton(dataPoint.getMZ()));
      intensityRange = intensityRange.span(Range.singleton((float) dataPoint.getIntensity()));
    }

    dataPointMap.put(scanNumber, dataPoint);
  }

  public void finalizeFeature() {
    // Trim the zero-intensity data points from the beginning and end
    while (!dataPointMap.isEmpty()) {
      Scan scanNumber = dataPointMap.firstKey();
      if (dataPointMap.get(scanNumber).getIntensity() > 0)
        break;
      dataPointMap.remove(scanNumber);
    }
    while (!dataPointMap.isEmpty()) {
      Scan scanNumber = dataPointMap.lastKey();
      if (dataPointMap.get(scanNumber).getIntensity() > 0)
        break;
      dataPointMap.remove(scanNumber);
    }

    // Check if we have any data points
    if (dataPointMap.isEmpty()) {
      throw (new IllegalStateException("Feature can not be finalized without any data points"));
    }

    // Find the data point with top intensity and use its RT and height
    dataPointMap.keySet().forEach(scan -> {
      DataPoint dataPoint = dataPointMap.get(scan);
      float rt = scan.getRetentionTime();
      if (dataPoint.getIntensity() > height) {
        height = dataPoint.getIntensity();
        representativeScan = scan;
        this.rt = rt;
      }
    });

    // Calculate feature area
    area = 0;
    Entry<Scan, DataPoint> lastEntry = null;
    for(Entry<Scan, DataPoint> entry : dataPointMap.entrySet()) {
      if(lastEntry == null) {
        lastEntry = entry;
      }
      else {
        // For area calculation, we use retention time in seconds
        double previousRT = lastEntry.getKey().getRetentionTime() * 60d;
        double currentRT = entry.getKey().getRetentionTime() * 60d;

        double rtDifference = currentRT - previousRT;

        // Intensity at the beginning and end of the interval
        double previousIntensity = lastEntry.getValue().getIntensity();
        double thisIntensity = entry.getValue().getIntensity();
        double averageIntensity = (previousIntensity + thisIntensity) / 2;

        // Calculate area of the interval
        area += (rtDifference * averageIntensity);
      }
    }

    // Calculate median MZ
    double[] mzArray = dataPointMap.values().stream().mapToDouble(dp -> dp.getMZ()).toArray();
    this.mz = MathUtils.calcQuantile(mzArray, 0.5f);

    fragmentScan = ScanUtils.findBestFragmentScan(dataFile, rtRange, mzRange);

    allMS2FragmentScanNumbers = ScanUtils.findAllMS2FragmentScans(dataFile, rtRange, mzRange);

    if (fragmentScan != null) {
      int precursorCharge = Objects.requireNonNullElse(fragmentScan.getPrecursorCharge(), 0);
      if ((precursorCharge > 0) && (this.charge == 0))
        this.charge = precursorCharge;
    }

  }

  public Scan getRepresentativeScanNumber() {
    return representativeScan;
  }

  public Scan getMostIntenseFragmentScanNumber() {
    return fragmentScan;
  }

  public Scan[] getAllMS2FragmentScanNumbers() {
    return allMS2FragmentScanNumbers;
  }

  public int getCharge() {
    return charge;
  }

  public void setCharge(int charge) {
    this.charge = charge;
  }

  public Double getFWHM() {
    return fwhm;
  }

  public void setFWHM(Double fwhm) {
    this.fwhm = fwhm;
  }

  public Double getTailingFactor() {
    return tf;
  }

  public void setTailingFactor(Double tf) {
    this.tf = tf;
  }

  public Double getAsymmetryFactor() {
    return af;
  }

  public void setAsymmetryFactor(Double af) {
    this.af = af;
  }

  // dulab Edit
  public void outputChromToFile() {
    int nothing = -1;
  }

  public void setFeatureInformation(SimpleFeatureInformation featureInfoIn) {
    this.featureInfo = featureInfoIn;
  }

  public SimpleFeatureInformation getFeatureInformation() {
    return featureInfo;
  }

  public void setFragmentScanNumber(Scan fragmentScanNumber) {
    this.fragmentScan = fragmentScanNumber;
  }

  public void setAllMS2FragmentScanNumbers(Scan[] allMS2FragmentScanNumbers) {
    this.allMS2FragmentScanNumbers = allMS2FragmentScanNumbers;
    // also set best scan by TIC
    Scan best = null;
    double tic = 0;
    if (allMS2FragmentScanNumbers != null) {
      for (Scan scan : allMS2FragmentScanNumbers) {
        if (tic < scan.getTIC()) {
          best = scan;
          tic = scan.getTIC();
        }
      }
    }
    setFragmentScanNumber(best);
  }
  // End dulab Edit

  private FeatureList featureList;

  public FeatureList getFeatureList() {
    return featureList;
  }

  public void setFeatureList(FeatureList featureList) {
    this.featureList = featureList;
  }


  public Collection<DataPoint> getDataPoints() {
    return dataPointMap.values();
  }
}
