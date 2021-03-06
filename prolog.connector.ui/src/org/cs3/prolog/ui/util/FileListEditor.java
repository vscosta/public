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
/*******************************************************************************
 * ld: 
 * most of this file was copied from the swt source. 
 * So i left the following Copyright notice intact.  
 */
/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.cs3.prolog.ui.util;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * An abstract field editor that manages a list of input values. 
 * The editor displays a list containing the values, buttons for
 * adding and removing values, and Up and Down buttons to adjust
 * the order of elements in the list.
 * <p>
 * Subclasses must implement the <code>parseString</code>,
 * <code>createList</code>, and <code>getNewInputObject</code>
 * framework methods.
 * </p>
 */
public  class FileListEditor extends FieldEditor{

	private List list;
	private Composite buttonBox;
	private Button addDirButton;
	private Button addFileButton;
	private Button removeButton;
	private Button upButton;
	private Button downButton;
	private SelectionListener selectionListener;
    private String addFileButtonLabel = "add File...";
    private String addDirButtonLabel = "add Directory...";
    private String dirChooserLabelText;
    private String lastPath;
    private boolean dirEndsWithSeparator=true;
    private String fileChooserLabelText;
    private String lastFilePath;
    private String[] filterExtensions;
	private IContainer rootContainer;
	private boolean workspaceResource;
/**
 * Creates a new list field editor 
 */
protected FileListEditor() {
}
/**
 * Creates a list field editor.
 * 
 * @param name the name of the preference this field editor works on
 * @param labelText the label text of the field editor
 * @param parent the parent of the field editor's control
 */
protected FileListEditor(String name, String labelText, String fileButtonLabel,Composite parent) {
    setAddFileButtonLabel(fileButtonLabel);
	init(name, labelText);
	//createControl(parent);
}
/**
 * Notifies that the Add button has been pressed.
 */
private void addDirPressed() {
	setPresentsDefaultValue(false);
	if(isWorkspaceResource()){
		Object[] input = getNewWsDir();
		//String[] items = new String[input.length];
		for (int i = 0; i < input.length; i++) {
			IPath p = (IPath) input[i];
			IPath root = getRootContainer().getFullPath();			
			p=p.removeFirstSegments(root.matchingFirstSegments(p));
			addPath(p.toPortableString());
		}
		//list.setItems(items);
		//selectionChanged();
	}else{
		String input = getNewDir();
		addPath(input);
	}

	
}
private boolean isWorkspaceResource() {
	return this.workspaceResource;
}
private void addPath(String input) {
	if (input != null) {
		int index = list.getSelectionIndex();
		if (index >= 0)
			list.add(input, index + 1);
		else
			list.add(input, 0);
		selectionChanged();
	}
}

/**
 * 
 */
protected void addFilePressed() {
    setPresentsDefaultValue(false);
	String input = getNewFile();

	addPath(input);
  
}


/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
@Override
protected void adjustForNumColumns(int numColumns) {
	Control control = getLabelControl();
	((GridData)control.getLayoutData()).horizontalSpan = numColumns;
	((GridData)list.getLayoutData()).horizontalSpan = numColumns - 1;
}
/**
 * Creates the Add, Remove, Up, and Down button in the given button box.
 *
 * @param box the box for the buttons
 */
private void createButtons(Composite box) {
	addDirButton = createPushButton(box, "HACK.addDir");//$NON-NLS-1$
	addFileButton = createPushButton(box, "HACK.addFile");//$NON-NLS-1$
	removeButton = createPushButton(box, "ListEditor.remove");//$NON-NLS-1$
	upButton = createPushButton(box, "ListEditor.up");//$NON-NLS-1$
	downButton = createPushButton(box, "ListEditor.down");//$NON-NLS-1$
}
/**
 * Combines the given list of items into a single string.
 * This method is the converse of <code>parseString</code>. 
 * <p>
 * Subclasses must implement this method.
 * </p>
 *
 * @param items the list of items
 * @return the combined string
 * @see #parseString
 */
protected  String createList(String[] items){
    	StringBuffer sb = new StringBuffer();
    	for (int i = 0; i < items.length; i++) {
            if(i>0){
                sb.append(File.pathSeparatorChar);
            }
            sb.append(items[i]);
        }
    	return sb.toString();
    }
/**
 * Helper method to create a push button.
 * 
 * @param parent the parent control
 * @param key the resource name used to supply the button's label text
 * @return Button
 */
private Button createPushButton(Composite parent, String key) {
	Button button = new Button(parent, SWT.PUSH);
	if(key.equals("HACK.addFile")){
	    button.setText(addFileButtonLabel);
	}
	else if(key.equals("HACK.addDir")){
	    button.setText(addDirButtonLabel);
	}
	else{
	    button.setText(JFaceResources.getString(key));
	}
	button.setFont(parent.getFont());
	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
	data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	button.setLayoutData(data);
	button.addSelectionListener(getSelectionListener());
	return button;
}
/**
 * Creates a selection listener.
 */
public void createSelectionListener() {
	selectionListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			Widget widget = event.widget;
			if (widget == addFileButton) {
				addFilePressed();
			} else
			if (widget == addDirButton) {
				addDirPressed();
			} else
				if (widget == removeButton) {
					removePressed();
				} else
					if (widget == upButton) {
						upPressed();
					} else
						if (widget == downButton) {
							downPressed();
						} else
							if (widget == list) {
								selectionChanged();
							}
		}
	};
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
@Override
protected void doFillIntoGrid(Composite parent, int numColumns) {
	Control control = getLabelControl(parent);
	GridData gd = new GridData();
	gd.horizontalSpan = numColumns;
	control.setLayoutData(gd);

	list = getListControl(parent);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.verticalAlignment = GridData.FILL;
	gd.horizontalSpan = numColumns - 1;
	gd.grabExcessHorizontalSpace = true;
	list.setLayoutData(gd);

	buttonBox = getButtonBoxControl(parent);
	gd = new GridData();
	gd.verticalAlignment = GridData.BEGINNING;
	buttonBox.setLayoutData(gd);
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
@Override
protected void doLoad() {
//	if (list != null) {
//		String s = getPreferenceStore().getString(getPreferenceName());
//		String[] array = parseString(s);
//		for (int i = 0; i < array.length; i++){
//			list.add(array[i]);
//		}
//	}
}


public void setValue(String value) {
	if (list != null) {
		list.removeAll();
		String[] array = parseString(value);
		for (int i = 0; i < array.length; i++){
			list.add(array[i]);
		}
	}

}

/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
@Override
protected void doLoadDefault() {
	if (list != null) {
		list.removeAll();
		String s = getPreferenceStore().getDefaultString(getPreferenceName());
		String[] array = parseString(s);
		for (int i = 0; i < array.length; i++){
			list.add(array[i]);
		}
	}
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
@Override
protected void doStore() {
//	String s = createList(list.getItems());
//	if (s != null)
//		getPreferenceStore().setValue(getPreferenceName(), s);
}

public String getValue() {
	String s = createList(list.getItems());
	return s==null?"":s;
}

/**
 * Notifies that the Down button has been pressed.
 */
private void downPressed() {
	swap(false);
}
/**
 * Returns this field editor's button box containing the Add, Remove,
 * Up, and Down button.
 *
 * @param parent the parent control
 * @return the button box
 */
public Composite getButtonBoxControl(Composite parent) {
	if (buttonBox == null) {
		buttonBox = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		buttonBox.setLayout(layout);
		createButtons(buttonBox);
		buttonBox.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent event) {
				addDirButton = null;
				addFileButton = null;
				removeButton = null;
				upButton = null;
				downButton = null;
				buttonBox = null;
			}
		});

	} else {
		checkParent(buttonBox, parent);
	}

	selectionChanged();
	return buttonBox;
}
/**
 * Returns this field editor's list control.
 *
 * @param parent the parent control
 * @return the list control
 */
