package org.dea.util.pdf.beans;

import java.awt.Rectangle;
import java.util.ArrayList;


public class PDFRegion {
	public ArrayList<PDFLine> lines;


	public PDFRegion(ArrayList<PDFLine> lines) {
		this.lines = lines;
	}

	public Rectangle getRect()
	{
		Rectangle res = null;
		for (PDFLine actLine : lines) {
			res = (res==null)?actLine.getRect():res.union(actLine.getRect());
		}
		return res;
	}


	public String getText()
	{
		String res = "";
		for (PDFLine actLine : lines) {
			res+=actLine.getText();
		}
		return res;
	}



}
