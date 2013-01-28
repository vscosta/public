package org.cs3.pdt.common.queries;

import static org.cs3.prolog.common.QueryUtils.bT;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.cs3.pdt.common.metadata.Goal;
import org.cs3.prolog.common.logging.Debug;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.ui.text.Match;

public class MetaPredicatesSearchQuery extends MarkerCreatingSearchQuery {
	
	private static final String ATTRIBUTE = "pdt.meta.predicate";
	private static final String SMELL_NAME = "PDT_Quickfix";
	private static final String QUICKFIX_DESCRIPTION = "PDT_QuickfixDescription";
	private static final String QUICKFIX_ACTION = "PDT_QuickfixAction";

	public MetaPredicatesSearchQuery(boolean createMarkers) {
		super(new Goal("", "", "", -1, ""), createMarkers, ATTRIBUTE, ATTRIBUTE);
		setSearchType("Undeclared meta predicates");
	}
	
	@Override
	protected String buildSearchQuery(Goal goal, String module) {
		return bT("find_undeclared_meta_predicate", "Module", "Name", "Arity", "MetaSpec", "MetaSpecAtom", "File", "Line", "PropertyList", "Directive");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Match constructPrologMatchForAResult(Map<String, Object> m) throws IOException {
		String definingModule = m.get("Module").toString();
		String functor = m.get("Name").toString();
		int arity=-1;
		try {
			arity = Integer.parseInt(m.get("Arity").toString());
		} catch (NumberFormatException e) {}
		
		IFile file = findFile(m.get("File").toString());
		int line = Integer.parseInt(m.get("Line").toString());

		Object prop = m.get("PropertyList");
		List<String> properties = null;
		if (prop instanceof Vector<?>) {
			properties = (Vector<String>)prop;
		}	
		Match match = createUniqueMatch(definingModule, functor, arity, file, line, properties, "", "definition");
		
		if (createMarkers && match != null) {
			try {
				String metaSpec = m.get("MetaSpecAtom").toString();
				IMarker marker = createMarker(file, "Meta predicate: " + metaSpec, line);
				marker.setAttribute(SMELL_NAME, "Meta Predicate");
				marker.setAttribute(QUICKFIX_ACTION, m.get("Directive").toString());
				marker.setAttribute(QUICKFIX_DESCRIPTION, "Declare meta predicate");
			} catch (CoreException e) {
				Debug.report(e);
			}
		}
		return match;
	}
	
}
