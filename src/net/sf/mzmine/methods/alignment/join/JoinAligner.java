/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.methods.alignment.join;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;



/**
 *
 */
public class JoinAligner implements Method,
        TaskListener, ListSelectionListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;


	public String toString() {
		return new String("Join Aligner");
	}



    /**
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public MethodParameters askParameters() {


        MZmineProject currentProject = MZmineProject.getCurrentProject();
        JoinAlignerParameters currentParameters = (JoinAlignerParameters) currentProject.getParameters(this);
        if (currentParameters == null)
            currentParameters = new JoinAlignerParameters();

		JoinAlignerParameterSetupDialog jaPSD = new JoinAlignerParameterSetupDialog(desktop.getMainFrame(), new String("Please give parameter values"), currentParameters);
		jaPSD.setVisible(true);

		// Check if user pressed cancel
		if (jaPSD.getExitCode()==-1) {
			return null;
		}

		currentParameters = jaPSD.getParameters();

		return currentParameters;

    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(MethodParameters parameters, OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

        logger.info("Running join aligner");

		Task alignmentTask = new JoinAlignerTask(dataFiles, (JoinAlignerParameters) parameters);
		taskController.addTask(alignmentTask, this);

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        myMenuItem = desktop.addMenuItem(MZmineMenu.ALIGNMENT,
                "Peak list aligner", this, null, KeyEvent.VK_A,
                false, false);

        desktop.addSelectionListener(this);


    }


    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        MethodParameters parameters = askParameters();
        if (parameters == null)
            return;

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();

        runMethod(parameters, dataFiles, null);

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        //myMenuItem.setEnabled(desktop.isDataFileSelected());

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();

		boolean allOk = true;

        for (OpenedRawDataFile file : dataFiles) {
			if (!file.getCurrentFile().hasData(PeakList.class)) {
				allOk = false;
            }
        }
        myMenuItem.setEnabled(allOk);

    }



    public void taskStarted(Task task) {
        // do nothing
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

			// TODO

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while aligning peak lists: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

	}

}
