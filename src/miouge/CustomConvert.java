package miouge;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CustomConvert {

	public static void main(String[] args) {
		
		boolean error = true;
		
		Options options = new Options();

        Option projectOpt = new Option("f", "file", true, "file to process");
        projectOpt.setRequired(true);
        options.addOption(projectOpt);
                        
        final Option debugOpt = Option.builder("d") 
                .longOpt("debug") 
                .desc("switch Debug/Verbose mode on") 
                .hasArg(false) 
                .required(false) 
                .build();
        options.addOption(debugOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
        	
            cmd = parser.parse(options, args);
            
        } catch (ParseException e) {
        	
            System.out.println(e.getMessage());
            formatter.printHelp("custom-convert", options);
            System.exit(1);
        }
        
        String file = cmd.getOptionValue("f", "default");
        boolean debug = cmd.hasOption("debug");
        
        if( file == null ) {
        	
            formatter.printHelp("custom-convert", options);
            System.exit(1);
        }        
        
        System.out.format( "using file=%s\n", file );
    
        if( debug ) {
        	System.out.format( "verbose/debug mode ON\n" );
        }
		
		try {
			
			Converter1 converter = new Converter1(); 
			converter.convert(file);
			
			error = false;
		}
		catch ( Exception e )
		{
			System.out.format( "Exception : %s\n", e.getMessage() );
		}
		
		if( error ) {
			System.out.format( "end of program (on error)\n" );
		}
		else {
			System.out.format( "end of program (good completion)\n" );
		}
	}}
