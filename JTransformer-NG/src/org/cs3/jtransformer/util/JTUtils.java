package org.cs3.jtransformer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cs3.jtransformer.JTDebug;
import org.cs3.jtransformer.JTransformer;
import org.cs3.jtransformer.JTransformerPlugin;
import org.cs3.jtransformer.OutputProjectCreationContributor;
import org.cs3.jtransformer.internal.astvisitor.Names;
import org.cs3.jtransformer.internal.natures.JTransformerNature;
import org.cs3.jtransformer.tests.FileAdaptationHelperTest;
import org.cs3.pdt.runtime.PrologInterfaceRegistry;
import org.cs3.pdt.runtime.PrologRuntimePlugin;
import org.cs3.pdt.ui.util.UIUtils;
import org.cs3.pl.prolog.PrologException;
import org.cs3.pl.prolog.PrologInterface;
import org.cs3.pl.prolog.PrologInterfaceException;
import org.cs3.pl.prolog.PrologSession;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.texteditor.quickdiff.compare.equivalence.Hash;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Some util methods.
 * 
 * @author Mark Schmatz
 *
 */
public class JTUtils
{
	private static Boolean useSameProjectNameSuffix = null;
	
	private static ThreadLocal tmpCTList = new ThreadLocal()
	{
		protected synchronized Object initialValue()
		{
			return new ArrayList();
		}
	};
	public static List getTmpCTList()
	{
		return (List) tmpCTList.get();
	}
	public static synchronized void setTmpCTList(List list)
	{
		getTmpCTList().clear();
		getTmpCTList().addAll(list);
	}

	
	/**
	 * Returns true if the output project (the adapted version of the original)
	 * gets the same project name as the original project plus a suffix.<br>
	 * (this is set via a system property)
	 * 
	 * @return boolean
	 */
	public static boolean useSameProjectNameSuffix()
	{
		if( useSameProjectNameSuffix == null )
		{
			useSameProjectNameSuffix = new Boolean(
				"true".equals(System.getProperty(JTConstants.SYSTEM_PROPERTY_USE_SAME_OUTDIR_WITH_SUFFIX, /*Default = */ "true")));
		}

		return useSameProjectNameSuffix.booleanValue();
	}
	
	/**
	 * This method has dependencies to Eclipse.<br><br>
	 * 
	 * Returns the absolute path where the adapted version of the project
	 * is stored.<br><br>
	 * 
	 * If the the system property
	 * <tt>LAJConstants.SYSTEM_PROPERTY_USE_SAME_OUTDIR_WITH_SUFFIX</tt> is
	 * set to <tt>true</tt> then the 'original' project location plus a suffix
	 * is used.<br>
	 * Otherwise the default output project location is used
	 * (normally, ends with '<i>LogicAJOutput</i>').
	 * @param project 
	 * 
	 * @see JTConstants.SYSTEM_PROPERTY_USE_SAME_OUTDIR_WITH_SUFFIX
	 * 
	 * @return String the absolute path of the output dir
	 * @throws JavaModelException
	 */
	public static String getOutputProjectPath(IProject project)
	{	
		IProject outputProject= ResourcesPlugin.getWorkspace().getRoot().getProject(JTUtils.getOutputProjectName(project));
		if(outputProject.getLocation() == null) { // TODO: only necessary for the test framework
			return ResourcesPlugin.getWorkspace().getRoot()
			.getLocation().toPortableString() + "/" + JTUtils.getOutputProjectName(project); 
		}
		return outputProject.getLocation().toPortableString();

	}

	/**
	 * Returns the output project name.
	 * If the the system property
	 * <tt>LAJConstants.SYSTEM_PROPERTY_USE_SAME_OUTDIR_WITH_SUFFIX</tt> is
	 * set to <tt>true</tt> then the project location of the 'original'
	 * project name plus a suffix.<br>
	 * Otherwise the default output project name is used
	 * (normally '<i>LogicAJOutput</i>').
	 *  
	 * @param srcProject The source project
	 * @return String
	 */
	public static String getOutputProjectName(IProject srcProject)
	{
		if( JTUtils.useSameProjectNameSuffix() )
		{
			String outputProjectName = srcProject.getName() + JTConstants.OUTPUT_PROJECT_NAME_SUFFIX;
			if( outputProjectName == null )
			{
				System.err.println("************************ schmatz: outputProjectName is null");
				outputProjectName = "LogicAJOutput";  // Fallback
			}
			return outputProjectName;
		}
		else
			return "LogicAJOutput";
	}

