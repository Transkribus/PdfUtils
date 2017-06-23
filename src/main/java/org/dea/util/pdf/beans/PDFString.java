package org.dea.util.pdf.beans;

import java.awt.Rectangle;


public class PDFString {
   	public String value;
   	public int left;
   	public int top;
   	public int right;
   	public int bottom;
	public boolean isBold;
	public float fs;


	public PDFString(String value, int l, int t, int r, int b, boolean isBold, float fs) {
		this.value = value;
		this.isBold = isBold;
		this.fs = fs;
		this.left = l;
		this.top = t;
		this.right = r;
		this.bottom = b;
	}


	public String toString() {
		String res = "";
		res += value + "["+left+", "+top+", "+right+", "+bottom+"] + {fs="+fs+"}\n";
		return res;

	}

	public Rectangle getRect()
	{
		return new Rectangle(left, top, right-left, bottom-top);
	}


}
