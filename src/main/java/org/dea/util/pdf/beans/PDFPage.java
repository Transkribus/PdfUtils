package org.dea.util.pdf.beans;

import java.awt.Rectangle;
import java.util.ArrayList;


public class PDFPage {
	public ArrayList<PDFRegion> regions;

	public PDFPage(ArrayList<PDFRegion> regions) {
		this.regions = regions;
	}

	public Rectangle getContentRect()
	{
		Rectangle res = null;
		for (PDFRegion actRegion : regions) {
			res = (res==null)?actRegion.getRect():res.union(actRegion.getRect());
		}
		return res;
	}




}
