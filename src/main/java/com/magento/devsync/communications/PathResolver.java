package com.magento.devsync.communications;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;

//TODO: THIS NEEDS TO TAKE INTO ACCOUNT MOUNTS FOR SERVER SIDE MAPPINGS.
//TOOD: NEEDS TO SUPPORT ~ FOR HOME DIRECTORY AS WELL IN CLIENT SIDE MAPPINGS.
public abstract class PathResolver {
	
	public static String fingerprint(File localPath) {
		// TODO:
//		MessageDigest md = MessageDigest.getInstance("MD5");
//		try (InputStream is = Files.newInputStream(localPath);
//		     DigestInputStream dis = new DigestInputStream(is, md)) {
//		  /* Read decorated stream (dis) to EOF as normal... */
//		}
//		byte[] digest = md.digest();
		return Long.toString(localPath.length());
	}
	
	abstract public File localPath(String abstractPath);
	
	abstract public String abstractPath(File localPath);

	/**
	 * Return file permissions mode of specified file or directory.
	 * @param f File to return mode of.
	 * @return The file mode.
	 */
	public static int mode(File f) {
		return f.canExecute() ? 1 : 0;
	}
	
//	/**
//	 * Change the file permissions of the specified file or directory.
//	 * @param f File/directory to change.
//	 * @param mode The new mode to set.
//	 */
//	public static void changeMode(File f, int mode) {
//		XXX
//	}

}
