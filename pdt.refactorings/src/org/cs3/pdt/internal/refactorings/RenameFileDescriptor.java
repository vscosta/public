/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2004-2012, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 ****************************************************************************/

package org.cs3.pdt.internal.refactorings;

import java.util.Map;

import org.cs3.pdt.transform.PrologRefactoringDescriptor;
import org.cs3.prolog.common.Option;
import org.cs3.prolog.common.SimpleOption;
import org.cs3.prolog.common.Util;
import org.cs3.prolog.pif.PrologInterface;
import org.cs3.prolog.ui.util.UIUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

public class RenameFileDescriptor extends PrologRefactoringDescriptor{
	
	
	@Override
	public String getParametersTerm(Map<String, String> parameterValues) {

		String newName = parameterValues.containsKey("NewName") ? "'"+parameterValues.get("NewName")+"'" : "NewName";
		String file = parameterValues.containsKey("File") ? parameterValues.get("File") : "File";
		return "params("+file+","+newName+")";
	}

	
	@Override
	public String getSelectionTerm(ISelection selection,
			IWorkbenchPart activePart) {

		IFile file = selectedFile(selection, activePart);
		if(file==null){
			return null;
		}
		return "file('"+Util.prologFileName(file.getLocation().toFile())+"')";
	}

	private IFile selectedFile(ISelection selection, IWorkbenchPart activePart) {
		if(activePart.getSite().getId().equals("org.cs3.pdt.internal.editors.PLEditor")){
			return UIUtils.getFileInActiveEditor();
		}
		IFile file=null;
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection)
					.getFirstElement();
			if (obj instanceof IFile) {
				file = (IFile) obj;
			} else if (obj instanceof IAdaptable) {
				IAdaptable a = (IAdaptable) obj;
				IFile r = (IFile) a.getAdapter(IFile.class);
				if (r != null
						&& (IResource.FILE == r.getType() )) {
					file = r;
				}
			}
		}
		return file;
	}

	@Override
	public Option[] getParameters(ISelection selection,
			IWorkbenchPart activePart) {
		
		return new Option[] {

				new SimpleOption("File", "Id of the file to rename.", "", Option.NUMBER, null) {
					
					@Override
					public boolean isVisible() {
						return false;
					}
					
					@Override
					public boolean isEditable() {
						return false;
					}
				},
				new SimpleOption("NewName", "New Name", "The new file name.",
						Option.STRING, null) };
	}
	
	
	@Override
	public PrologInterface getPrologInterface(ISelection selection,IWorkbenchPart activePart) throws CoreException{
		IFile file = selectedFile(selection, activePart);
		return PDTCoreUtils.getPrologProject(file).getMetadataPrologInterface();
		
	}
}


