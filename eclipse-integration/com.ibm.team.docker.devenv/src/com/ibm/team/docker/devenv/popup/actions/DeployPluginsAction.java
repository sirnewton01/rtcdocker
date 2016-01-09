package com.ibm.team.docker.devenv.popup.actions;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
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

public class DeployPluginsAction implements IObjectActionDelegate {

	private Shell shell;
	private List<IFile> files = new ArrayList<IFile>();
	
	public DeployPluginsAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public void run(IAction action) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		
		final List<IFile> filesToSend = new ArrayList<IFile>(files);
		
		try {
			dialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					
					monitor.beginTask("Sending updated plugins", filesToSend.size());
					
					HttpClient client = new HttpClient();
					try {
						ByteArrayOutputStream bytes = new ByteArrayOutputStream();
						ZipOutputStream zipOutput = new ZipOutputStream(bytes);
						
						for (IFile file: filesToSend) {
							zipOutput.putNextEntry(new ZipEntry(file.getName()));
							InputStream is = new BufferedInputStream(file.getContents());
							int b = is.read();
							while (b!= -1) {
								zipOutput.write(b);
								b = is.read();
							}
							is.close();
							
							monitor.worked(1);
						}
						
						zipOutput.close();
						
						PutMethod submitBundlesReq = new PutMethod("http://localhost:9000/submitbundles");
						submitBundlesReq.setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(bytes.toByteArray()), bytes.size()));
						int resp = client.executeMethod(submitBundlesReq);
						
						submitBundlesReq.releaseConnection();
						if (resp != 200) {
							throw new InvocationTargetException(new IllegalStateException("Error status returned from docker container: " + submitBundlesReq.getStatusLine()));
						}
						
						PostMethod patchServerReq = new PostMethod("http://localhost:9000/patchserver");
						resp = client.executeMethod(patchServerReq);
						
						patchServerReq.releaseConnection();
						if (resp != 200) {
							throw new InvocationTargetException(new IllegalStateException("Error status returned from docker container: " + patchServerReq.getStatusLine()));
						}
					} catch (Exception e1) {
						throw new InvocationTargetException(e1);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), e.getMessage(), e));
			MessageDialog.openError(shell, "Error deploying bundles", e.getMessage());
		} catch (InterruptedException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), e.getMessage(), e));
			MessageDialog.openError(shell, "Error deploying bundles", e.getMessage());
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		files.clear();
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structSel = (IStructuredSelection)selection;
			
			IProject project = ((IProject)structSel.getFirstElement());
			IResource plugins = project.findMember("plugins");
			
			if (plugins == null) {
				action.setEnabled(false);
				return;
			}

			IFolder pluginsFolder = (IFolder)plugins;

			try {
				pluginsFolder.accept(new IResourceVisitor() {
					
					@Override
					public boolean visit(IResource resource) throws CoreException {
						if (resource instanceof IFile) {
							IFile file = (IFile)resource;
							
							if ("jar".equals(file.getFileExtension())) {
								files.add(file);
							}
						}
						return true;
					}
				});
			} catch (CoreException e) {
				// Ignore core exception
			}
		}
		
		action.setEnabled(files.size() > 0);
	}
}
