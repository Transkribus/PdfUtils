package org.dea.util.pdf;


import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import com.itextpdf.awt.geom.AffineTransform;
import com.itextpdf.awt.geom.Line2D;
import com.itextpdf.awt.geom.Point2D;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfLayer;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfWriter;



/**
 * Abstract class for building PDFs with Itext.
 * Based on FEP's PDF_Document
 * @author philip
 *
 */
public abstract class APdfDocument {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(APdfDocument.class);
	
	//public final URL RESOURCE = this.getClass().getClassLoader().getResource("findTagname.js");
	
	
	
	protected Document document;
	protected final int marginLeft;
	protected final int marginRight;
	protected final int marginTop;
	protected final int marginBottom;
	protected final File pdfFile;
	protected PdfLayer ocrLayer;
	protected PdfLayer imgLayer;
	protected PdfWriter writer;
	
	protected PdfPCell cell;
	
	protected float scaleFactorX = 1.0f;
	protected float scaleFactorY = 1.0f;
		
	double currentLineTx;
	double currentLineTy;
	double currentRotation = 0;
	float currentPrintWidth;
	float currentPrintWidthScale = (float) 1.0;

	public APdfDocument(final File pdfFile) throws DocumentException, IOException {
		this(pdfFile, 0, 0, 0, 0);
	}
	
	public APdfDocument(final File pdfFile, final int marginLeft, final int marginTop, final int marginBottom, final int marginRight) throws DocumentException, IOException {
		this.pdfFile = pdfFile;
		this.marginLeft = marginLeft;
		this.marginTop = marginTop;
		this.marginBottom = marginBottom;
		this.marginRight = marginRight;
		createDocument();
	}
	
	private void createDocument() throws DocumentException, IOException {
			document = new Document();
			writer = PdfWriter.getInstance(document, new FileOutputStream(this.pdfFile));
			writer.setPdfVersion(PdfWriter.VERSION_1_7);
			
			//to yield PDF/A
			//writer.setPDFXConformance(PdfWriter.PDFX1A2001);
			writer.setUserunit(1);
			document.setMargins(marginRight, marginLeft, marginTop, marginBottom);
			document.open();

			ocrLayer = new PdfLayer("OCR", writer);
			imgLayer = new PdfLayer("Image", writer);
	}
	
	public void close() {
		document.close();
	}
	
	public int getPageNumber() {
		logger.debug("page number " + writer.getPageNumber());
		return writer.getPageNumber();
	}

	/**
	 * @param boundRect The bounding Rectangle for this string
	 * @param baseLineMeanY baseLine y-value. May be null! Then this is approximated from the rectangle
	 * @param text the text content
	 * @param cb 
	 * @param cutoffLeft
	 * @param cutoffTop
	 * @param bf
	 */
	protected void addString(java.awt.Rectangle boundRect, Double baseLineMeanY, final String text, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf, double angle) {
		if(baseLineMeanY == null || baseLineMeanY == 0) {
			//no baseline -> divide bounding rectangle height by three and expect the line to be in the upper two thirds
			double oneThird = (boundRect.getMaxY() - boundRect.getMinY())/3;
			baseLineMeanY = boundRect.getMaxY() - oneThird;
		}
		
//		final float posX = Double.valueOf(boundRect.getMinX() - cutoffLeft+marginLeft).floatValue();
//		final float posY = document.getPageSize().getHeight() - (Double.valueOf(baseLineMeanY-cutoffTop+marginTop).floatValue());
		double c_height = baseLineMeanY-boundRect.getMinY();
		
		if(c_height <= 0.0){
			c_height = 10.0;
		}
		
		cb.beginText();
//		cb.setHorizontalScaling(100);
//		cb.moveText(posX, posY);
		cb.setFontAndSize(bf, (float) c_height);
		Chunk c = new Chunk(text);

		AffineTransform transformation=new AffineTransform();
		final double tx = (boundRect.getMinX()-cutoffLeft+marginLeft)*scaleFactorX;
		final double ty = (document.getPageSize().getHeight()) - (baseLineMeanY-cutoffTop+marginTop)*scaleFactorY;
		transformation.setToTranslation(tx, ty);
		
		float scaling_x=(Double.valueOf((boundRect.getMaxX()-1)-boundRect.getMinX())).floatValue()/cb.getEffectiveStringWidth(text, true)*scaleFactorX;
		float scaling_y=scaleFactorY;			
		transformation.scale(scaling_x, scaling_y);
		transformation.rotate(angle*0.0175);

		cb.setTextMatrix(transformation);
		cb.showText(c.getContent());
		cb.endText();
	}
	
