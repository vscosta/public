/*****************************************************************************
 * This file is part of the Prolog Development Tool (PDT)
 * 
 * Author: null (among others)
 * WWW: http://sewiki.iai.uni-bonn.de/research/pdt/start
 * Mail: pdt@lists.iai.uni-bonn.de
 * Copyright (C): 2004-2012, CS Dept. III, University of Bonn
 * 
 * All rights reserved. This program is  made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 ****************************************************************************/

package org.cs3.prolog.common;

import java.util.ArrayList;
import java.util.List;

import org.cs3.prolog.pif.PrologInterface;
import org.cs3.prolog.pif.PrologInterfaceException;

public class QueryUtils {
	
	/*
	/****************************************************************************************************
	 *.-~:~-..-~:~-..-~:~-..-~:~-...-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-.
	 *									  -= Global Constants =-
	 *.-~:~-..-~:~-..-~:~-..-~:~-...-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-.
	 ****************************************************************************************************/	
	
	public final static String USER_MODULE = "user";
	
	/*
	/****************************************************************************************************
	 *.-~:~-..-~:~-..-~:~-..-~:~-...-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-.
	 *									 	 -= SWI-Prolog =-
	 *.-~:~-..-~:~-..-~:~-..-~:~-...-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-.
	 ****************************************************************************************************/		
	
	/**
	 * Returns the version of SWI-Prolog which is currently used by a given Factbase 
	 * 
	 * @param pif 
	 * @return String - Version of SWI-Prolog
	 * @throws PrologInterfaceException 
	 */
	public static String getSWIVersion(PrologInterface pif) throws PrologInterfaceException {
		
		return (String) pif.queryOnce("current_prolog_flag(version,B)").get("B");
	}
	
	
	/*
	/****************************************************************************************************
	 *.-~:~-..-~:~-..-~:~-..-~:~-...-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-.
	 *									  	-= Modules =-
	 *.-~:~-..-~:~-..-~:~-..-~:~-...-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-.
	 ****************************************************************************************************/	
	
	/**
	 * Tests if the module name equals the default user name
	 * 
	 * @param moduleName - variable to be tested
	 * @return true - yes USER_MODULE - else false
	 */
	public static boolean isDefaultModule(String moduleName) {
		return (moduleName == null) || ("".equals(moduleName)) || (USER_MODULE.equals(moduleName));
	}
	
	/*
	/****************************************************************************************************
	 *.-~:~-..-~:~-..-~:~-..-~:~-...-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-.
	 *								 	-= Predicate Builder =-
	 *.-~:~-..-~:~-..-~:~-..-~:~-...-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-..-~:~-.
	 ****************************************************************************************************/	
	
	/**
	 * Shortcut for buildTerm(String functor, Object...args)
	 * @see buildTerm(String functor, Object...args)
	 */
	public static String bT(String functor, Object...args) {
		return buildTerm(functor, args);
	}
	
	/**
	 * Build a term with the given functor and args
	 * @param functor
	 * @param args
	 * @return query String ready to handover to the queryOnce or queryAll
	 */
	public static String buildTerm(String functor, Object...args) {
		
		if(args == null || args.length == 0 || (args.length == 1 && args.toString().equals(""))) {
			return functor;
		}
		
		StringBuffer puffer = new StringBuffer();
		puffer.append(functor);
		puffer.append("(");
		
		if(args.length == 1) {
			puffer.append(args[0].toString());
		} else {
			for(Object arg:args) {
				if(arg instanceof Iterable<?>) {
					puffer.append(listToArgList((Iterable<?>)arg));
				} else {
					puffer.append(arg.toString());
				}
				puffer.append(",");
			}
			puffer.delete(puffer.length()-1, puffer.length());
		}
		puffer.append(")");
		return puffer.toString();
	}
	
	/**
	 * Converts an Iterable to a String in prologstyle like "[ , , ]" 
	 * 
	 * @param listToString - Iterable or subclass of Iterable
	 * @return String - Iterable wrapped in "[]" and separated by ","
	 */
	public static String listToArgList(Iterable<?> listToString) {
		
		String listString = null;
		StringBuffer puffer = new StringBuffer();
		puffer.append("[");
		
		if(listToString == null) {
			return puffer.append("]").toString();
		} 
		
		for (Object unknown:listToString) {
			puffer.append(unknown);
			puffer.append(",");
		}
		puffer.replace(puffer.length()-1, puffer.length(), "]");
		listString = puffer.toString();
		
		if(listString == null) {
			//TODO: error correction
		}
		
		return listString;
	}
	
	/**
	 * Converts a given amount of Objects to a single String
	 * representing a list in prolog notation
	 * 
	 * @param objects
	 * @return - String - wrapped in "[]" and separated by ","
	 */
	public static String objectsToArgList(Object...objects) {
		
		List<Object> temp = new ArrayList<Object>();
		
		for(Object obj:objects) {
			temp.add(obj);
		}
		
		return listToArgList(temp);
	}


}