	// ------------------------------------------------------------
	
	/**
	 * Returns the location of the workspace root.
	 * 
	 * @return String The OS string
	 */
	public static String getWorkspaceRootLocation()
	{
		return ResourcesPlugin.getWorkspace().getRoot()
			.getLocation().toOSString();
	}
	
	/**
	 * Copies all needed files like the class path, etc.
	 * 
	 * @see FileAdaptationHelperTest
	 * 
	 * @param srcProject
	 * @param destProject
	 * @throws CoreException
	 * @throws IOException 
	 */
	// New by Mark Schmatz
	public static void copyAllNeededFiles(IProject srcProject) throws CoreException
	{
		IProject destProject = JTUtils.getOutputProject(srcProject);
		boolean isBundle = false;
		
		if( !srcProject.isOpen() )
			srcProject.open(null);
		if( !destProject.isOpen() && destProject.exists() )
			destProject.open(null);
		
		if( destProject.isOpen() )
		{
			String srcProjectName = srcProject.getName();
			String destProjectName = destProject.getName();

			/*
			 * Check whether we have a OSGi bundle as source project...
			 */
			if( isBundle(srcProject) )
				isBundle = true;

			// ----
			
			List neededFileForCopying = new ArrayList();

			Map classPathReplacement = new HashMap();
			classPathReplacement.put("(<classpathentry .*?kind=\"lib\" \\s*?path=\")([^/].*?\"/>)",
					"$1/" + srcProject.getName() + "/$2");

			classPathReplacement.put("(<classpathentry .*?sourcepath=\")([^/].*?)",
					"$1/" + srcProject.getName() + "/$2");

			if( !isBundle )
			{
				neededFileForCopying.add(new FileAdaptationHelper(JTConstants.DOT_CLASSPATH_FILE,classPathReplacement));
			}
			
			Map projectFileReplacement = new HashMap();
			projectFileReplacement.put(
					"<nature>org.cs3.jtransformer.JTransformerNature</nature>",
					""
			);

			neededFileForCopying.add(new FileAdaptationHelper(JTConstants.DOT_PROJECT_FILE, projectFileReplacement));

			
			List<OutputProjectCreationContributor> contributors = loadExtensions("outputProjectCreation");
			
			for (OutputProjectCreationContributor contributor : contributors) {
				neededFileForCopying.addAll(contributor.getFileAdaptionHelpers(srcProject, projectFileReplacement));
			}
//			neededFileForCopying.addAll(getFileAdaptionHelpers(srcProject, destProject,
//					classPathReplacement));
			
			// ----
			
			Iterator iterator = neededFileForCopying.iterator();
			while( iterator.hasNext() )
			{
				FileAdaptationHelper fah = (FileAdaptationHelper) iterator.next();
				copyFile(srcProject, destProject, fah.getFileName());
				if( fah.needsAdaptation() )
				{
					adaptFile(destProject, fah, false);
				}
			}
		}
	}
	
	public static boolean isBundle(IProject srcProject) {
		return fileExists(srcProject, JTConstants.BUNDLE_MANIFEST_FILE);
	}
	
	static public List<OutputProjectCreationContributor> loadExtensions(String id)
    
