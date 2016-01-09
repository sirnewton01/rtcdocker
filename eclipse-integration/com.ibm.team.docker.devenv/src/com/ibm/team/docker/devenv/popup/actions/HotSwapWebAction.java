package com.ibm.team.docker.devenv.popup.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.team.docker.devenv.Activator;

public class HotSwapWebAction implements IObjectActionDelegate {

	private Shell shell;
	private List<IFile> webFiles = new ArrayList<IFile>();

	public HotSwapWebAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public void run(IAction action) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);

		final List<IFile> filesToSend = new ArrayList<IFile>(webFiles);

		try {
			dialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					monitor.beginTask("Hot swapping web resources",
							filesToSend.size());

					
					HttpClient client = new HttpClient();
					
					for (IFile file: filesToSend) {
						PutMethod swapResourcesReq = new PutMethod(
								"http://localhost:9000/swapresource" + file.getFullPath());
						swapResourcesReq.setRequestEntity(new FileRequestEntity(new File(file.getLocation().toOSString()), "text/plain"));
						try {
							int resp = client.executeMethod(swapResourcesReq);
		
							swapResourcesReq.releaseConnection();
							if (resp != 200) {
								throw new InvocationTargetException(
										new IllegalStateException(
												"Error status returned from docker container: "
														+ swapResourcesReq.getStatusLine()));
							}
						} catch (Exception e) {
							throw new InvocationTargetException(e);
						}
						
						monitor.worked(1);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Activator
					.getDefault()
					.getLog()
					.log(new Status(IStatus.ERROR, Activator.getDefault()
							.getBundle().getSymbolicName(), e.getMessage(), e));
			MessageDialog.openError(shell, "Error swapping web resources",
					e.getMessage());
		} catch (InterruptedException e) {
			Activator
					.getDefault()
					.getLog()
					.log(new Status(IStatus.ERROR, Activator.getDefault()
							.getBundle().getSymbolicName(), e.getMessage(), e));
			MessageDialog.openError(shell, "Error swapping web resources",
					e.getMessage());
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		webFiles.clear();

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structSel = (IStructuredSelection) selection;

			for (Object o : structSel.toArray()) {
				IFile file = (IFile) o;
				if (file.getName().endsWith(".js")
						|| file.getName().endsWith(".html")
						|| file.getName().endsWith(".css")) {
					webFiles.add((IFile) o);
				}
			}
		}

		action.setEnabled(webFiles.size() > 0);
	}
}