public List getListControl(Composite parent) {
	if (list == null) {
		list = new List(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		list.setFont(parent.getFont());
		list.addSelectionListener(getSelectionListener());
		list.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent event) {
				list = null;
			}
		});
	} else {
		checkParent(list, parent);
	}
	return list;
}
/**
 * Creates and returns a new item for the list.
 * <p>
 * Subclasses must implement this method.
 * </p>
 *
 * @return a new item
 */
protected  String getNewDir(){

	DirectoryDialog dialog = new DirectoryDialog(getShell());
	if (dirChooserLabelText != null)
		dialog.setMessage(dirChooserLabelText);
	if (lastPath != null) {
		if (new File(lastPath).exists())
			dialog.setFilterPath(lastPath);
	}
	String dir = dialog.open();
	if (dir != null) {
		dir = dir.trim();
		if (dir.length() == 0)
			return null;
		if(dirEndsWithSeparator&& !dir.endsWith(File.separator)){
		    dir+=File.separator;
		}
		else if(!dirEndsWithSeparator&& dir.endsWith(File.separator)){
		    dir=dir.substring(0,dir.length()-1);
		}
		lastPath = dir;
	}
	return dir;
}


/**
 * Creates and returns a new item for the list.
 * <p>
 * Subclasses must implement this method.
 * </p>
 *
 * @return a new item
 */
protected  Object[] getNewWsDir(){
	//ContainerSelectionDialog dialog = new ContainerSelectionDialog()
	IContainer root = getRootContainer();
	root=root==null?ResourcesPlugin.getWorkspace().getRoot():root;
	ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),root,true,getDirChooserLabelText());
	
	//DirectoryDialog dialog = new DirectoryDialog(getShell());
	if (dirChooserLabelText != null){
		dialog.setMessage(dirChooserLabelText);
	}
//	String[] items = list.getItems();
//	Vector v = new Vector();
//	for (int i = 0; i < items.length; i++) {
//		String item = items[i];
//		v.add(root.getFullPath().append(item));
//	}
//	dialog.setInitialElementSelections(v);
	dialog.setBlockOnOpen(true);
	
	
	dialog.open();
	Object[] result = dialog.getResult();
	
	return result;
}


