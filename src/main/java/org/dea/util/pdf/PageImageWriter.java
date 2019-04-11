package org.dea.util.pdf;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Observable;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.io.RandomAccessRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.DocumentException;

public class PageImageWriter extends Observable {
	private static final Logger logger = LoggerFactory.getLogger(PageImageWriter.class);
	
	protected String extractDir;
	
	/**
	 * Object constructor uses user's temporary directory by default
	 */
	public PageImageWriter() {
		this.extractDir = System.getProperty("java.io.tmpdir") + File.separator + "img" + File.separator;
	}	
	

    public String getExtractDirectory() {
    	return extractDir;
    }
    
    /**
     * Parses a PDF document and renders all images to an output directory.
     * @param filepath Path to PDF document
     * @param outDir Output directory will be expanded with pdf basename
     * @throws IOException
     * @throws DocumentException
     * @return name of created folder
     */
    public String extractImages(String filepath, String outDir) 
    	throws IOException, SecurityException {
    	
        
    	// extract file name
    	File file = new File(filepath);
    	final String name = FilenameUtils.getBaseName(file.getName());

    	// create file reader for input pdf and make sure to use only half of the available free memory
    	PDDocument document = PDDocument.load(file, MemoryUsageSetting.setupMixed(Runtime.getRuntime().freeMemory() /2));
    	PDFRenderer renderer = new PDFRenderer(document);
    	// create output folder/filename(s)
        File dir = new File(outDir, name);   
        
        if (!dir.exists() && !dir.mkdirs()){
        	throw new IOException("The output directory could not be created at " + dir.getAbsolutePath() + " - Please choose a writeable path.");
        }
        
        // set extract directory name
        extractDir = dir.getAbsolutePath();
        
        final String out = extractDir + File.separator 
        		+ name + "-%04d.%s";

        // prepare parsing objects
        
        for (int i = 0; i < document.getNumberOfPages(); i++) {
        	setChanged();
        	notifyObservers("Extracting image for page " + (i+1) + " from PDF");
        	BufferedImage bim = renderer.renderImageWithDPI(i, 300);
        	final String fileName = String.format(out, i+1, "jpg");
        	logger.debug("Writing page " + i + " to: " + fileName);
        	ImageIO.write(bim, "jpg", new File(fileName));
        	
        	logger.debug(getClass().getName()+ " on page "+ (i+1));
        }
        document.close();
    	
        return dir.getAbsolutePath();
    }
    
