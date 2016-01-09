package com.ibm.team.eclipse.setup;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;

import com.ibm.team.filesystem.client.FileSystemException;
import com.ibm.team.filesystem.client.IOperationFactory;
import com.ibm.team.filesystem.client.ISandbox;
import com.ibm.team.filesystem.client.internal.PathLocation;
import com.ibm.team.filesystem.client.internal.SharingManager;
import com.ibm.team.filesystem.client.operations.ILoadOperation;
import com.ibm.team.filesystem.client.operations.LoadDilemmaHandler;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.rcp.ui.teamnavigator.ConnectedProjectAreaRegistry;
import com.ibm.team.repository.client.ILoginHandler2;
import com.ibm.team.repository.client.ILoginInfo2;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.client.login.UsernameAndPasswordLoginInfo;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.scm.client.IWorkspaceConnection;
import com.ibm.team.scm.client.IWorkspaceManager;
import com.ibm.team.scm.common.IVersionableHandle;
import com.ibm.team.scm.common.IWorkspaceHandle;
import com.ibm.team.scm.common.dto.IAncestorReport;
import com.ibm.team.scm.common.dto.IComponentizedAncestorList;
import com.ibm.team.scm.common.dto.IWorkspaceSearchCriteria;
import com.ibm.team.scm.common.internal.dto.NameItemPair;

@SuppressWarnings("restriction")
public class StartupHandler implements IStartup {

