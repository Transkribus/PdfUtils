package org.dea.util.pdf.alto;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;

public class AltoPdfExporter {
	private static final Logger logger = LoggerFactory.getLogger(AltoPdfExporter.class);
	private static final String ALTO_DIR = "alto";
	private static final String IMG_DIR = "OCRmaster";
	private static final String PDF_DIR = "PDF";
	private static final float JPEG_QUALITY = 0.9f;
	private static final float JPEG_QUALITY_IF_JPG = 0.4f;
	
	static int totalPdfs = 0;
		
	public static void main(String[] args) {

		if(args.length == 0 || args.length > 3){
			usage();
			return;
		}
		final String path;
		boolean createSinglePagePdfs = false;
		boolean doCompressTifOrJpg = false;
		path = args[args.length-1];
		
		for(int i = 0; i < args.length-1; i++){
			switch(args[i]){
			case "-s":
				createSinglePagePdfs = true;
				break;
			case "-j":
				doCompressTifOrJpg = true;
				break;
			default:
				usage();
				return;
			}
		}
		
		File dir = new File(path);
		if(!dir.exists()){
			logger.error("Not an existing directory: " + args[0]);
			usage();
			return;
		}
		
		logger.info("Searching documents in: " + dir.getAbsolutePath());
		logger.info("Single page PDF creation is " + (createSinglePagePdfs?"active":"inactive"));
		logger.info("TIF/JPG to JPEG compression is " + (doCompressTifOrJpg?"active":"inactive"));
		
		List<File> docDirs = new LinkedList<>();
		addDocDirs(dir, docDirs);
		
		int countDirs = 0;
		
		for(File d : docDirs){
			countDirs++;
			final String outFilePath = d.getAbsolutePath() + File.separator + d.getName() + ".pdf";
			File out = new File(outFilePath);
			logger.info("Creating PDF: " + d.getAbsolutePath() + " -> " + out.getName());
			
//			if (new File(outFilePath).exists()){
//				logger.info("PDF exists: " + outFilePath);
//				//move one layer up
//				try {
//					logger.info("new file location: " + d.getParentFile().getAbsolutePath() + File.separator + d.getName() + ".pdf");
//					//System.in.read();
//					FileUtils.moveFile(new File(outFilePath), new File(d.getParentFile().getAbsolutePath() + File.separator + d.getName() + ".pdf"));
//					//System.in.read();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//			}
//			else{
				try {
					createPdf(d, out, createSinglePagePdfs, doCompressTifOrJpg);
					logger.info(countDirs + " collection PDFs created.");
					totalPdfs++;
				} catch (Exception e) {
					logger.error("Could not create PDF for dir: " + d.getAbsolutePath(), e);
					continue;
				}
//			}
		}
	}

