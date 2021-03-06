/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * Author: Lukas Degener (among others)
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2004-2012, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 ****************************************************************************/

package org.cs3.pdt.internal.actions;

import static org.cs3.pdt.common.search.SearchConstants.RESULT_KIND_DYNAMIC;
import static org.cs3.pdt.common.search.SearchConstants.RESULT_KIND_FOREIGN;
import static org.cs3.pdt.common.search.SearchConstants.RESULT_KIND_MULTIFILE;
import static org.cs3.prolog.common.QueryUtils.bT;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.cs3.pdt.PDT;
import org.cs3.pdt.common.PDTCommonPredicates;
import org.cs3.pdt.common.PDTCommonUtil;
import org.cs3.pdt.common.metadata.Goal;
import org.cs3.pdt.common.metadata.SourceLocation;
import org.cs3.pdt.internal.editors.PLEditor;
import org.cs3.prolog.common.Util;
import org.cs3.prolog.common.logging.Debug;
import org.cs3.prolog.pif.PrologInterfaceException;
import org.cs3.prolog.session.PrologSession;
import org.cs3.prolog.ui.util.UIUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * @see IWorkbenchWindowActionDelegate
 */
public class FindPredicateActionDelegate extends TextEditorAction {
	
	private ITextEditor editor;

	/**
	 *  
	 */
	public FindPredicateActionDelegate(ITextEditor editor) {
		super(ResourceBundle.getBundle(PDT.RES_BUNDLE_UI), FindPredicateActionDelegate.class.getName(), editor);
		this.editor = editor;

	}

