package org.dea.util.pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfImageObject;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;


/**
 * @author lange
 * Extract images from a given pdf file
 * Note: extracted images are named with ascending numbers,
 *       however, the number refers to internal document object counting, 
 *       not pages nor count of images
 * FIXME: check whether renaming is necessary 
 */
public class ImageExtractor {
    
    /**
     * Parses a PDF and extracts all the images.
     * @param src the source PDF
     */
    public void extractImages(String filepath)
        throws IOException, DocumentException {
        PdfReader reader = new PdfReader(filepath);
        
        File file = new File(filepath);
        final String parentDir = file.getParent();
        final String name = FilenameUtils.getBaseName(file.getName());
        final String out = parentDir + File.separator + name + "-%s.%s";
        
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        MyImageRenderListener listener = new MyImageRenderListener(out);
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            parser.processContent(i, listener);           
        }
        reader.close();
    }

    /**
     * Parses a PDF document and renders all images to an output directory.
     * @param filepath Path to PDF document
     * @param outDir Output directory
     * @throws IOException
     * @throws DocumentException
     * @return name of created folder
     */
    public String extractImages(String filepath, String outDir) 
    	throws IOException, DocumentException, SecurityException {
    	
    	// create file reader for input pdf
        PdfReader reader = new PdfReader(filepath);
                
        // extract file name
        File file = new File(filepath);
        final String name = FilenameUtils.getBaseName(file.getName());
        
        // create output folder/filename(s)
        File dir = new File(outDir + File.separator + name);
        dir.mkdir();
        
        final String out = dir.getPath() + File.separator 
        		+ name + "-%s_%s.%s";

        // prepare parsing objects
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        MyImageRenderListener listener = new MyImageRenderListener(out);
        
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
        	listener.setPageNumber(i);
            parser.processContent(i, listener);
            System.out.println("DEBUG -- "+getClass().getName()+ " on page "+ i);
        }
        reader.close();
    	
        return dir.getAbsolutePath();
    }
    /**
     * Main method.
     * @param    args    no arguments needed
     * @throws DocumentException 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, DocumentException {
    //	String test = "/mnt/dea_scratch/TRP/PdfTestDoc/A 0073.pdf";
    //    new ImageExtractor().extractImages(test);
     	String outDir = System.getProperty("java.io.tmpdir") + "pdftest" + File.separator;
    	String test = outDir + "ND3370_Wurster_Tafelwerk_0004_Scans.pdf";
    	
    	System.out.println("Writing to " + outDir);
    	new ImageExtractor().extractImages(test, outDir);
    }
    
    public class MyImageRenderListener implements RenderListener {
    	 
        /** The ouput path (absolute) of the new document. */
        protected String path = "";
        
        /** The current page number */
        protected int num;
     
        /**
         * Creates a RenderListener that will look for images.
         */
        public MyImageRenderListener(String path) {
            this.path = path;
        	this.num = 0;
        }

        public void setPageNumber(int num) {
        	this.num = num;
        }
        
        /**
         * @see com.itextpdf.text.pdf.parser.RenderListener#beginTextBlock()
         */
        public void beginTextBlock() {
        }
     
        /**
         * @see com.itextpdf.text.pdf.parser.RenderListener#endTextBlock()
         */
        public void endTextBlock() {
        }
     
        /**
         * @see com.itextpdf.text.pdf.parser.RenderListener#renderImage(
         *     com.itextpdf.text.pdf.parser.ImageRenderInfo)
         */
        public void renderImage(ImageRenderInfo renderInfo) {
            try {
                String filename;
                FileOutputStream os;
                PdfImageObject image = renderInfo.getImage();
                if (image == null) return;
                filename = String.format(path, num, renderInfo.getRef().getNumber(), image.getFileType());
//                System.out.println("Writing to " + filename);
                
                os = new FileOutputStream(filename);
                os.write(image.getImageAsBytes());
                //ImageIO.write(image.getBufferedImage(), "png", os);
                os.flush();
                os.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
     
        /**
         * @see com.itextpdf.text.pdf.parser.RenderListener#renderText(
         *     com.itextpdf.text.pdf.parser.TextRenderInfo)
         */
        public void renderText(TextRenderInfo renderInfo) {
        }
    }
}
