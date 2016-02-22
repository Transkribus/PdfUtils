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

public class ImageExtractor {
	/** The new document to which we've added a border rectangle. */
    public static final String RESULT = "results/part4/chapter15/Img%s.%s";
    
    /**
     * Parses a PDF and extracts all the images.
     * @param src the source PDF
     * @param dest the resulting PDF
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
     * Main method.
     * @param    args    no arguments needed
     * @throws DocumentException 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, DocumentException {
    	String test = "/mnt/dea_scratch/TRP/PdfTestDoc/A 0073.pdf";
        new ImageExtractor().extractImages(test);
    }
    
    public class MyImageRenderListener implements RenderListener {
    	 
        /** The new document to which we've added a border rectangle. */
        protected String path = "";
     
        /**
         * Creates a RenderListener that will look for images.
         */
        public MyImageRenderListener(String path) {
            this.path = path;
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
                filename = String.format(path, renderInfo.getRef().getNumber(), image.getFileType());
                os = new FileOutputStream(filename);
                os.write(image.getImageAsBytes());
                os.flush();
                os.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
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
