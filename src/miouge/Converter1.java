package miouge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

import miouge.beans.FileItem;

public class Converter1 {
	
	
	class Operation {
		
		long epoch;
		
		String label;
		
		double amount;
		
		String comment;
		
		String category;
		
	}
	
	void presetCategory( Operation o ) {
		
		if( o.label.equals( "VIR MENSUEL FTN COMMUN vers PEL") ) { o.comment = "versement PEL"; o.category = "MOUVEMENT_INTERNE"; }
	}	
	
	void convert( String filename ) throws Exception {
		
		System.out.format( "converter1 <%s>\n", filename );
		
		String workingDir = Paths.get(".").toAbsolutePath().normalize().toString();
		
		//Path workingDir = FileSystems.getDefault().getPath(".");
		
		System.out.format( "working folder is <%s>\n", workingDir );
		
		File f = new File(filename);
		if(f.exists() && f.isFile()) { 
			// ok			
		}
		else {
		
			System.out.format( "file <%s> don't exist\n", filename );
		}				
		
		FileItem fi = new FileItem();
		
		fi.name = f.getName();
		fi.fullpathname = workingDir + "/" + f.getName();
		
		System.out.format( "fi.fullpathname <%s>\n", fi.fullpathname );
				
		Unzipper unzipper = new Unzipper();
		unzipper.unpackZIP( 1, fi, workingDir.toString() );
				
		ArrayList<FileItem> files = new ArrayList<>();			
		Tools.listInputFiles( workingDir, ".*\\.csv", files, false, true );

		if( files.size() == 0 ) {			
			return;
		}
		
		FileItem originalCSV = files.get(0);
		
		String iniFileEncoding = "ISO-8859-1";
		File source = new File( originalCSV.fullpathname );
	
	   InputStreamReader isr = new InputStreamReader( new FileInputStream( source ), Charset.forName( iniFileEncoding ) ); 
	
	   ZoneId zoneId = ZoneId.of( "Europe/Paris" );
	   
	   ArrayList<Operation> operations = new ArrayList<Operation>(); 	   
		
		CSVParser csvParser = new CSVParserBuilder().withSeparator(';').build(); // custom separator		
		try( CSVReader reader = new CSVReaderBuilder( isr ) // new FileReader( originalCSV.fullpathname ))
				.withCSVParser(csvParser) // custom CSV
				.build()
		) {
			List<String[]> lines = reader.readAll();
			lines.forEach( fields -> {
				
				// 31/08/2022;31/08/2022;CARTE 20/07 CARREFOUR TARNOS TARNOS;-19,93;
												
				if( fields.length > 0 && fields[0].equals("Date opération") == true ) {

					// skip header;
					return;
				}
				
				String timestamp = fields[0];
				String label = fields[2];				
								
				Operation operation = new Operation();
				
				String amountStr = "0";
				
				if( fields[3].length() > 0 ) {
					amountStr = fields[3];
				}
				else if( fields[4].length() > 0 ) {
					amountStr = fields[4];
				}
				
				operation.amount = Double.valueOf(amountStr.replace(',', '.'));
				
				// process CB
				
				System.out.format( "[%s]\n", label );				
				
				int pos = label.indexOf("CARTE", 0 );				
				if( pos == 0 ) {
					
					String dateCB = label.substring(6, 6+5);
					System.out.format( "date CB [%s]", dateCB );
					timestamp = dateCB + timestamp.substring(5, 10); // date of CB + year of operation
					
					label = "CARTE" + label.substring(11, label.length());
				}
																	
				// STD_SLASH_FULL_FR("dd/MM/yyyy HH:mm:ss"),
				operation.epoch = EpochTool.convertToEpoch( timestamp + " 00:00:00", EpochTool.Format.STD_SLASH_FULL_FR, zoneId );
					
				if( operation.epoch > EpochTool.getNowEpoch() )
				{
					// taking care of end of year operations
					try {
						operation.epoch = EpochTool.add(operation.epoch, -1, null, null, null, null, null, zoneId, false );
					} catch (Exception e) {
						e.printStackTrace();					
					}					
				}
								
				System.out.format( "%s - %s \n", 
						EpochTool.convertToString( operation.epoch, EpochTool.Format.STD_DASH_FULL ),
						label
				);
				
				operation.label = label;
				operations.add( operation );
			});
		}	
		
		
		System.out.format( "operations nb =%d\n", operations.size() );
		
		// sort operation by date ascending
		
		// order span results using their spanType and the calibration span order type
		operations.sort(
			( Operation o1, Operation o2 ) -> {
				
				// must returns : a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.				
				// Tracer.info(String.format( "1 : %s(%d) / 2 %s(%d) ", type[0], rank[0], type[1], rank[1]));
				
				if( o1.epoch < o2.epoch ) {
					return 1;
				}
				else if( o1.epoch > o2.epoch ) {
					return -1;
				}
				return 0;
			}
		);
		
		// sorted operations ...
		
		
		for( Operation op : operations ) {
						
			System.out.format( "%s|%s|%f\n", EpochTool.convertToString( op.epoch, EpochTool.Format.STD_SLASH_FULL_FR ), op.label, op.amount );
		}
				
		// TODO : preset category for well known operations
		
		for( Operation op : operations ) {
			
			presetCategory( op );
		}
		
		// output modified csv
		
		String outputCSV  = originalCSV.folderOnly + "\\output.csv"; 
		
		List<String[]> stringArray = new ArrayList<String[]>();			

		for( Operation op : operations ) {
			
			String[] array = new String[5];
			stringArray.add(array);
			
			array[0] = EpochTool.convertToString( op.epoch, EpochTool.Format.STD_SLASH_DAY_FR );
			array[1] = op.label;
			array[2] = op.comment;
			array[3] = Double.toString(op.amount);
			array[4] = op.category;
		}
		
	     CSVWriter writer = new CSVWriter(new FileWriter(outputCSV), ';', '\u0000', '\\', "\n" );
	     
	     writer.writeAll(stringArray);
	     writer.close();
	     
	     System.out.println( String.format( "output CSV flush for %d operations : OK", operations.size()));	
		
		return;
	}
}
