package com.redmintie.minecraft.modpackinstaller;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.google.gson.Gson;
import com.redmintie.minecraft.modpackinstaller.util.IOUtil;
import com.redmintie.minecraft.modpackinstaller.util.Logger;

public class ModpackInstaller implements Runnable {
	private JProgressBar bar = new JProgressBar();
	private int runs = 0;
	private int count = 0;
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.warning("Could not set system Look and Feel.");
		}
		SwingUtilities.invokeLater(new ModpackInstaller());
	}
	@Override
	public void run() {
		// runs it in a different Thread
		runs++;
		if (runs == 1) {
			new Thread(this, "Minecraft Modpack Installer Thread").start();
			return;
		}
		
		// creates the JFrame
		JFrame frame = new JFrame("Minecraft Modpack Installer");
		Logger.setFrame(frame);
		
		// creates the JLabel
		JLabel label = new JLabel("Installing Modpack...");
		label.setBorder(new EmptyBorder(10, 10, 10, 10));
		frame.add(label, BorderLayout.CENTER);
		
		// creates the JProgressBar
		bar.setIndeterminate(true);
		frame.add(bar, BorderLayout.SOUTH);
		
		// sets the icon
		try {
			frame.setIconImage(ImageIO.read(getClass().getResourceAsStream("/res/icon.png")));
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.warning("Could not set icon.");
		}
		
		// shows the JFrame
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		// finds the Minecraft directory
		File minecraft = null;
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			minecraft = new File(new File(System.getenv("appdata")), ".minecraft");
		} else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			minecraft = new File(new File(System.getProperty("user.home")), "Library/Application Support/minecraft");
		} else {
			minecraft = new File(new File(System.getProperty("user.home")), ".minecraft");
		}
		if (!minecraft.isDirectory()) {
			Logger.error("Could not find Minecraft directory.");
		}
		
		// creates a temporary folder
		int id = 0;
		while (new File(minecraft, "modpacks/.temp" + id).isDirectory()) {
			id++;
		}
		File temp = new File(minecraft, "modpacks/.temp" + id);
		temp.mkdirs();
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			IOUtil.makeFileHidden(temp);
		}
		
		// chooses a Modpack
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Choose a Modpack");
		chooser.setFileFilter(new ModpackFileFilter());
		File modpack = null;
		if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			modpack = chooser.getSelectedFile();
			if (!modpack.exists()) {
				IOUtil.deleteFile(temp);
				Logger.error("Modpack does not exist.");
			}
		} else {
			IOUtil.deleteFile(temp);
			System.exit(0);
		}
		
		// extracts the Modpack
		ZipInputStream zip = null;
		String name = null;
		int files = 0;
		try {
			zip = new ZipInputStream(new FileInputStream(modpack));
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.getName().endsWith("/") || entry.getName().endsWith("\\")) {
					continue;
				}
				files++;
				if (entry.getName().startsWith("version/") || entry.getName().startsWith("version\\")) {
					if (entry.getName().endsWith(".json")) {
						name = entry.getName().substring(8, entry.getName().length() - 5);
					}
				}
				File file = new File(temp, entry.getName());
				if (!file.getParentFile().isDirectory()) {
					file.getParentFile().mkdirs();
				}
				FileOutputStream stream = new FileOutputStream(file);
				IOUtil.copy(zip, stream);
				stream.close();
			}
		} catch (Exception ex) {
			IOUtil.deleteFile(temp);
			ex.printStackTrace();
			Logger.error("Could not extract the Modpack.");
		} finally {
			if (zip != null) {
				try {
					zip.close();
				} catch (Exception ex) {
					ex.printStackTrace();
					Logger.warning("Could not close the Modpack.");
				}
			}
		}
		
		// checks the Modpack name
		if (name == null) {
			IOUtil.deleteFile(temp);
			Logger.error("Could not get the Modpack name.");
		} else if (new File(minecraft, "modpacks/" + name).isDirectory() || new File(minecraft, "versions/" + name).isDirectory()) {
			if (JOptionPane.showConfirmDialog(frame,
					"<html>A Modpack with the same name already exists.<br>"
					+ "Would you like to Overwrite it?</html>",
					frame.getTitle(), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				IOUtil.deleteFile(new File(minecraft, "modpacks/" + name + "/mods"));
			} else {
				IOUtil.deleteFile(temp);
				System.exit(0);
			}
		}
		
		// installs the files
		try {
			install(minecraft, temp, name, files);
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.error("Could not install the Modpack.");
		} finally {
			IOUtil.deleteFile(temp);
		}
		
		// creates the profile
		HashMap<String, String> profile = new HashMap<String, String>();
		profile.put("name", name);
		profile.put("lastVersionId", name);
		profile.put("gameDir", new File(minecraft, "modpacks/" + name).getAbsolutePath());
		profile.put("javaArgs", "-Xmx2G");
		
		// adds the profile
		bar.setIndeterminate(true);
		try {
			Gson gson = new Gson();
			File profiles = new File(minecraft, "launcher_profiles.json");
			FileReader reader = new FileReader(profiles);
			HashMap json = gson.fromJson(reader, HashMap.class);
			reader.close();
			((Map)json.get("profiles")).put(name, profile);
			PrintWriter writer = new PrintWriter(new FileWriter(profiles));
			writer.println(gson.toJson(json));
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.error("Could not add profile.");
		}
		
		JOptionPane.showMessageDialog(frame,
				"<html>Modpack successfully installed.<br>"
				+ "Minecraft Launcher will need to be restarted for the changes to take effect.</html>",
				frame.getTitle(), JOptionPane.INFORMATION_MESSAGE);
		System.exit(0);
	}
	private void install(File minecraft, File temp, String modpack, int files) throws IOException {
		bar.setMaximum(files);
		bar.setIndeterminate(false);
		install(minecraft, temp, modpack, files, temp);
	}
	private void install(File minecraft, File temp, String modpack, int files, File file) throws IOException {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				install(minecraft, temp, modpack, files, child);
			}
		} else {
			count++;
			bar.setValue(count);
			String name = file.getAbsolutePath().substring(temp.getAbsolutePath().length() + 1);
			File dest = null;
			if (name.startsWith("modpack/") || name.startsWith("modpack\\")) {
				dest = new File(minecraft, "modpacks/" + modpack + "/" + name.substring(8));
			}
			if (name.startsWith("version/") || name.startsWith("version\\")) {
				dest = new File(minecraft, "versions/" + modpack + "/" + name.substring(8));
			}
			if (dest != null) {
				if (!dest.getParentFile().isDirectory()) {
					dest.getParentFile().mkdirs();
				}
				FileInputStream in = new FileInputStream(file);
				FileOutputStream out = new FileOutputStream(dest);
				IOUtil.copy(in, out);
				in.close();
				out.close();
			}
		}
	}
}