    public void extractText(String filePath, String outDir) throws IOException{
    	
    	// extract file name
    	File file = new File(filePath);
    	final String name = FilenameUtils.getBaseName(file.getName());
    	
        File dir = new File(outDir, name);   
        
        if (!dir.exists() && !dir.mkdirs()){
        	throw new IOException("The output directory could not be created at " + dir.getAbsolutePath() + " - Please choose a writeable path.");
        }
        
        String imageDirString = "Y:/HTR/für_Digitexx/NAN_2020_(National_Archive_Netherlands)/Images/" + name;
        File imgDir = new File(imageDirString);
        
        logger.debug("imgDir number of files: " + imgDir.listFiles().length);
                
        // set extract directory name
        extractDir = dir.getAbsolutePath();
        
        final String out = extractDir + File.separator 
        		+ name + "-%04d.%s";
    	// PDFBox 2.0.8 require org.apache.pdfbox.io.RandomAccessRead
    	// import org.apache.pdfbox.io.RandomAccessFile;
        PDFTextStripper pdfStripper = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        
        // PDFBox 2.0.8 require org.apache.pdfbox.io.RandomAccessRead 
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        PDFParser parser = new PDFParser(randomAccessFile);
        parser.parse();
        cosDoc = parser.getDocument();
        pdfStripper = new PDFTextStripper();
        pdDoc = new PDDocument(cosDoc);
        
        pdfStripper.setStartPage(1);
        pdfStripper.setEndPage(pdDoc.getNumberOfPages());
        String parsedText = pdfStripper.getText(pdDoc);
        
        parsedText = parsedText.replaceAll("p. +", "p. ");
        //logger.debug("parsed text: " + parsedText);
        
        logger.debug("number of pages in this doc: "+ imgDir.listFiles().length);

        int pageNr = 3;
        for (int i = 3; i<imgDir.listFiles().length; i++){
        	
        	String indexString1 = "p. "+i+" ";
        	String indexString2 = "p. "+(i+1)+" ";
        	
        	String altIndexString1 = "p."+i;
        	String altIndexString2 = "p."+(i+1);
        	
        	logger.debug("i is"+ i);
        	
        	if (i == 18){
        		i=i+3;
        		pageNr = pageNr+4;
        		continue;
        	}
        	
        	if (i == 3){
                String resultText = getTextForPage(parsedText, null, indexString1);
                writeTextFile(out, pageNr, resultText);
        	}
        	
        	if ((parsedText.indexOf(indexString1) != -1 || parsedText.indexOf(altIndexString1) != -1) && (parsedText.indexOf(indexString2) != -1 || parsedText.indexOf(altIndexString2) != -1)){
            	String resultText2 = getTextForPage(parsedText, indexString1, indexString2);
            	pageNr += 1;
            	writeTextFile(out, pageNr, resultText2);
        	}
        	else{
        		
        		//first try to add another " "
//        		String indexString1Alt = "p.  "+i;
//            	String indexString2Alt = "p.  "+(i+1);
//            	if (parsedText.indexOf(indexString2Alt) != -1){
//                	String resultText2 = getTextForPage(parsedText, indexString1, indexString2Alt);
//                	pageNr += 1;
//                	writeTextFile(out, pageNr, resultText2);
//            	}
//            	else if (parsedText.indexOf(indexString1Alt) != -1){
//                	String resultText2 = getTextForPage(parsedText, indexString1Alt, indexString2);
//                	pageNr += 1;
//                	writeTextFile(out, pageNr, resultText2);
//            	}
//            	else{
            		String tmpIndexString = "p. "+(i+2);
            		String [] tmp = parsedText.split("p. "+(i+1)); 
            		
            		logger.debug("index i with troubles: " + i);
            		logger.debug("length of tmp: " + tmp.length);
            		System.in.read();
            		
            		if (tmp.length>2){
            			logger.debug("index i with troubles: " + i);
            			
            			parsedText = parsedText.replaceFirst(tmpIndexString, indexString2);
            			//logger.debug(parsedText);
            			i--; 
            		}
            		else{
            			logger.debug("index i with troubles: im else " + i);
            			pageNr += 1;
            			writeTextFile(out, pageNr, "");
            			pageNr += 1;
            			String resultText2 = getTextForPage(parsedText, indexString1, tmpIndexString);
            			writeTextFile(out, pageNr, resultText2);
            			i++;
            		}
            	
     
            	
       		
        	}
        }
          	
//        while ( (parsedText.indexOf("p. "+i) != -1 || parsedText.indexOf("p."+i) != -1) && (parsedText.indexOf("p. "+(i+1)) != -1 || parsedText.indexOf("p."+(i+1)) != -1)){
//        	
//        	
//        	
//        	String indexString1 = "p. "+i;
//        	String indexString2 = "p. "+(i+1);
//        	
//        	String resultText2 = getTextForPage(parsedText, indexString1, indexString2);
//        	writeTextFile(out, i+1, resultText2);
//        	
//            //System.in.read();− p. 18
//            i++;
////            if (i == 19){
////            	System.in.read();
////            }
//        }
        
    }


	private void writeTextFile(String out, int pageNr, String resultText) throws IOException {
    	final String fileName = String.format(out, pageNr, "txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(resultText);
        //Close writer
        writer.close();
		
	}


	private String getTextForPage(String parsedText, String indexString1, String indexString2) throws IOException {
		
		logger.debug("index 1: " + indexString1);
		logger.debug("index 2: " + indexString2);

		String tmpText = "";
		if(indexString1 != null){
			
			if (parsedText.indexOf(indexString1) == -1){
				indexString1 = indexString1.replaceAll(" ", "");
			}
			
			if (parsedText.indexOf(indexString2) == -1){
				indexString2 = indexString2.replaceAll(" ", "");
			}
			
			logger.debug("parsedText.indexOf(indexString1): " + parsedText.indexOf(indexString1));
			logger.debug("parsedText.indexOf(indexString2): " + parsedText.indexOf(indexString2));
			//System.in.read();
			
			tmpText = parsedText.substring(parsedText.indexOf(indexString1), parsedText.indexOf(indexString2)-2);
			tmpText = tmpText.replaceFirst(indexString1, "").trim();
		}
		else{
			tmpText = parsedText.substring(0, parsedText.indexOf(indexString2)-2);
		}
	

    	//logger.debug("text: " + tmpText);
        
        String[] textLines = tmpText.split(System.lineSeparator());
        
        String resultText = "";
        
        for (String textLine : textLines){
        	if (textLine.matches("\\d+ ")){
        		logger.debug("textLine digit " + textLine);
        		
        		
        	}
        	else{
        		//logger.debug("textLine " + textLine);
        		resultText += textLine.trim() + System.lineSeparator();
        	}
        	
        }

       // logger.debug("resulting text: " + resultText);
        return resultText;
	}
 

}