	private static void addDocDirs(File dir, List<File> docDirs) {
		//if is leaf
		File altoDir = new File(dir.getAbsolutePath() + File.separator + ALTO_DIR);
		File imgDir = new File(dir.getAbsolutePath() + File.separator + IMG_DIR);
		if(altoDir.exists() && imgDir.exists()){
			docDirs.add(dir);
		} else {
			File[] dirs = dir.listFiles(new FileFilter(){
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}});
			for(File f : dirs){
				addDocDirs(f, docDirs);
			}
		}
	}

	public static void createPdf(File dir, File out, boolean createSinglePagePdfs, boolean doCompressTif) throws DocumentException, IOException {
		List<Pair<File, File>> files = findFiles(dir);
		createPdf(files, out, createSinglePagePdfs, doCompressTif);
	}

	public static void createPdf(List<Pair<File, File>> files, File out, boolean createSinglePagePdfs, boolean doCompressTifOrJpg) throws DocumentException, IOException {
		String pdfDir = null;
		if(createSinglePagePdfs){
			pdfDir = out.getParent() + File.separator + PDF_DIR;
			new File(pdfDir).mkdir();
		}
		long start = System.currentTimeMillis();
		AltoPdfDocument pdf = new AltoPdfDocument(out);
		for(Pair<File, File> e : files){
			
			Image img;
			File imgFile = e.getLeft();
			File tmp = null;
			final String imgName = imgFile.getName();
			if(isTif(imgName) && doCompressTifOrJpg){
				tmp = new File(imgFile.getParent() + File.separator 
						+ FilenameUtils.getBaseName(imgFile.getName()) + "_tmp.jpeg");
				TiffToJpg(imgFile, tmp, JPEG_QUALITY);
				img = Image.getInstance(tmp.getAbsolutePath());				
			} else if(isJPG(imgName) && doCompressTifOrJpg){
				tmp = new File(imgFile.getParent() + File.separator 
						+ FilenameUtils.getBaseName(imgFile.getName()) + "_tmp.jpeg");
				JpgToJpg(imgFile, tmp, JPEG_QUALITY_IF_JPG);
				img = Image.getInstance(tmp.getAbsolutePath());				
			}else {			
				img = Image.getInstance(imgFile.getAbsolutePath());
			}
			

			//to avoid size adoption a second time - so make a second instance for usage in the single page export
			Image singleImg = Image.getInstance(img);
			
			File xmlFile = null;
			if (e != null){
				xmlFile = e.getRight();
			}
			
			pdf.addPage(img, xmlFile, false);
			
			if(createSinglePagePdfs){
				File singlePdfOut = new File(pdfDir + File.separator + FilenameUtils.getBaseName(imgFile.getName())+".pdf");
				createPdf(singleImg, xmlFile, singlePdfOut);
			}
			if(tmp != null){
				tmp.delete();
			}
		}
		logger.info(files.size() + " single PDFs created.");
		totalPdfs += files.size();
		logger.info("--- (" + totalPdfs + ") total PDFs created.");
		
		pdf.close();
		long end = System.currentTimeMillis();
		logger.info(end-start + " ms");
	}

	private static boolean isTif(String imgName) {
		return imgName.endsWith("tiff") || imgName.endsWith("tif")
				|| imgName.endsWith("TIFF") || imgName.endsWith("TIF");
	}
	
	private static boolean isJPG(String imgName) {
		return imgName.endsWith("jpg") || imgName.endsWith("jpeg")
				|| imgName.endsWith("JPG") || imgName.endsWith("JPEG");
	}

	public static void createPdf(Image img, File alto, File singlePdfOut) throws IOException, DocumentException {
		//logger.info("Creating single page PDF: " + singlePdfOut.getAbsolutePath());
		AltoPdfDocument singlePdf = new AltoPdfDocument(singlePdfOut);
		singlePdf.addPage(img, alto, false);
		singlePdf.close();
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
			final String name = FilenameUtils.getBaseName(img.getName());
			File xml = new File(altoDir.getAbsolutePath() + File.separator + name + XML_EXT);
			if(!xml.exists()){
				xml = null;
				//throw new IOException("No XML found for file " + img.getAbsolutePath() + " at " + altoDir.getAbsolutePath());
			}
			files.add(Pair.of(img, xml));
		}
		return files;
	}
	
	private static void usage() {
		System.out.println("Use: java -jar jarFileName [-s] inputDir\n-s\talso create single PDF for each page\n-j\tcompress TIF to JPEG");
		return;		
	}
	

	public static File TiffToJpg(File tiffFile, File out, final float quality) throws IOException {
	    SeekableStream s = new FileSeekableStream(tiffFile);
	    TIFFDecodeParam param = null;
	    ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
	    RenderedImage op = dec.decodeAsRenderedImage(0);
	    FileOutputStream fos = new FileOutputStream(out);
	    
	    JPEGEncodeParam params = new JPEGEncodeParam();
	    params.setQuality(quality);
	    
		ImageEncoder encoder = ImageCodec.createImageEncoder("jpeg", fos,
				params);
		encoder.encode(op);
	    
	    fos.close();
	    return out;
	}
	
	public static File JpgToJpg(File tiffFile, File out, final float quality) throws IOException {
	    SeekableStream s = new FileSeekableStream(tiffFile);
	    TIFFDecodeParam param = null;
	    ImageDecoder dec = ImageCodec.createImageDecoder("jpeg", s, param);
	    RenderedImage op = dec.decodeAsRenderedImage(0);
	    FileOutputStream fos = new FileOutputStream(out);
	    
	    JPEGEncodeParam params = new JPEGEncodeParam();
	    params.setQuality(quality);
	    
		ImageEncoder encoder = ImageCodec.createImageEncoder("jpeg", fos,
				params);
		encoder.encode(op);
	    
	    fos.close();
	    return out;
	}
	
	/*
	 * another possibility to compress jpg when input is also jpg
	 */
	public static File compressJpg(File jpgFile, File out){
		InputStream inputStream;
        try {
            inputStream = new FileInputStream(jpgFile);
            BufferedImage image = ImageIO.read(inputStream);              
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();      
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY_IF_JPG);      
           
            
        	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        	ImageOutputStream ios = ImageIO.createImageOutputStream(bytes);
        	writer.setOutput(ios);

        	IIOImage iioImage = new IIOImage(image, null, null);
        	writer.write(null, iioImage, param);
        	ios.close();
        	writer.dispose();
        	
        	FileOutputStream fos = new FileOutputStream(out);
        	fos.write(bytes.toByteArray());
        	return out;
            
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
	}
}
