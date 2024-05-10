/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.fraggraphdashboard;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.modules.tools.id_fraggraph.FragmentUtils;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

public class FragGraphPrecursorFormulaTask extends FxUpdateTask<FragDashboardModel> {

  private static final int DEFAULT_MAX_FORMULA_COUNT = 100;

  private final IonType ionTypeOverride;
  private final MZTolerance formulaTolerance;
  private final boolean checkCHONPS;
  private final boolean checkRDBE;

  private MolecularFormulaGenerator generator;
  private final int maxFormulaCount;
  private int formulaCount = 0;
  private final Integer charge;
  private final PolarityType polarity;
  private final double averageMZ;
  private final String desc;
  private final List<IonModification> assignedIonTypes;
  private final MassSpectrum ms2Spectrum;
  private final MassSpectrum measuredIsotopePattern;

  @NotNull
  private MZTolerance ms2Tolerance = new MZTolerance(0.005, 15);
  private final MolecularFormulaRange elements;

  public FragGraphPrecursorFormulaTask(@NotNull FragDashboardModel model, ParameterSet parameters) {
    this(model, null, //
        parameters.getValue(FragmentGraphCalcParameters.ms1Tolerance), //
        parameters.getEmbeddedParameterValue(FragmentGraphCalcParameters.heuristicParams)
            .getValue(ElementalHeuristicParameters.checkNOPS), //
        true, //
        parameters.getValue(FragmentGraphCalcParameters.maximumFormulae), //
        parameters.getValue(FragmentGraphCalcParameters.polarity), //
        parameters.getValue(FragmentGraphCalcParameters.adducts), //
        parameters.getValue(FragmentGraphCalcParameters.ms2Tolerance), //
        parameters.getValue(FragmentGraphCalcParameters.elements));
  }

  // todo make use of polarity and the assigned ion types if we have a row
  public FragGraphPrecursorFormulaTask(@NotNull FragDashboardModel model,
      @Nullable IonType ionTypeOverride, @NotNull MZTolerance formulaTolerance, boolean checkCHONPS,
      boolean checkRDBE, int maxFormulaCount, @NotNull PolarityType polarity,
      @NotNull List<IonModification> assignedIonTypes, @NotNull MZTolerance ms2Tolerance,
      @NotNull MolecularFormulaRange elements) {
    super("Calculate precursor formulae", model);
    this.ionTypeOverride = ionTypeOverride;
    this.formulaTolerance = formulaTolerance;
    this.checkCHONPS = checkCHONPS;
    this.checkRDBE = checkRDBE;
    this.maxFormulaCount = maxFormulaCount;
    this.ms2Tolerance = ms2Tolerance;
    try {
      this.elements = (MolecularFormulaRange) elements.clone();
      assignedIonTypes.forEach(
          ion -> FragmentUtils.reflectIonTypeInFormulaRange(new IonType(ion), this.elements));
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    this.assignedIonTypes = assignedIonTypes;
    this.charge = 1;
    this.polarity = polarity;
    this.averageMZ = model.getPrecursorMz();
    this.desc = getName();
    this.ms2Spectrum = model.getSpectrum();
    this.measuredIsotopePattern = model.getIsotopePattern();
  }

  @Override
  public @NotNull String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return generator == null ? 0 : generator.getFinishedPercentage();
  }

  @Override
  public void process() {
    final boolean couldBeRadical = assignedIonTypes.stream().anyMatch(IonModification::isElectron);

    generator = setUpFormulaGenerator();
    generateFormulae(couldBeRadical, generator);
    if (formulaCount == 0) {
      DesktopService.getDesktop().displayMessage(
          "No formulae found. Consider changing the tolerances or the element ranges.");
    }
  }

  @Override
  protected void updateGuiModel() {

  }


  /**
   * Sets up the molecular formula generator for the neutralised ion mass. This includes the mass of
   * the adduct so the fragments can but don't have to include the adduct in their formula.
   */
  public MolecularFormulaGenerator setUpFormulaGenerator() {
//    final MolecularFormulaRange elementCounts = FragmentUtils.setupFormulaRange(assignedIonTypes);
    final double neutralMassWithAdduct =
        averageMZ * charge + polarity.getSign() * charge * FormulaUtils.electronMass;
    final Range<Double> formulaMassRange = formulaTolerance.getToleranceRange(
        neutralMassWithAdduct);

    final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    return new MolecularFormulaGenerator(builder, formulaMassRange.lowerEndpoint(),
        formulaMassRange.upperEndpoint(), elements);
  }

  public void generateFormulae(boolean couldBeRadical, MolecularFormulaGenerator generator) {
    IMolecularFormula formula = null;
    do {
      formula = generator.getNextFormula();
      if (isCanceled() || formulaCount >= maxFormulaCount) {
        break;
      }

      if (isFeasibleFormula(couldBeRadical, formula)) {
        formula.setCharge(polarity.getSign() * charge);

        final IsotopePattern calcIsotopePattern = IsotopePatternCalculator.calculateIsotopePattern(
            formula, 0.01, formulaTolerance.getMzToleranceForMass(averageMZ), charge, polarity,
            false);
        final float isotopeSimilarity = IsotopePatternScoreCalculator.getSimilarityScore(
            measuredIsotopePattern, calcIsotopePattern, formulaTolerance, 0.1);

        final MSMSScore msmsScore = MSMSScoreCalculator.evaluateMSMS(ms2Tolerance, formula,
            ScanUtils.extractDataPoints(ms2Spectrum), averageMZ, charge);

        final ResultFormula resultFormula = new ResultFormula(formula, calcIsotopePattern,
            isotopeSimilarity, msmsScore.explainedIntensity(), msmsScore.annotation(), averageMZ);

        // i know we should not do this, but it will take forever otherwise.
        FxThread.runLater(() -> model.getPrecursorFormulae().add(resultFormula));
        formulaCount++;
      }
    } while (formula != null);
  }

  private boolean isFeasibleFormula(boolean couldBeRadical, IMolecularFormula formula) {
    if (formula == null) {
      return false;
    }

    if (checkCHONPS && ElementalHeuristicChecker.checkFormula(formula, checkCHONPS, checkCHONPS,
        true)) {

      final Double rdbe = RDBERestrictionChecker.calculateRDBE(formula);
      if (checkRDBE && rdbe != null) {
        if (rdbe - rdbe.intValue() > 0) {
          return true;
        } else if (Double.compare(rdbe - rdbe.intValue(), 0) == 0 && couldBeRadical) {
          return true;
        }
      }
    }
    return false;
  }
}