 {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint =
       registry.getExtensionPoint(JTransformer.PLUGIN_ID + "." + id);
       
    IConfigurationElement[] extensions = registry
	.getConfigurationElementsFor(JTransformer.PLUGIN_ID + "." + id);
    List<OutputProjectCreationContributor> contributors = new ArrayList<OutputProjectCreationContributor>();
    for(int i = 0;i < extensions.length;i++)
    {
    	try {
			contributors.add((OutputProjectCreationContributor)
					extensions[i].createExecutableExtension("class"));
		} catch (InvalidRegistryObjectException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
    
    }
    return contributors;
 }
	
	
	/**
	 * Returns <tt>true</tt> if the file for the given <tt>filename</tt> exists in
	 * <tt>srcProject</tt>; <tt>false</tt> otherwise.
	 * 
	 * @param srcProject
	 * @param filename
	 * @return boolean
	 */
	public static boolean fileExists(IProject srcProject, String filename)
	{
		IFile file = srcProject.getFile(new Path(filename));
		if( file.exists() )
			return true;
		else
			return false;
	}

	/**
	 * Stores the given CT names in the comma separated String <tt>ctNameList</tt>
	 * into a file in the given path (<tt>absolutePathOfOutputProject</tt>).
	 * 
	 * @param ctNameList
	 * @param absolutePathOfOutputProject
	 */
	// New by Mark Schmatz
	public static void storeCTList(Map ctNamesAndFiles, String absolutePathOfOutputProject)
	{
		List list = new ArrayList();
		for (Iterator iter = ctNamesAndFiles.keySet().iterator(); iter.hasNext();)
		{
			String ctName = (String) iter.next();
			CtProperties properties = (CtProperties)ctNamesAndFiles.get(ctName);

			if(properties.isDynamic()) {
				String ctFilename = properties.getFileName();
				String adviceKind = properties.getAdviceKind();
				
				String variableBinding = ctName.substring(ctName.indexOf('('), ctName.indexOf(')')+1);
				String first = "'" + ctFilename + "'" + variableBinding;
				String second = ctName.substring(1, ctName.lastIndexOf("'"));
				String third = ctName;
				
				list.add(
						adviceKind + JTConstants.CTNAME_FILENAME_SEPARATOR + 
						first + JTConstants.CTNAME_FILENAME_SEPARATOR + 
						second + JTConstants.CTNAME_FILENAME_SEPARATOR +
						third);
			}
		}		
		/*
		 * After the CT list is created and stored
		 * save it in a temp list so that after copying
		 * the manifest it can be extended by exporting
		 * the CT packages.
		 * 
		 */
		setTmpCTList(list);
		
		storeListInFile(list, absolutePathOfOutputProject, JTConstants.CT_LIST_FILENAME);
	}
	
	/**
	 * OBSOLETE:
	 * 
	 * Returns the CT packages from the temp CT list
	 * as comma separated values String in order that
	 * it can be inserted in the manifest file.
	 *  
	 * @return Stirng
	 *
	public static String getCTPackagesAsCSV(List tmpCTList)
	{
		StringBuffer buffer = new StringBuffer();
		String str = "";
		
		if( tmpCTList != null )
		{
			Iterator iterator = tmpCTList.iterator();
			while( iterator.hasNext() )
			{
				String ctLine = (String) iterator.next();
				StringTokenizer st = new StringTokenizer(ctLine, JTConstants.CTNAME_FILENAME_SEPARATOR);
				String ctName = st.nextToken().trim().replaceAll("/", ".");
				String ctFilename = st.nextToken().trim();
				
				Pattern p = Pattern.compile("'(.*?)"+ctFilename+"'\\(.*?\\)");
				Matcher m = p.matcher(ctName);
				if( m.find() )
				{
					String ctPackage = m.group(1);
					// Delete trailing dot
					ctPackage = ctPackage.substring(0, ctPackage.length()-1);
					// Add the prefixing resources package
					String subDirForCts = makeItAPackagePart(JTConstants.SUBDIR_FOR_CTS);
					ctPackage = subDirForCts + ctPackage;

					if( buffer.indexOf(ctPackage) == -1 )
					{
						// We don't want duplicates...
						buffer.append(ctPackage).append(", ");
					}
				}
			}
			
			if( buffer.length() > 2)
				str = buffer.substring(0, buffer.length()-2);
		}
		
		return str;
	}

	// OBSOLETE
	public static String makeItAPackagePart(String subDirForCts)
	{
		if( JTConstants.SUBDIR_FOR_CTS != null && !"".equals(JTConstants.SUBDIR_FOR_CTS) )
			return JTConstants.SUBDIR_FOR_CTS + ".";
		else
			return "";
	}
	*/
	
	/**
	 * Stores the list of full qualified Java class names of all not-extern
	 * classes of the bundle (except the bundle activator) into a
	 * file in the given path (<tt>absolutePathOfOutputProject</tt>).
	 *
	 * @param prologSession
	 * @param absolutePathOfOutputProject
	 * @return <tt>true</tt> if everything went right; <tt>false</tt> otherwise
	 */
	// New by Mark Schmatz
	public static boolean storeJavaFileList(PrologSession prologSession, String absolutePathOfOutputProject) throws PrologInterfaceException
	{
		boolean ok = true;

		// Note: fullQualifiedName/2 requires that at least one argument is bound!
		List queryList = prologSession.queryAll(
				"class(ResolvedServiceClassId,_,_),not(local(ResolvedServiceClassId)), not(anonymousClass(ResolvedServiceClassId)), not(externT(ResolvedServiceClassId)), fullQualifiedName(ResolvedServiceClassId, FqClassName)."
		);
		
		if( queryList != null )
		{
			Set set = new HashSet();
			
			Iterator iterator = queryList.iterator();
			while( iterator.hasNext() )
			{
				HashMap map = (HashMap) iterator.next();
				String fqClassName = (String) map.get("FqClassName");
				set.add(fqClassName);
			}

			/*
			 * Dirty (because I have to adapt the Strings when I
			 * rename a class or package)
			 * 
			 * Needed to be sure that all needed classes are consulted
			 * during the first consult process...
			 */
			set.add("org.cs3.ditrios.facade.cslogicaj.DitriosFacade");
			set.add("org.aspectj.lang.JoinPoint");
			set.add("org.cs3.ditrios.facade.core.DitriosClientService");
			// end - Ditry
			
			storeListInFile(new ArrayList(set), absolutePathOfOutputProject, JTConstants.FQCN_LIST_FILENAME);
		}
		else
			ok = false;
	
		return ok;
	}

	/**
	 * Stores the Strings in the given list into the given file and path.
	 * 
	 * @param stringList
	 * @param absolutePath
	 */
	// New by Mark Schmatz
	public static void storeListInFile(List stringList, String absolutePath, String fileName)
	{
		try
		{
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(getFileInstance(absolutePath + JTConstants.COMPLETE_RESOURCES_FILELISTS_FOLDER, fileName)));
			Iterator iterator = stringList.iterator();
			while( iterator.hasNext() )
			{
				String elem = (String) iterator.next();
				bw.write(elem + "\n");
			}
			bw.flush();
			bw.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Returns a File instance for the given filename and path.
	 * If the path does not exist it will be deep-created.<br>
	 * The file may exist or not.
	 * 
	 * @param pathName
	 * @param filename
	 * @return File
	 */
	public static File getFileInstance(String pathName, String filename)
	{
		File path = new File(pathName);
		if( !path.exists() )
			path.mkdirs();
		
		if( !pathName.endsWith("/") && !pathName.endsWith("\\") )
			pathName += File.separator;
			
		File file = new File(pathName + filename);
		return file;
	}
	
	/**
	 * Util method returning the Prolog interface.
	 * 
	 * Uses the method {@link JTransformerNature#getPrologInterface()}.
	 * 
	 * To ensure that the prolog interface nature is correctly loaded this method MUST be called.
	 * It is not sufficient to get a PrologInterface via its key from the {@link PrologInterfaceRegistry}.
	 * 
	 * @param project
	 * @return PrologInterface
	 * @throws CoreException
	 */
	public static PrologInterface getPrologInterface(IProject project) throws CoreException
	{
		JTransformerNature nature = JTransformerPlugin.getNature( project);
		if(nature == null) {
			throw new IllegalArgumentException("project does not have a JTransformer nature: " + project.getName());
		}
		return nature.getPrologInterface();
	}
	
	// ------------------------------------------------------------------

	/**
	 * Copies the file for <tt>fileName</tt> from the
	 * source project to the dest project if it exists.
	 * If it doesn't exist nothing will be done.
	 * 
	 * @param srcProject
	 * @param destProject
	 * @param fileName
	 * @throws CoreException
	 */
	// Modified by Mark Schmatz
	public static void copyFile(IProject srcProject, IProject destProject, final String fileName) throws CoreException
	{
		IFile file = srcProject.getFile(new Path(fileName));
		try {
			if( file.exists() )
			{
				IFile old = destProject.getFile(new Path(fileName));
				if( old.exists() )
				{
					// Just to be sure: delete the file if it exists...
					old.refreshLocal(IResource.DEPTH_INFINITE, null);
					old.delete(true, false, null);
					old.refreshLocal(IResource.DEPTH_INFINITE, null);
				}
				file.copy(new Path(destProject.getFullPath() + fileName), true, null);
				destProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				old.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		} catch(ResourceException ex) {
			JTDebug.error(ex.getLocalizedMessage());
		}
	}

	/**
	 * Adapts the encapsulated file in the given helper so that it
	 * fits the needs in the output project.
	 *  
	 * @param destProject
	 * @param fileName
	 * @throws CoreException
	 */
	// New by Mark Schmatz
	public static void adaptFile(IProject destProject, FileAdaptationHelper cfh, boolean deleteInnerEmptyLines) throws CoreException
	{
		IFile file = destProject.getFile(new Path(cfh.getFileName()));
		if( file.exists() )
		{
			String fileContent = getFileContent(file);
			
			fileContent = cfh.adaptContent(fileContent);
		
			if( deleteInnerEmptyLines )
				fileContent = removeEmptyLines(fileContent) + "\n";
			
			byte[] buffer = fileContent.getBytes();
			InputStream is = new ByteArrayInputStream(buffer);
			
			file.setContents(is, IFile.FORCE, null);
		}
	}

	public static String removeEmptyLines(String str)
	{
		String tmp = "";
		while( !tmp.equals(str) )
		{
			tmp = str;
			str = str.replaceAll("\n\n", "\n");
			str = str.replaceAll("\r\n\r\n", "\r\n");
		}
		return str;
	}

	/**
	 * Returns the content of the file as String.
	 * 
	 * @param file
	 * @return String
	 * @throws CoreException
	 */
	private static String getFileContent(IFile file) throws CoreException
	{
		if( file.exists() )
		{
			try
			{
				StringBuffer strb = new StringBuffer();
				InputStream is = file.getContents();
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line = null;
				while( (line = br.readLine()) != null )
				{
					strb.append(line).append("\n");
				}
				br.close();
				is.close();
				
				return strb.toString();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		return null;
	}
	/**
	 * Remove the output project from the class path.
	 * @param outPath
	 * @param javaProject
	 * @return
	 * @throws JavaModelException
	 */
	static public List getFilteredClasspath(IPath outPath, IJavaProject javaProject) throws JavaModelException {
		IClasspathEntry[] classpath = javaProject.getRawClasspath();
		
		List filteredClassPath = new ArrayList();
		for (int i = 0; i < classpath.length; i++) {
			IPath path= classpath[i].getPath();
			if(!path.equals(outPath)) {
				filteredClassPath.add(classpath[i]);
			}
		}
		return filteredClassPath;
	}
	/**
	 * 
	 * @param javaProject
	 * @param destProject
	 * @throws CoreException 
	 */
	public static void addReferenceToOutputProjectIfNecessary(IJavaProject javaProject, IProject destProject) throws CoreException {
		IPath outPath = new Path("/"+destProject.getName());
		List filteredClassPath = JTUtils.getFilteredClasspath(outPath, javaProject);
		// add reference to output project
		filteredClassPath.add(JavaCore.newProjectEntry(outPath));

		javaProject.setRawClasspath(
				(IClasspathEntry[])filteredClassPath.toArray(new IClasspathEntry[0]), null);

		javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);		
	}
	
	/**
	 * 
	 * @param javaProject
	 * @param destProject
	 * @throws CoreException 
	 */
	public static void removeReferenceToOutputProjectIfNecessary(IJavaProject javaProject, IProject destProject, IProgressMonitor monitor) throws CoreException {
		IPath outPath = new Path("/"+destProject.getName());
		javaProject.setRawClasspath(
				(IClasspathEntry[])JTUtils.getFilteredClasspath(outPath, javaProject).toArray(new IClasspathEntry[0]), monitor);
		javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);		
	}
	
	/**
	 * 
	 * @param lajProject
	 * @return
	 */
	public static IProject getOutputProject(IProject project) {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(JTUtils.getOutputProjectName(project));
	}
	public static JTransformerNature getNature(IProject project) throws CoreException {
		return JTransformerPlugin.getNature(project);
	}
	
	public static boolean allProjectsHaveJTNature(List projects) throws CoreException {
		for (Iterator iter = projects.iterator(); iter.hasNext();) {
			IProject project = (IProject) iter.next();	
			if(!project.getDescription().hasNature(JTransformer.NATURE_ID)) {
				return false;
			}
		}
		return true;
	}
	/**
	 * @param project the project on which to delete the markers 
	 * @throws CoreException 
	 * @throws PrologInterfaceException
	 * @throws PrologException
	 */
	// Modified Dec 21, 2004 (AL)
	// Clearing all Markers from current Workspace
	// that have got LogicAJPlugin "Nature"
	public static void clearAllMarkersWithJTransformerFlag(IProject project) throws CoreException {
		removeJTProblemsMarkers(project);	

		
	}
	private static void removeJTProblemsMarkers(IProject project) throws CoreException {
		IMarker[] currentMarkers = project.findMarkers(IMarker.PROBLEM, true,
				IResource.DEPTH_INFINITE);

		if (currentMarkers != null) {
			for (int i = 0; i < currentMarkers.length; i++) {
				if (currentMarkers[i]
						.getAttribute(JTransformer.PROBLEM_MARKER_ID) != null) {
					// Hier k�nnte in Zukunft noch eine Abfrage hinzu
					// um welchen Aspekt es sich gerade handelt.
					// Gibts im Moment aber nicht. Deswegen
					// l�sche ich einfach alle
					currentMarkers[i].delete();
				}
			}

		}
	}

	public static String quote(String str) {
		return "'" + str + "'";
	}
	
	/**
	 * Open view "viewId" and set its  status line to "message".
	 * 
	 * If viewId is null the message is set for the currenly active view. 
	 * 
	 * @param viewId
	 * @param message
	 */
	static public void setStatusMessage(final String viewId, final String message) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if(viewId != null)
					showView(viewId);
				getActionBarContributor().setMessage(message);
			}

		});
	}	
	
	/**
	 * 
	 * Open view "viewId" and set its  status line to "message".
	 * 
	 * If viewId is null the message is set for the currenly active view. 
	 * 
	 * @param viewId
	 * @param message
	 */
	static public void setStatusErrorMessage(final String viewId, final String message) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if(viewId != null)
					showView(viewId);
				getActionBarContributor().setErrorMessage(message);
			}

		});
	}

	
	static private void showView(final String viewId) {
		try {
			JTUtils.getActivePage().showView(viewId);
		} catch (PartInitException e) {
			JTDebug.report(e);
		}
	}

	/**
	 * Must be run in sync with the display thread.
	 * 
	 * E.g. PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() { ...});
	 * @return
	 */
	static private IStatusLineManager getActionBarContributor() {
		IViewSite site=(IViewSite)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite();
		System.err.println("DEBUG: " + site.getClass().getName());
		return site.getActionBars().getStatusLineManager();
	}
	
	/**
	 * Select a text region in file filename which starts at offset start with length length.
	 * @param start
	 * @param length
	 * @param filename
	 * @throws PartInitException
	 */
	static public void selectInEditor(int start, int length, String filename) throws PartInitException {
		Path path = new Path(filename);
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		if (file == null) {
			setStatusErrorMessage(null, "could not find the file: '" + filename + "' in the workspace.");
			return;
		}
		openInEditor(file, true);
		IDocument document = ((AbstractTextEditor)getActiveEditor()).getDocumentProvider().getDocument(getActiveEditor().getEditorInput());
		ISelection selection = new TextSelection(document,start,length);
		getActiveEditor().getEditorSite().getSelectionProvider().setSelection(selection);
	}

	static public IEditorPart openInEditor(IFile file, boolean activate) throws PartInitException {
		if (file != null) {
			IWorkbenchPage p= getActivePage();
			if (p != null) {
				IEditorPart editorPart= IDE.openEditor(p, file, activate);
				return editorPart;
			}
		}
		return null; 
	} 

	

	/**
	 * Must be run in sync with the display thread.
	 * 
	 * E.g. PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() { ...});
	 * 
	 * @return
	 * @throws NullPointerException if not synchronized with the Display
	 */
	static public IEditorPart getActiveEditor() {
		return getActivePage().getActiveEditor();
	}

	/**
	 * Must be run in sync with the display thread.
	 * 
	 * E.g. PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() { ...});
	 * 
	 * @return
	 * @throws NullPointerException if not synchronized with the Display
	 */
	static public IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
	public static void logAndDisplayUnknownError(final Exception e) {
		UIUtils.getDisplay().asyncExec(new Runnable() {
			public void run() {
				Shell shell = getShell(true);
		UIUtils.logAndDisplayError(JTransformerPlugin.getDefault().getErrorMessageProvider(), shell, 
				JTransformer.ERR_UNKNOWN,
				JTransformer.ERR_CONTEXT_EXCEPTION,
				e);
			}

		});
	}
	
	public static void logAndDisplayError(final Exception e,final int code,final int context) {
		UIUtils.getDisplay().asyncExec(new Runnable() {
			public void run() {
				UIUtils.logAndDisplayError(JTransformerPlugin.getDefault().getErrorMessageProvider(),
											UIUtils.getDisplay().getActiveShell(), 
						code,
						context,
						e);
			}
		});
	}
	
	public static void logError(Exception e,int code,int context) {
		UIUtils.logError(JTransformerPlugin.getDefault().getErrorMessageProvider(),  
				code,
				context,
				e);
	}

	/**
	 * Returns the active shell.
	 * 
	 * @param force if the active shell from the current display
	 * is null get the first shell from the getDisplay().getShells()
	 * array. 
	 * @return
	 */
	public static Shell getShell( boolean force) {
		Shell shell = null;
		if (UIUtils.getDisplay().getThread() != Thread.currentThread()) {
			return getShellFromUIThread(force);
		}
		if(UIUtils.getDisplay().getActiveShell() != null){
			shell=UIUtils.getDisplay().getActiveShell();
		} else {
			if(force && UIUtils.getDisplay().getShells().length > 0) {
				shell = UIUtils.getDisplay().getShells()[0];
			}
		}
		return shell;
	}
	private static Shell getShellFromUIThread(final boolean force) {
		final Shell[] shell = new Shell[1];
		UIUtils.getDisplay().syncExec(new Runnable() {

			public void run() {
				shell[0] = getShell(force);
			}
			
		});
		return shell[0];
	}

	static public IPath getPersistentFactbaseFileForPif(String key) {

		return JTransformerPlugin.getDefault().getStateLocation().append(
						new Path(Names.PERSISTANT_FACTS_FILE_PREFIX + key + ".pl"));
	}
	public static String iPathToPrologFilename(IPath init) {
		return init.toOSString().replace('\\', '/');
	}
	
	/**
	 * Create a temporary sessions
	 * runs query and disposes the session.
	 * 
	 * @param pif
	 * @param query
	 * @throws PrologInterfaceException
	 */
	public static Map queryOnceSimple(PrologInterface pif, String query) throws PrologInterfaceException {
		PrologSession session = null;
    	try {
    		session = pif.getSession();
       		return session.queryOnce(query);

    	} finally {
    		if(session != null){
    			session.dispose();
    		}
    	}
	}
	public static String getKeyForPrologInterface(PrologInterface pif) {
		return PrologRuntimePlugin.getDefault().getPrologInterfaceRegistry().getKey(pif);
	}
	
	
    /**
     * Removes the associates factbase file and
     * removes the jt_facade:unmodifiedPersistantFactbase flag.   
     * @throws PrologInterfaceException 
     * 
     * @throws PrologInterfaceException
     */

	public static void clearPersistentFacts(String key) throws PrologInterfaceException  {
		JTDebug.info("clearing persistent factbase: "+ key);
		if( key != null) {
			IPath location = JTUtils.getPersistentFactbaseFileForPif(key);
			if(location.toFile().isFile()){
				JTDebug.info("removed persistent factbase file for factbase: "+ key);
				location.toFile().delete();
			}
			
			PrologInterface pif = PrologRuntimePlugin.getDefault().getPrologInterfaceRegistry().getPrologInterface(key);
			if(pif != null && pif.isUp()){
				JTUtils.queryOnceSimple(pif,"jt_facade:setUnmodifiedPersistantFactbase(false)");
			}
		}
	}
	public static String getFactbaseKeyForProject(IProject project) {
		return JTransformerPlugin.getDefault().getPreferenceValue(project, JTransformer.PROLOG_RUNTIME_KEY, null);
	}
	
	/**
	 * Returns the corresponding subscription key for a runtime (factbase) key. 
	 * 
	 * @param key
	 * @return
	 */
	public static String getSubscriptionIDForRuntimeKey(String key) {
		return JTransformer.SUBSCRIPTION_PREFIX + key;
	}
	/**
	 * (De)activates Eclipse auto-building.
	 * @param autoBuilding
	 * @see IWorkspaceDescription#setAutoBuilding(boolean)
	 */
	public static void setAutoBuilding(boolean autoBuilding) {
		ResourcesPlugin.getWorkspace().getDescription().setAutoBuilding(autoBuilding);
		JTDebug.info(autoBuilding ? "Deactivated" : "Activated" + " auto-building.");
	}
	/**
	 * Persistent factbase exists for PrologInterface key "key".
	 * 
	 * @param key
	 * @return
	 */
	public static boolean persistentFactbaseFileExistsForPifKey(String key) {
		return getPersistentFactbaseFileForPif(key).toFile().exists();
	}

	
}
