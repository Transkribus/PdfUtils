package org.dea.util.pdf.beans;

import java.awt.Rectangle;
import java.util.ArrayList;


public class PDFPage {
	public ArrayList<PDFRegion> regions;
	public int pageIndex;

	public PDFPage(int page, ArrayList<PDFRegion> regions) {
		this.regions = regions;
		this.pageIndex = page;
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
