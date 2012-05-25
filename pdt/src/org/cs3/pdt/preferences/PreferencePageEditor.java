package org.cs3.pdt.preferences;

import org.cs3.pdt.PDT;
import org.cs3.pdt.PDTPlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class PreferencePageEditor extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePageEditor() {
		super(GRID);
		setPreferenceStore(PDTPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PDT.PREF_EXTERNAL_FILE_SAVE_WARNING, "Ask before saving external files", getFieldEditorParent()));
		
		addField(new BooleanFieldEditor(PDT.PREF_AUTO_COMPLETE_ARGLIST, "Create arglist in auto completion", getFieldEditorParent()));
		
		addField(new BooleanFieldEditor(PDT.PREF_SHOW_SYSTEM_PREDS, "Show system predicates in outline", getFieldEditorParent()));
		
//		// A comma separated list of filter ids that should be activated at startup
//		StringFieldEditor sfe = new StringFieldEditor(PDT.PREF_OUTLINE_FILTERS, "Default active Filters for the Prolog Outline",
//				getFieldEditorParent());
//		addField(sfe);
//
//		BooleanFieldEditor bfe = new BooleanFieldEditor(PDT.PREF_OUTLINE_SORT,
//				"Whether the Prolog Outline is to be sorted lexicographical", getFieldEditorParent());
//		bfe.setEnabled(false, getFieldEditorParent());
//		addField(bfe);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}

}