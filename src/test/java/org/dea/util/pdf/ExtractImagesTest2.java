package org.dea.util.pdf;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractImagesTest2 {
	private static final Logger logger = LoggerFactory.getLogger(ExtractImagesTest2.class);
	
    public File[] finder(String dirName){
        File dir = new File(dirName);

        return dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".pdf");
			}
		});

    }
	
	public void extractAllPdfs(String basePath) {
		File[] files = finder(basePath);
		
		
		
		for (File file : files) {
			try {
				File destPath = new File(basePath + File.separator + FilenameUtils.getBaseName(file.getName()));
				destPath.mkdirs();
				logger.info("Found "+file.getAbsolutePath());
				extractImagesTest(file.getAbsolutePath(), basePath, true);
			} catch (SecurityException | IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	public void extractImagesTest(String pdfPath, String tmpDirPath, boolean keep) throws SecurityException, IOException{

		final File largePdf = new File(pdfPath);
		File tmpDir = new File(tmpDirPath);
		if(!largePdf.isFile()) {
			logger.info("Skipping test as test file is not available.");
		}
		
		PageImageWriter imgWriter = new PageImageWriter();
		try {
			imgWriter.extractImages(largePdf.getAbsolutePath(), tmpDir.getAbsolutePath());
		} finally {
			if (!keep) {
				String finalOutPath = imgWriter.getExtractDirectory();
				FileUtils.deleteDirectory(new File(finalOutPath));
			}
		}
		
	}
	
//	@Test
	public void extractImagesTest() throws SecurityException, IOException {
		try {
			extractImagesTest("/mnt/dea_scratch/TRP/bugs/large_pdf_oom/Beinecke_DL_Voynich Manuscript.pdf","/tmp/pdf/", false);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public static void main(String [] args) {
		ExtractImagesTest2 test = new ExtractImagesTest2();
//		try {
//			if (args.length > 0) {
//				test.extractImagesTest(args[0], System.getProperty("java.io.tmpdir"), false);
//			} else test.extractImagesTest();
//
//		} catch (SecurityException | IOException e) {
//				// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

//		test.extractAllPdfs("C:\\Projekte\\Daten_Transkribus_Table_Collection\\datasets\\BELGRADE\\tabele\\vojna geografija");
		
		//test.extractAllPdfs("Y:/HTR/für_Digitexx/NAN_2020_(National_Archive_Netherlands)/Input_Test");
		
		test.extractAllPdfs("Y:/HTR/für_Digitexx/Dialektarchiv/PDFs_Test");
	}
}
