package org.dea.util.pdf;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import java.awt.Color;

import com.itextpdf.awt.geom.AffineTransform;
import com.itextpdf.awt.geom.Line2D;
import com.itextpdf.awt.geom.Point2D;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Utilities;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfLayer;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;



/**
 * Abstract class for building PDFs with Itext.
 * Based on FEP's PDF_Document
 * @author philip
 *
 */
public abstract class APdfDocument {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(APdfDocument.class);
	
	public final URL RESOURCE
	= getClass().getResource("findTagname.js");
	
	protected Document document;
	protected final int marginLeft;
	protected final int marginRight;
	protected final int marginTop;
	protected final int marginBottom;
	protected final File pdfFile;
	protected PdfLayer ocrLayer;
	protected PdfLayer imgLayer;
	protected PdfWriter writer;
	
	protected float scaleFactorX = 1.0f;
	protected float scaleFactorY = 1.0f;
	
	float splittingX = 0.0f;
	float splittingY = 0.0f;
	
	
	
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
			writer.setUserunit(1);
			document.setMargins(marginRight, marginLeft, marginTop, marginBottom);
			document.open();

			ocrLayer = new PdfLayer("OCR", writer);
			imgLayer = new PdfLayer("Image", writer);
	}
	
	public void close() {
		document.close();
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
	protected void addString(java.awt.Rectangle boundRect, Double baseLineMeanY, final String text, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf) {
		if(baseLineMeanY == null) {
			//no baseline -> divide bounding rectangle height by three and expect the line to be in the upper two thirds
			double oneThird = (boundRect.getMaxY() - boundRect.getMinY())/3;
			baseLineMeanY = boundRect.getMaxY() - oneThird;
		}
		
		final float posX = Double.valueOf(boundRect.getMinX() - cutoffLeft+marginLeft).floatValue();
		final float posY = document.getPageSize().getHeight() - (Double.valueOf(baseLineMeanY-cutoffTop+marginTop).floatValue());
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
	 */
	protected void addUniformString(double c_height, float posX, float posY, final String text, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf, float twelfth, boolean searchAction, String color) throws IOException {

		if(c_height <= 0.0){
			c_height = 10.0;
		}
		
		cb.beginText();
		cb.moveText(posX, posY);
		cb.setFontAndSize(bf, (float) c_height);
				
		float effTextWidth = cb.getEffectiveStringWidth(text, false);
		
		float effPrintWidth = (document.getPageSize().getWidth()/scaleFactorX - twelfth) - posX;
		
		cb.endText();
		
//		logger.debug("text " + text);
//		logger.debug("effTextWidth " + effTextWidth);
//		logger.debug("effPrintWidth " + effPrintWidth);
		
		float printwidth = effTextWidth;
		
		if ( effTextWidth > effPrintWidth){
			printwidth = effPrintWidth / effTextWidth;
			cb.setHorizontalScaling(printwidth*100);
			//logger.debug("width exceeds page width: scale with " + tmp);
		}	

		Chunk c = new Chunk(text);
		Phrase phrase;
		
		AffineTransform transformation=new AffineTransform();
		final double tx = (posX-cutoffLeft+marginLeft)*scaleFactorX;
		final double ty = (document.getPageSize().getHeight()) - posY*scaleFactorY;
		transformation.setToTranslation(tx, ty);
					
		transformation.scale(scaleFactorX, scaleFactorY);		
		
		if (color != null){
			
			float startX = (float) tx;
			float startY = (float) ty;
			
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
            cb.rectangle(startX, startY, printwidth*scaleFactorX, (float) c_height*scaleFactorY);
//            //Draw the rectangle with a border. NOTE: Use cb.Fill() to draw without the border
            cb.fill();
//            //Unwind the graphics state
            cb.restoreState();
		}
		

		cb.beginText();
		cb.setHorizontalScaling(100);
		cb.setTextMatrix(transformation);
		

		if (searchAction){
			//logger.debug("find tagname: " + text);
			
			c.setAction(PdfAction.javaScript(String.format("findTagname('%s');", text), writer));
			//c.setAction(PdfAction.javaScript("app.alert('Think before you print');", writer));
			c.append(", ");
			//c.append(new String(rs.getBytes("given_name"), "UTF-8"));
		    phrase = new Phrase(c);
		    writer.addJavaScript(Utilities.readFileToString(RESOURCE.getPath()));
		        // Add this Chunk to every page
		    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phrase, (float) tx, (float) ty, 0);
   
		}
		else{
			cb.showText(c.getContent());
		}

		cb.endText();

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
	
	
	protected void highlightUniformString(double c_height, float posX, float posY, final String lineText, final String tagText, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf, float twelfth, String color, float yShift) throws IOException {

		if(c_height <= 0.0){
			c_height = 10.0;
		}
		
		cb.beginText();
		cb.moveText(posX, posY);
		cb.setFontAndSize(bf, (float) c_height);
				
		float effTextLineWidth = cb.getEffectiveStringWidth(lineText, false);
		float effTagTextWidth = cb.getEffectiveStringWidth(tagText, false);
		
		float effPrintWidth = (document.getPageSize().getWidth()/scaleFactorX - twelfth) - posX;
		
		cb.endText();
		
//		logger.debug("text " + text);
//		logger.debug("effTextWidth " + effTextWidth);
//		logger.debug("effPrintWidth " + effPrintWidth);
		
		float printwidth = effTextLineWidth;
		
		float printwidthScale = 1;
		
		if ( effTextLineWidth > effPrintWidth){
			printwidth = effPrintWidth / effTextLineWidth;
			printwidthScale = printwidth;
			cb.setHorizontalScaling(printwidth*100);
			//logger.debug("width exceeds page width: scale with " + tmp);
		}	
		
		float effTagTextStart = cb.getEffectiveStringWidth(lineText.substring(0, lineText.indexOf(tagText)), false);
	
		AffineTransform transformation=new AffineTransform();
		final double tx = (posX-cutoffLeft+marginLeft+effTagTextStart)*scaleFactorX;
		final double ty = (document.getPageSize().getHeight()) - posY*scaleFactorY;
		transformation.setToTranslation(tx, ty);
					
		transformation.scale(scaleFactorX, scaleFactorY);
				
		if (color != null){
			
			float startX = (float) tx;
			float startY = (float) ty - yShift*scaleFactorY;
			
			float endX = (float) tx + effTagTextWidth*printwidthScale*scaleFactorX;
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
		else{
			scaleFactorX = scaleFactorY = 72f / 300;
			xSize = (float) (image.getPlainWidth() / 300*72);
			ySize = (float) (image.getPlainHeight() / 300*72);
		}
		
		splittingX = xSize/12;
		splittingY = ySize/12;

		//document.setPageSize(new Rectangle(image.getScaledWidth(), image.getScaledHeight()));
		document.setPageSize(new Rectangle(xSize+marginRight+marginLeft, ySize+marginTop+marginBottom));
		//document.setPageSize(new Rectangle(image.getPlainWidth()+marginRight+marginLeft, image.getPlainHeight()+marginTop+marginBottom));
	}


}