	/**
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run() {
		try {
			final Goal goal = ((PLEditor) editor).getSelectedPrologElement();
			Shell shell = editor.getEditorSite().getShell();
			if (goal == null) {

				UIUtils.displayMessageDialog(shell, "PDT Plugin", "Cannot locate a predicate at the specified location.");
				return;
			}
			final IFile file = UIUtils.getFileInActiveEditor();
			if (file == null) {
				// UIUtils.logAndDisplayError(PDTPlugin.getDefault().getErrorMessageProvider(),
				// shell,
				// PDT.ERR_NO_ACTIVE_FILE, PDT.CX_FIND_PREDICATE, null);
			}

			Job j = new Job("Searching predicate definition") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						monitor.beginTask("searching...", IProgressMonitor.UNKNOWN);

						run_impl(goal, file);

					} catch (Throwable e) {
						Debug.report(e);

					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}

			};
			j.schedule();
		} catch (Throwable t) {
			Debug.report(t);
		}

	}

	public void dispose() {
	}

	private static class SourceLocationAndResultKind {
		SourceLocation location;
		String resultKind;

		SourceLocationAndResultKind(SourceLocation location, String resultKind) {
			this.location = location;
			this.resultKind = resultKind;
		}
	}

	private void run_impl(final Goal goal, IFile file) throws CoreException {
		PrologSession session = null;
		try {
			session = PDTCommonUtil.getActivePrologInterface().getSession();
			SourceLocationAndResultKind res = findFirstClauseLocation(goal, session);
			if (res != null) {
				if (res.location != null) {
					PDTCommonUtil.showSourceLocation(res.location);
					if (RESULT_KIND_MULTIFILE.equals(res.resultKind)) {
						new FindDefinitionsActionDelegate(editor).run();
					}
				} else {
					if (RESULT_KIND_DYNAMIC.equals(res.resultKind)) {
						UIUtils.displayMessageDialog(
								editor.getSite().getShell(),
								"Dynamic predicate declared in user",
								"There is no Prolog source code for this predicate.");
						return;
					} else if (RESULT_KIND_FOREIGN.equals(res.resultKind)) {
						UIUtils.displayMessageDialog(
								editor.getSite().getShell(),
								"External language predicate",
								"There is no Prolog source code for this predicate (only compiled external language code).");
						return;
					}
				}
			} else {
				if (!"lgt".equals(file.getFileExtension())) {
					final List<Map<String, Object>> result = session.queryAll(bT(PDTCommonPredicates.FIND_ALTERNATIVE_PREDICATES, Util.quoteAtom(UIUtils.prologFileName(file)), Util.quoteAtom(goal.getTermString()), "RefModule", "RefName", "RefArity", "RefFile", "RefLine"));
					if (result.isEmpty()) {
						UIUtils.displayMessageDialog(
								editor.getSite().getShell(),
								"Undefined predicate",
								"The selected predicate is not defined.");
						return;
					} else {
						editor.getEditorSite().getShell().getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								AlternativeDialog alternativeDialog = new AlternativeDialog(editor.getEditorSite().getShell(), goal, result);
								alternativeDialog.setBlockOnOpen(false);
								alternativeDialog.open();
							}
						});
					}
				} else {
					UIUtils.displayMessageDialog(
							editor.getSite().getShell(),
							"Undefined predicate",
							"The selected predicate is not defined.");
					return;
				}
			}
			return;
		} catch (Exception e) {
			Debug.report(e);
		} finally {
			if (session != null)
				session.dispose();
		}
//		UIUtils.getDisplay().asyncExec(new Runnable() {
//
//			@Override
//			public void run() {
//				MessageBox messageBox = new MessageBox(UIUtils.getActiveShell(), SWT.ICON_WARNING | SWT.OK);
//
//				messageBox.setText("Open Declaration");
//				messageBox.setMessage("Cannot open declaration. No active Prolog Console available for a fallback.");
//				messageBox.open();
//			}
//		});

	}

	private SourceLocationAndResultKind findFirstClauseLocation(Goal goal, PrologSession session) throws PrologInterfaceException {
		// TODO: Schon im goal definiert. M�sste nur noch dort gesetzt werden:
		String enclFile = UIUtils.getFileFromActiveEditor();
		// TODO: if (enclFile==null) ... Fehlermeldung + Abbruch ...

		// Folgendes liefert bei Prolog-Libraries die falschen Ergebnisse,
		// obwohl das aufgerufene Pr�dikat das Richtige tut, wenn es direkt
		// in einem Prolog-Prozess aufgerufen wird:
		// if(goal.getModule()==null) {
		// String query = "module_of_file('" + enclFile + "',Module)";
		// String referencedModule = (String)
		// session.queryOnce(query).get("Module");
		// goal.setModule(referencedModule);
		// }
		// In der Klasse DefinitionsSearchQuery funktioniert es aber!

		String module = "_";
		if (goal.getModule() != null) {
			module = Util.quoteAtomIfNeeded(goal.getModule());
		}

		String term = goal.getTermString();
		String quotedTerm = Util.quoteAtom(term);

		String query = bT(PDTCommonPredicates.FIND_PRIMARY_DEFINITION_VISIBLE_IN, Util.quoteAtom(enclFile), goal.getLine(), quotedTerm, module, "File", "Line", "ResultKind");
		Debug.info("open declaration: " + query);
		Map<String, Object> clause = session.queryOnce(query);
		if (clause == null) {
			return null;
		}
		String resultKind = clause.get("ResultKind").toString();
		if (RESULT_KIND_FOREIGN.equals(resultKind) || RESULT_KIND_DYNAMIC.equals(resultKind)) {
			return new SourceLocationAndResultKind(null, resultKind);
		}
		
		if (clause.get("File") == null) {
			throw new RuntimeException("Cannot resolve file for primary declaration of " + quotedTerm);
		}
		SourceLocation location = new SourceLocation((String) clause.get("File"), false);
		location.setLine(Integer.parseInt((String) clause.get("Line")));
		return new SourceLocationAndResultKind(location, resultKind);
	}
	
	private static class AlternativeDialog extends Dialog {

		private List<Map<String, Object>> alternatives;
		private org.eclipse.swt.widgets.List list;
		private Goal goal;

		protected AlternativeDialog(Shell parentShell, Goal goal, List<Map<String, Object>> alternatives) {
			super(parentShell);
			setShellStyle(getShellStyle() | SWT.RESIZE);
			this.alternatives = alternatives;
			this.goal = goal;
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite = (Composite) super.createDialogArea(parent);
			
			Label label = new Label(composite, SWT.WRAP);
			label.setText("The selected predicate " + goal.getSignature() + " was not found. A list of similar predicates is listed below.\n" +
					"Select a predicate and press OK to jump to it.");
			
		    GridData gridData = new GridData();
		    gridData.grabExcessHorizontalSpace = true;
		    gridData.horizontalAlignment = GridData.FILL;
		    gridData.heightHint = convertHeightInCharsToPixels(3);
		    
		    label.setLayoutData(gridData);
			
			list = new org.eclipse.swt.widgets.List(composite, SWT.NONE);
			for (Map<String, Object> alternative : alternatives) {
				list.add(getTextForPred(alternative));
			}
			list.setSelection(0);
			
		    gridData = new GridData();
		    gridData.grabExcessHorizontalSpace = true;
		    gridData.horizontalAlignment = GridData.FILL;
		    gridData.grabExcessVerticalSpace = true;
		    gridData.verticalAlignment = GridData.FILL;
		    
		    list.setLayoutData(gridData);

			return composite;
		}
		
		private String getTextForPred(Map<String, Object> predicate) {
			StringBuffer buf = new StringBuffer();
			buf.append(predicate.get("RefModule"));
			buf.append(":");
			buf.append(predicate.get("RefName"));
			buf.append("/");
			buf.append(predicate.get("RefArity"));
			if ("-1".equals(predicate.get("RefLine"))) {
				buf.append(" (no source)");
			}
			return buf.toString();
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Undefined predicate");
		}
		
		@Override
		protected Point getInitialSize() {
			return new Point(400, 300);
		}
		
		@Override
		protected void okPressed() {
			int selection = list.getSelectionIndex();
			if (selection >= 0) {
				Map<String, Object> predicate = alternatives.get(selection);
				if (!"-1".equals(predicate.get("RefLine"))) {
					try {
						PDTCommonUtil.selectInEditor(Integer.parseInt(predicate.get("RefLine").toString()), predicate.get("RefFile").toString(), true);
					} catch (PartInitException e) {
						Debug.report(e);
					} catch (NumberFormatException e) {
						Debug.report(e);
					}
				}
			}
			super.okPressed();
		}

	}
	
}



