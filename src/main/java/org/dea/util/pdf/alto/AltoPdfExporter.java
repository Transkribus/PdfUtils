package org.dea.util.pdf.alto;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.dea.util.file.FileUtils;

import com.itextpdf.text.DocumentException;

public class AltoPdfExporter extends Observable {
	private static final Logger logger = Logger.getLogger(AltoPdfExporter.class);
	private static final String ALTO_DIR = "alto";
	private static final String IMG_DIR = "OCRmaster";
	
	public static void main(String[] args) {
		if(args.length != 2){
			usage();
			return;
		}	
		File dir = new File(args[0]);
		if(!dir.exists()){
			logger.error("Not an existing directory: " + args[0]);
			usage();
			return;
		}
		File out = new File(args[1]);
		
		try {
			createPdf(dir, out);
		} catch (DocumentException | IOException e) {
			logger.error("Could not create PDF!", e);
		}
	}

	public static void createPdf(File dir, File out) throws DocumentException, IOException {
		List<Pair<File, File>> files = findFiles(dir);
		createPdf(files, out);
	}

	public static void createPdf(List<Pair<File, File>> files, File out) throws DocumentException, IOException {
		long start = System.currentTimeMillis();
		AltoPdfDocument pdf = new AltoPdfDocument(out);
		for(Pair<File, File> e : files){
			pdf.addPage(e.getLeft(), e.getRight(), true);
		}
		pdf.close();
		long end = System.currentTimeMillis();
		logger.info(end-start + " ms");
	}

	public static List<Pair<File, File>> findFiles(File dir) throws IOException {
		final File imgDir = new File(dir.getAbsolutePath() + File.separator + IMG_DIR);
		final File altoDir = new File(dir.getAbsolutePath() + File.separator + ALTO_DIR);
		return findFiles(imgDir, altoDir);
	}

	public static List<Pair<File, File>> findFiles(File imgDir, File altoDir) throws IOException {
		final String XML_EXT = ".xml";
		File[] imgFilesArr = imgDir.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				String[] exts = {"jpeg", "jpg", "jp2", "TIF", "TIFF", "tif", "tiff"};
				for(String s : exts){
					if(name.endsWith(s)){
						return true;
					}
				}
				return false;
			}
		});
		List<File> imgFiles = new ArrayList<>(Arrays.asList(imgFilesArr));
		Collections.sort(imgFiles);
		
		List<Pair<File, File>> files = new ArrayList<>(imgFiles.size());
		for(File img : imgFiles){
			final String name = FileUtils.getFileNameWithoutExtension(img);
			File xml = new File(altoDir.getAbsolutePath() + File.separator + name + XML_EXT);
			if(!xml.exists()){
				throw new IOException("No XML found for file " + img.getAbsolutePath() + " at " + altoDir.getAbsolutePath());
			}
			files.add(Pair.of(img, xml));
		}
		return files;
	}
	
	private static void usage() {
		System.out.println("Use: java -jar jarFileName inputDir pdfFileName");
		return;		
	}
}
