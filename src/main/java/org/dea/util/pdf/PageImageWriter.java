package org.dea.util.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Observable;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
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
        	final String fileName = String.format(out, i+1, "png");
        	logger.debug("Writing page " + i + " to: " + fileName);
        	ImageIO.write(bim, "png", new File(fileName));
        	
        	logger.debug(getClass().getName()+ " on page "+ (i+1));
        }
        document.close();
    	
        return dir.getAbsolutePath();
    }
 

}