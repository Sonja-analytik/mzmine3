/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types.tasks;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.lang.reflect.InvocationTargetException;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Task for creating graphical nodes, having (ModularFeature feature, AtomicDouble progress)
 * constructor
 */
public class FeatureGraphicalNodeTask extends AbstractTask {

  Class<? extends Node> nodeClass;
  private StackPane pane;
  private ModularFeature feature;
  private String collHeader;
  private AtomicDouble progress = new AtomicDouble(0d);

  public FeatureGraphicalNodeTask(Class<? extends Node> nodeClass, StackPane pane,
      ModularFeature feature,
      String collHeader) {
    super(null); // no new data stored -> null
    this.nodeClass = nodeClass;
    this.pane = pane;
    this.feature = feature;
    this.collHeader = collHeader;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    Node n = null;
    try {
      // create instance of nodeClass node with (ModularFeatureListRow, AtomicDouble) constructor
      n = nodeClass.getConstructor(new Class[]{ModularFeature.class, AtomicDouble.class})
          .newInstance(feature, progress);
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
        | InvocationTargetException e) {
      e.printStackTrace();
    }
    final Node node = n;
    // save chart for later
    feature.addBufferedColChart(collHeader, n);

    if (n != null) {
      Platform.runLater(() -> pane.getChildren().add(node));
    }
    setStatus(TaskStatus.FINISHED);
    progress.set(1d);
  }

  @Override
  public String getTaskDescription() {
    return "Creating a graphical column for col: " + collHeader
        + " for m/z: " + feature.getMZ() + " in file: " + feature.getRawDataFile();
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }
}