private IContainer getRootContainer() {
	return this.rootContainer;
}
/**
 * @return
 */
private String getNewFile() {

	FileDialog dialog = new FileDialog(getShell(),SWT.OPEN);
	if(filterExtensions!=null){
	    dialog.setFilterExtensions(filterExtensions);
	}
	if (fileChooserLabelText != null)
	    dialog.setText(fileChooserLabelText);		
	if (lastFilePath != null) {
		if (new File(lastFilePath).exists())
			dialog.setFilterPath(lastFilePath);
	}
	String file = dialog.open();
	if (file != null) {
		file = file.trim();
		if (file.length() == 0)
			return null;
		
		lastFilePath = file.substring(0,file.lastIndexOf(File.separator));
	}
	return file;

}

/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
@Override
public int getNumberOfControls() {
	return 2;
}
/**
 * Returns this field editor's selection listener.
 * The listener is created if nessessary.
 *
 * @return the selection listener
 */
private SelectionListener getSelectionListener() {
	if (selectionListener == null)
		createSelectionListener();
	return selectionListener;
}
/**
 * Returns this field editor's shell.
 * <p>
 * This method is internal to the framework; subclassers should not call
 * this method.
 * </p>
 *
 * @return the shell
 */
protected Shell getShell() {
	if (addDirButton == null)
		return null;
	return addDirButton.getShell();
}
/**
 * Splits the given string into a list of strings.
 * This method is the converse of <code>createList</code>. 
 * <p>
 * Subclasses must implement this method.
 * </p>
 *
 * @param stringList the string
 * @return an array of <code>String</code>
 * @see #createList
 */
protected String[] parseString(String stringList){
    return stringList.split(File.pathSeparator);
}
/**
 * Notifies that the Remove button has been pressed.
 */
private void removePressed() {
	setPresentsDefaultValue(false);
	int index = list.getSelectionIndex();
	if (index >= 0) {
		list.remove(index);
		selectionChanged();
	}
}
/**
 * Notifies that the list selection has changed.
 */
private void selectionChanged() {

	int index = list.getSelectionIndex();
	int size = list.getItemCount();

	removeButton.setEnabled(index >= 0);
	upButton.setEnabled(size > 1 && index > 0);
	downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
}
/* (non-Javadoc)
 * Method declared on FieldEditor.
 */
@Override
public void setFocus() {
	if (list != null) {
		list.setFocus();
	}
}
/**
 * Moves the currently selected item up or down.
 *
 * @param up <code>true</code> if the item should move up,
 *  and <code>false</code> if it should move down
 */
private void swap(boolean up) {
	setPresentsDefaultValue(false);
	int index = list.getSelectionIndex();
	int target = up ? index - 1 : index + 1;

	if (index >= 0) {
		String[] selection = list.getSelection();
		Assert.isTrue(selection.length == 1);
		list.remove(index);
		list.add(selection[0], target);
		list.setSelection(target);
	}
	selectionChanged();
}
/**
 * Notifies that the Up button has been pressed.
 */
private void upPressed() {
	swap(true);
}

/*
 * @see FieldEditor.setEnabled(boolean,Composite).
 */
@Override
public void setEnabled(boolean enabled, Composite parent){
	super.setEnabled(enabled,parent);
	getListControl(parent).setEnabled(enabled);
	addDirButton.setEnabled(enabled);
	addFileButton.setEnabled(enabled);
	removeButton.setEnabled(enabled);
	upButton.setEnabled(enabled);
	downButton.setEnabled(enabled);
}
    public String getAddDirButtonLabel() {
        return addDirButtonLabel;
    }
    public void setAddDirButtonLabel(String addDirButtonLabel) {
        this.addDirButtonLabel = addDirButtonLabel;
    }
    public String getAddFileButtonLabel() {
        return addFileButtonLabel;
    }
    public void setAddFileButtonLabel(String addFileButtonLabel) {
        this.addFileButtonLabel = addFileButtonLabel;
    }
    public String getDirChooserLabelText() {
        return dirChooserLabelText;
    }
    public void setDirChooserLabelText(String dirChooserLabelText) {
        this.dirChooserLabelText = dirChooserLabelText;
    }
    public boolean isDirEndsWithSeparator() {
        return dirEndsWithSeparator;
    }
    public void setDirEndsWithSeparator(boolean dirEndsWithSeparator) {
        this.dirEndsWithSeparator = dirEndsWithSeparator;
    }
    public String getFileChooserLabelText() {
        return fileChooserLabelText;
    }
    public void setFileChooserLabelText(String fileChooserLabelText) {
        this.fileChooserLabelText = fileChooserLabelText;
    }
    public String[] getFilterExtensions() {
        return filterExtensions;
    }
    public void setFilterExtensions(String[] filterExtensions) {
        this.filterExtensions = filterExtensions;
    }
	public void setRelative(boolean relative) {
	}
	public void setRootContainer(IContainer rootContainer) {
		this.rootContainer = rootContainer;
	}
	public void setWorkspaceResource(boolean workspaceResource) {
		this.workspaceResource = workspaceResource;
	}
	
}



