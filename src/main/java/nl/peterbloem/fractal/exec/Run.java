package nl.peterbloem.fractal.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import nl.peterbloem.fractal.IFSs;
import nl.peterbloem.kit.Functions;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.data.Point;

public class Run 
{
	
	@Option(
		name="--type",
		usage="Selects the type of experiment, one of: ifs (Learn an IFS model), mog (Compare against mixture-of-Gaussians), time (measure time complexity, ignores all other settings).")
	private static String type = "ifs";
	
	@Option(name="--ifs.data", usage="source of the data")
	private static String dataSource;
	
	@Option(name="--ifs.file", usage="A CSV file containing double values (each row is interpreted as a vector).")
	private static File file = null;
	
	@Option(name="--ifs.data-size", usage="The number of samples taken from the target model.")
	private static int dataSize = 10000;
	
	@Option(
			name="--components",
			usage="How many components to use.")
	private static int components = 3;
	
	@Option(
			name="--iterations",
			usage="For how many iterations to run the algorithm.")
	private static int iterations = 100;
	
	@Option(
			name="--depth",
			usage="The depth which to evaluate the model (careful, complexity grows exponentially).")
	private static int depth = 6;
	
	@Option(
			name="--samples",
			usage="Size of the sample used for training each iteration.")
	private static int samples = 500;
	
	@Option(
			name="--repeats",
			usage="How many parallel runs to do.")
	private static int repeats = 10;

	@Option(
			name="--ifs.hq",
			usage="Whether to create high quality images (slower, but prettier).")
	private static boolean hq;
		
	@Option(
			name="--help",
			usage="Print usage information.")
	private static boolean help;
	
	/**
	 * Main executable function
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException 
	{	
		Run run = new Run();
		
		// * Parse the command-line arguments
    	CmdLineParser parser = new CmdLineParser(run);
    	try
		{
			parser.parseArgument(args);
		} catch (CmdLineException e)
		{
	    	System.err.println(e.getMessage());
	        System.err.println("java -jar ifsem.jar [options...]");
	        parser.printUsage(System.err);
	        
	        System.exit(1);	
	    }
    	
    	if(help)
    	{
	        parser.printUsage(System.out);
	        
	        System.exit(0);	
    	}
    	Global.log().info("Using " + Global.numThreads() + " concurrent threads");
		Functions.tic();

    	if ("ifs".equals(type.toLowerCase())) 
    	{
    		
    		LearnIFS experiment = new LearnIFS();
    		List<Point> data = null;
    		
    		if("sierpinski".equals(dataSource.toLowerCase().trim()))
    		{
    			data = IFSs.sierpinskiSim().generator(50).generate(dataSize);
    		} else {
    			Global.log().severe("Data source ("+dataSource+") not recognized.");
    	        parser.printUsage(System.err);
    	        System.exit(-1);
    		}
    		
    		
    		
    		experiment.data = data;
    		experiment.numComponents = components;
    		experiment.iterations = iterations;
    		experiment.depth = depth;
    		experiment.sampleSize = samples;
    		experiment.repeats = repeats;
    		experiment.highQuality = hq;
    		
    		experiment.run();
    		    			
    	} else if ("mog".equals(type.toLowerCase())) {
    		
    			
    	} else if ("time".equals(type.toLowerCase())) {
    		
			
    	} else 
    	{
    		Global.log().severe("Experiment type " + type + " not recognized. Exiting.");
	        System.err.println("java -jar ifsem.jar [options...]");
	        parser.printUsage(System.err);
    		System.exit(-1);
    	}
    	
		Global.log().info("Experiment finished. Time taken: " + Functions.toc() + " seconds.");

	}

}
