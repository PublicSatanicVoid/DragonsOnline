package mc.dragons.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileUtil {
	public static void copyFolder(File source, File dest) {
		try {
			Files.walk(source.toPath(), FileVisitOption.FOLLOW_LINKS).forEach(s -> {
				try {
					Files.copy(s, dest.toPath().resolve(source.toPath().relativize(s)), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ignored) {}
			});
		} catch (IOException ignored) {
		}
	}

	public static void deleteFolder(File folder) {
		if(!folder.delete()) {
			File[] sub = folder.listFiles();
			for(File f : sub) {
				deleteFolder(f);
			}
			folder.delete(); // don't call recursively in case something else is preventing it from deleting
		}
	}
}