	/**
	 * @param boundRect The bounding Rectangle for this string
	 * @param baseLineMeanY baseLine y-value. May be null! Then this is approximated from the rectangle
	 * @param text the text content
	 * @param cb 
	 * @param cutoffLeft
	 * @param cutoffTop
	 * @param bf
	 * @param color 
	 * @throws IOException 
	 * @throws DocumentException 
	 */
	protected void addUniformString(double c_height, float posX, float posY, final Phrase textPhrase, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf, float twelfth, boolean searchAction, String color, double rotation) throws IOException, DocumentException {

		/*
		 * states that text gets read right to left
		 */
		currentRotation = rotation;
		currentPrintWidthScale = 1;
		
		if(c_height <= 0.){
			c_height = 10.0/scaleFactorY;
		}
		
		if (c_height > 300){
			c_height = 150;
		}
		
		//c_height = c_height/scaleFactorY;

//		logger.debug("text heigth " + c_height*scaleFactorY);
//		logger.debug("posX " + posX);
//		logger.debug("posY " + posY);
		
		cb.beginText();
		cb.setHorizontalScaling(100);
		cb.moveText(posX, posY);

		cb.setFontAndSize(bf, (float) c_height);
						
		float effTextWidth = cb.getEffectiveStringWidth(textPhrase.getContent(), false);	
		float effPrintWidth = (document.getPageSize().getWidth()/scaleFactorX - twelfth) - posX;

		cb.endText();

//		logger.debug("rotation " + rotation);
//		logger.debug("effPrintWidth " + effPrintWidth);

		
		if ( effTextWidth > effPrintWidth && rotation == 0){
			currentPrintWidthScale = effPrintWidth / effTextWidth;
			//cb.setHorizontalScaling(currentPrintWidthScale*100);
			logger.debug("width exceeds page width: scale with " + currentPrintWidthScale*100);
		}	
		
		Phrase phraseNew = new Phrase();
		for (Chunk ch : textPhrase.getChunks()){
			//ch.setHorizontalScaling(currentPrintWidthScale);
			Chunk tmpChunk = new Chunk(ch.getContent());
			//tmpChunk.setLineHeight((float) c_height*scaleFactorY);
			tmpChunk.setAttributes(ch.getAttributes());
			tmpChunk.setFont(new Font(ch.getFont().getBaseFont(), (float) c_height*scaleFactorY));
			
			
			//if (ch.getAttributes() != null && ch.getAttributes().containsKey("UNDERLINE")){

			tmpChunk.setHorizontalScaling(currentPrintWidthScale);
			phraseNew.add(tmpChunk);
		}
		
//		for (Entry<String, Object> entry : ct.getAttributes().entrySet()){
//			logger.debug("entry key " + entry.getKey());
//			logger.debug("entry value " + entry.getValue().toString());
//		}
		
		currentPrintWidth = effTextWidth*currentPrintWidthScale;

		Chunk c = new Chunk(textPhrase.getContent());
				
		logger.debug("rotation " + rotation);
		if (Math.abs(rotation) > 1.5){
			if ((document.getPageSize().getWidth()/scaleFactorX - twelfth) < posX){
				posX = (float) ((document.getPageSize().getWidth()/scaleFactorX - twelfth)-c_height);
			}
		}

		AffineTransform transformation=new AffineTransform();
		currentLineTx = (posX-cutoffLeft+marginLeft)*scaleFactorX;
		currentLineTy = (document.getPageSize().getHeight()) - posY*scaleFactorY;
		

		if (color != null){
			
			float startX = (float) currentLineTx;
			float startY = (float) currentLineTy;
			
//			float endX = (float) tx + printwidth*scaleFactorX;
//			float endY = (float) ty;
//			
//			drawColorLine(cb, color, startX, startY, endX, endY);
			
			
            cb.saveState();
//            //Set the fill color based on eve/odd
            Color currColor = Color.decode(color);
            cb.setColorFill(new BaseColor(currColor.getRGB()));
//            //Optional, set a border
//            //cb.SetColorStroke(BaseColor.BLACK)
//            //Draw a rectangle.
            cb.rectangle(startX, startY, currentPrintWidth*scaleFactorX, (float) c_height*scaleFactorY);
//            //Draw the rectangle with a border. NOTE: Use cb.Fill() to draw without the border
            cb.fill();
//            //Unwind the graphics state
            cb.restoreState();
		}
		

		cb.beginText();

		
		transformation.setToTranslation(currentLineTx, currentLineTy);
		transformation.scale(scaleFactorX, scaleFactorY);
		transformation.rotate(rotation);
		
		//cb.setTextMatrix(transformation);
		cb.endText();
		

		if (searchAction && c != null){
			logger.debug("find tagname: " + textPhrase.getContent());
			
			c.setAction(PdfAction.javaScript(String.format("findTagname('%s');", textPhrase.getContent()), writer));
			//c.setAction(PdfAction.javaScript("app.alert('Think before you print');", writer));
			c.append(", ");
			//c.append(new String(rs.getBytes("given_name"), "UTF-8"));

		    //logger.debug("Resource Path: " + RESOURCE.getPath());

		    InputStream is= this.getClass().getClassLoader().getResourceAsStream("js/findTagname.js");
		    String jsString = fromStream(is);
		    
		    //writer.addJavaScript(Utilities.readFileToString(javaScriptFile));
		        // Add this Chunk to every page
		    writer.addJavaScript(jsString);
		    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phraseNew, (float) currentLineTx, (float) currentLineTy, 0);
   
		}
		else{
			//cb.showTextAligned();(Element.ALIGN_LEFT, c.getContent(), (float) tx, (float) ty, rotation);
			//cb.showText(c.getContent());
			//logger.debug("rotate: " + rotation );
			ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phraseNew, (float) currentLineTx, (float) currentLineTy, (float) rotation);

		}

		

	}
	
	/**
	 * @param boundRect The bounding Rectangle for this string
	 * @param baseLineMeanY baseLine y-value. May be null! Then this is approximated from the rectangle
	 * @param text the text content
	 * @param cb 
	 * @param cutoffLeft
	 * @param cutoffTop
	 * @param bf
	 * @param color 
	 * @throws IOException 
	 * @throws DocumentException 
	 */
	protected void addUniformTagList(double c_height, float posX, float posY, final String text, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf, float twelfth, boolean searchAction, String color, double rotation) throws IOException, DocumentException {

		/*
		 * states that text gets read right to left
		 */
		boolean rtl = false;
		currentRotation = rotation;
		
		if(c_height <= 0.0 || c_height > 300){
			c_height = 10.0/scaleFactorY;
		}

		//logger.debug("text heigth " + c_height);
		cb.beginText();
		cb.moveText(posX, posY);
		
		
		cb.setFontAndSize(bf, (float) c_height);
						
		float effTextWidth = cb.getEffectiveStringWidth(text, false);	
		float effPrintWidth = (document.getPageSize().getWidth()/scaleFactorX - twelfth) - posX;

		cb.endText();
		

		cb.setHorizontalScaling(100);
		if ( effTextWidth > effPrintWidth && rotation == 0){
			currentPrintWidthScale = effPrintWidth / effTextWidth;
			cb.setHorizontalScaling(currentPrintWidthScale*100);
			logger.debug("width exceeds page width: scale with " + currentPrintWidthScale*100);
		}	
		
		currentPrintWidth = effTextWidth*currentPrintWidthScale;

		Chunk c = new Chunk(text);
		
		/*
		 *       document.open();

			      Chunk underline = new Chunk("Underline. ");
			      underline.setUnderline(0.1f, -2f); //0.1 thick, -2 y-location
			      document.add(underline);
			
			      document.add(new Paragraph("   "));
			
			      Chunk strikethrough = new Chunk("Strikethrough.");
			      strikethrough.setUnderline(0.1f, 3f); //0.1 thick, 2 y-location
			      document.add(strikethrough);
			
			      document.close();
		 */
		
		
		Phrase phrase;
		
		//logger.debug("rotation " + rotation);
		if (Math.abs(rotation) > 1.5){
			if ((document.getPageSize().getWidth()/scaleFactorX - twelfth) < posX){
				posX = (float) ((document.getPageSize().getWidth()/scaleFactorX - twelfth)-c_height);
			}
		}

		AffineTransform transformation=new AffineTransform();
		currentLineTx = (posX-cutoffLeft+marginLeft)*scaleFactorX;
		currentLineTy = (document.getPageSize().getHeight()) - posY*scaleFactorY;
		
		//for right to left writing
		if(rtl){
			//evt. genauere Position der Textregion ermitteln
			if ((posX+effTextWidth) > (twelfth*11*scaleFactorX)){
				
			}
			currentLineTx = (document.getPageSize().getWidth() - (twelfth*scaleFactorX));
		}
		


		if (color != null){
			
			float startX = (float) currentLineTx;
			float startY = (float) currentLineTy;
			
//			float endX = (float) tx + printwidth*scaleFactorX;
//			float endY = (float) ty;
//			
//			drawColorLine(cb, color, startX, startY, endX, endY);
			
			
            cb.saveState();
//            //Set the fill color based on eve/odd
            Color currColor = Color.decode(color);
            cb.setColorFill(new BaseColor(currColor.getRGB()));
//            //Optional, set a border
//            //cb.SetColorStroke(BaseColor.BLACK)
//            //Draw a rectangle.
            cb.rectangle(startX, startY, currentPrintWidth*scaleFactorX, (float) c_height*scaleFactorY);
//            //Draw the rectangle with a border. NOTE: Use cb.Fill() to draw without the border
            cb.fill();
//            //Unwind the graphics state
            cb.restoreState();
		}
		else{
			logger.debug("Color is null");
		}
		

		cb.beginText();

		
		transformation.setToTranslation(currentLineTx, currentLineTy);
		transformation.scale(scaleFactorX, scaleFactorY);
		transformation.rotate(rotation);
		
		cb.setTextMatrix(transformation);

		if (searchAction && c != null){
			//logger.debug("find tagname: " + text);
			
			c.setAction(PdfAction.javaScript(String.format("findTagname('%s');", text), writer));
			//c.setAction(PdfAction.javaScript("app.alert('Think before you print');", writer));
			c.append(", ");
			//c.append(new String(rs.getBytes("given_name"), "UTF-8"));
		    phrase = new Phrase(c);

		    //logger.debug("Resource Path: " + RESOURCE.getPath());

		    InputStream is= this.getClass().getClassLoader().getResourceAsStream("js/findTagname.js");
		    String jsString = fromStream(is);
		    
		    //writer.addJavaScript(Utilities.readFileToString(javaScriptFile));
		        // Add this Chunk to every page
		    writer.addJavaScript(jsString);
		    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phrase, (float) currentLineTx, (float) currentLineTy, 0);
   
		}
		else{

			if (rtl){
				phrase = new Phrase(c);
				ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, phrase, (float) currentLineTx, (float) currentLineTy, 0, PdfWriter.RUN_DIRECTION_RTL, 0);
			}
			else{
				
				//cb.showTextAligned();(Element.ALIGN_LEFT, c.getContent(), (float) tx, (float) ty, rotation);
				cb.showText(c.getContent());
			}

		}

		cb.endText();

	}
	
	public static String fromStream(InputStream in) throws IOException
	{
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    StringBuilder out = new StringBuilder();
	    String newLine = System.getProperty("line.separator");
	    String line;
	    while ((line = reader.readLine()) != null) {
	        out.append(line);
	        out.append(newLine);
	    }
	    return out.toString();
	}
	
	protected void highlightStringUnderImg(java.awt.Rectangle boundRect,
			Double baseLineMeanY, String text, String tagText,
			PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf,
			float twelth, String color, float yShift) {
		if(baseLineMeanY == null) {
			//no baseline -> divide bounding rectangle height by three and expect the line to be in the upper two thirds
			double oneThird = (boundRect.getMaxY() - boundRect.getMinY())/3;
			baseLineMeanY = boundRect.getMaxY() - oneThird;
		}
		
		double c_height = baseLineMeanY-boundRect.getMinY();
		
		if(c_height <= 0.0){
			c_height = 10.0;
		}
			
		cb.beginText();
		cb.setHorizontalScaling(100);
//		cb.moveText(posX, posY);
		cb.setFontAndSize(bf, (float) c_height);
		
		final double tx = (boundRect.getMinX()-cutoffLeft+marginLeft)*scaleFactorX;
		final double ty = (document.getPageSize().getHeight()) - (baseLineMeanY-cutoffTop+marginTop)*scaleFactorY;
		float width = cb.getEffectiveStringWidth(text, true);
		
		float effTagTextStart = cb.getEffectiveStringWidth(text.substring(0, text.indexOf(tagText)), false);
		
		cb.endText();
		
		cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_STROKE);
		
		if (color != null){
			
			float startX = (float) tx;
			float startY = (float) ty - yShift*scaleFactorY;
			
			float endX = (float) tx + width*scaleFactorX;
			
			drawColorLine(cb, color, startX, startY, endX, startY);

		}
		
		cb.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_INVISIBLE);

		
	}
	
	protected void highlightAllTagsOnImg(List<java.util.Map.Entry<Line2D,String>> lines, PdfContentByte cb, int cutoffLeft, int cutoffTop ) {
		
		Iterator lineIterator = lines.iterator();
		
		while (lineIterator.hasNext()){
			
			Entry<Line2D,String> pair = (Entry<Line2D, String>) lineIterator.next();
			Point2D p1 = pair.getKey().getP1();
			Point2D p2 = pair.getKey().getP2();
			String color = pair.getValue();
			
			final float p1x = (float) ((p1.getX()-cutoffLeft+marginLeft)*scaleFactorX);
			final float p1y = (float) ((document.getPageSize().getHeight()) - (p1.getY()-cutoffTop+marginTop)*scaleFactorY);
			
			final float p2x = (float) ((p2.getX()-cutoffLeft+marginLeft)*scaleFactorX);
			final float p2y = (float) ((document.getPageSize().getHeight()) - (p2.getY()-cutoffTop+marginTop)*scaleFactorY);
						
			if (color != null){
				
				drawColorLine(cb, color, p1x, p1y, p2x, p2y);
	
			}
		

		}

		
	}
	
	
	protected void highlightUniformTagString(double c_height, float posX, float posY, final String lineText, final String tagText, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf, float twelfth, String color, float yShift, int offset) throws IOException {

//		if(c_height <= 0.0){
//			c_height = 10.0;
//		}
//		
		cb.beginText();
		cb.moveText(posX, posY);
		cb.setFontAndSize(bf, (float) c_height);
//				
//		float effTextLineWidth = cb.getEffectiveStringWidth(lineText, false);
//		float effTagTextWidth = cb.getEffectiveStringWidth(tagText, false);
//		
//		float effPrintWidth = (document.getPageSize().getWidth()/scaleFactorX - twelfth) - posX;
//		
		cb.endText();
//		
////		logger.debug("text " + text);
////		logger.debug("effTextWidth " + effTextWidth);
		logger.debug("currentPrintWidthScale " + currentPrintWidthScale);
		
//		float printwidth = effTextLineWidth;
//		
//		float printwidthScale = currentPrintwidth;
//		
//		if ( effTextLineWidth > effPrintWidth){
//			printwidth = effPrintWidth / effTextLineWidth;
//			printwidthScale = printwidth;
//			cb.setHorizontalScaling(printwidth*100);
//			//logger.debug("width exceeds page width: scale with " + tmp);
//		}	
		
		cb.setHorizontalScaling(currentPrintWidthScale*100);
		
		float effTagTextWidth = cb.getEffectiveStringWidth(tagText, false);
		float effTagTextStart = cb.getEffectiveStringWidth(lineText.substring(0, offset), false);
	
//		AffineTransform transformation=new AffineTransform();
//		final double tx = (posX-cutoffLeft+marginLeft+effTagTextStart)*scaleFactorX;
//		final double ty = (document.getPageSize().getHeight()) - posY*scaleFactorY;
		
		final double tx = currentLineTx + effTagTextStart*scaleFactorX;
		final double ty = currentLineTy;
		
		//transformation.setToTranslation(tx, ty);			
//		//transformation.setToTranslation(currentLineTx, currentLineTy);
//		transformation.scale(scaleFactorX, scaleFactorY);
//		transformation.rotate(1.5);
////		
//		cb.setTextMatrix(transformation);
				
		if (color != null){
			
			float startX = (float) tx*currentPrintWidthScale;
			float startY = (float) ty - yShift*scaleFactorY;
			
			float endX = startX + effTagTextWidth*scaleFactorX*currentPrintWidthScale;
			float endY = (float) ty - yShift*scaleFactorY;
			
			drawColorLine(cb, color, startX, startY, endX, endY);

		}
		
	}

