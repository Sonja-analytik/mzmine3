package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.util.FormulaUtils;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public abstract class AbstractLipidFragmentFactory {

  protected Range<Double> mzTolRangeMSMS;
  protected ILipidAnnotation lipidAnnotation;
  protected IonizationType ionizationType;
  protected LipidFragmentationRule[] rules;
  protected DataPoint dataPoint;
  protected Scan msMsScan;

  public AbstractLipidFragmentFactory(Range<Double> mzTolRangeMSMS,
      ILipidAnnotation lipidAnnotation, IonizationType ionizationType,
      LipidFragmentationRule[] rules, DataPoint dataPoint, Scan msMsScan) {
    this.mzTolRangeMSMS = mzTolRangeMSMS;
    this.lipidAnnotation = lipidAnnotation;
    this.ionizationType = ionizationType;
    this.rules = rules;
    this.dataPoint = dataPoint;
    this.msMsScan = msMsScan;
  }

  public LipidFragment findCommonLipidFragment() {
    LipidFragment lipidFragment = null;
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      LipidFragment detectedFragment = checkForCommonRuleTypes(rule);
      if (detectedFragment != null) {
        lipidFragment = detectedFragment;
        break;
      }
    }
    return lipidFragment;
  }

  private LipidFragment checkForCommonRuleTypes(LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    return switch (ruleType) {
      case HEADGROUP_FRAGMENT ->
          checkForHeadgroupFragment(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      case HEADGROUP_FRAGMENT_NL ->
          checkForHeadgroupFragmentNL(rule, mzTolRangeMSMS, lipidAnnotation, dataPoint, msMsScan);
      default -> null;
    };
  }

  private LipidFragment checkForHeadgroupFragment(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateMzRatio(fragmentFormula);
    if (mzTolRangeMSMS.contains(mzFragmentExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), mzFragmentExact, dataPoint,
          lipidAnnotation.getLipidClass(), null, null, null, msMsScan);
    } else {
      return null;
    }
  }

  private LipidFragment checkForHeadgroupFragmentNL(LipidFragmentationRule rule,
      Range<Double> mzTolRangeMSMS, ILipidAnnotation lipidAnnotation, DataPoint dataPoint,
      Scan msMsScan) {
    String fragmentFormula = rule.getMolecularFormula();
    Double mzFragmentExact = FormulaUtils.calculateExactMass(fragmentFormula);
    Double mzPrecursorExact =
        MolecularFormulaManipulator.getMass(lipidAnnotation.getMolecularFormula(),
            AtomContainerManipulator.MonoIsotopic) + rule.getIonizationType().getAddedMass();
    Double mzExact = mzPrecursorExact - mzFragmentExact;
    if (mzTolRangeMSMS.contains(mzExact)) {
      return new LipidFragment(rule.getLipidFragmentationRuleType(),
          rule.getLipidFragmentInformationLevelType(), mzExact, dataPoint,
          lipidAnnotation.getLipidClass(), null, null, LipidChainType.ACYL_CHAIN, msMsScan);
    } else {
      return null;
    }
  }

  protected double ionizeFragmentBasedOnPolarity(Double mzExact, PolarityType polarityType) {
    if (polarityType.equals(PolarityType.NEGATIVE)) {
      return mzExact + IonizationType.NEGATIVE.getAddedMass();
    } else if (polarityType.equals(PolarityType.POSITIVE)) {
      return mzExact + IonizationType.POSITIVE.getAddedMass();
    }
    return mzExact;
  }

}
