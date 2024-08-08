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

public class ConverterFTN {

	class Operation {
		
		long epoch;
		
		String label;
		
		double amount; // with . decimal point
		
		String buyer;
		
		String comment;
		
		String subCategory;
		
	}
	
	int presetInfo( Operation o ) {
		
		if( o.label.equals( "VIR ENVEA")                        ) { o.subCategory = "SALAIRE"; return 0; }
		if( o.label.equals( "VIR TRESORERIE BAYONNE MUNICIPAL") ) { o.subCategory = "SALAIRE"; return 0; }
		if( o.label.equals( "VIR SGC BAYONNE")                  ) { o.subCategory = "SALAIRE"; return 0; }
		if( o.label.equals( "VIR CAF DES PA")                   ) { o.subCategory = "ALLOC CAF"; return 0; }
		
		if( o.label.equals( "PRLV DIRECTION GENERALE DES FINA") ) { o.subCategory = "IMPOTS"; return 0; }
		
		if( o.label.equals( "PRLV FREE MOBILE")                 ) { o.subCategory = "TELECOMMUNICATION"; return 0; }
		if( o.label.equals( "PRLV Free Telecom")                ) { o.subCategory = "TELECOMMUNICATION"; return 0; }

		if( o.label.equals( "PRLV Kereis France")                  ) { o.subCategory = "LOGEMENT"; return 0; }
		if( o.label.equals( "PRLV Cbp France devient Kereis F")    ) { o.subCategory = "LOGEMENT"; return 0; }
				
		if( o.label.equals( "PRLV EDF clients particuliers")    ) { o.subCategory = "LOGEMENT"; return 0; }
		if( o.label.equals( "PRLV ECHEANCE PRET")               ) { o.subCategory = "LOGEMENT"; return 0; }
		if( o.label.equals( "VIR MENSUEL REMB CREDIT PTZ")      ) { o.subCategory = "LOGEMENT"; return 0; }
		
		if( o.label.equals( "PRLV TRES." ) || o.label.equals( "PRLV SGC" ) ) {
			if( -20.0 < o.amount && o.amount < -60.0 ) {
				o.subCategory = "LOGEMENT";
				o.comment = "CAPB - eau";
				return 0;
			}
		}
		
		if( o.label.equals( "PRLV AUTOROUTES DU SUD DE LA FRA") ) { o.subCategory = "VEHICULE"; return 0; }
		if( o.label.equals( "PRLV Autoroutes du Sud de la Fra") ) { o.subCategory = "VEHICULE"; return 0; }
		
		if( o.label.equals( "VIR GENERATION")                   ) { o.comment = "remboursement"; o.subCategory = "SANTE"; return 0; }
		if( o.label.equals( "VIR C.P.A.M. DE BAYONNE")          ) { o.comment = "remboursement"; o.subCategory = "SANTE"; return 0; }
		
		if( o.label.equals( "PRLV ASG LARGENTE")                ) { o.comment = "prélèvement mensuel"; o.subCategory = "SCOLARITE"; return 0; }
		
		if( o.label.equals( "VIR MENSUEL FTN COMMUN vers PEL")  ) { o.comment = "PEL enfants"; o.subCategory = "MOUVEMENT INTERNE"; return 0; }
		
		if( o.label.equals( "CARTE VERGERS DE CAZAU TARNOS")       ) { o.comment = "verget de cazaubon"; o.subCategory = "ALIMENTATION"; return 0; }
		if( o.label.equals( "CARTE SARL MIRENTXU TARNOS")          ) { o.comment = "légume tarnos";      o.subCategory = "ALIMENTATION"; return 0; }
		
		if( o.label.equals( "CARTE CARREFOUR TARNOS TARNOS")        ) {                                   o.subCategory = "ALIMENTATION"; return 0; }
		if( o.label.equals( "CARTE PICARD 0694 BOUCAU")             ) {                                   o.subCategory = "ALIMENTATION"; return 0; }
		if( o.label.contains("CARTE") && o.label.contains("OTSOKOP")){                                   o.subCategory = "ALIMENTATION"; return 0; }
		if( o.label.equals( "CARTE VERGERS DE CAZAU TARNOS")        ) { o.comment = "verget de cazaubon"; o.subCategory = "ALIMENTATION"; return 0; }
		if( o.label.equals( "CARTE MA BOUTIQUE D'AS TARNOS")        ) { o.comment = "magasin chinois de TARNOS"; o.subCategory = "ALIMENTATION"; return 0; }
		
		if( o.label.contains("CARTE") && o.label.contains("DAC")    ) { o.comment = "essence"; o.subCategory = "VEHICULE"; return 0; }
		if( o.label.contains("CARTE") && o.label.contains("VL")     ) { o.comment = "essence"; o.subCategory = "VEHICULE"; return 0; }

		o.subCategory = "???";
		return 1;
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
				
				// amount : two possible column (fist debt, second credit)
				String amountStr = "0";
				
				if( fields[3].length() > 0 ) {
					amountStr = fields[3];
				}
				else if( fields[4].length() > 0 ) {
					amountStr = fields[4];
				}
				
				operation.amount = Double.valueOf(amountStr.replace(',', '.'));
				
				// process CB
				
				// System.out.format( "[%s]\n", label );
				
				int pos = label.indexOf("CARTE", 0 );
				if( pos == 0 ) {
					
					String dateCB = label.substring(6, 6+5);
					// System.out.format( "date CB [%s]", dateCB );
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
		
		// sort operations (oldest at bottom) ...

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

//		for( Operation op : operations ) {
//
//			System.out.format( "%s|%s|%f\n", EpochTool.convertToString( op.epoch, EpochTool.Format.STD_SLASH_FULL_FR ), op.label, op.amount );
//		}

		// preset information for well known operations

		int unknownCount = 0;
		
		for( Operation op : operations ) {

			unknownCount += presetInfo( op );
		}
		
		// output modified csv

		String outputCSV  = originalCSV.folderOnly + "\\output.csv";

		List<String[]> stringArray = new ArrayList<String[]>();

		
		
		for( Operation o : operations ) {

			String[] array = new String[6];
			stringArray.add(array);

			array[0] = EpochTool.convertToString( o.epoch, EpochTool.Format.STD_SLASH_DAY_FR );
			array[1] = o.label;
			array[2] = Double.toString(o.amount).replace('.', ',' );
			array[3] = o.buyer;
			array[4] = o.comment;
			array[5] = o.subCategory;
		}

		CSVWriter writer = new CSVWriter(new FileWriter(outputCSV), ';', '\u0000', '\\', "\n" );

		writer.writeAll(stringArray);
		writer.close();

		System.out.println( String.format( "--------------------------------------" ));
		System.out.println( String.format( "Operations with unknown category : %d", unknownCount ));
		System.out.println( String.format( "CSV flushed OK, operations lines : %d", operations.size()));
		System.out.println( String.format( "--------------------------------------" ));
		
		return;
	}
}
