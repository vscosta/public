/*
 */
package org.cs3.pdt.internal.natures;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


import org.cs3.pdt.IPrologProject;
import org.cs3.pdt.PDT;
import org.cs3.pdt.PDTPlugin;
import org.cs3.pl.common.Debug;
import org.cs3.pl.prolog.PrologInterface;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 */
public class PrologProjectNature implements IProjectNature, IPrologProject {

	private IProject project;

	/**
	 * @see IProjectNature#configure
	 */
	public void configure() throws CoreException {
		try {
			Debug.debug("configure was called");
			IProjectDescription descr = project.getDescription();
			ICommand builder = descr.newCommand();
			builder.setBuilderName(PDT.BUILDER_ID);
			ICommand builders[] = descr.getBuildSpec();
			for (int i = 0; i < builders.length; i++) {
				if (builders[i].getBuilderName().equals(PDT.BUILDER_ID)) {
					return;
				}
			}
			ICommand newBuilders[] = new ICommand[builders.length + 1];
			System.arraycopy(builders, 0, newBuilders, 0, builders.length);
			newBuilders[builders.length] = builder;
			descr.setBuildSpec(newBuilders);
			project.setDescription(descr, null);
			
			
			PrologInterface pif = getPrologInterface();
			if (pif.isUp()) {
				Job j = new Job("building Prolog Metadata for project " + project.getName()) {
					protected IStatus run(IProgressMonitor monitor) {
						try {
							IWorkspaceDescription wd = ResourcesPlugin
									.getWorkspace().getDescription();
							if (wd.isAutoBuilding()) {
								project.build(IncrementalProjectBuilder.FULL_BUILD,
										PDT.BUILDER_ID,null,
										monitor);
							}
						} catch (OperationCanceledException opc) {
							return Status.CANCEL_STATUS;
						} catch (CoreException e) {
							return new Status(Status.ERROR, PDT.PLUGIN_ID, -1,
									"exception caught during build", e);
						}
						return Status.OK_STATUS;
					}

					public boolean belongsTo(Object family) {
						return family == ResourcesPlugin.FAMILY_MANUAL_BUILD;
					}

				};				
				j.schedule();
			} else {
				// if the pif is not up yet, this is no problem at all: the reload
				// hook will
				// take care of the due build in its afterInit method.
				;
			}
			
		} catch (Throwable t) {
			Debug.report(t);
			throw new RuntimeException(t);
		}
	}

	/**
	 * @see IProjectNature#deconfigure
	 */
	public void deconfigure() throws CoreException {
		try {
			IProjectDescription descr = project.getProject().getDescription();
			org.cs3.pl.common.Debug.debug("deconfigure was called");
			ICommand builders[] = descr.getBuildSpec();
			int index = -1;
			for (int i = 0; i < builders.length; i++) {
				if (builders[i].getBuilderName().equals(PDT.BUILDER_ID)) {
					index = i;
					break;
				}
			}
			if (index != -1) {
				ICommand newBuilders[] = new ICommand[builders.length - 1];
				System.arraycopy(builders, 0, newBuilders, 0, index);
				System.arraycopy(builders, index + 1, newBuilders, index,
						builders.length - index - 1);
				descr.setBuildSpec(newBuilders);
			}
		} catch (Throwable t) {
			Debug.report(t);
			throw new RuntimeException(t);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return this.project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.IPrologProject#getSourcePath()
	 */
	public String getSourcePath() throws CoreException {
		String val = null;
		val = getProject().getPersistentProperty(
				new QualifiedName("", PDT.PROP_SOURCE_PATH));
		if (val == null) {
			val = PDTPlugin.getDefault().getPreferenceValue(
					PDT.PREF_SOURCE_PATH_DEFAULT, "");
		}
		return val;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.IPrologProject#setSourcePath(java.lang.String)
	 */
	public void setSourcePath(String path) throws CoreException {
		getProject().setPersistentProperty(
				new QualifiedName("", PDT.PROP_SOURCE_PATH), path);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.IPrologProject#getExistingSourcePathEntries()
	 */
	public Set getExistingSourcePathEntries() throws CoreException {
		Set r = new HashSet();
		String[] elms = getSourcePath().split(
				System.getProperty("path.separator"));
		for (int i = 0; i < elms.length; i++) {
			IProject p = getProject();

			if (elms[i] == null || "".equals(elms[i])
					|| File.separator.equals(elms[i])
					|| "/".equals(elms[i].trim())) {
				r.add(p);
			} else {
				IFolder folder = p.getFolder(elms[i]);
				if (folder.exists()) {
					r.add(folder);
				}
			}
		}
		return r;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.IPrologProject#isPrologSource(org.eclipse.core.resources.IResource)
	 */
	public boolean isPrologSource(IResource resource) throws CoreException {
		Set sourcePathEntries = getExistingSourcePathEntries();

		if (!resource.exists()) {
			return false;
		}
		if (sourcePathEntries.contains(resource)) {
			return true;
		}
		if (resource.getType() == IResource.FILE) {
			String ext = resource.getFileExtension();
			return ext != null && ext.equals("pl")
					&& isPrologSource(resource.getParent());
		} else if (resource.getType() == IResource.FOLDER) {
			return isPrologSource(resource.getParent());
		}
		return false;
	}

	public void setAutoConsulted(IFile file, boolean val) throws CoreException {
		file.setPersistentProperty(
		// TODO: toggled functionality - to test
				new QualifiedName("", PDT.PROP_NO_AUTO_CONSULT), val ? "false"
						: "true");
	}

	public boolean isAutoConsulted(IFile file) throws CoreException {
		// if it is no source file, there is no need to consult it.
		if (!isPrologSource(file)) {
			return false;
		}

		// if the "master switch" says "no auto-consult", then there is no
		// auto-consult.
		String val = PDTPlugin.getDefault().getPreferenceValue(
				PDT.PREF_AUTO_CONSULT, "false");
		if ("false".equalsIgnoreCase(val)) {
			return false;
		}

		// finally, if auto-consult is enabled, and the file is not explicitly
		// excluded, then auto-consult it.
		val = file.getPersistentProperty(new QualifiedName("",
				PDT.PROP_NO_AUTO_CONSULT));
		// TODO: toggled functionality - to test
		boolean autoConsult = !(val != null && val.equalsIgnoreCase("true"));
		return autoConsult;
	}

	public PrologInterface getPrologInterface() {
		PrologInterface pif = PDTPlugin.getDefault().getPrologInterface(
				getProject().getName());

		if (!pif.isUp()) {
			try {
				pif.start();
			} catch (IOException e) {
				Debug.report(e);
				throw new RuntimeException(e);
			}
		}		
		
		return pif;
	}

}