private void drawColorLine(PdfContentByte cb, String color, float startX,
			float startY, float endX, float endY) {
	    cb.saveState();
	    //Set the fill color based on eve/odd
	    Color currColor = Color.decode(color);
	    cb.setColorFill(new BaseColor(currColor.getRGB()));
	    cb.setColorStroke(new BaseColor(currColor.getRGB()));
	    
	    cb.moveTo(startX, startY);
	    cb.lineTo(endX, endY);
	
	    cb.stroke();
	    //Optional, set a border
	    //cb.SetColorStroke(BaseColor.BLACK)
	    //Draw a rectangle. NOTE: I'm subtracting 5 from the y to account for padding
	    //cb.rectangle((float) tx, (float) ty, effTagTextWidth*scaleFactorX, (float) c_height*scaleFactorY);
	    //Draw the rectangle with a border. NOTE: Use cb.Fill() to draw without the border
	    //cb.fill();
	    //Unwind the graphics state
	    cb.restoreState();	
	}

//	private void addTocLinks(FEP_Document doc, FEP_Page page, int cutoffTop) {
//		FEP_TocEntry [] entries=FEPQueries.selectFEPTocEntriesByTocPage(page.getFep_Document_ID(),page.getFep_Page_ID());
//		for(FEP_TocEntry e : entries)	{
//			//string is one of toc entry
//			int l = 0;
//			//int l = e.getH_pos();
//			int t = (int)document.getPageSize().getHeight() - e.getV_pos() + cutoffTop - marginTop;
//			//int r = e.getH_pos()+e.getWidth();
//			int r = (int)document.getPageSize().getWidth();
//			int b = (int)document.getPageSize().getHeight() - (e.getV_pos()+e.getHeight()) + cutoffTop - marginTop;
//			
//			if (e.getStart_page()>=1 && e.getStart_page()<=doc.getNr_of_images()) {
//				writer.getDirectContent().setAction(PdfAction.gotoLocalPage(e.getStart_page(), 
//						new PdfDestination(PdfDestination.FIT), writer), l, t, r, b);
//			}
//			else {
//				// TODO: warning that toc link wasn't added
//				ExportService.logger.warn("warning: toc-link could not be added because of invalid start-page: pid="+e.getFep_Page_ID()+" label="+e.getLabel()+" startPage="+e.getStart_page());
//			}
//		}
//		
//	}
//
//	public void addPageLabels(FEP_Document doc) {
//	
//		PdfPageLabels pageLabels = new PdfPageLabels();
//				
//		FEP_Pagination[] paginations = FEPQueries.selectAllFEPPagination(doc.getFep_Document_ID());
//		for(FEP_Pagination pagination : paginations)	{
//			pageLabels.addPageLabel(pagination.getFep_Page_ID(), PdfPageLabels.EMPTY, pagination.getValue());
//		
//		writer.setPageLabels(pageLabels);
//
//		}
//
//	}
//
//
//
//	public void addBookmarks(FEP_Document doc) {
//
//		PdfOutline root = writer.getRootOutline();
//		FEP_Toc[] tocs = FEPQueries.selectFEPToc(doc.getFep_Document_ID());
//		for(FEP_Toc toc : tocs)	{
//			FEP_TOC_Pages[] tocPages = FEPQueries.selectFEPTocPages(doc.getFep_Document_ID());
//			FEP_TocHeading[] headings = FEPQueries.selectFEPHeadingsForToc(doc.getFep_Document_ID(), toc.getToc_ID());
//			for(FEP_TocHeading heading :headings)	{
//				PdfAction dest = PdfAction.gotoLocalPage(1, new PdfDestination(PdfDestination.FIT), writer);
//				if(tocPages.length>0){
//					dest = PdfAction.gotoLocalPage(tocPages[0].getFep_Page_ID(), new PdfDestination(PdfDestination.FIT), writer);
//				}
//				
//				PdfOutline h = new PdfOutline(root, dest, heading.getLabel());
//				addTocEntries(h,doc.getFep_Document_ID(),heading.getToc_ID(),heading.getToc_Heading_ID());
//				
//			}
//		}
//	
//		
//	}
//
//	private void addTocEntries(PdfOutline item, int docid, int tocID,
//			int parent) {
//		FEP_TocEntry[] entries=FEPQueries.selectFEPTocEntriesByParentEntryID(docid,tocID,parent);
//		for(FEP_TocEntry e : entries)	{
//			if(e.getStart_page()>0)
//			{
//			PdfAction destination = PdfAction.gotoLocalPage(e.getStart_page(), new PdfDestination(PdfDestination.FIT), writer);
//			PdfOutline outline = new PdfOutline(item, destination, e.getLabel());	
//			addTocEntries(outline, docid, tocID, e.getToc_Entry_ID());
//			}
//			}
//		}
//		
//
//
//	public void addPage(FEP_Document doc, FEP_Page page, File image) throws DocumentException, MalformedURLException, IOException {
//		Image img = Image.getInstance(image.getAbsolutePath());
//		int cutoffLeft=0;
//		int cutoffTop=0;
//		setPageSize(img);		
//		document.newPage();
//		document.add(img);		
//		addText(doc, page,cutoffLeft,cutoffTop);
//		
//	}
//
//	public void addPage(FEP_Document doc, FEP_Page page, File image, FEP_Print_Space printspace,
//			PODMargins margin) throws MalformedURLException, IOException, DocumentException {
//		int cutoffLeft=0;
//		int cutoffTop=0;
//		boolean even=true;
//		
//		FEP_Pagination pagination = FEPQueries.selectFEPPagination(page.getFep_Document_ID(),page.getFep_Page_ID());
//		if(printspace!=null)	{
//			cutoffLeft=printspace.getP1x();
//			cutoffTop=printspace.getP1y();
//			margin.calculateMargins(printspace);
//		}
//
//		marginBottom = margin.getBottomMargin();
//		marginTop = margin.getTopMargin();
//		
//		Image img = Image.getInstance(image.getAbsolutePath());
//		
//		if(pagination!=null){
//		even=pagination.getOdd().equalsIgnoreCase("false");
//		}
//		
//		img.setAbsolutePosition(margin.getLeftMargin(even), marginBottom);
//		marginLeft = margin.getLeftMargin(even);
//		marginRight = margin.getRightMargin(even);		
//		setPageSize(img);	
//		document.newPage();
//		document.add(img);
//		addText(doc, page,cutoffLeft,cutoffTop);
//				
//	}
	
