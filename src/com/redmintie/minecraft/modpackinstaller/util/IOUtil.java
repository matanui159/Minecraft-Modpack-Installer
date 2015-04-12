package com.redmintie.minecraft.modpackinstaller.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {
	private static byte[] buffer = new byte[1024];
	public static void copy(InputStream in, OutputStream out) throws IOException {
		int length = 0;
		while ((length = in.read(buffer)) > 0) {
			out.write(buffer, 0, length);
		}
	}
	public static void makeFileHidden(File file) {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			try {
				Runtime.getRuntime().exec("attrib +H \"" + file.getAbsolutePath() + "\"").waitFor();
			} catch (Exception ex) {
				Logger.warning("Could not make file '" + file.getAbsolutePath() + "' hidden.");
			}
		}
	}
	public static void deleteFile(File file) {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				deleteFile(child);
			}
		}
		file.delete();
	}
}