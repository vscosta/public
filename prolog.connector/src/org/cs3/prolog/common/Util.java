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

package org.cs3.prolog.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.cs3.prolog.common.logging.Debug;
import org.cs3.prolog.internal.pif.socket.SocketPrologInterface;
import org.cs3.prolog.pif.PrologInterface;

/**
 * contains static methods that do not quite fit anywhere else :-)=
 */
public class Util {
	
	/**
	 * converts a logical character offset to a physical character offset. E.g.
	 * prolog uses logical offsets in the sense that it counts any line
	 * delimiter as a single character, even if it is CRLF, etc.
	 * 
	 * Eclipse documents and views however seem to count physical characters,
	 * i.e. the CRLF line delimiter would count as two characters.
	 * 
	 * @param data the text
	 * @param logical the logical offset
	 * @return the physical offset
	 */
	public static int logicalToPhysicalOffset(String data, int logical) {
		int physical = 0;
		int nextPos = data.indexOf("\r\n");
		while (nextPos >= 0 && nextPos < logical) {
			physical += (nextPos + 2);
			logical -= (nextPos + 1);
			data = data.substring(nextPos + 2);
			nextPos = data.indexOf("\r\n");
		}
		return physical + logical;
	}
	
	
	/**
	 * @see logicalToPhysicalOffset(String data, int logical)
	 * 
	 * @param data the text
	 * @param physical the physical offset
	 * @return the logical offset
	 */
	public static int physicalToLogicalOffset(String data, int physical) {
		int logical = 0;
		int nextPos = data.indexOf("\r\n");
		while (nextPos >= 0 && nextPos < physical) {
			physical -= (nextPos + 2);
			logical += (nextPos + 1);
			data = data.substring(nextPos + 2);
			nextPos = data.indexOf("\r\n");
		}
		return physical + logical;
	}

	/**
	 * getting the lock file for starting a socket server
	 * 
	 * @return the lock file
	 */
	public static File getLockFile() {
		String tmpdir = System.getProperty("java.io.tmpdir");
		return new File(tmpdir, generateFingerPrint());
	}

	private static String generateFingerPrint() {
		long l = System.currentTimeMillis();
		double m = Math.random();
		return "fp_" + l + "_" + m;
	}