//	public void addPageLabels(FEP_Document doc, int startPage, int endPage) {
//		PdfPageLabels pageLabels = new PdfPageLabels();
//		
//		FEP_Pagination[] paginations = FEPQueries.selectFEPPagination(doc.getFep_Document_ID(),startPage,endPage);
//		for(FEP_Pagination pagination : paginations)	{
//			pageLabels.addPageLabel(pagination.getFep_Page_ID()-(startPage-1), PdfPageLabels.EMPTY, pagination.getValue());
//		
//		writer.setPageLabels(pageLabels);
//
//		}
//	}

	

	protected void setPageSize(Rectangle r)
	{
		document.setPageSize(r);
	}
	
	protected void setPageSize(Image image)
	{
		float xSize;
		float ySize;
		
		if (image.getDpiX() > 72f){
			scaleFactorX = scaleFactorY = 72f / image.getDpiX();
			xSize = (float) (image.getPlainWidth() / (image.getDpiX()))*72;
			ySize = (float) (image.getPlainHeight() / (image.getDpiY())*72);
		}
		//if dpi is unknown, assumption is 300 dpi
		else{
			scaleFactorX = scaleFactorY = 72f / 300;
			xSize = (float) (image.getPlainWidth() / 300*72);
			ySize = (float) (image.getPlainHeight() / 300*72);
		}
		
		//document.setPageSize(new Rectangle(image.getScaledWidth(), image.getScaledHeight()));
		document.setPageSize(new Rectangle(xSize+marginRight+marginLeft, ySize+marginTop+marginBottom));
		//document.setPageSize(new Rectangle(image.getPlainWidth()+marginRight+marginLeft, image.getPlainHeight()+marginTop+marginBottom));
	}
	
	protected void addTitleString(String text, float posY,
			float leftGap, float overallLineMeanHeight, PdfContentByte cb, BaseFont bfArialBoldItalic) {
		
//		logger.debug("overallLineMeanHeight: " + overallLineMeanHeight);
//		logger.debug("document.getPageSize().getHeight(): " + document.getPageSize().getHeight());
//		logger.debug("document.getPageSize().getWidth(): " + document.getPageSize().getWidth());
//		logger.debug("left Gap: " + leftGap);
//		logger.debug("text: " + text);

		if(overallLineMeanHeight <= 0.0 || overallLineMeanHeight > 300){
			overallLineMeanHeight = (float) (10.0/scaleFactorY);
		}
				
		//Phrase phrase = new Phrase(text, new Font (bfArialBoldItalic, overallLineMeanHeight*scaleFactorY));
		Chunk chunk = new Chunk(text, new Font (bfArialBoldItalic, overallLineMeanHeight*scaleFactorY));
		


		//
		/*
		 * 
		 * 
		 *         Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(dest));
        document.open();
 
        // the direct content
        PdfContentByte cb = writer.getDirectContent();
        // the rectangle and the text we want to fit in the rectangle
        Rectangle rect = new Rectangle(100, 150, 220, 200);
        String text = "test";
        // try to get max font size that fit in rectangle
        BaseFont bf = BaseFont.createFont();
        int textHeightInGlyphSpace = bf.getAscent(text) - bf.getDescent(text);
        float fontSize = 1000f * rect.getHeight() / textHeightInGlyphSpace;
        Phrase phrase = new Phrase("test", new Font(bf, fontSize));
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, phrase,
                // center horizontally
                (rect.getLeft() + rect.getRight()) / 2,
                // shift baseline based on descent
                rect.getBottom() - bf.getDescentPoint(text, fontSize),
                0);
 
        // draw the rect
        cb.saveState();
        cb.setColorStroke(BaseColor.BLUE);
        cb.rectangle(rect.getLeft(), rect.getBottom(), rect.getWidth(), rect.getHeight());
        cb.stroke();
        cb.restoreState();
 
//        document.close();
//		 */
		currentLineTx = 0;//(leftGap+marginLeft)*scaleFactorX;
		currentLineTy = document.getPageSize().getHeight() - posY*scaleFactorY;
		
		currentLineTx = (leftGap == 0 ? document.getPageSize().getWidth()/2 : leftGap*scaleFactorY);
		
        Paragraph paragraph = new Paragraph();

        paragraph.setSpacingBefore(overallLineMeanHeight*scaleFactorY);
        paragraph.setSpacingAfter(overallLineMeanHeight*scaleFactorY);
        if (leftGap == 0){
        	paragraph.setAlignment(Element.ALIGN_CENTER);
        }
        else{
        	paragraph.setAlignment(Element.ALIGN_LEFT);
        }
        paragraph.setLeading(0, (float) 1.1);
        
        paragraph.add(chunk);
        
        try {
        	if (document.topMargin() == 0){
        		document.setMargins(0, 0, overallLineMeanHeight*scaleFactorY*3, 0);
        		document.open();
        	}
        	if (document.leftMargin() == 0 && leftGap != 0){
        		document.setMargins(leftGap*scaleFactorX, 0, overallLineMeanHeight*scaleFactorY*3, 0);
        		document.open();
        	}
        	//set the top margin to some value - open must be there to newly set the margin
        	
        	//
        	
			document.add(paragraph);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}


}
