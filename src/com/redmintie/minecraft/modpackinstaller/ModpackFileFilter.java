package com.redmintie.minecraft.modpackinstaller;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ModpackFileFilter extends FileFilter {
	@Override
	public String getDescription() {
		return "Minecraft Modpacks (*.mpk)";
	}
	@Override
	public boolean accept(File file) {
		return file.isDirectory() || file.getName().endsWith(".mpk");
	}
}