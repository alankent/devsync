package com.magento.devsync.communications;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

import com.magento.devsync.config.Mount;
import com.magento.devsync.config.SyncRule;

public abstract class PathResolver {

	/**
	 * Convert a client relative path to a real file name that can be used to open files.
	 * On the client, "~/" is permitted at the start of the path to refer to the users home directory.
	 * On the server, the configuration file mapping is used.
	 */
	abstract public File localPath(String abstractPath);
	
	public static String fingerprint(File localPath) {
		// MD5 checksum the file.
		String fingerprint = localPath.canExecute() ? "exe-" : "plain-";
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] readBuf = new byte[8192];
			try (InputStream is = new FileInputStream(localPath);
					DigestInputStream dis = new DigestInputStream(is, md)) {
				while (dis.read(readBuf) >= 0) {
					// Empty - it is merging into the MD5 checksum.
				}
			}
			byte[] digest = md.digest();
			fingerprint += DatatypeConverter.printHexBinary(digest);
		} catch (Exception e) {
			throw new RuntimeException("Failed to form fingerprint of " + localPath, e);
		}
		return fingerprint;
	}
	
	/**
	 * Create a local path by joining the mount path and sync rule path.
	 */
	public static String path(Mount m, SyncRule sr) {
		if (m.local == null || m.local == "" || m.local == ".") {
			return sr.path;
		}
		return m.local + "/" + sr.path;
	}
}
