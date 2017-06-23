package org.dea.util.pdf.beans;

import java.awt.Rectangle;
import java.util.ArrayList;


public class PDFLine {
	public ArrayList<PDFString> strings;

	public PDFLine(ArrayList<PDFString> strings) {
		this.strings = strings;
	}


	public Rectangle getRect()
	{
		Rectangle res = null;
		for (PDFString actString : strings) {
			res = (res==null)?actString.getRect():res.union(actString.getRect());
		}
		return res;
	}

	public String getText()
	{
		String res = "";
		for (PDFString actString : strings) {
			res+=actString.value;
		}
		return res;
	}


}
