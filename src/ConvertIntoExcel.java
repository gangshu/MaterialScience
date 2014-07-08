
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class ConvertIntoExcel {

	public static void main(String[] args) {
		
		String inFolder = "F:/HTPT_cooling";
		String outFolder = "F:/";
		
		convert(inFolder, outFolder);
	}
	
	private static void convert(String inFolder, String outFolder){
		
		ArrayList<String> files = new ArrayList<String>();
		
		/*
		 * Search for qualified files
		 */
		for (String file : Directory.getDirectoryFiles(inFolder, ".txt", true))
			if (file.matches("^.*Fe [0-9]+.*$"))
				files.add(file);
		
		/*
		 * Group them by temperature
		 */
		HashMap<String, ArrayList<String>> temperature_group = new HashMap<String, ArrayList<String>>();
		for (String file : files){
			String lastPart = file;
			lastPart = lastPart.replace(inFolder, "");
			lastPart = lastPart.replace("/", "");
			lastPart = lastPart.substring(0, lastPart.indexOf("-"));
			
			ArrayList<String> group = temperature_group.get(lastPart);
			if (group == null)
				group = new ArrayList<String>();
			group.add(file);
			temperature_group.put(lastPart, group);
		}
		
		Iterator<Entry<String, ArrayList<String>>> iter = temperature_group.entrySet().iterator();
		while (iter.hasNext()){
			Entry<String, ArrayList<String>> entry = iter.next();
			
			//Collect data files from group 
			int maxLen = 0;
			ArrayList<ArrayList<Data>> datALs = new ArrayList<ArrayList<Data>>();
			for (String file : entry.getValue()){
				ArrayList<Data> datAL = read(file);
				datALs.add(datAL);
				maxLen = datAL.size() > maxLen ? datAL.size() : maxLen;
			}
			
			//Write into Excel file
			String temperature = entry.getKey();
			String outfile = outFolder + "/" + temperature + ".csv";
			outfile.replace("\\", "/");
			outfile.replace("//", "/");
			ArrayList<String> outAL = new ArrayList<String>();
			for (int i = 0; i < maxLen; i++){
				
				String outStr = "";
				
				for (ArrayList<Data> datAL : datALs){

					if (i == 0){
						String lastPart = datAL.get(0).file;
						lastPart = lastPart.replace(inFolder, "");
						lastPart = lastPart.replace("/", "");
						lastPart = lastPart.replace(".txt", "");
						outStr = outStr + lastPart + ",";
					}
					else{
						outStr = outStr + ",";
					}
					
					if (i < datAL.size()){
						Data dat = datAL.get(i);
						outStr = outStr + dat.Deg + "," + dat.CPS + ",";
					}
					else{
						outStr = outStr + "," + ",";
					}
				}
				
				outAL.add(outStr);
			}
			
			write(outfile, outAL);
			
			System.out.println(temperature + "\t" + datALs.size());
		}
		
	}
	
	private static class Data{
		String file;
		String Deg, CPS, ESD;
	}
	
	private static ArrayList<Data> read(String file){
		
		ArrayList<Data> datAL = new ArrayList<Data>();
		
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String inline;
			boolean bStart = false;
			while ((inline = in.readLine()) != null){
				if (inline.matches("^Range [0-9]*")){
					bStart = true;
					continue;
				}
				if (bStart){
					inline = inline.replaceAll("[ ]+", " ");
					inline = inline.replace(" ", "\t");
					inline = inline.replace("\t\t", "\t");
					String[] items = inline.split("\t");
					Data data = new Data();
					data.file = file;
					data.Deg = items[1];
					data.CPS = items[2];
					data.ESD = items[3];
					datAL.add(data);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return datAL;		
	}
	
	public static void write(String outFile, ArrayList<String> outputAL) {

		BufferedWriter out = null;
		try {
			Directory.createAbsolutePath(outFile);
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outFile, false)));
			if (outputAL != null)
				for (String str : outputAL)
					out.write(str + "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}



class Directory {

	public static String formatAbsolutePath(String absPath) {
		absPath = absPath.replaceAll("//", "/");
		absPath = absPath.replaceAll("//", "/");

		if (absPath.endsWith("/"))
			absPath = absPath.substring(0, absPath.length() - 1);

		return absPath;
	}

	public static void createAbsolutePath(String absPath) {

		try {
			String tarfileDir = absPath.substring(0, absPath.lastIndexOf("/"));
			File f = new File(tarfileDir);
			if (!f.exists()) {
				f.mkdirs();
			}
		} catch (Exception e) {
			e.getStackTrace();
		}

	}

	public static void copyFile(String srcfile, String tarfile) {

		createAbsolutePath(tarfile);

		try {
			File sourceFile = new File(srcfile);
			File destFile = new File(tarfile);

			if (!destFile.exists()) {
				destFile.createNewFile();
			}
			FileChannel source = null;
			FileChannel destination = null;
			try {
				source = new FileInputStream(sourceFile).getChannel();
				destination = new FileOutputStream(destFile).getChannel();
				destination.transferFrom(source, 0, source.size());
			} finally {
				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void copyAbsolutePath(String srcPath, String tarPath,
			String fileType, boolean goDeeper) {
		srcPath = formatAbsolutePath(srcPath);
		ArrayList<String> srcFileList = getDirectoryFiles(srcPath, fileType,
				goDeeper);

		String tarfile;
		if (srcFileList != null)
			for (String srcfile : srcFileList) {
				tarfile = tarPath + "/" + srcfile.replace(srcPath, "");
				tarfile = formatAbsolutePath(tarfile);
				createAbsolutePath(tarfile);
				// System.out.println(tarfile);
				copyFile(srcfile, tarfile);
			}

	}

	public static void renameFile(String oldFileName, String newFileName) {

		try {
			File file = new File(oldFileName);
			file.renameTo(new File(newFileName));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void delDirectoryFiles(String absPath, String fileType,
			boolean goDeeper) {
		try {
			File f = new File(absPath);
			if (f.isDirectory()) {
				File[] fList = f.listFiles();
				for (int j = 0; j < fList.length; j++)
					if (fList[j].isDirectory() && goDeeper)
						delDirectoryFiles(fList[j].getPath(), fileType,
								goDeeper);

				for (int j = 0; j < fList.length; j++)
					if (fList[j].isFile())
						if (fList[j].getPath().endsWith(fileType))
							fList[j].delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void delDirectoryEmptyFiles(String absPath, String fileType,
			boolean goDeeper) {
		try {
			File f = new File(absPath);

			if (f.isDirectory()) {
				File[] fList = f.listFiles();
				for (int j = 0; j < fList.length; j++)
					if (fList[j].isDirectory() && goDeeper)
						delDirectoryEmptyFiles(fList[j].getPath(), fileType,
								goDeeper);

				for (int j = 0; j < fList.length; j++)
					if (fList[j].isFile())
						if (fList[j].getPath().endsWith(fileType)
								&& fList[j].length() == 0)
							fList[j].delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<String> getDirectoryFolders(String absPath) {
		try {
			ArrayList<String> folderList = new ArrayList<String>();
			absPath = formatAbsolutePath(absPath);
			File f = new File(absPath);
			if (f.isDirectory()) {
				File[] fList = f.listFiles();
				for (int j = 0; j < fList.length; j++)
					if (fList[j].isDirectory()) {
						folderList.add(new String(fList[j].getPath()));
					}
			}
			return folderList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static ArrayList<String> getDirectoryFiles(String absPath,
			String fileType, boolean goDeeper) {
		try {
			absPath = formatAbsolutePath(absPath);
			Dir dir = new Dir();
			dir.getDir(absPath, fileType, goDeeper);
			return dir.FileList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static class Dir {
		public ArrayList<String> FileList = new ArrayList<String>();

		public void getDir(String absPath, String fileType, boolean goDeeper)
				throws Exception {
			try {
				File f = new File(absPath);
				if (f.isDirectory()) {
					File[] fList = f.listFiles();
					for (int j = 0; j < fList.length; j++)
						if (fList[j].isDirectory() && goDeeper) {
							getDir(fList[j].getPath(), fileType, goDeeper);
						}

					String str = "";

					for (int j = 0; j < fList.length; j++)
						if (fList[j].isFile()) {
							str = fList[j].getPath();
							if (str.endsWith(fileType) || fileType.equals("*")) {
								str = str.replace("\\", "/");
								FileList.add(new String(str));
							}
						}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
