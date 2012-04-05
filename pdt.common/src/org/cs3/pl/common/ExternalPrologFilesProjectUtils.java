package org.cs3.pl.common;

import java.io.IOException;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ExternalPrologFilesProjectUtils {
	
	private static IProject externalPrologFilesProject;
	private static final String name = "External Prolog Files";
	
	public static IProject getExternalPrologFilesProject() throws CoreException {
		if (externalPrologFilesProject == null) {
			externalPrologFilesProject = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (!externalPrologFilesProject.exists()) {
				externalPrologFilesProject.create(null);
			}
			if (!externalPrologFilesProject.isOpen()) {
				externalPrologFilesProject.open(null);
			}
		}
		return externalPrologFilesProject;
	}
	
	public static IFile linkFile(String fileName) throws CoreException {
		return linkFile(new Path(fileName));
	}

	public static IFile linkFile(IPath path) throws CoreException {
		if (!path.toFile().exists()) {
			return null;
		}
		IProject project = getExternalPrologFilesProject();
		IPath pathWithoutDevice;
		try {
			pathWithoutDevice = new Path(path.toFile().getCanonicalPath()).setDevice(null);
		} catch (IOException e) {
			pathWithoutDevice = path.setDevice(null);
		}
		IFile file = project.getFile(pathWithoutDevice);
		if (!file.exists()) {
			createParentsIfNecessary(file);
			file.createLink(path, IResource.BACKGROUND_REFRESH, null);
		}
		return file;
	}
	
	private static void createParentsIfNecessary(IFile folder) {
		IResource parent = folder.getParent();
		Stack<IResource> nonExistentParents = new Stack<IResource>();
		while (!parent.exists()) {
			nonExistentParents.push(parent);
			parent = parent.getParent();
		}
		if (nonExistentParents.size() > 0) {
			while (!nonExistentParents.isEmpty()) {
				IResource element = nonExistentParents.pop();
				if (element instanceof IFolder) {
					try {
						((IFolder) element).create(true, true, null);
					} catch (CoreException e) {
					}
				}
			}
		}
	}
	
	public static boolean isExternalFile(IFile file) {
		IProject project;
		try {
			project = getExternalPrologFilesProject();
		} catch (CoreException e) {
			return false;
		}
		return file.getProject().equals(project);
	}
}