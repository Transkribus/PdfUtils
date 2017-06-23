package org.dea.util.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.dea.util.pdf.beans.PDFLine;
import org.dea.util.pdf.beans.PDFPage;
import org.dea.util.pdf.beans.PDFRegion;
import org.dea.util.pdf.beans.PDFString;

/**
 *	Extracts text with coordinates from given PDF file. Currently works for single column pages only.
 *  also the values for SCALE, lineDstThreshold and regionDstThreshold can vary from PDF to PDF
 *  and I haven't figured out how to get that information out of the PDF yet.
 *
 * @author Raphael Unterweger
 *
 */
public class PDFTextExtractor extends PDFTextStripper
{
    public static final float SCALE = 4.17f;
    public static final float lineDstThreshold = 4f;
    public static final float regionDstThreshold = 60f;
    private final PDDocument document;

    private ArrayList<PDFPage> pages;

    private ArrayList<PDFLine> lineBuffer;
	private PDFRegion lastRegion;
    private float actLineBottom = Float.MAX_VALUE;
    private float avarageLinelenght = 0;


    public PDFTextExtractor(PDDocument document) throws IOException
    {
        this.document = document;
        this.pages = new ArrayList<PDFPage>();
        this.lineBuffer = new ArrayList<PDFLine>();
    }

    /**
     * This will print the documents data.
     *
     * @param args The command line arguments.
     *
     * @throws IOException If there is an error parsing the document.
     */
    public static void main(String[] args) throws IOException
    {
    	//test the output
    	System.out.println("Start:");
    	String actPDFLoc = "Z:\\DIG_Auftraege\\D_2017_1062_Gottardi_OCR\\Kurzgefaßte_Geschichte_Statistik_und_Topographie_von_Tirol.pdf";

    	ArrayList<PDFPage> pages = processPDF(actPDFLoc);

    	for (PDFPage pdfPage : pages) {
    		System.out.println("outputting page: " + pdfPage.pageIndex);
    		ArrayList<PDFRegion> regions = pdfPage.regions;


    		for (PDFRegion pdfRegion : regions) {
        		ArrayList<PDFLine> lines = pdfRegion.lines;
        		System.out.println("outputting region:");
        		for (PDFLine pdfLine : lines) {
					System.out.println(pdfLine.getText() + "\n" );
				}

			}
		}

    	System.out.println("Fin!");
    }


    /**
     *
     * @param actPDFLoc - local system location of the PDF file
     * @return List of segmented pages
     */
    public static ArrayList<PDFPage> processPDF(String actPDFLoc) {
        try
        {
        	PDDocument document = PDDocument.load(new File(actPDFLoc));
        	PDFTextExtractor stripper = new PDFTextExtractor(document);
            stripper.setSortByPosition(true);
            for (int page = 0; page < document.getNumberOfPages(); ++page)
            {
            	stripper.lineBuffer = new ArrayList<PDFLine>();
            	stripper.avarageLinelenght = 0;
            	stripper.actLineBottom = Float.MAX_VALUE;

                stripper.stripPage(page);

                PDFPage resultPage = new PDFPage(page, new ArrayList<PDFRegion>());
                stripper.createRegions(resultPage);

            	stripper.pages.add(resultPage);
            }


            return stripper.pages;

        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
        return null;
	}

    /**
     * 	Creates out of the buffered lines for the actual page the regions using 3 simple rules:
     * 		1) if no region is present -> create one
     * 		2) if lineDistance to previous line exeeds given threshold -> create region
     * 		3) if previous line is shorter then 80% of the avarage line length -> create region
     * */
	private void createRegions(PDFPage resultPage) {
        ArrayList<Float> lineDistances = calculateLineDistances();

		for (int i = 0; i < lineBuffer.size(); i++) {
			if (i==0 || lineDistances.get(i-1) > regionDstThreshold || lineBuffer.get(i-1).getRect().width < avarageLinelenght*0.8)
			{
				PDFRegion newRegion = new PDFRegion(new ArrayList<PDFLine>());
				newRegion.lines.add(lineBuffer.get(i));
				resultPage.regions.add(newRegion);
				lastRegion = newRegion;
			}
			else
				lastRegion.lines.add(lineBuffer.get(i));
		}

	}


	private ArrayList<Float> calculateLineDistances() {
		ArrayList<Float> result = new ArrayList<Float>();
		for (int i = 1; i < lineBuffer.size(); i++) {
			PDFLine actLine = lineBuffer.get(i);
			PDFLine previousLine = lineBuffer.get(i-1);
			result.add((float)(actLine.getRect().y - previousLine.getRect().y));
			avarageLinelenght += actLine.getRect().width * 1f / lineBuffer.size();
		}
        return result;
	}

	private void stripPage(int page) throws IOException
    {

        setStartPage(page + 1);
        setEndPage(page + 1);

        Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        writeText(document, dummy);

    }




    @Override
    protected void writeString(String value, List<TextPosition> textPositions) throws IOException
    {
    	float wordL = Float.MAX_VALUE;
    	float wordT = Float.MAX_VALUE;
    	float wordR = Float.MIN_VALUE;
    	float wordB = Float.MIN_VALUE;
    	float actFontSize = 0f;


        for (TextPosition text : textPositions)
        {
        	float actL = text.getXDirAdj();
        	float actB = text.getYDirAdj();
        	float actR = text.getXDirAdj() + text.getWidthDirAdj();
        	float actT = text.getYDirAdj() - text.getHeightDir();

        	wordL = Math.min(actL, wordL);
        	wordT = Math.min(actT, wordT);
        	wordR = Math.max(actR, wordR);
        	wordB = Math.max(actB, wordB);

        	actFontSize = text.getFontSize();
        }

        PDFString actString = new PDFString(value, (int)(wordL*SCALE), (int)(wordT*SCALE), (int)(wordR*SCALE), (int)(wordB*SCALE), false, actFontSize );
        //just a very simple rule for linebreaks
        if (wordB < actLineBottom + lineDstThreshold && wordB  > actLineBottom - lineDstThreshold && lineBuffer.size()>0)
        {
        	//previous line
        	PDFLine actLine = lineBuffer.get(lineBuffer.size()-1);
        	actLine.strings.add(actString);

        }
        else
        {
        	//new line
        	PDFLine actLine = new PDFLine(new ArrayList<PDFString>());
        	actLine.strings.add(actString);
        	lineBuffer.add(actLine);
        }
        actLineBottom = wordB;


    }

}