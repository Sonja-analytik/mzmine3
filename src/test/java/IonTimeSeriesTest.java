/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IonTimeSeriesTest {

  private static final Logger logger = Logger.getLogger(IonTimeSeriesTest.class.getName());
  final double[] intensities = {663.0, 683.0, 624.0, 505.0, 505.0, 525.0, 515.0, 753.0, 624.0,
      753.0, 584.0, 545.0, 693.0, 545.0, 555.0, 594.0, 763.0, 535.0, 545.0, 713.0, 505.0, 525.0,
      574.0, 584.0, 525.0, 564.0, 545.0, 584.0, 574.0, 515.0, 525.0, 614.0, 614.0, 535.0, 673.0,
      564.0, 564.0, 654.0, 1040.0, 574.0, 673.0, 545.0, 733.0, 2188.0, 525.0, 525.0, 1188.0, 604.0,
      505.0, 525.0, 525.0, 525.0, 545.0, 515.0, 515.0, 505.0, 515.0, 515.0, 921.0, 555.0, 564.0,
      505.0, 663.0, 525.0, 634.0, 693.0, 1287.0, 545.0, 574.0, 772.0, 594.0, 515.0, 693.0, 574.0,
      1584.0, 1357.0, 1149.0, 574.0, 515.0, 505.0, 574.0, 693.0, 634.0, 594.0, 564.0, 1227.0, 525.0,
      683.0, 564.0, 515.0, 634.0, 624.0, 1208.0, 564.0, 634.0, 535.0, 555.0, 505.0, 594.0, 545.0,
      555.0, 525.0, 555.0, 1070.0, 515.0, 564.0, 525.0, 545.0, 1119.0, 535.0, 525.0, 574.0, 644.0,
      772.0, 555.0, 564.0, 525.0, 564.0, 505.0, 654.0, 802.0, 545.0, 852.0, 515.0, 525.0, 673.0,
      604.0, 525.0, 515.0, 564.0, 753.0, 574.0, 535.0, 624.0, 604.0, 574.0, 505.0, 624.0, 604.0,
      535.0, 535.0, 1010.0, 654.0, 693.0, 604.0, 634.0, 832.0, 594.0, 515.0, 753.0, 574.0, 624.0,
      555.0, 614.0, 624.0, 644.0, 634.0, 584.0, 594.0, 604.0, 763.0, 525.0, 515.0, 673.0, 535.0,
      772.0, 1060.0, 584.0, 555.0, 545.0, 1169.0, 1436.0, 584.0, 654.0, 505.0, 584.0, 515.0, 683.0,
      693.0, 535.0, 525.0, 614.0, 614.0, 555.0, 525.0, 535.0, 644.0, 594.0, 624.0, 594.0, 634.0,
      555.0, 515.0, 1168.0, 525.0, 594.0, 515.0, 792.0, 693.0, 515.0, 564.0, 545.0, 525.0, 1129.0,
      525.0, 525.0, 693.0, 614.0, 673.0, 703.0, 505.0, 634.0, 535.0, 525.0, 505.0, 515.0, 594.0,
      644.0, 555.0, 525.0, 1020.0, 1238.0, 792.0, 555.0, 555.0, 525.0, 624.0, 584.0, 1079.0, 545.0,
      535.0, 555.0, 1010.0, 564.0, 614.0, 564.0, 555.0, 2149.0, 594.0, 535.0, 505.0, 564.0, 564.0,
      644.0, 693.0, 574.0, 555.0, 525.0, 535.0, 881.0, 1614.0, 1179.0, 584.0, 1188.0, 763.0, 713.0,
      1159.0, 574.0, 1149.0, 545.0, 673.0, 505.0, 505.0, 594.0, 564.0, 1297.0, 525.0, 644.0, 545.0,
      555.0, 1247.0, 505.0, 535.0, 1238.0, 1485.0, 594.0, 574.0, 772.0, 574.0, 1168.0, 564.0, 505.0,
      574.0, 564.0, 555.0, 505.0, 614.0, 733.0, 644.0, 594.0, 1000.0, 505.0, 545.0, 574.0, 1149.0,
      555.0, 673.0, 881.0, 1575.0, 1407.0, 1664.0, 644.0, 654.0, 644.0, 1070.0, 525.0, 574.0, 574.0,
      624.0, 673.0, 505.0, 634.0, 594.0, 624.0, 564.0, 505.0, 1158.0, 624.0, 1069.0, 792.0, 525.0,
      505.0, 584.0, 1079.0, 1089.0, 624.0, 654.0, 594.0, 1337.0, 663.0, 614.0, 535.0, 1278.0, 535.0,
      1020.0, 763.0, 901.0, 515.0, 555.0, 654.0, 545.0, 693.0, 1823.0, 624.0, 555.0, 1020.0, 515.0,
      644.0, 693.0, 980.0, 535.0, 1752.0, 1377.0, 564.0, 852.0, 2278.0, 614.0, 545.0, 584.0, 614.0,
      1307.0, 505.0, 574.0, 515.0, 574.0, 782.0, 515.0, 1040.0, 525.0, 723.0, 644.0, 564.0, 604.0,
      763.0, 921.0, 1178.0, 515.0, 1257.0, 812.0, 693.0, 1139.0, 564.0, 1307.0, 802.0, 1851.0,
      505.0, 1961.0, 822.0, 634.0, 1218.0, 1040.0, 1060.0, 654.0, 1614.0, 733.0, 515.0, 525.0,
      2278.0, 901.0, 535.0, 1060.0, 1535.0, 1040.0, 624.0, 683.0, 564.0, 555.0, 654.0, 564.0, 693.0,
      594.0, 535.0, 525.0, 1248.0, 1575.0, 584.0, 535.0, 564.0, 822.0, 505.0, 574.0, 1218.0, 614.0,
      663.0, 535.0, 545.0, 525.0, 654.0, 505.0, 663.0, 545.0, 555.0, 1020.0, 1139.0, 1277.0, 852.0,
      505.0, 574.0, 525.0, 1179.0, 683.0, 644.0, 772.0, 1595.0, 1931.0, 654.0, 1347.0, 555.0,
      1089.0, 693.0, 1317.0, 1713.0, 1713.0, 1218.0, 1149.0, 564.0, 574.0, 584.0, 1347.0, 525.0,
      545.0, 1100.0, 535.0, 634.0, 1188.0, 535.0, 505.0, 1198.0, 594.0, 564.0, 564.0, 505.0, 574.0,
      515.0, 743.0, 634.0, 614.0, 535.0, 634.0, 555.0, 525.0, 713.0, 545.0, 594.0, 505.0, 505.0,
      703.0, 624.0, 584.0, 644.0, 1486.0, 693.0, 525.0, 515.0, 614.0, 525.0, 634.0, 733.0, 961.0,
      545.0, 584.0, 535.0, 535.0, 673.0, 624.0, 584.0, 555.0, 1208.0, 574.0, 525.0, 693.0, 1149.0,
      584.0, 594.0, 1178.0, 545.0, 673.0, 673.0, 594.0, 515.0, 574.0, 1168.0, 574.0, 505.0, 515.0,
      1169.0, 535.0, 1188.0, 1347.0, 535.0, 545.0, 545.0, 515.0, 683.0, 515.0, 555.0, 772.0, 1109.0,
      515.0, 941.0, 663.0, 564.0, 634.0, 1356.0, 772.0, 525.0, 515.0, 1158.0, 1109.0, 753.0, 792.0,
      1714.0, 515.0, 1357.0, 604.0, 525.0, 2267.0, 515.0, 515.0, 1872.0, 683.0, 505.0, 594.0,
      1278.0, 624.0, 1228.0, 515.0, 594.0, 525.0, 545.0, 644.0, 515.0, 1109.0, 515.0, 545.0, 713.0,
      1099.0, 545.0, 703.0, 545.0, 614.0, 545.0, 505.0, 515.0, 555.0, 713.0, 1703.0, 584.0, 1753.0,
      564.0, 525.0, 832.0, 1079.0, 1109.0, 1782.0, 1060.0, 1079.0, 535.0, 713.0, 1911.0, 545.0,
      525.0, 763.0, 1684.0, 535.0, 535.0, 604.0, 515.0, 713.0, 634.0, 525.0, 743.0, 812.0, 574.0,
      574.0, 545.0, 782.0, 545.0, 505.0, 1237.0, 654.0, 951.0, 743.0, 663.0, 604.0, 555.0, 1466.0,
      1040.0, 604.0, 1030.0, 614.0, 535.0, 1485.0, 515.0, 1099.0, 505.0, 594.0, 604.0, 733.0,
      1208.0, 505.0, 584.0, 574.0, 634.0, 683.0, 545.0, 574.0, 901.0, 515.0, 2317.0, 1020.0, 1763.0,
      1247.0, 545.0, 545.0, 505.0, 545.0, 1139.0, 792.0, 1228.0, 1506.0, 743.0, 564.0, 1327.0,
      1228.0, 515.0, 1506.0, 594.0, 564.0, 782.0, 713.0, 1327.0, 634.0, 802.0, 525.0, 1139.0, 535.0,
      663.0, 555.0, 1010.0, 505.0, 594.0, 535.0, 743.0, 634.0, 1129.0, 505.0, 505.0, 604.0, 1079.0,
      535.0, 535.0, 535.0, 515.0, 545.0, 733.0, 525.0, 1881.0, 644.0, 1268.0, 753.0, 673.0, 1367.0,
      594.0, 525.0, 545.0, 535.0, 713.0, 1020.0, 1288.0, 525.0, 693.0, 1802.0, 743.0, 545.0, 515.0,
      723.0, 564.0, 535.0, 1069.0, 673.0, 564.0, 505.0, 555.0, 545.0, 644.0, 535.0, 792.0, 574.0,
      970.0, 505.0, 1248.0, 723.0, 673.0, 792.0, 574.0, 525.0, 1803.0, 1060.0, 515.0, 594.0, 792.0,
      594.0, 574.0, 733.0, 1109.0, 525.0, 574.0, 1109.0, 1060.0, 644.0, 644.0, 1406.0, 505.0, 535.0,
      1812.0, 604.0, 535.0, 990.0, 614.0, 624.0, 921.0, 1040.0, 555.0, 624.0, 1268.0, 644.0, 1367.0,
      584.0, 584.0, 505.0, 1386.0, 1179.0, 515.0, 713.0, 614.0, 564.0, 525.0, 634.0, 1010.0, 525.0,
      1168.0, 604.0, 852.0, 584.0, 1030.0, 634.0, 505.0, 525.0, 772.0, 1020.0, 584.0, 921.0, 515.0,
      535.0, 2060.0, 1149.0, 604.0, 535.0, 654.0, 1287.0, 1178.0, 505.0, 624.0, 772.0, 614.0,
      1070.0, 584.0, 1060.0, 1683.0, 1069.0, 1227.0, 584.0, 614.0, 555.0, 505.0, 515.0, 812.0,
      535.0, 535.0, 1070.0, 812.0, 505.0, 604.0, 555.0, 634.0, 535.0, 584.0, 555.0, 693.0, 574.0,
      555.0, 515.0, 574.0, 545.0, 624.0, 822.0, 535.0, 515.0, 703.0, 654.0, 1238.0, 584.0, 584.0,
      1168.0, 644.0, 555.0, 614.0, 1268.0, 654.0, 1139.0, 584.0, 842.0, 1159.0, 535.0, 663.0, 614.0,
      535.0, 525.0, 634.0, 1852.0, 584.0, 1712.0, 525.0, 1743.0, 1366.0, 545.0, 525.0, 1386.0,
      723.0, 545.0, 644.0, 743.0, 634.0, 673.0, 545.0, 812.0, 545.0, 515.0, 535.0, 614.0, 505.0,
      535.0, 772.0, 545.0, 584.0, 555.0, 535.0, 505.0, 763.0, 555.0, 564.0, 505.0, 574.0, 525.0,
      515.0, 505.0, 564.0, 574.0, 545.0, 545.0, 555.0, 594.0, 1910.0, 515.0, 1327.0, 743.0, 891.0,
      555.0, 505.0, 1248.0, 1376.0, 663.0, 505.0, 574.0, 604.0, 634.0, 584.0, 604.0, 594.0, 535.0,
      1436.0, 743.0, 515.0, 1149.0, 634.0, 624.0, 1426.0, 505.0, 574.0, 723.0, 574.0, 535.0, 525.0,
      1089.0, 525.0, 525.0, 574.0, 584.0, 505.0, 545.0, 564.0, 624.0, 1238.0, 723.0, 614.0, 584.0,
      663.0, 673.0, 1099.0, 545.0, 515.0, 1257.0, 564.0, 1178.0, 2308.0, 1040.0, 594.0, 545.0,
      673.0, 564.0, 535.0, 713.0, 555.0, 564.0, 594.0, 743.0, 525.0, 1228.0, 1426.0, 1149.0, 624.0,
      1109.0, 1189.0, 654.0, 535.0, 604.0, 614.0, 505.0, 614.0, 555.0, 604.0, 1089.0, 515.0, 505.0,
      525.0, 555.0, 614.0, 555.0, 505.0, 545.0, 1258.0, 733.0, 634.0, 634.0, 673.0, 535.0, 1079.0,
      1852.0, 594.0, 1228.0, 634.0, 594.0, 555.0, 1268.0, 644.0, 555.0, 663.0, 545.0, 634.0, 703.0,
      564.0, 693.0, 535.0, 505.0, 515.0, 1941.0, 574.0, 624.0, 1040.0, 1129.0, 545.0, 535.0, 535.0,
      525.0, 564.0, 673.0, 723.0, 614.0, 792.0, 644.0, 2199.0, 505.0, 555.0, 1080.0, 624.0, 594.0,
      634.0, 1495.0, 862.0, 624.0, 535.0, 2130.0, 1139.0, 644.0, 1099.0, 505.0, 624.0, 1020.0,
      703.0, 525.0, 703.0, 614.0, 644.0, 1199.0, 644.0, 2150.0, 1129.0, 525.0, 980.0, 594.0, 604.0,
      574.0, 634.0, 584.0, 1772.0, 545.0, 1050.0, 842.0, 673.0, 515.0, 545.0, 723.0, 733.0, 574.0,
      1189.0, 614.0, 594.0, 555.0, 564.0, 545.0, 594.0, 753.0, 1060.0, 545.0, 683.0, 545.0, 1387.0,
      614.0, 535.0, 654.0, 555.0, 683.0, 574.0, 644.0, 545.0, 564.0, 515.0, 753.0, 545.0, 644.0,
      564.0, 545.0, 683.0, 822.0, 505.0, 1139.0, 1139.0, 723.0, 1129.0, 634.0, 574.0, 1020.0, 505.0,
      663.0, 1040.0, 555.0, 525.0, 545.0, 564.0, 1129.0, 2120.0, 733.0, 564.0, 1258.0, 505.0,
      1168.0, 663.0, 594.0, 673.0, 525.0, 1396.0, 505.0, 505.0, 545.0, 525.0, 614.0, 743.0, 505.0,
      772.0, 2179.0, 1238.0, 515.0, 881.0, 1159.0, 772.0, 545.0, 505.0, 634.0, 555.0, 644.0, 505.0,
      1030.0, 961.0, 772.0, 1228.0, 644.0, 555.0, 584.0, 1159.0, 1684.0, 505.0, 1783.0, 535.0,
      703.0, 555.0, 1723.0, 574.0, 683.0, 564.0, 703.0, 525.0, 663.0, 1080.0, 555.0, 584.0, 525.0,
      564.0, 564.0, 535.0, 555.0, 1535.0, 604.0, 545.0, 574.0, 1139.0, 1179.0, 881.0, 1208.0,
      1010.0, 525.0, 564.0, 713.0, 505.0, 1713.0, 980.0, 812.0, 574.0, 564.0, 594.0, 663.0, 515.0,
      564.0, 1070.0, 555.0, 1188.0, 1674.0, 1347.0, 515.0, 624.0, 505.0, 723.0, 703.0, 1516.0,
      1218.0, 891.0, 673.0, 584.0, 644.0, 1426.0, 842.0, 1327.0, 604.0, 525.0, 772.0, 1159.0, 753.0,
      763.0, 564.0, 1267.0, 525.0, 594.0, 594.0, 505.0, 535.0, 515.0, 693.0, 515.0, 673.0, 505.0,
      515.0, 584.0, 683.0, 515.0, 763.0, 693.0, 545.0, 1158.0, 545.0, 970.0, 1307.0, 535.0, 525.0,
      654.0, 723.0, 515.0, 584.0, 545.0, 555.0, 515.0, 584.0, 525.0, 842.0, 624.0, 1129.0, 1308.0,
      535.0, 614.0, 515.0, 505.0, 555.0, 574.0, 604.0, 564.0, 862.0, 505.0, 515.0, 555.0, 654.0,
      545.0, 564.0, 1287.0, 525.0, 505.0, 683.0, 535.0, 564.0, 505.0, 772.0, 555.0, 673.0, 1129.0,
      525.0, 505.0, 1060.0, 1139.0, 1169.0, 535.0, 713.0, 515.0, 753.0, 1258.0, 1198.0, 594.0,
      515.0, 1188.0, 535.0, 525.0, 545.0, 584.0, 594.0, 1308.0, 545.0, 614.0, 673.0, 594.0, 1238.0,
      555.0, 584.0, 1020.0, 792.0, 505.0, 822.0, 535.0, 2297.0, 1267.0, 535.0, 545.0, 1742.0, 772.0,
      545.0, 525.0, 594.0, 584.0, 535.0, 584.0, 564.0, 505.0, 574.0, 723.0, 663.0, 574.0, 525.0,
      525.0, 525.0, 693.0, 624.0, 1643.0, 525.0, 604.0, 614.0, 792.0, 980.0, 733.0, 1406.0, 555.0,
      515.0, 1159.0, 564.0, 505.0, 634.0, 555.0, 1040.0, 535.0, 525.0, 505.0, 564.0, 584.0, 683.0,
      604.0, 525.0, 644.0, 1089.0, 574.0, 871.0, 1208.0, 535.0, 574.0, 505.0, 525.0, 1030.0, 634.0,
      574.0, 673.0, 1386.0, 574.0, 763.0, 644.0, 654.0, 535.0, 594.0, 772.0, 604.0, 634.0, 584.0,
      663.0, 693.0, 594.0, 624.0, 1228.0, 555.0, 525.0, 584.0, 1297.0, 1327.0, 594.0, 733.0, 1247.0,
      535.0, 564.0, 1515.0, 644.0, 1079.0, 2069.0, 555.0, 663.0, 673.0, 574.0, 604.0, 1109.0, 535.0,
      654.0, 733.0, 555.0, 654.0, 614.0, 584.0, 555.0, 1129.0, 564.0, 1129.0, 564.0, 545.0, 1237.0,
      624.0, 594.0, 525.0, 594.0, 505.0, 683.0, 634.0, 505.0, 604.0, 644.0, 1158.0, 1139.0, 614.0,
      594.0, 515.0, 634.0, 574.0, 1090.0, 564.0, 525.0, 535.0, 545.0, 624.0, 663.0, 663.0, 1168.0,
      1139.0, 1129.0, 1158.0, 624.0, 555.0, 604.0, 574.0, 733.0, 713.0, 525.0, 1525.0, 644.0,
      1129.0, 574.0, 901.0, 555.0, 574.0, 1653.0, 693.0, 703.0, 555.0, 545.0, 564.0, 564.0, 515.0,
      614.0, 1089.0, 614.0, 1100.0, 614.0, 505.0, 614.0, 654.0, 574.0, 594.0, 1080.0, 555.0, 1228.0,
      1564.0, 1149.0, 594.0, 515.0, 594.0, 505.0, 733.0, 624.0, 1189.0, 574.0, 505.0, 644.0, 584.0,
      545.0, 535.0, 624.0, 525.0, 1090.0, 753.0, 1248.0, 535.0, 911.0, 564.0, 505.0, 515.0, 574.0,
      525.0, 614.0, 624.0, 525.0, 624.0, 555.0, 1060.0, 703.0, 703.0, 1139.0, 624.0, 1594.0, 574.0,
      1387.0, 594.0, 644.0, 614.0, 1228.0, 594.0, 683.0, 604.0, 614.0, 515.0, 525.0, 842.0, 584.0,
      614.0, 594.0, 624.0, 614.0, 564.0, 663.0, 1100.0, 1050.0, 535.0, 1822.0, 574.0, 525.0, 1396.0,
      525.0, 693.0, 1258.0, 1377.0, 525.0, 555.0, 951.0, 673.0, 772.0, 644.0, 604.0, 545.0, 515.0,
      604.0, 941.0, 1436.0, 1099.0, 555.0, 1099.0, 525.0, 535.0, 545.0, 1119.0, 733.0, 1129.0,
      614.0, 584.0, 584.0, 574.0, 505.0, 693.0, 1208.0, 713.0, 505.0, 515.0, 555.0, 634.0, 535.0,
      574.0, 1189.0, 1050.0, 1218.0, 535.0, 683.0, 654.0, 1228.0, 505.0, 624.0, 782.0, 1852.0,
      505.0, 1129.0, 515.0, 713.0, 505.0, 753.0, 535.0, 604.0, 535.0, 634.0, 1228.0, 634.0, 505.0,
      555.0, 1110.0, 604.0, 535.0, 624.0, 1158.0, 634.0, 545.0, 604.0, 1188.0, 584.0, 584.0, 693.0,
      624.0, 535.0, 525.0, 812.0, 515.0, 772.0, 525.0, 614.0, 1159.0, 594.0, 624.0, 564.0, 505.0,
      802.0, 614.0, 525.0, 594.0, 594.0, 624.0, 862.0, 505.0, 614.0, 545.0, 515.0, 545.0, 644.0,
      614.0, 525.0, 505.0, 693.0, 1119.0, 673.0, 634.0, 901.0, 654.0, 515.0, 1020.0, 545.0, 535.0,
      505.0, 545.0, 604.0, 545.0, 555.0, 535.0, 515.0, 1148.0, 555.0, 535.0, 564.0, 614.0, 564.0,
      584.0, 564.0, 535.0, 584.0, 753.0, 703.0, 574.0, 604.0, 654.0, 663.0, 574.0, 1337.0, 822.0,
      545.0, 505.0, 1148.0, 584.0, 584.0, 693.0, 1287.0, 505.0, 673.0, 564.0, 525.0, 1486.0, 584.0,
      1396.0, 1158.0, 555.0, 574.0, 535.0, 614.0, 1267.0, 574.0, 604.0, 1851.0, 663.0, 505.0, 634.0,
      564.0, 525.0, 564.0, 654.0, 1248.0, 574.0, 564.0, 535.0, 594.0, 564.0, 545.0, 594.0, 1090.0,
      654.0, 842.0, 525.0, 574.0, 663.0, 515.0, 1951.0, 1129.0, 644.0, 1525.0, 1089.0, 564.0, 515.0,
      505.0, 763.0, 535.0, 673.0, 594.0, 584.0, 535.0, 505.0, 515.0, 515.0, 614.0, 535.0, 505.0,
      951.0, 545.0, 525.0, 683.0, 564.0, 693.0, 545.0, 1267.0, 1773.0, 1070.0, 1109.0, 1169.0,
      1218.0, 584.0, 624.0, 1377.0, 1347.0, 624.0, 545.0, 614.0, 555.0, 663.0, 594.0, 782.0, 535.0,
      871.0, 594.0, 545.0, 723.0, 505.0, 564.0, 505.0, 594.0, 584.0, 1169.0, 574.0, 1188.0, 1396.0,
      624.0, 1693.0, 1188.0, 1199.0, 1376.0, 2782.0, 862.0, 1178.0, 594.0, 555.0, 525.0, 505.0,
      545.0, 1228.0, 574.0, 683.0, 1099.0, 515.0, 555.0, 683.0, 525.0, 693.0, 515.0, 535.0, 594.0,
      515.0, 654.0, 535.0, 683.0, 515.0, 644.0, 654.0, 525.0, 525.0, 634.0, 1179.0, 941.0, 703.0,
      832.0, 1208.0, 1079.0, 535.0, 545.0, 564.0, 743.0, 644.0, 545.0, 693.0, 654.0, 515.0, 614.0,
      555.0, 545.0, 594.0, 1258.0, 673.0, 564.0, 535.0, 535.0, 594.0, 842.0, 1040.0, 624.0, 545.0,
      673.0, 713.0, 505.0, 693.0, 564.0, 654.0, 574.0, 1129.0, 604.0, 911.0, 614.0, 594.0, 644.0,
      1030.0, 1040.0, 842.0, 624.0, 604.0, 555.0, 1762.0, 1218.0, 663.0, 713.0, 574.0, 713.0, 515.0,
      624.0, 604.0, 663.0, 644.0, 832.0, 584.0, 1139.0, 1416.0, 1753.0, 812.0, 505.0, 574.0, 515.0,
      604.0, 614.0, 1208.0, 1466.0, 683.0, 545.0, 2060.0, 1377.0, 515.0, 1842.0, 753.0, 644.0,
      832.0, 1119.0, 584.0, 1327.0, 584.0, 683.0, 525.0, 852.0, 1288.0, 545.0, 525.0, 594.0, 505.0,
      901.0, 1060.0, 545.0, 545.0, 525.0, 644.0, 614.0, 1277.0, 545.0, 2238.0, 1129.0, 515.0, 515.0,
      525.0, 515.0, 1129.0, 505.0, 842.0, 871.0, 505.0, 624.0, 535.0, 545.0, 535.0, 535.0, 634.0,
      545.0, 1871.0, 584.0, 535.0, 763.0, 683.0, 535.0, 753.0, 505.0, 555.0, 723.0, 624.0, 525.0,
      1158.0, 535.0, 515.0, 564.0, 574.0, 614.0, 535.0, 555.0, 614.0, 1159.0, 1178.0, 614.0, 555.0,
      1228.0, 555.0, 891.0, 525.0, 1149.0, 614.0, 1040.0, 555.0, 505.0, 564.0, 505.0, 604.0, 723.0,
      753.0, 574.0, 525.0, 574.0, 584.0, 564.0, 515.0, 624.0, 505.0, 564.0, 693.0, 1109.0, 505.0,
      663.0, 505.0, 1099.0, 614.0, 505.0, 693.0, 792.0, 1684.0, 594.0, 564.0, 604.0, 505.0, 663.0,
      545.0, 515.0, 515.0, 1317.0, 1129.0, 515.0, 713.0, 555.0, 535.0, 1149.0, 584.0, 733.0, 673.0,
      980.0, 693.0, 505.0, 535.0, 525.0, 584.0, 604.0, 881.0, 564.0, 515.0, 871.0, 753.0, 624.0,
      604.0, 634.0, 624.0, 614.0, 1644.0, 594.0, 1109.0, 1198.0, 525.0, 1109.0, 515.0, 564.0, 515.0,
      564.0, 515.0, 584.0, 515.0, 505.0, 505.0, 644.0, 555.0, 624.0, 525.0, 654.0, 1040.0, 525.0,
      1218.0, 1278.0, 1931.0, 535.0, 525.0, 2921.0, 1317.0, 574.0, 1070.0, 1218.0, 2198.0, 1407.0,
      644.0, 842.0, 1060.0, 1624.0, 644.0, 505.0, 634.0, 654.0, 525.0, 574.0, 535.0, 2001.0, 564.0,
      1129.0, 1109.0, 574.0, 1823.0, 1426.0, 1109.0, 584.0, 1366.0, 1377.0, 1416.0, 614.0, 624.0,
      1694.0, 713.0, 584.0, 693.0, 594.0, 584.0, 505.0, 683.0, 525.0, 693.0, 545.0, 634.0, 574.0,
      555.0, 703.0, 505.0, 545.0, 545.0, 545.0, 1258.0, 584.0, 594.0, 1416.0, 515.0, 1248.0, 555.0,
      604.0, 634.0, 663.0, 723.0, 594.0, 1060.0, 545.0, 941.0, 703.0, 663.0, 1109.0, 574.0, 555.0,
      584.0, 1624.0, 1188.0, 535.0, 574.0, 733.0, 535.0, 1129.0, 535.0, 525.0, 555.0, 713.0, 505.0,
      545.0, 634.0, 505.0, 555.0, 525.0, 782.0, 525.0, 673.0, 584.0, 584.0, 862.0, 673.0, 515.0,
      743.0, 594.0, 564.0, 525.0, 624.0, 1149.0, 1248.0, 505.0, 574.0, 594.0, 525.0, 1297.0, 535.0,
      515.0, 624.0, 515.0, 644.0, 1218.0, 574.0, 654.0, 594.0, 525.0, 505.0, 584.0, 733.0, 634.0,
      545.0, 703.0, 772.0, 555.0, 574.0, 505.0, 822.0, 733.0, 574.0, 515.0, 545.0, 594.0, 1218.0,
      505.0, 881.0, 555.0, 505.0, 545.0, 535.0, 713.0, 713.0, 555.0, 515.0, 535.0, 1050.0, 614.0,
      505.0, 604.0, 564.0, 515.0, 1595.0, 545.0, 535.0, 545.0, 654.0, 951.0, 545.0, 505.0, 555.0,
      683.0, 535.0, 624.0, 535.0, 654.0, 545.0, 634.0, 1218.0, 535.0, 723.0, 713.0, 1912.0, 614.0,
      584.0, 644.0, 1515.0, 535.0, 525.0, 574.0, 743.0, 515.0, 535.0, 515.0, 535.0, 673.0, 505.0,
      614.0, 515.0, 515.0, 555.0, 1089.0, 564.0, 555.0, 555.0, 525.0, 505.0, 654.0, 1080.0, 584.0,
      723.0, 1168.0, 782.0, 594.0, 604.0, 594.0, 594.0, 1158.0, 871.0, 673.0, 673.0, 753.0, 584.0,
      545.0, 743.0, 505.0, 604.0, 654.0, 693.0, 515.0, 545.0, 1070.0, 535.0, 505.0, 604.0, 852.0,
      1109.0, 1188.0, 525.0, 1674.0, 614.0, 555.0, 535.0, 1298.0, 555.0, 743.0, 772.0, 535.0, 535.0,
      644.0, 1080.0, 634.0, 614.0, 584.0, 555.0, 594.0, 1079.0, 505.0, 555.0, 535.0, 574.0, 1149.0,
      535.0, 515.0, 1168.0, 594.0, 505.0, 515.0, 525.0, 1090.0, 505.0, 545.0, 1090.0, 1138.0, 614.0,
      624.0, 505.0, 1783.0, 545.0, 1584.0, 772.0, 683.0, 584.0, 614.0, 1079.0, 555.0, 1080.0, 564.0,
      574.0, 555.0, 525.0, 733.0, 584.0, 1060.0, 525.0, 574.0, 555.0, 723.0, 634.0, 753.0, 822.0,
      505.0, 515.0, 564.0, 604.0, 763.0, 1159.0, 1257.0, 604.0, 614.0, 1961.0, 525.0, 1050.0, 525.0,
      1317.0, 604.0, 1198.0, 683.0, 584.0, 535.0, 525.0, 545.0, 693.0, 1119.0, 2891.0, 555.0, 545.0,
      525.0, 574.0, 604.0, 614.0, 1198.0, 644.0, 505.0, 525.0, 663.0, 515.0, 584.0, 505.0, 515.0,
      1218.0, 564.0, 1277.0, 663.0, 763.0, 683.0, 515.0, 624.0, 535.0, 1159.0, 525.0, 515.0, 525.0,
      1079.0, 535.0, 594.0, 723.0, 505.0, 525.0, 515.0, 1555.0, 733.0, 763.0, 624.0, 545.0, 515.0,
      574.0, 515.0, 703.0, 1703.0, 584.0, 812.0, 683.0, 505.0, 1109.0, 515.0, 1149.0, 1585.0,
      1436.0, 525.0, 772.0, 574.0, 1178.0, 634.0, 574.0, 1139.0, 535.0, 693.0, 1288.0, 822.0,
      1129.0, 555.0, 713.0, 1476.0, 1179.0, 505.0, 1109.0, 564.0, 634.0, 594.0, 535.0, 594.0, 683.0,
      594.0, 505.0, 564.0, 624.0, 683.0, 604.0, 763.0, 881.0, 634.0, 1267.0, 624.0, 564.0, 654.0,
      1148.0, 574.0, 1268.0, 584.0, 1871.0, 654.0, 505.0, 654.0, 525.0, 594.0, 525.0, 535.0, 515.0,
      1109.0, 1010.0, 1891.0, 1119.0, 1089.0, 535.0, 584.0, 505.0, 614.0, 535.0, 683.0, 525.0,
      663.0, 1090.0, 713.0, 663.0, 515.0, 594.0, 614.0, 733.0, 743.0, 604.0, 1565.0, 574.0, 1070.0,
      634.0, 654.0, 564.0, 1248.0, 1624.0, 1070.0, 505.0, 564.0, 525.0, 1119.0, 515.0, 683.0, 505.0,
      515.0, 1704.0, 1099.0, 604.0, 545.0, 1248.0, 555.0, 515.0, 624.0, 584.0, 654.0, 545.0, 792.0,
      782.0, 545.0, 574.0, 1208.0, 743.0, 644.0, 713.0, 663.0, 564.0, 614.0, 673.0, 545.0, 1277.0,
      515.0, 1863.0, 1099.0, 1050.0, 515.0, 753.0, 1278.0, 525.0, 624.0, 1287.0, 594.0, 564.0,
      812.0, 614.0, 564.0, 683.0, 1515.0, 535.0, 2179.0, 545.0, 614.0, 574.0, 614.0, 545.0, 980.0,
      555.0, 505.0, 614.0, 535.0, 555.0, 1288.0, 782.0, 743.0, 564.0, 634.0, 535.0, 1317.0, 792.0,
      564.0, 505.0, 1297.0, 1664.0, 505.0, 604.0, 505.0, 1079.0, 574.0, 743.0, 881.0, 862.0, 535.0,
      584.0, 594.0, 673.0, 812.0, 763.0, 663.0, 505.0, 535.0, 525.0, 505.0, 1377.0, 772.0, 584.0,
      515.0, 871.0, 1138.0, 1030.0, 634.0, 644.0, 624.0, 515.0, 535.0, 683.0, 584.0, 545.0, 535.0,
      832.0, 505.0, 624.0, 574.0, 644.0, 525.0, 604.0, 683.0, 515.0, 1327.0, 1060.0, 1020.0, 733.0,
      525.0, 555.0, 1089.0, 634.0, 1654.0, 1753.0, 2100.0, 594.0, 1070.0, 1119.0, 772.0, 1694.0,
      1743.0, 515.0, 1574.0, 654.0, 515.0, 515.0, 693.0, 584.0, 555.0, 1297.0, 683.0, 693.0, 564.0,
      703.0, 594.0, 525.0, 1218.0, 1139.0, 832.0, 921.0, 614.0, 1327.0, 525.0, 644.0, 1050.0, 673.0,
      594.0, 525.0, 1020.0, 505.0, 604.0, 703.0, 663.0, 644.0, 574.0, 1109.0, 1762.0, 545.0, 535.0,
      505.0, 594.0, 505.0, 545.0, 1921.0, 1040.0, 634.0, 555.0, 2208.0, 772.0, 683.0, 644.0, 564.0,
      545.0, 614.0, 1615.0, 1703.0, 515.0, 703.0, 763.0, 614.0, 535.0, 535.0, 555.0, 663.0, 1169.0,
      673.0, 881.0, 673.0, 535.0, 703.0, 555.0, 505.0, 772.0, 555.0, 545.0, 515.0, 1476.0, 1268.0,
      2070.0, 1307.0, 525.0, 802.0, 555.0, 733.0, 1148.0, 733.0, 525.0, 733.0, 663.0, 564.0, 1139.0,
      564.0, 564.0, 1743.0, 2001.0, 881.0, 535.0, 515.0, 1030.0, 525.0, 1624.0, 584.0, 584.0,
      1941.0, 574.0, 505.0, 604.0, 525.0, 644.0, 535.0, 515.0, 614.0, 535.0, 1060.0, 693.0, 763.0,
      604.0, 525.0, 584.0, 842.0, 604.0, 515.0, 505.0, 822.0, 574.0, 1020.0, 683.0, 1624.0, 792.0,
      594.0, 644.0, 584.0, 644.0, 555.0, 634.0, 545.0, 515.0, 505.0, 743.0, 1040.0, 1654.0, 515.0,
      584.0, 574.0, 624.0, 1277.0, 1159.0, 574.0, 525.0, 663.0, 703.0, 525.0, 574.0, 584.0, 535.0,
      1733.0, 515.0, 525.0, 584.0, 564.0, 594.0, 1119.0, 535.0, 505.0, 574.0, 584.0, 1228.0, 1050.0,
      1119.0, 683.0, 545.0, 703.0, 693.0, 564.0, 1852.0, 1406.0, 535.0, 564.0, 555.0, 594.0, 921.0,
      644.0, 753.0, 1099.0, 654.0, 1861.0, 703.0, 881.0, 931.0, 545.0, 624.0, 961.0, 564.0, 1119.0,
      1099.0, 634.0, 663.0, 713.0, 535.0, 763.0, 535.0, 555.0, 515.0, 1060.0, 564.0, 604.0, 663.0,
      980.0, 555.0, 515.0, 555.0, 515.0, 614.0, 505.0, 515.0, 525.0, 555.0, 723.0, 663.0, 515.0,
      733.0, 970.0, 594.0, 535.0, 535.0, 624.0, 624.0, 644.0, 564.0, 1040.0, 832.0, 515.0, 624.0,
      505.0, 782.0, 555.0, 574.0, 594.0, 1693.0, 634.0, 525.0, 1694.0, 584.0, 594.0, 545.0, 2030.0,
      525.0, 693.0, 1208.0, 574.0, 1050.0, 624.0, 1208.0, 624.0, 1258.0, 584.0, 525.0, 911.0, 634.0,
      535.0, 594.0, 545.0, 1416.0, 525.0, 970.0, 584.0, 535.0, 594.0, 525.0, 624.0, 1723.0, 525.0,
      545.0, 505.0, 693.0, 515.0, 535.0, 505.0, 1218.0, 614.0, 673.0, 802.0, 703.0, 535.0, 663.0,
      525.0, 535.0, 654.0, 564.0, 535.0, 594.0, 1357.0, 832.0, 614.0, 634.0, 792.0, 634.0, 525.0,
      584.0, 525.0, 1069.0, 505.0, 1188.0, 535.0, 1237.0, 515.0, 564.0, 614.0, 574.0, 614.0, 505.0,
      525.0, 505.0, 525.0, 634.0, 901.0, 584.0, 1248.0, 1525.0, 574.0, 604.0, 822.0, 624.0, 931.0,
      515.0, 1822.0, 683.0, 1060.0, 1456.0, 782.0, 574.0, 743.0, 555.0, 555.0, 1337.0, 584.0, 663.0,
      1129.0, 753.0, 584.0, 535.0, 1089.0, 1465.0, 535.0, 1327.0, 535.0, 515.0, 564.0, 1040.0,
      1435.0, 574.0, 525.0, 515.0, 1208.0, 1198.0, 525.0, 634.0, 614.0, 1030.0, 535.0, 1267.0,
      2189.0, 584.0, 614.0, 614.0, 634.0, 1099.0, 604.0, 515.0, 683.0, 594.0, 1228.0, 713.0, 505.0};

  public static IonTimeSeries<? extends Scan> makeSimpleTimeSeries() throws IOException {

    RawDataFile file = new RawDataFileImpl("test", null, null, Color.BLACK);
    List<Scan> scans = new ArrayList();
    scans.add(new SimpleScan(file, 0, 1, 1f, null, new double[]{10d, 10d}, new double[]{10d, 10d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(10d, 10d)));
    scans.add(new SimpleScan(file, 1, 1, 1f, null, new double[]{11d, 11d}, new double[]{11d, 11d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(11d, 11d)));
    SimpleIonTimeSeries series = new SimpleIonTimeSeries(null, new double[]{5d, 10d},
        new double[]{30d, 31d}, scans);
    return series;
  }

  public static IonTimeSeries<Frame> makeIonMobilityTimeSeries() throws IOException {
    IMSRawDataFile file = new IMSRawDataFileImpl("test", null, null, Color.BLACK);

    List<Frame> frames = new ArrayList<>();
    SimpleFrame frame = new SimpleFrame(file, 1, 1, 1f, new double[]{1d}, new double[]{1d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(11d, 11d),
        MobilityType.TIMS, null, null);
    frame.setMobilities(new double[]{1d, 2d});

    List<BuildingMobilityScan> mobilityScans = new ArrayList<>();
    mobilityScans.add(new BuildingMobilityScan(0, new double[]{1d, 1d}, new double[]{2d, 2d}));
    mobilityScans.add(new BuildingMobilityScan(1, new double[]{2d, 2d}, new double[]{4d, 4d}));

    frame.setMobilityScans(mobilityScans, false);

    SimpleIonMobilitySeries ionMobilitySeries = new SimpleIonMobilitySeries(null,
        new double[]{1d, 2d}, new double[]{2d, 4d}, frame.getMobilityScans());
    file.addScan(frame);

    return IonMobilogramTimeSeriesFactory.of(null, List.of(ionMobilitySeries),
        new BinningMobilogramDataAccess(file, 1));
  }

  private static void testSortedSeries(int[] indices, IonTimeSeries<?> series) {
    for (int i = 1; i < series.getNumberOfValues(); i++) {
      final double i1 = series.getIntensity(indices[i - 1]);
      final double i2 = series.getIntensity(indices[i]);
      Assertions.assertTrue(i1 >= i2);
    }
  }

  @Test
  void testCasting() {

    try {
      IonTimeSeries<? extends Scan> scanSeries = makeSimpleTimeSeries();
      Assertions.assertTrue(scanSeries instanceof SimpleIonTimeSeries);
      Assertions.assertFalse(scanSeries instanceof IonMobilogramTimeSeries);

      List<Scan> scans = (List<Scan>) scanSeries.getSpectra();
      Assertions.assertTrue(scans.get(0) instanceof Scan);
      Assertions.assertFalse(scans.get(0) instanceof Frame);

      IonTimeSeries<? extends Scan> imFrameSeries = makeIonMobilityTimeSeries();
      Assertions.assertFalse(imFrameSeries instanceof SimpleIonTimeSeries);
      Assertions.assertTrue(imFrameSeries instanceof IonMobilogramTimeSeries);

      List<Scan> frames = (List<Scan>) imFrameSeries.getSpectra();
      Assertions.assertTrue(frames.get(0) instanceof Scan);
      Assertions.assertTrue(frames.get(0) instanceof Frame);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  void testIntensitySortedSeries() {
    final RawDataFile file = new RawDataFileImpl("test", null, null);
    final List<Scan> scans = makeSomeScans(file, 10);
    final double[] intensities0 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
    final double[] intensities1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
//    final double[] intensities2 = {0, 9, 3, 5, 4, 7, 2, 6, 8, 1};
    final double[] intensities2 = {0, 9, 3, 5, 7, 8, 2, 4, 6, 1};
    final double[] mzs = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    final SimpleIonTimeSeries i0 = new SimpleIonTimeSeries(null, mzs, intensities0, scans);
    final SimpleIonTimeSeries i1 = new SimpleIonTimeSeries(null, mzs, intensities1, scans);
    final SimpleIonTimeSeries i2 = new SimpleIonTimeSeries(null, mzs, intensities2, scans);
    final SimpleIonTimeSeries i3 = new SimpleIonTimeSeries(null, intensities, intensities,
        makeSomeScans(file, intensities.length));

    testSortedSeries(IonTimeSeriesUtils.getIntensitySortedIndices(i0), i0);
    testSortedSeries(IonTimeSeriesUtils.getIntensitySortedIndices(i1), i1);
    final int[] intensitySortedIndices = IonTimeSeriesUtils.getIntensitySortedIndices(i2);
    testSortedSeries(intensitySortedIndices, i2);
    testSortedSeries(IonTimeSeriesUtils.getIntensitySortedIndices(i3), i3);
  }

  public List<Scan> makeSomeScans(RawDataFile file, int numFrames) {
    final List<Scan> scans = new ArrayList<>();
    for (int i = 0; i < numFrames; i++) {
      final SimpleScan scan = new SimpleScan(file, i, 1, i, null, new double[]{0d, 1},
          new double[]{15d, 1E5}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "test",
          Range.closed(0d, 1000d));
      scans.add(scan);
    }
    return scans;
  }
}
