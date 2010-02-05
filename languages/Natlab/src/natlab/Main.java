package natlab;

import natlab.options.Options;
//import natlab.toolkits.analysis.ForVisitor;
import ast.*;
import natlab.server.*;
import natlab.toolkits.analysis.ForVisitor;

import natlab.toolkits.analysis.varorfun.*;
import natlab.toolkits.analysis.example.*;

/*import matlab.MatlabParser;
import matlab.TranslationProblem;
import matlab.OffsetTracker;
import matlab.TextPosition;*/
import matlab.*;
import matlab.FunctionEndScanner.NoChangeResult;
import matlab.FunctionEndScanner.ProblemResult;
import matlab.FunctionEndScanner.TranslationResult;

import org.antlr.runtime.ANTLRReaderStream;


import beaver.Parser;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Main entry point for McLab compiler. Includes a main method that
 * deals with command line options and performs the desired
 * functions. Also includes static methods used to simplify tasks such
 * as parsing program code and translating from matlab to natlab.
 */
public class Main
{
    private static Options options;
    private static final int SERVER_PORT = 47146; //default server port
    private static final long HEART_RATE = 4000; //in milliseconds
    private static final long HEART_DELAY = 5000; //delay till first heart beat check is made
    
    /**
     * Main method deals with command line options and execution of
     * desired functions.
     */
    public static void main(String[] args) throws Exception
    {

        //natlab.toolkits.analysis.functionhandle.TestAnalysis.main(args);
        //System.exit(0);
        boolean quiet; //controls the suppression of messages
        ArrayList errors = new ArrayList();
        options = new Options();
        
        if( processCmdLine( args ) ){
            
            if( options.help() ){
                System.err.println(options.getUsage());
            }
            else{
                quiet = options.quiet();  //check if quiet mode is active
                
                if( options.e() ){
                    if( !quiet )
                        System.err.println("exhaustive list");
                }
                if( options.d() ){
                    if( !quiet )
                        System.err.println("dynamic linking");
                }
		
                if( options.server() ){
                    //in server mode
                    if( !quiet )
                        System.err.println("server mode");
                    
                    //setup port number
                    int portNumber = SERVER_PORT;
                    if( options.sp().length() > 0 )
                        portNumber = Integer.parseInt(options.sp());
                    
                    if( !quiet )
                        System.err.println("opening server on port " + portNumber );
                    //start the server
                    ServerSocket serverSocket = null;
                    try{
                        serverSocket = new ServerSocket( portNumber );
                    }
                    catch( IOException e ){
                        System.err.println("Server could not be opened on port "+portNumber);
                        System.exit(1);
                    }
                    if( !quiet )
                        System.err.println("Server started");
                    
                    //wait for and accept a connection
                    Socket clientSocket = null;
                    
                    try{
                        clientSocket = serverSocket.accept();
                    }catch( IOException e ){
                        System.err.println("accept client failed");
                        System.exit(1);
                        return;
                    }
                    
                    if( !quiet )
                        System.err.println("Client connected");
                    
                    BufferedReader in;
                    PrintWriter out;
                    try{
                        out = new PrintWriter( clientSocket.getOutputStream(), true );
                        in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream()));
                    }catch( IOException e){
                        System.err.println("Server input stream creation failed");
                        System.exit(1);
                        return;
                    }
                    int inCharInt = 0;
                    StringBuffer commandBuf;
                    XMLCommandHandler xmlCH = new XMLCommandHandler();
                    boolean shutdown = false;
                    //create flag object for keeping track of heart beats
                    HeartBeatFlag hbFlag = new HeartBeatFlag();
                    //create the timer that checks the heart beat signal
                    Timer heartBeat = new Timer();
                    if( !options.noheart() ){
                        //start the timer with a HeartBeatTask and an initial delay
                        heartBeat.schedule(new HeartBeatTask(hbFlag, out, quiet) ,HEART_DELAY, HEART_RATE );
                    }
                    //loop until told to shutdown
                    while( !shutdown ) {
                        commandBuf = new StringBuffer();
                        try{
                            //loop while reading in from socket
                            //stop when a null byte is reached
                            while( true ){
                                inCharInt = in.read();
                                if( inCharInt == 0 ){
                                    break;
                                }
                                commandBuf.append((char)inCharInt);
                            }                                
                        }catch( IOException e ){}
                        if( xmlCH.parse( commandBuf.toString() ) ) {
                            boolean parsing = false;
                            Reader source = new StringReader( "" );
                            String fileName = "";
                            String cmd = xmlCH.getCommand();
                            ArrayList serverErrors = new ArrayList();
                            if( cmd.equalsIgnoreCase( "parsefile" ) ){
                                fileName = xmlCH.getBody();
                                if( !quiet ){
                                    System.err.println(" parsefile cmd ");
                                    System.err.println("  parse file "+fileName);
                                }
                                if( fileName == null ){
                                    String FNstr="";
                                    if( !quiet )
                                        FNstr+="  file name was null"+"/n";
                                    CompilationProblem FileNullcerror = new CompilationProblem(FNstr+="<errorlist><error>parsefile error: filename was null</error></errorlist>\0");
                                    errors.add(FileNullcerror);
                                    continue;
                                }
                                parsing = true;
                                if( options.matlab() ){
                                    if( !quiet )
                                        System.err.println(" translating");
                                    source = translateFile( fileName, serverErrors );
                                }
                                else{
                                    try{
                                        source = new FileReader( fileName );
                                    }catch( FileNotFoundException e){
                                        String FNFEstr="";
                                        if( !quiet )
                                            FNFEstr+="file: "+fileName+" not found"+"/n";
                                        CompilationProblem FileNotFoundcerror = new CompilationProblem(FNFEstr+="<errorlist><error>file: "+fileName+" not found</error></errorlist>\0");
                                        errors.add(FileNotFoundcerror);
                                    }
                                }
                            }
                            else if( cmd.equalsIgnoreCase( "parsetext" ) ){
                                String programText = xmlCH.getBody();
                                if( !quiet ){
                                    System.err.println(" parsetext cmd ");
                                    System.err.println("  text to parse:");
                                    System.err.println( programText );
                                }
                                if( programText == null ){
                                    if( !quiet )
                                        System.err.println("  program text was null");
                                    out.print("<errorlist><error>parsetext error: program text was null</error></errorlist>\0");
                                    out.flush();
                                    continue;
                                }
                                fileName = "source/text";
                                parsing = true;
                                if( options.matlab() ){
                                    if( !quiet )
                                        System.err.println(" translating");
                                    source = translateFile( fileName, programText, serverErrors );
                                }
                                else
                                    source = new StringReader( programText );
                            }
                            else if( cmd.equalsIgnoreCase( "shutdown" ) ) {
                                if( !quiet ){
                                    System.err.println(" shutdown cmd ");
                                }
                                out.print("<shutdown />\0");
                                out.flush();
                                shutdown = true;
                                //cancel the heartbeat timer - orly?
                                heartBeat.cancel();
                                try{
                                    clientSocket.close();
                                    out.close();
                                    in.close();
                                    serverSocket.close();
                                }catch(IOException e){}
                                
                                continue;
                            }
                            //when you get a heartbeat signal, set the heart beat flag.
                            else if( cmd.equalsIgnoreCase( "heartbeat" ) ) {
                                hbFlag.set();
                            }
                            else{
                                if( !quiet ){
                                    System.out.println(" ignored cmd ");
                                }
                            }
                            
                            if( parsing ) {
                                if( options.matlab() ){
                                    /*if( !quiet )
                                      System.err.println(" translating ");
                                      source = translateFile( fileName, source, serverErrors );
                                    */
                                    if( serverErrors.size() > 0 ){
                                        String serverEstr="";
                                        if( !quiet )
                                            serverEstr+="errors\n" + serverErrors+"/n";
                                        CompilationProblem Servercerror = new CompilationProblem(serverEstr+="<errorlist><error>Error has occured in translating, more information to come later</error></errorlist>\0");
                                        errors.add(Servercerror);
                                        continue;
                                        
                                    }
                                    if( source == null ){
                                        String fooEstr="";
                                        if( !quiet )
                                            fooEstr+="skipping file"+"/n";
                                        CompilationProblem Foocerror = new CompilationProblem(fooEstr+="<errorlist><error>Error has occured in translating, more information to come later</error></errorlist>\0");
                                        errors.add(Foocerror);
                                        continue;
                                    }
                                }
                                if( !quiet )
                                    System.err.println(" parsing ");
                                Program prog = parseFile( fileName, source, serverErrors );
                                if( serverErrors.size() > 0){
                                    String serverEstr="";
                                    if( !quiet )
                                        serverEstr+="errors\n" + serverErrors+"/n";
                                    CompilationProblem Servercerror = new CompilationProblem(serverEstr+="<errorlist><error>Error has occured in translating, more information to come later</error></errorlist>\0");
                                    errors.add(Servercerror);
                                    continue;
                                }
                                if( prog == null ){
                                    String serverEstr="";
                                    if( !quiet )
                                        serverEstr+="errors\n" + serverErrors+"/n";
                                    CompilationProblem Servercerror = new CompilationProblem(serverEstr+="<errorlist><error>Error has occured, more information to come later</error></errorlist>\0");
                                    errors.add(Servercerror);
                                    continue;
                                }
                                CompilationUnits cu = new CompilationUnits();
                                cu.addProgram( prog );
                                if( !quiet )
                                    System.err.println(" sending resp: \n" + cu.XMLtoString(cu.ASTtoXML()));
                                
                                out.print(cu.XMLtoString(cu.ASTtoXML())+ "\0");
                                out.flush();
                                if( !quiet )
                                    System.err.println(" resp sent");
                            }
                        }
                    }
                    if(!quiet)
                        System.err.println( "server shutdown" );
                }
                else if( options.getFiles().size() == 0 ){
                    System.err.println("No files provided, must have at least one file.");
                }
                else{ 
                    //Plain cmd line mode

                    //parse each file and put them in a list of Programs
                    ArrayList<Program> programs = new ArrayList<Program>( options.getFiles().size() );
                    ArrayList<String> fileNames = new ArrayList<String>( options.getFiles().size() );
                    
                    for( Object o : options.getFiles() ){
                        //When processing files there are currently two options.
                        //Either the matlab flag is set or it isn't.
                        //When it is set, each file is translated to natlab.
                        //The resulting string is put into a StringReader and 
                        //assigned to fileReader. Otherwise fileReader is 
                        //assigned a FileReader instance pointing to the 
                        //current file. In either case, fileReader has 
                        //the correct value.
                        String file = (String) o;
                        Reader fileReader = new StringReader("");

                        fileNames.add( file );
			
                        //checks if dependence analysis flag is set.
                        //If the flag is set then the type of dependence test that needs to be applied.
                        if(options.danalysis()){                            
                            Program prog = null;							
                            if( !quiet )
                                System.err.println("Dependence Tester");
                            
                            if(options.gcd()){
                                if( !quiet )	
                                    System.err.println("Dependence Analysis with GCD Test");
                                fileReader=translateFile(file,errors);
                                prog = parseFile( file,  fileReader, errors );
				
                                if(prog!=null)
                                    {
                                        String testType="gcd";
                                        parseProgramNode(prog,testType);
                                        //System.out.println("Out of parser for gcd testing");
                                    }
                            }
                            
                            if(options.bj()){
                                if( !quiet )
                                    System.err.println("Dependence Analysis with Banerjee's Test");
                            }
                        }
                        
                        if( options.matlab() ){
                            //translate each file from matlab to natlab
                            //if successful the result will end up as a StringReader instance 
                            // in fileReader variable
                            //try{
                            if( !quiet )
                                System.err.println("Translating "+file+" to Natlab");
                            fileReader = translateFile( file, errors );
                            
                            if( errors.size() > 0 )
                                System.err.print( errors.toString() );
                            if( fileReader == null ){
                                System.err.println("\nSkipping " + file);
                                break;
                            }
                        }
                        else{
                            //treat as a natlab input, set fileReader to a new 
                            //FileReader instance pointing to the current file
                            try{
                                fileReader = new FileReader( file );
                            }catch(FileNotFoundException e){
                                System.err.println("File "+file+" not found!\nAborting");
                                System.exit(1);
                            }
                        }
                        if( !quiet )
                            System.err.println("Parsing: " + file);
                        //parse the file
                        Program prog = parseFile( file,  fileReader, errors );
                        
                        //report errors
                        if( errors.size() > 0 )
                            System.err.print( errors.toString() );
                        
                        if( prog == null ){
                            System.err.println("\nSkipping " + file);
                            break;
                        }
                        
                        //Weeding checks
                        //if(prog.errorCheck()){
                        //    System.err.println("\nWeeding Failed, Skipping: " + file);
                        //    break;
                        //}
                        
                        programs.add(prog);
                    }
                    
                    //Take all resulting Program nodes and place them in a
                    //CompilationUnits instance
                    CompilationUnits cu = new CompilationUnits();
                    for( Program p : programs ){
                        cu.addProgram( p );
                    }
                    
                    if( options.xml() ){
                        //System.out.println(cu.ASTtoXML());
                        System.out.print(cu.XMLtoString(cu.ASTtoXML()));
                    }
                    else if( options.pretty() ){
                        if( !quiet )
                            System.err.println("Pretty Printing");

                        //NOTE: This might be fragile, what if Program nodes are removed?
                        if( options.od().length() > 0 ){
                            File outputDir = new File( options.od() );
                            try{
                                if( !outputDir.exists() && !outputDir.mkdirs() ){
                                    System.err.println( "Could not create output directory." );
                                    System.err.println( "Some directories may have been created though." );
                                    System.exit(1);
                                }
                            }catch( SecurityException e ){
                                System.err.println( "Security error, is there a security manager active?" );
                                System.err.println( e );
                                System.exit(1);
                            }
                            FileWriter outFile;
                            String fileName;
                            for( int i = 0; i<fileNames.size(); i++ ){
                                    fileName = (new File(fileNames.get(i)) ).getName();
                                try{
                                    outFile = new FileWriter( new File( outputDir, fileName ) );
                                    outFile.write( cu.getProgram(i).getPrettyPrinted() );
                                    outFile.close();
                                }catch( IOException e ){
                                    System.err.println( "Problem writing to file "+new File( outputDir, fileName ) );
                                    System.err.println( e );
                                    System.exit(1);
                                }
                            }
                        }
                        else
                            System.out.println(cu.getPrettyPrinted());
                    }
                    else if( options.vfpreorder() ){
                        VFPreorderAnalysis a = new VFPreorderAnalysis( cu );
                        a.analyze();
                        System.out.println(cu.getPrettyPrinted());
                        System.out.println( a.getCurrentSet().toString() );

                        FlowAnalysisTestTool testTool = new FlowAnalysisTestTool( cu, VFStructuralForwardAnalysis.class );
                        System.out.println( testTool.run() );

                        System.out.println( "********\ndoing vf data collection");
                        VFDataCollector vfdata = new VFDataCollector( cu );
                        vfdata.analyze();

                        System.out.println( "********\ndoing use data collection");
                        UseDataCollector udata = new UseDataCollector( cu );
                        udata.analyze();

                        System.out.println( "********\ndoing assigned data collection");
                        AssignedDataCollector adata = new AssignedDataCollector( cu );
                        adata.analyze();

                        System.out.println( "use "+udata.getCurrentSet().toString() );
                        System.out.println( "v or f"+vfdata.getCurrentSet().toString() );

                        int[] counts = vfdata.countData("USE");
                        System.out.println( counts[0]+"/"+counts[1]+"/"+counts[2] );
                        System.out.println( "assigned "+adata.getCurrentSet().toString() );
                        counts = adata.countData("USE");
                        System.out.println( counts[0]+"/"+counts[1]+"/"+counts[2] );
                    }
                    else if( options.run() ){
                        FlowAnalysisTestTool testTool = new FlowAnalysisTestTool( cu, DefiniteAssignment.class );
                        System.out.println( testTool.run());
                    }
                }
            }
            //TODO-JD: probably shouldn't need to exit?
            System.exit(0);
        }
    }
    
    private static void parseProgramNode(Program prog,String testType)
    {
        ForVisitor forVisitor = new ForVisitor(testType);
        prog.apply(forVisitor);   
    }
    
    /**
     * Perform the reading and translation of a given file.
     *
     * @param fName    The name of the file to be translated.
     * @param errList  A list of errors for error collection.
     * 
     * @return A reader object giving access to the translated
     * source.
     */
    public static Reader translateFile(String fName, ArrayList errList)
    {
        BufferedReader in = null;
        PositionMap prePosMap = null;
        try{
            in = new BufferedReader( new FileReader( fName ) );
            FunctionEndScanner prescanner = new FunctionEndScanner(in);
            FunctionEndScanner.Result result = prescanner.translate();
            in.close();
            
            if(result instanceof NoChangeResult){
                in = new BufferedReader( new FileReader( fName ) );
            }else {
                in.close();
                if(result instanceof ProblemResult){
                    for(TranslationProblem prob : ((ProblemResult) result).getProblems()){
                        CompilationProblem translationcproblem = new CompilationProblem(prob.getLine(),prob.getColumn(),prob+"\n");
                        errList.add(translationcproblem);
                    }
                    return null; //terminate early since extraction parser can't work without balanced 'end's
                } else if(result instanceof TranslationResult){
                    TranslationResult transResult = (TranslationResult) result;
                    in = new BufferedReader(new StringReader(transResult.getText()));
                    prePosMap = transResult.getPositionMap();
                }
            }
        }catch(FileNotFoundException e){
            CompilationProblem FileNotFoundcerror = new CompilationProblem("File "+fName+" not found!\nAborting\n");
            errList.add(FileNotFoundcerror);
            return null;
        }
        catch(IOException e){
            CompilationProblem IOcerror = new CompilationProblem("Error translating "+fName+"\n"+e.getMessage());
            errList.add( IOcerror);
            return null;
        }
        
        return finishTranslateFile(fName, in, prePosMap, errList);
        
    }
    /**
     * Perform the translation of a given string containing source
     * code.
     *
     * @param fName    The name of the file to which the source
     * belongs.
     * @param source   The string containing the source code.
     * @param errList  A list of errors for error collection.
     * 
     * @return A reader object giving access to the translated
     * source.
     */
    public static Reader translateFile(String fName, String source, ArrayList errList)
    {
        BufferedReader in = null;
        PositionMap prePosMap = null;
        try{
            in = new BufferedReader( new StringReader( source ) );
            FunctionEndScanner prescanner = new FunctionEndScanner(in);
            FunctionEndScanner.Result result = prescanner.translate();
            in.close();
            
            if(result instanceof NoChangeResult){
                in = new BufferedReader( new StringReader( source ) );
            }else {
                in.close();
                if(result instanceof ProblemResult){
                    for(TranslationProblem prob : ((ProblemResult) result).getProblems()){
                        CompilationProblem translationcproblem = new CompilationProblem(prob.getLine(),prob.getColumn(),prob+"\n");
                        errList.add(translationcproblem);
                    }
                    return null; //terminate early since extraction parser can't work without balanced 'end's
                } else if(result instanceof TranslationResult){
                    TranslationResult transResult = (TranslationResult) result;
                    in = new BufferedReader(new StringReader(transResult.getText()));
                    prePosMap = transResult.getPositionMap();
                }
            }
        }catch(FileNotFoundException e){
            CompilationProblem FileNotFoundcerror = new CompilationProblem("File "+fName+" not found!\nAborting\n");
            errList.add(FileNotFoundcerror);
            return null;
        }
        catch(IOException e){
            CompilationProblem IOcerror = new CompilationProblem("Error translating "+fName+"\n"+e.getMessage());
            errList.add( IOcerror);
            return null;
        }
        
        return finishTranslateFile(fName, in, prePosMap, errList);
    }
    /**
     * Translate a given file and return a reader to access the
     * translated version. This method is used by the translateFile
     * methods and performs the final part of the translation.
     *
     * @param fName     The name of the file to which the source
     * belongs.
     * @param in        The source.
     * @param prPosMap  The position map to map from the original
     * translated file positions and original file positions.
     * @param errList   A list of errors for error collection.
     * 
     * @return A reader object giving access to the translated
     * source.
     */
    private static Reader finishTranslateFile(String fName, BufferedReader in, 
                                              PositionMap prePosMap, ArrayList errList)
    {
        try{
            OffsetTracker offsetTracker = new OffsetTracker(new TextPosition(1, 1));
            List<TranslationProblem> problems = new ArrayList<TranslationProblem>();
            String destText = MatlabParser.translate(new ANTLRReaderStream(in), 1, 1, offsetTracker, problems);
            
            if( problems.isEmpty() ){
                PositionMap posMap = offsetTracker.buildPositionMap();
                
                if( prePosMap != null ){
                    posMap = new CompositePositionMap(posMap, prePosMap);
                }
                //TODO-JD: do something with the posMap
                return new StringReader(destText);
            }
            else{
                for(TranslationProblem prob : problems){
                    CompilationProblem Translationcproblem = new CompilationProblem(prob.getLine(),prob.getColumn(),prob.getMessage()+"\n");
                    errList.add(Translationcproblem);
                    
                }
                return null;
            }
        }catch(FileNotFoundException e){
            CompilationProblem FileNotFoundcerror = new CompilationProblem("File "+fName+" not found!\nAborting\n");
            errList.add(FileNotFoundcerror);
            return null;
        }
        catch(IOException e){
            CompilationProblem IOcerror = new CompilationProblem("Error translating "+fName+"\n"+e.getMessage());
            errList.add( IOcerror);
            return null;
        }
        
    }
    
    /**
     * Parse a given file and return the Program ast node. This
     * expects the program to already be in natlab syntax.
     *
     * @param fName  The name of the file being parsed.
     * @param file   The reader object containing the source being
     * parsed.
     * @param errList   A list of errors for error collection.
     *
     * @return The Program node for the given file being parsed if no
     * errors. If an error occurs then null is returned. 
     */
    public static Program parseFile(String fName, Reader file, ArrayList errList )
    {
        NatlabParser parser = new NatlabParser();
        NatlabScanner scanner = null;
        CommentBuffer cb = new CommentBuffer();
        
        parser.setCommentBuffer(cb);
        
        try{
            scanner = new NatlabScanner( file );
            scanner.setCommentBuffer( cb );
            try{
                
                Program prog = (Program)parser.parse(scanner);
		
                if( parser.hasError() ){
                    String delim = "],[";
                    for( String error : parser.getErrors()){
                        //return an array of string with {line, column, msg}
                        CompilationProblem parserError;
                        try{
                            String[] message = error.split(delim);
                            parserError = new CompilationProblem( Integer.valueOf(message[0]).intValue(),
                                                                  Integer.valueOf(message[1]).intValue(),
                                                                  message[3]);
                        }
                        catch( PatternSyntaxException e ){
                            parserError = new CompilationProblem( error );
                        }
                        errList.add(parserError);}
                    prog = null;
                }
                return prog;
                
            }catch(Parser.Exception e){
                String ErrorString= e.getMessage()+"\n";
                for(String error : parser.getErrors()) {
                    ErrorString+= error + "\n";
                }
                CompilationProblem Parsercerror = new CompilationProblem(ErrorString);
                errList.add(Parsercerror);
                return null;
            } 
        }catch(FileNotFoundException e){
            CompilationProblem FileNotFoundcerror = new CompilationProblem("File "+fName+" not found!\nAborting\n");
            errList.add(FileNotFoundcerror);
            return null;
        }
        catch(IOException e){
            CompilationProblem IOcerror = new CompilationProblem("Error translating "+fName+"\n"+e.getMessage());
            errList.add( IOcerror);
            return null;
        }
    }
    
    private static boolean processCmdLine(String[] args)
    {
        try{
            options.parse( args );
            
            if( args.length == 0 ){
                System.err.println("No options given\n" +
                                   "Try -help for usage");
                return false;
            }
            return true;
        }catch( NullPointerException e ){
            System.err.println("options variable not initialized");
            throw e;
        }
    }
    
}