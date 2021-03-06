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

package org.cs3.prolog.ui.util;

import org.cs3.prolog.common.Option;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

public class EnumEditor extends OptionEditor {
	private static final int TEXT_FIELD_WIDTH = 20;
	private Combo combo;
	
	public EnumEditor(Composite parent, Option option) {
		super(parent, option);
	}

	@Override
	protected void createControls(Composite composite) {
		GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        composite.setLayout(layout);
        
        GridData data = new GridData(GridData.FILL);
        data.grabExcessHorizontalSpace = true;
        data.verticalAlignment = GridData.FILL;
        data.horizontalAlignment = GridData.FILL;
        composite.setLayoutData(data);


        combo = new Combo(composite, SWT.READ_ONLY);
        String[][] enumValues = option.getEnumValues();
        for (int i = 0; i < enumValues.length; i++) {
			combo.add(getLabelForIndex(i),i);
			
		}
        
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.verticalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
        combo.setLayoutData(gd);
        combo.addSelectionListener(new SelectionListener() {
            String old="";
            @Override
			public void widgetSelected(SelectionEvent e) {
                String newValue = getValue();
				firePropertyChange(old,newValue);
                old=newValue;
            }

            
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {             
                widgetSelected( e);
            }
        });

	}
	private String getValueForIndex(int ix) {
		String value = option.getEnumValues()[ix][1];
		return value;
	}

	private String getLabelForIndex(int ix) {
		String label = option.getEnumValues()[ix][0];
		return label;
	}

	@Override
	public String getValue() {
		int ix = combo.getSelectionIndex();
        String newValue = getValueForIndex(ix);
		return newValue;
	}

	@Override
	public void setValue(String value) {
		combo.select(getIndexForValue(value));

	}

	private int getIndexForValue(String value) {
		for(int i=0;i<option.getEnumValues().length;i++){
			if(getValueForIndex(i).equals(value)){
				return i;
			}
		}
		return -1;
	}

}


