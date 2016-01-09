package com.ibm.team.docker.devenv.popup.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
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
import com.ibm.team.docker.devenv.internal.HotSwapHelper;

public class HotSwapAction implements IObjectActionDelegate {

	private Shell shell;
	private List<IFile> javaSourceFiles = new ArrayList<IFile>();
	
	public HotSwapAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	public void run(IAction action) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		
		final List<IFile> filesToSend = new ArrayList<IFile>(javaSourceFiles);
		
		try {
			dialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					
					monitor.beginTask("Hot swapping classes", filesToSend.size());
					HotSwapHelper helper = new HotSwapHelper();
					try {
						helper.connect("localhost", "8000");
					} catch (Exception e1) {
						throw new InvocationTargetException(e1);
					}
					
					try {
						for (IFile sourceFile: filesToSend) {
							IProject project = sourceFile.getProject();
							IPath projectPath = project.getLocation();
							IPath sourceFilePath = sourceFile.getLocation();
							String nameMatcher = sourceFile.getName().replace(".java", "");
							
							IPath projectRelPath = sourceFilePath.makeRelativeTo(projectPath).removeFirstSegments(1).removeLastSegments(1);
							IPath expectedBinPath = projectPath.append("bin").append(projectRelPath);
							
							// Scan for any files that match the name in the binary path
							File binPath = new File(expectedBinPath.toOSString());
							if (binPath.exists()) {
								for (File f: binPath.listFiles()) {
									if (f.getName().startsWith(nameMatcher+".") || f.getName().startsWith(nameMatcher+"$") && f.getName().endsWith(".class")) {
										try {
											helper.replace(f, getClassName(f));
										} catch (Exception e) {
											throw new InvocationTargetException(e);
										}
									}
								}
							}
						}
					} finally {
						try {
							helper.disconnect();
						} catch (Exception e) {
							// Best effort
							e.printStackTrace();
						}
					}
				}
			});
		} catch (InvocationTargetException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), e.getMessage(), e));
			MessageDialog.openError(shell, "Error swapping classes", e.getMessage());
		} catch (InterruptedException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), e.getMessage(), e));
			MessageDialog.openError(shell, "Error swapping classes", e.getMessage());
		}
	}
	
	private static String getClassName(File f) throws IOException {
		String classPath = f.getCanonicalPath().split("/bin/")[1];
		
		classPath = classPath.replace('/', '.');
		classPath = classPath.replace('\\', '.');
		classPath = classPath.substring(0, classPath.length()-6);
		
		return classPath;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		javaSourceFiles.clear();
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structSel = (IStructuredSelection)selection;
			
			for (Object o: structSel.toArray()) {
				ICompilationUnit cu = (ICompilationUnit)o;
				
				IResource resource = cu.getResource();

				if (resource instanceof IFile) {
					javaSourceFiles.add((IFile)resource);
				}
			}
		}
		
		action.setEnabled(javaSourceFiles.size() > 0);
	}
}
