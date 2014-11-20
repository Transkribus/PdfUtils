package org.dea.util.pdf;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;

/**
 * Wrapper class for building PDFs from TrpDocuments with Itext.
 * Based on FEP's PDF_Document
 * @author philip
 *
 */
public class AltoPdfDocument extends APdfDocument {
	private static final Logger logger = Logger.getLogger(AltoPdfDocument.class);
	
	public AltoPdfDocument(final File pdfFile) throws DocumentException, IOException {
		this(pdfFile, 0, 0, 0, 0);
	}

	public AltoPdfDocument(final File pdfFile, final int marginLeft, final int marginTop, final int marginBottom, final int marginRight) throws DocumentException, IOException {
		super(pdfFile, marginLeft, marginTop, marginBottom, marginRight);
	}

	public void addPage(File imgFile, File altoFile, boolean addAdditionalPlainTextPage) throws IOException, DocumentException {
		
		//FIXME use this only on cropped (printspace) images!!
		java.awt.Rectangle printspace = null;
//		if(pc.getPage() != null && pc.getPage().getPrintSpace() != null){
//			java.awt.Polygon psPoly = PageXmlUtils.buildPolygon(pc.getPage().getPrintSpace().getCoords());
//			printspace = psPoly.getBounds();
//		}

		Image img = Image.getInstance(imgFile.getAbsolutePath());
		int cutoffLeft=0;
		int cutoffTop=0;
		
		if(printspace==null) {
			setPageSize(img);
		} else {
			int width=(int)printspace.getWidth();
			int height=(int)printspace.getHeight();
			setPageSize(new Rectangle(width, height));
			cutoffLeft=printspace.x;
			cutoffTop=printspace.y;
		}
		
		document.newPage();
		document.add(img);
		
		//open alto xml
		Document alto = null;
		try {
			alto = parseDomFromFile(altoFile);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		addText(alto, cutoffLeft, cutoffTop);
		
		if(addAdditionalPlainTextPage){
			document.newPage();		
			addText(alto ,cutoffLeft,cutoffTop);
		}
	}

	private void addText(Document alto, int cutoffLeft, int cutoffTop) throws DocumentException, IOException {
		PdfContentByte cb = writer.getDirectContentUnder();
		cb.setColorFill(BaseColor.BLACK);
		cb.setColorStroke(BaseColor.BLACK);
		BaseFont bf = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.CP1250, BaseFont.NOT_EMBEDDED);
		cb.beginLayer(ocrLayer);
		cb.setFontAndSize(bf, 32);
		
//		alto.getDocumentElement().normalize();
		//get all elements: /alto/Layout/Page/PrintSpace/TextBlock/TextLine/String
		NodeList stringElems = alto.getElementsByTagName("String");
		
		for (int i = 0; i < stringElems.getLength(); i++) {
			Node n = stringElems.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element sElem = (Element)n;
				
				final String textContent = sElem.getAttribute("CONTENT");
				final String hStr = sElem.getAttribute("HEIGHT");
				final String wStr = sElem.getAttribute("WIDTH");
				final String yStr = sElem.getAttribute("VPOS");
				final String xStr = sElem.getAttribute("HPOS");
				int x;
				int y;
				int height;
				int width;
				try{
					x = Integer.parseInt(xStr);
					y = Integer.parseInt(yStr);
					height = Integer.parseInt(hStr);
					width = Integer.parseInt(wStr);
				} catch (NumberFormatException nfe){
					logger.fatal("Could not parse int value!", nfe);
					continue;
				}

				//set the base line at 1/10 of the lines height
				Double baseLineMeanY = y + height*0.9;
				
				java.awt.Rectangle boundRect = new java.awt.Rectangle(x, y, width, height);
				addString(boundRect, baseLineMeanY, textContent, cb, cutoffLeft, cutoffTop, bf);
			}
		}
		
		cb.endLayer();	
		
//		addTocLinks(doc, page,cutoffTop);
	}
	
	public static Document parseDomFromFile(File sourceFile) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		return dBuilder.parse(sourceFile);
	}
}