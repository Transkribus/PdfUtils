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
 * @author Raphael Unterweger
 */
public class PDFTextExtractor extends PDFTextStripper
{
    public static int SCALE = 4;
    public static float lineDstThreshold = 5f;

    private final PDDocument document;
    private float actLineBottom = Float.MAX_VALUE;

    private ArrayList<PDFLine> lineBuffer;


    private ArrayList<PDFPage> pages;

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
    	String actPDFLoc = "/mnt/dea_scratch/TRP/PDFTextExtractorTest/Kurzgefaßte_Geschichte_Statistik_und_Topographie_von_Tirol.pdf";

    	ArrayList<PDFPage> pages = processPDF(actPDFLoc);

    	for (PDFPage pdfPage : pages) {
    		System.out.println("outputting");
			System.out.println(pdfPage);
		}

    	System.out.println("Fin!");
    }



    public static ArrayList<PDFPage> processPDF(String actPDFLoc) {
        try
        {
        	PDDocument document = PDDocument.load(new File(actPDFLoc));
        	PDFTextExtractor stripper = new PDFTextExtractor(document);
            stripper.setSortByPosition(true);
            for (int page = 0; page < document.getNumberOfPages(); ++page)
            {
            	if (page!=8)
            		continue;

                stripper.stripPage(page);

                PDFPage resultPage = new PDFPage(new ArrayList<PDFRegion>());
            	PDFRegion region = new PDFRegion(new ArrayList<PDFLine>());
            	region.lines = stripper.lineBuffer;
            	resultPage.regions.add(region);


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

	private void stripPage(int page) throws IOException
    {

//        setStartPage(page + 1);
//        setEndPage(page + 1);

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
        	float actT = text.getYDirAdj();
        	float actR = text.getXDirAdj() + text.getWidthDirAdj();
        	float actB = text.getYDirAdj() + text.getHeightDir();

        	wordL = Math.min(actL, wordL);
        	wordT = Math.min(actT, wordT);
        	wordR = Math.max(actR, wordR);
        	wordB = Math.max(actB, wordB);

        	actFontSize = text.getFontSize();
        }

        PDFString actString = new PDFString(value, (int)(wordL*SCALE), (int)(wordT*SCALE), (int)(wordR*SCALE), (int)(wordB*SCALE), false, actFontSize );
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