	@Override
	public void earlyStartup() {
		// Only run this once on an eclipse workspace
		String runAlready = Activator.getDefault().getPreferenceStore().getString("IS_RUN");
		if ("Yes".equals(runAlready)) {
			return;
		}
		Activator.getDefault().getPreferenceStore().setValue("IS_RUN", "Yes");
		
		final IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				Shell s = workbench.getActiveWorkbenchWindow().getShell();
				
				String rh = System.getenv("RTC_HOSTNAME");
				if (rh == null || rh.length() == 0) {
					rh = "localhost";
				}
				final String repoUrl = "https://"+rh+":9443/ccm";
				String un = System.getenv("RTC_USER");
				if (un == null || un.length() == 0) {
					un = "deb";
				}
				final String userName = un;
				String pw = System.getenv("RTC_PASSWORD");
				if (pw == null || pw.length() == 0) {
					pw = "deb";
				}
				final String password = pw;
				String pa = System.getenv("RTC_PROJECT_AREA");
				if (pa == null || pa.length() == 0) {
					pa = "JKE Banking (Change Management)";
				}
				final String projectAreaName = pa;
				String stm = System.getenv("RTC_STREAM");
				if (stm == null || stm.length() == 0) {
					stm = "BRM Stream";
				}
				final String streamName = stm;
				String wks = System.getenv("RTC_WORKSPACE");
				if (wks == null || wks.length() == 0) {
					wks = userName.substring(0,  1).toUpperCase() + userName.substring(1) + " " + stm + " Workspace";
				}
				final String repositoryWorkspace = wks;
				
				if (!MessageDialog.openConfirm(s, "Setup", "Would you like to automatically set up this client with these settings? "+ repoUrl+" "+userName+"/"+password+" Stream: "+stm)) {
					return;
				}
				
				try {
					ProgressMonitorDialog dialog = new ProgressMonitorDialog(s);
					
					dialog.run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							SubMonitor progress = SubMonitor.convert(monitor, 100);
							progress.setTaskName("Setting up eclipse client");
							
							try {
								// Try to log into the repository
								progress.setTaskName("Setting up eclipse client - logging into the server once it is available");
								ITeamRepository repo = TeamPlatform.getTeamRepositoryService().getTeamRepository(repoUrl, 0);
								repo.registerLoginHandler(new ILoginHandler2() {
									
									@Override
									public ILoginInfo2 challenge(ITeamRepository repository) {
										return new UsernameAndPasswordLoginInfo(userName, password);
									}
								});
								
								boolean loginSuccessful = false;
								
								while (!loginSuccessful) {
									try {
										repo.login(progress.newChild(10));
										loginSuccessful = true;
									} catch (TeamRepositoryException e) {
										Thread.sleep(30000);
									}
								}
								
								TeamPlatform.getTeamRepositoryService().addTeamRepository(repo);
								
								// Try to connect to the project area
								progress.setTaskName("Setting up eclipse client - connecting to project area " + projectAreaName + " once it is available");
								boolean connectedProjectArea = false;
								IProjectArea projectArea = null;
								while (!connectedProjectArea) {
									IProcessItemService itemService = (IProcessItemService)repo.getClientLibrary(IProcessItemService.class);
									@SuppressWarnings("rawtypes")
									List projectAreas = itemService.findAllProjectAreas(IProcessClientService.ALL_PROPERTIES, progress.newChild(10));
									for (Object p: projectAreas) {
										if (p instanceof IProjectArea) {
											IProjectArea pa = (IProjectArea)p;
											if (pa.getName().equals(projectAreaName)) {
												ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(pa);
												projectArea = pa;
												connectedProjectArea = true;
											}
										}
									}
									
									if (!connectedProjectArea) {
										Thread.sleep(30000);
									}
								}
								
								IWorkspaceManager wksmgr = (IWorkspaceManager) repo.getClientLibrary(IWorkspaceManager.class);
								
								progress.setTaskName("Setting up eclipse client - creating repository workspace " + repositoryWorkspace);
								boolean foundStream = false;
								IWorkspaceHandle workspace = null;
								IWorkspaceHandle streamHandle = null;
								IWorkspaceConnection stream = null;
								while (!foundStream) {
									try {
										IWorkspaceSearchCriteria criteria = IWorkspaceSearchCriteria.FACTORY.newInstance();
										criteria.setExactName(streamName);
										criteria.setKind(IWorkspaceSearchCriteria.STREAMS);
										List<IWorkspaceHandle> workspaces = wksmgr.findWorkspaces(criteria, 1, progress.newChild(1));
										if (workspaces.size() > 0) {
											foundStream = true;
											streamHandle = workspaces.get(0);
											stream = wksmgr.getWorkspaceConnection(streamHandle, progress.newChild(1));
										} else {
											Thread.sleep(30000);
										}
									} catch (FileSystemException e) {
										ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
										repo.logout();
										Thread.sleep(30000);
										repo.login(progress.newChild(1));
										ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
									} catch (TeamRepositoryException e) {
										ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
										repo.logout();
										Thread.sleep(30000);
										repo.login(progress.newChild(1));
										ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
									} catch (Exception e) {
										ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
										repo.logout();
										Thread.sleep(30000);
										repo.login(progress.newChild(1));
										ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
									}
								}
								
								{
									boolean success = false;
									while (!success) {
										try {
											IWorkspaceSearchCriteria criteria = IWorkspaceSearchCriteria.FACTORY.newInstance();
											criteria.setExactName(repositoryWorkspace);
											criteria.setKind(IWorkspaceSearchCriteria.WORKSPACES);
											List<IWorkspaceHandle> workspaces = wksmgr.findWorkspaces(criteria, 1, progress.newChild(1));
											if (workspaces.size() > 0) {
												workspace = workspaces.get(0);
											} else {
												// Create the repository workspace
												workspace = wksmgr.createWorkspace(repo.loggedInContributor(), repositoryWorkspace, "", stream, stream, progress.newChild(10)).getResolvedWorkspace();
											}
											success = true;
										} catch (FileSystemException e) {
											ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
											repo.logout();
											Thread.sleep(30000);
											repo.login(progress.newChild(1));
											ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
											stream = wksmgr.getWorkspaceConnection(streamHandle, progress.newChild(1));
										} catch (TeamRepositoryException e) {
											ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
											repo.logout();
											Thread.sleep(30000);
											repo.login(progress.newChild(1));
											ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
											stream = wksmgr.getWorkspaceConnection(streamHandle, progress.newChild(1));
										} catch (Exception e) {
											ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
											repo.logout();
											Thread.sleep(30000);
											repo.login(progress.newChild(1));
											ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
											stream = wksmgr.getWorkspaceConnection(streamHandle, progress.newChild(1));
										}
									}
								}
								
								progress.setTaskName("Setting up eclipse client - loading repository workspace " + repositoryWorkspace + " once it is available");
								boolean loadedRepositoryWorkspace = false;
								while (!loadedRepositoryWorkspace) {
									try {
										IWorkspaceConnection workspaceConnection = wksmgr.getWorkspaceConnection(workspace, progress.newChild(1));
										List<IComponentizedAncestorList> projectAncestors = workspaceConnection.configuration().fetchAncestorsByName(".project", progress.newChild(1));
										
										ILoadOperation loadOp = IOperationFactory.instance.getLoadOperation(LoadDilemmaHandler.getDefault());
										ISandbox workspaceSandbox = SharingManager.getInstance().getSandbox(new PathLocation(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString()), false);
										
										loadOp.setEclipseSpecificLoadOptions(ILoadOperation.IMPORT_PROJECTS);
										for (IComponentizedAncestorList ancestors: projectAncestors) {
											List<IVersionableHandle> handlesToLoad = new ArrayList<IVersionableHandle>(ancestors.getAncestorReports().size());
											for (IAncestorReport report: ancestors.getAncestorReports().values()) {
												@SuppressWarnings("unchecked")
												List<NameItemPair> pairs = report.getNameItemPairs();
												if (pairs.size() > 1) {
													NameItemPair projectParentFolder = pairs.get(pairs.size()-2);
													handlesToLoad.add(projectParentFolder.getItem());
												}
											}
											
											loadOp.requestLoad(workspaceSandbox, null, workspaceConnection, ancestors.getComponent(), handlesToLoad);
										}
										
										loadOp.run(progress.newChild(30));
										loadedRepositoryWorkspace = true;
									} catch (FileSystemException e) {
										ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
										repo.logout();
										Thread.sleep(30000);
										repo.login(progress.newChild(1));
										ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
									} catch (TeamRepositoryException e) {
										ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
										repo.logout();
										Thread.sleep(30000);
										repo.login(progress.newChild(1));
										ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
									} catch (Exception e) {
										ConnectedProjectAreaRegistry.getDefault().removeConnectedProjectAreas(Collections.singletonList(projectArea));
										repo.logout();
										Thread.sleep(30000);
										repo.login(progress.newChild(1));
										ConnectedProjectAreaRegistry.getDefault().addConnectedProjectArea(projectArea);
									}
								}								
							} catch (TeamRepositoryException e) {
								e.printStackTrace();
							}
						}
					});
					
					// Open the pending changes view and team artifacts views
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("com.ibm.team.process.rcp.ui.teamArtifactsNavigator"); //$NON-NLS-1$
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("com.ibm.team.filesystem.ui.changes.views.LocalWorkspaceChangesView"); //$NON-NLS-1$

					// Close the welcome window
			       final IIntroManager introManager = PlatformUI.getWorkbench().getIntroManager();
			       IIntroPart part = introManager.getIntro();
			       if (part != null) {
			    	   introManager.closeIntro(part);
			       }
				} catch (InvocationTargetException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
			
		});
		
	}

}
