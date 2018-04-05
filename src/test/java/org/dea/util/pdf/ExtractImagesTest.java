package org.dea.util.pdf;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractImagesTest {
	private static final Logger logger = LoggerFactory.getLogger(ExtractImagesTest.class);
	
//	@Test
	public void extractImagesTest() throws SecurityException, IOException {
		
		final String largePdfPath = "/mnt/dea_scratch/TRP/bugs/large_pdf_oom/Beinecke_DL_Voynich Manuscript.pdf";
		final String tmpDirPath = "/tmp/pdf/";
		
		final File largePdf = new File(largePdfPath);
		File tmpDir = new File(tmpDirPath);
		if(!largePdf.isFile()) {
			logger.info("Skipping test as test file is not available.");
		}
		
		PageImageWriter imgWriter = new PageImageWriter();
		try {
			imgWriter.extractImages(largePdf.getAbsolutePath(), tmpDir.getAbsolutePath());
		} finally {
			String finalOutPath = imgWriter.getExtractDirectory();
			FileUtils.deleteDirectory(new File(finalOutPath));
		}
	}
}