	/**
	 * pretty print of a Map
	 * 
	 * @param input the map that should be printed
	 * @return the String representation
	 */
	public static String prettyPrint(Map<String, ?> input) {
		if (input != null) {
			boolean first = true;
			StringBuffer result = new StringBuffer();
			Set<String> keys = input.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				if (!first) {
					result.append(", ");
				}
				String key = it.next();
				Object value = input.get(key);
				String valueAsString = value.toString();
				result.append(key + "-->" + valueAsString);
				first = false;

			}
			return result.toString();
		}
		return "";
	}
	
	/**
	 * pretty print of an array
	 *  
	 * @param a the array that should be printed
	 * @return the String representation
	 */
	public static String prettyPrint(Object[] a) {
		if (a == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < a.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(a[i].toString());
		}
		return sb.toString();
	}
	
	/**
	 * pretty print of a collection
	 *  
	 * @param input the collection that should be printed
	 * @return the String representation
	 */
	public static String prettyPrint(Collection<?> input) {
		if (input != null && !input.isEmpty()) {
			Iterator<?> it = input.iterator();
			return concatinateElements(it);
		}
		return "";
	}

	
	private static String concatinateElements(Iterator<?> it) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		while ( it.hasNext()) {
			if (!first) {
				sb.append(", ");
			}
			Object next = it.next();
			String elm = next == null ? "<null>" : next.toString();
			sb.append(elm);
			first = false;
		}
		return sb.toString();
	}

	/**
	 * get the logfile (create if necessary)
	 * 
	 * @param dir path to the directory
	 * @param name filename
	 * @return the logFile
	 * @throws IOException
	 */
	public static File getLogFile(String dir, String name) throws IOException {
		File logFile = new File(dir,name);
				
		if (!logFile.exists()) {
			logFile.getParentFile().mkdirs();
			logFile.createNewFile();
		}
		return logFile.getCanonicalFile();
	}

	/**
	 * copy an InputStream to an OutputStream
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copy(InputStream in, OutputStream out)
			throws IOException {
		BufferedInputStream bIn = null;
		BufferedOutputStream bOut = null;
		try {
			bIn = new BufferedInputStream(in);
			bOut = new BufferedOutputStream(out);
			byte[] buf = new byte[255];
			int read = -1;
			while ((read = bIn.read(buf)) > -1) {
				out.write(buf, 0, read);
			}
		} finally {
			bOut.flush();
		}
	}


	/**
	 * normalize String for Windows system
	 *  
	 * @param s the String
	 * @return normalized String
	 */
	public static String normalizeOnWindows(String s) {
		boolean windowsPlattform = isWindows();
		if (windowsPlattform) {
			s = s.replace('\\', '/').toLowerCase();
		}
		return s;
	}

	/**
	 * checks if current OS is Windows
	 * 
	 * @return true if current OS is Windows
	 */
	public static boolean isWindows() {
		boolean windowsPlattform = System.getProperty("os.name").indexOf(
				"Windows") > -1;
		return windowsPlattform;
	}

	/**
	 * checks if current OS is MacOS
	 * 
	 * @return true if current OS is MacOS
	 */
	public static boolean isMacOS() {
		boolean mac = System.getProperty("os.name").indexOf("Mac") > -1;
		return mac;
	}

	/**
	 * normalize a Prolog filename
	 * @param f the file
	 * @return normalized path to the file
	 */
	public static String prologFileName(File f) {
		try {
			return normalizeOnWindows(f.getCanonicalPath());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Read InputStream to String
	 * 
	 * @param in the InputStream
	 * @return String representation
	 * @throws IOException
	 */
	public static String toString(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int read = in.read(buf);
		while (read > 0) {
			out.write(buf, 0, read);
			read = in.read(buf);
		}
		return out.toString();
	}

	// specify buffer size for extraction
	static final int BUFFER = 2048;

	/**
	 * escapes special characters in a String
	 * @param s the input string
	 * @return the output String
	 */
	public static String escape(String s) {
		StringBuffer result = new StringBuffer(s.length() + 10);
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			switch (c) {
			case '<':
				result.append("&lt;"); //$NON-NLS-1$
				break;
			case '>':
				result.append("&gt;"); //$NON-NLS-1$
				break;
			case '"':
				result.append("&quot;"); //$NON-NLS-1$
				break;
			case '\'':
				result.append("&apos;"); //$NON-NLS-1$
				break;
			case '&':
				result.append("&amp;"); //$NON-NLS-1$
				break;
			case '{':
				result.append("&cbo;"); //$NON-NLS-1$
				break;
			case '}':
				result.append("&cbc;"); //$NON-NLS-1$
				break;
			default:
				result.append(c);
				break;
			}
		}
		return result.toString();
	}

	public static String unescapeBuffer(StringBuffer line) {
		return unescape(line.toString(),0,line.length());
	}
	
	private static String unescape(String line, int start, int end) {
		StringBuffer sb = new StringBuffer();
		boolean escape = false;
		StringBuffer escBuf = new StringBuffer();
		for (int i = start; i < end; i++) {
			char c = line.charAt(i);
			switch (c) {
			case '&':
				escape = true;
				break;
			case ';':
				if (escape) {
					escape = false;
					String escSeq = escBuf.toString();
					escBuf.setLength(0);
					if ("lt".equals(escSeq)) {
						sb.append('<');
					} else if ("gt".equals(escSeq)) {
						sb.append('>');
					} else if ("cbo".equals(escSeq)) {
						sb.append('{');
					} else if ("cbc".equals(escSeq)) {
						sb.append('}');
					} else if ("amp".equals(escSeq)) {
						sb.append('&');
					} else if ("apos".equals(escSeq)) {
						sb.append('\'');
					} else if ("quot".equals(escSeq)) {
						sb.append('\"');
					}
				} else {
					sb.append(c);
				}
				break;
			default:
				if (escape) {
					escBuf.append(c);
				} else {
					sb.append(c);
				}
				break;
			}
		}
		return sb.toString();
	}

	/**
	 * find a free port for communication with Prolog
	 * 
	 * @return free port number
	 * @throws IOException
	 */
	public static int findFreePort() throws IOException {
		ServerSocket ss = new ServerSocket(0);
		int port = ss.getLocalPort();
		ss.close();
		return port;
	}

	/**
	 * quote atom
	 * 
	 * @param term unquoted atom
	 * @return quoted atom
	 */
	public static String quoteAtom(String term) {

		return "'" + term.replace("'", "\\'") + "'";
	}

	public static String splice(Collection<String> c, String delim) {
		if (c != null && !c.isEmpty()) {
			StringBuffer sb = new StringBuffer();
			for (Iterator<String> it = c.iterator(); it.hasNext();) {
				Object next = it.next();
				sb.append(next);
				if (it.hasNext()) {
					sb.append(delim);
				}
			}
			return sb.toString();
		}
		return "";

	}
	
	/**
	 * unquote atom (replace ' at the start and end)
	 * 
	 * @param atom quoted atom
	 * @return unquoted atom
	 */
	public static String unquoteAtom(String atom) {
		atom = atom.trim();
		if (atom.length() == 0 || atom.charAt(0) != '\'') {
			return atom;
		}
		atom = atom.substring(1, atom.length() - 1);
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < atom.length(); i++) {
			char c = atom.charAt(i);
			if (c == '\\') {
				int len = appendUnescapedChar(atom, i, sb);
				i += len - 1;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	/**
	 * unquote String or atom (replace " or ' at the start and end)
	 * 
	 * @param atom quoted atom
	 * @return unquoted atom
	 */
	public static String unquoteStringOrAtom(String atom) {
		atom = atom.trim();
		if (atom.length() == 0){
			return atom;
		}
		if( atom.charAt(0) == '\"' || atom.charAt(0) == '\'') {
			atom = atom.substring(1, atom.length() - 1);
		}
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < atom.length(); i++) {
			char c = atom.charAt(i);
			if (c == '\\') {
				int len = appendUnescapedChar(atom, i, sb);
				i += len - 1;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static int appendUnescapedChar(String image, int i, StringBuffer sb) {
		if (image.length() <= i + 1) {
			sb.append('\\');
			return 1;
		}
		char c = image.charAt(i + 1);
		if (Character.isDigit(c)) {
			return appendUnescapedOctalCharSpec(image, i, sb);
		}
		switch (c) {
		case 'a':
			// sb.append('\a'); there is no bell char in java
			return 2;
		case 'b':
			sb.append('\b');
			return 2;
		case 'c':
			// sb.append('\c'); not supported
			return 2;
		case '\n':
			// ignoring
			return 2;
		case 'f':
			sb.append('\f');
			return 2;
		case 'n':
			sb.append('\n');
			return 2;
		case 'r':
			sb.append('\r');
			return 2;
		case 't':
			sb.append('\t');
			return 2;
		case 'v':
			// sb.append('\v'); vertical tabs are not supported in java
			return 2;
		case 'x':
			return appendUnescapedHexCharSpec(image, i, sb);
		case '\\':
			sb.append('\\');
			return 2;
		case '\'':
			sb.append('\'');
			return 2;
		default:
			sb.append('\\');
			return 1;
//			sb.append(c);
//			return 2;
		}
	}

	private static int appendUnescapedOctalCharSpec(String image, int i,
			StringBuffer sb) {
		String val = "";
		int j = i + 2;
		while (j < image.length() && j < i + 4 && isOctDigit(image.charAt(j))) {
			val += image.charAt(j);
			j++;
		}
		sb.append((char) Integer.parseInt(val, 8));
		if (j < image.length() && image.charAt(j) == '\\') {
			return 1 + j - i;
		}
		return j - i;
	}

	private static int appendUnescapedHexCharSpec(String image, int i,
			StringBuffer sb) {

		String val = "";
		int j = i + 2;
		while (j < image.length() && j < i + 4 && isHexDigit(image.charAt(j))) {
			val += image.charAt(j);
			j++;
		}
		sb.append((char) Byte.parseByte(val));
		if (j < image.length() && image.charAt(j) == '\\') {
			return 1 + j - i;
		}
		return j - i;
	}

	private static boolean isHexDigit(char c) {

		return Character.isDigit(c) || 'a' <= Character.toLowerCase(c)
				&& Character.toLowerCase(c) <= 'f';
	}

	private static boolean isOctDigit(char c) {

		return Character.isDigit(c) || '0' <= c && c <= '7';
	}

	public static void split(String string, String search, Collection<String> results) {
		if (string == null) {
			return;
		}
		int i = -1;
		while ((i = string.indexOf(search, 0)) >= 0) {
			results.add(string.substring(0, i).trim());
			string = string.substring(i + search.length());
		}
		String rest = string.trim();
		if (rest.length() > 0) {
			results.add(rest);
		}

	}

	public static String[] split(String string, String search) {
		Vector<String> v = new Vector<String>();
		split(string, search, v);
		return v.toArray(new String[v.size()]);

	}

	public static String hideStreamHandles(String string, String replace) {
		int i = -1;
		String search = "$stream(";
		StringBuffer sb = new StringBuffer();
		while ((i = string.indexOf(search, 0)) >= 0) {
			sb.append(string.substring(0, i));
			sb.append(replace);
			int j = string.indexOf(')', i + search.length());
			string = string.substring(j + 1);
		}
		sb.append(string);
		return sb.toString();

	}

	public static String splice(Object[] c, String delim) {
		if (c != null && c.length > 0) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < c.length; i++) {
				if (i > 0) {
					sb.append(delim);
				}
				Object next = c[i];
				sb.append(next);

			}
			return sb.toString();
		}
		return "";
	}

	public static boolean flagsSet(int flags, int set) {

		return (flags & set) == set;
	}
	
	
	public static String guessEnvironmentVariables() {
		if (isMacOS()) {
			String home = System.getProperty("user.home");
			return "DISPLAY=:0.0, HOME=" + home;
		}
		return "";
	}

	
	/**
	 * guess executable name for SWI Prolog depending on the OS
	 * @return executable name for SWI Prolog
	 */
	public static String guessExecutableName() {

		String guessedExecutable = guessExecutableName__();
		Debug.info("Guessed Prolog executable with GUI: " + guessedExecutable);
		return guessedExecutable;

	}
	
	public static String getExecutablePreference() {
		if (isWindows()) {
			String exec = findWindowsExecutable(PDTConstants.WINDOWS_EXECUTABLES);
			if (exec.startsWith("\"")) {
				exec = exec.substring(1);
			}
			if (exec.endsWith("\"")) {
				exec = exec.substring(0, exec.length() - 1);
			}
			return exec;
		} else {
			return findUnixExecutable(PDTConstants.UNIX_COMMAND_LINE_EXECUTABLES);
		}
	}


	private static String commandLineArguments = null;
	
	public static String getCommandLineArguments() {
		Debug.debug("getCommandLineArguments start");
		if (commandLineArguments == null) {
		
			String swiExecutable;
			
			if (isWindows()) {
				swiExecutable = findWindowsExecutable(PDTConstants.WINDOWS_COMMAND_LINE_EXECUTABLES);			
			} else {
				swiExecutable = findUnixExecutable(PDTConstants.UNIX_COMMAND_LINE_EXECUTABLES);
			}
			
			String bits = "";
			try {
				Process p = Runtime.getRuntime().exec(new String[]{
						swiExecutable,
						"-g",
						"current_prolog_flag(address_bits,A),writeln(A),halt."});
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				bits = reader.readLine();
				p.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
	
			if (bits.equals("64")) {
				// no parameters for SWI-Prolog 64bit
				commandLineArguments = "";
			} else {
				commandLineArguments = PDTConstants.STACK_COMMMAND_LINE_ARGUMENTS;
			}

		}
		Debug.debug("getCommandLineArguments end: '" + commandLineArguments+ "'");
		
		return commandLineArguments;
	}

	public static String getCurrentSWIVersionFromCommandLine() throws IOException{
//		return "51118_64";// TEMPversion +"_"+bits;
		
			String swiExecutable;
			if (isWindows()) {
				swiExecutable = findWindowsExecutable(PDTConstants.WINDOWS_COMMAND_LINE_EXECUTABLES);			
			} else {
				swiExecutable = findUnixExecutable(PDTConstants.UNIX_COMMAND_LINE_EXECUTABLES);
			}
			
			String bits = "";
			String version ="";
			Process p = Runtime.getRuntime().exec(new String[]{
					swiExecutable,
					"-g",
					"current_prolog_flag(version,V),writeln(V),current_prolog_flag(address_bits,A),writeln(A),halt."});
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			version = reader.readLine();
			bits = reader.readLine();
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				// TR: fatal anyway:
				throw new RuntimeException(e);
			}
	

		return version +"_"+bits;
	}
	
	private static String guessExecutableName__() {

		if (isWindows()) {
			return "cmd.exe /c start \"cmdwindow\" /min "
				+ findWindowsExecutable(PDTConstants.WINDOWS_EXECUTABLES) + " " + getCommandLineArguments();
		}
		// return "xterm -e xpce"; // For Mac and Linux with console
		return findUnixExecutable(PDTConstants.UNIX_COMMAND_LINE_EXECUTABLES) + " " + getCommandLineArguments();

	}
	
	/**
	 * 
	 * @return the OS specific invocation command
	 */
	public static String getInvocationCommand() {
		if (isWindows()) {
			return "cmd.exe /c start \"cmdwindow\" /min ";
		} else {
			return "";
		}
	}

	public static String guessCommandLineExecutableName() {

		String guessedExecutable = guessCommandLineExecutableName__();
		Debug.info("Guessed Prolog executable WITHOUT GUI: " + guessedExecutable);
		return guessedExecutable;

	}
	
	private static String guessCommandLineExecutableName__() {

		if (isWindows()) {
			return //"cmd.exe /c start \"cmdwindow\" /min "
			findWindowsExecutable(PDTConstants.WINDOWS_COMMAND_LINE_EXECUTABLES) + " " + getCommandLineArguments();
		}
		// return "xterm -e xpce"; // For Mac and Linux with console
		return findUnixExecutable(PDTConstants.UNIX_COMMAND_LINE_EXECUTABLES) + " " + getCommandLineArguments();

	}

	/**
	 * Finds the current SWI-Prolog executable for UNIX/BSD-BASED OS
	 * @param unixCommandLineExecutables 
	 * @return the complete path of the executable otherwise it will return xpce
	 */
	private static String findUnixExecutable(String unixCommandLineExecutables) {
		String[] default_exec = unixCommandLineExecutables.split(",");
		
		// TODO shall we look for the env. variables as we do for Windows ?
		String[] appendPath = null;

		// Hack to resolve the issue of locating xpce in MacOS
		if (isMacOS()) {
			appendPath = new String[1];
			appendPath[0] = "PATH=$PATH:" + System.getProperty("user.home") + "/bin:/opt/local/bin";
		}

		try {
			
			for (String exec : default_exec) {

				Process process = Runtime.getRuntime().exec(
						"which " + exec, appendPath);
	
				if (process == null)
					return null;
	
				BufferedReader br = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				String path = br.readLine();
	
				if (path == null || path.startsWith("no " + default_exec))
					continue;
				else {
					return path;
				}
			}
			return default_exec[0];

		} catch (IOException e) {

			return default_exec[0];
		}
	}

	/**
	 * Finds the current SWI-Prolog executable for Windows OS
	 * @param executables 
	 * @return the complete path of the executable otherwise it will return plwin
	 */
	private static String findWindowsExecutable(String executables) {
		String[] default_exec = executables.split(",");
		String plwin = null;

		String path;
		try {

			Process process = Runtime.getRuntime().exec(
					"cmd.exe /c echo %PATH%");

			if (process == null)
				return default_exec[0];

			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			path = br.readLine();

			if (path == null)
				return default_exec[0];

			// TODO just search in case of executable was not found.
			String[] paths = split(path, ";");
			File exeFile = null;

			for (int i = 0; i < paths.length; i++) {
				
				for (String exec : default_exec) {
					if (exec.indexOf(".exe") == -1)
						exec += ".exe";

					String currPath = paths[i] + "\\" + exec;
					exeFile = new File(currPath);

					if (exeFile.exists()) {
						plwin = "\"" + currPath + "\"";
						break;
					}
				}
				if(plwin!=null){
					break;
				}
			}
			if(plwin== null){
				return default_exec[0];
			}
			return plwin;

		} catch (IOException e) {

			return default_exec[0];
		}
	}

	/**
	 * @param c
	 * @return
	 */
	public static boolean isVarChar(char c) {
		if (c == '_')
			return true;
		if (c >= 'A' && c <= 'Z')
			return true;
		if (c >= 'a' && c <= 'z')
			return true;
		if (c >= '0' && c <= '9')
			return true;
		return false;
	}

	/**
	 * @param c
	 * @return true if prefix is a variable prefix (upper case letter or underscore)
	 */
	public static boolean isVarPrefix(char c) {
		return (Character.isUpperCase(c) || c == '_');
	}
	
	/**
	 * @param prefix
	 * @return true if prefix is a functor prefix (lower case letter)
	 */
	public static boolean isFunctorPrefix(String prefix) {
		if (prefix == null | prefix.length() == 0)
			return false;
		if (prefix.charAt(0) >= 'a' && prefix.charAt(0) <= 'z')
			return true;
	
		return false;
	}

	/**
	 * @param prefix
	 * @return
	 */
	public static boolean isVarPrefix(String prefix) {
		if (prefix.length() == 0)
			return false;
		return isVarPrefix(prefix.charAt(0));
	}

	/**
	 * Returns true if c is a valid character as part of a Prolog
	 * predicate name that is NOT enclosed in simple quotes.
	 * @param c character in question
	 * @return 
	 */
	static public boolean isNormalPredicateNameChar(char c) {
		if (c >= 'a' && c <= 'z') return true;
		if (c >= '0' && c <= '9') return true;
		if (c >= 'A' && c <= 'Z') return true;
		if (c == '_' || c == ':')  return true;
		return false;
	}

	/**
	 * Returns true if c is a character that may be contained in a Prolog
	 * predicate name that IS enclosed in simple quotes.
	 * @param c character in question
	 * @return 
	 */
	static public boolean isSpecialPredicateNameChar(char c) {
		return (c == '\''  
			 || c == '\\'
			 || c == '.' 
			 || c == '+' 
			 || c == '-' 
			 || c == '*' 
             || c == '$'
        // TODO: add all the other special characters!
		);
	}

	
	/**
	 * Returns true if c is a valid character as part of a Prolog
	 * predicate name (including module definition).
	 * @param c character in question
	 * @return 
	 */
	public static boolean isPredicateNameChar(char c) {
		return (isNormalPredicateNameChar(c) || isSpecialPredicateNameChar(c));
	}

	public static boolean isNonQualifiedPredicateNameChar(char c) {
		return isPredicateNameChar(c) && c != ':';
	}

	public static boolean isFunctorChar(char c) {
		if (c >= 'a' && c <= 'z')
			return true;
		if (c >= '0' && c <= '9')
			return true;
		if (c >= 'A' && c <= 'Z')
			return true;
		if (c == '_')
			return true;
	
		return false;
	}


	public static boolean isSingleSecondChar(char c) {
		if (c >= '0' && c <= '9')
			return true;
		if (c >= 'A' && c <= 'Z')
			return true;
		return false;
	}


	/** 
	 * read text from file to String
	 * 
	 * @param f
	 * @return content of the file
	 */
	public static String readFromFile(File f) {
		StringBuffer buf = new StringBuffer();
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(f));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				buf.append(line + "\n");
			}
		} catch (Exception e) {
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ioe) {
				}
			}
		}
		return buf.toString();
	}

	public static String createExecutable(String invocation, String execution, String commandLineArguments, String startupFiles) {
		StringBuffer executable = new StringBuffer(invocation);
		executable.append(" ");
		if (isWindows()) {
			executable.append("\"");
		}
		executable.append(execution);
		if (isWindows()) {
			executable.append("\"");
		}
		
		if (commandLineArguments != null && !commandLineArguments.isEmpty() && !commandLineArguments.trim().isEmpty()) {
			executable.append(" ");
			executable.append(commandLineArguments);
		}
		if (startupFiles != null && !startupFiles.isEmpty() && !startupFiles.trim().isEmpty()) {
			executable.append(" -s ");
			executable.append(startupFiles);
		}
		return executable.toString();
	}

	/**
	 * quote atom if it isn't already quoted
	 * @param term atom (quoted or unquoted)
	 * @return quoted atom
	 */

	public static String quoteAtomIfNeeded(String term) {
		if (term.startsWith("'") && term.endsWith("'")) {
			return term;
		} else {
			return "'" + term.replace("'", "\\'") + "'";
		}
	}

	/**
	 * 
	 * @return a new standalone Prolog Interface
	 * @throws IOException
	 */
	public static PrologInterface newStandalonePrologInterface() throws IOException {
		return newStandalonePrologInterface(null);
	}
	
	/**
	 * 
	 * @param executable
	 * @return a new standalone Prolog Interface
	 * @throws IOException
	 */
	public static PrologInterface newStandalonePrologInterface(String executable) throws IOException {
		String tempDir = System.getProperty("java.io.tmpdir");
		copyConsultServerToTempDir(tempDir);
		SocketPrologInterface pif = new SocketPrologInterface(null);
		if (executable == null) {
			pif.setExecutable(Util.guessExecutableName());
		} else {
			pif.setExecutable(executable);
		}
		pif.setConsultServerLocation(Util.prologFileName(new File(tempDir, "consult_server.pl")));
		pif.setHost("localhost");
		pif.setTimeout("15000");
		pif.setStandAloneServer("false");
		pif.setHidePlwin(true);
		pif.setUseSessionPooling(true);
		return pif;
	}
	
	private static void copyConsultServerToTempDir(String tempDir) throws IOException {
		InputStream resourceAsStream;
		resourceAsStream = PrologInterface.class.getClassLoader().getResourceAsStream("library/socket/consult_server.pl");
		if (resourceAsStream == null) {
			resourceAsStream = PrologInterface.class.getClassLoader().getResourceAsStream("consult_server.pl");
		}
		if (resourceAsStream == null) {
			throw new RuntimeException("Cannot find consult_server.pl!");
		}
		File consultServerPl = new File(tempDir, "consult_server.pl");
		if (consultServerPl.exists()) {
			consultServerPl.delete();
		}
		copy(resourceAsStream, new FileOutputStream(consultServerPl));
	}

	private static Set<File> tempFiles = new HashSet<File>();
	
	public static void addTempFile(File tempFile) {
		if (tempFile != null) {
			tempFiles.add(tempFile);
		}
	}
	
	public static Set<File> getTempFiles() {
		return new HashSet<File>(tempFiles);
	}

	public static String getLogtalkStartupFile() {
		if (Util.isWindows()) {
			return "\"%LOGTALKHOME%\\integration\\logtalk_swi.pl\"";
		} else {
			String logtalkHome = System.getenv("LOGTALKHOME");
			if (logtalkHome != null) {
				return new File(logtalkHome, "integration/logtalk_swi.pl").getAbsolutePath();
			} else {
				return "";
			}
		}
	}
	
	public static String getLogtalkEnvironmentVariables() {
		if (Util.isWindows()) {
			return "";
		} else {
			StringBuffer buf = new StringBuffer();
			String guessedEnvironmentVariables = Util.guessEnvironmentVariables();
			if (!guessedEnvironmentVariables.isEmpty()) {
				buf.append(guessedEnvironmentVariables);
				buf.append(", ");
			}
			buf.append("LOGTALKHOME=");
			buf.append(System.getenv("LOGTALKHOME"));
			buf.append(", ");
			buf.append("LOGTALKUSER=");
			buf.append(System.getenv("LOGTALKUSER"));
			return buf.toString();
		}
	}
}

