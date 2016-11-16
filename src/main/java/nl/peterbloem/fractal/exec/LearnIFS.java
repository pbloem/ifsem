package nl.peterbloem.fractal.exec;

import static nl.peterbloem.kit.Series.series;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import nl.peterbloem.fractal.EM;
import nl.peterbloem.fractal.IFS;
import nl.peterbloem.fractal.IFSs;
import nl.peterbloem.fractal.util.Draw;
import nl.peterbloem.kit.FileIO;
import nl.peterbloem.kit.Global;
import nl.peterbloem.kit.Series;
import nl.peterbloem.kit.data.Map;
import nl.peterbloem.kit.data.Point;
import nl.peterbloem.kit.data.Similitude;
import nl.peterbloem.kit.search.Parametrizable;

public class LearnIFS 
{
	public List<Point> data;
	
	public boolean highQuality;

	public int numComponents;
	
	public int depth;

	public int sampleSize;
	
	public int iterations;

	public int repeats;

	public List<Double> likelihoods = new ArrayList<Double>();

	public double bestLikelihood = Double.POSITIVE_INFINITY;
	public List<IFS<Similitude>> bestHistory = null;
	public List<List<Double>> bestDepths = null;
	public List<Similitude> bestPosts = null;
	
	public void run()
		throws IOException
	{
		Global.randomSeed();
		
		BufferedImage image = Draw.draw(data, 400, true);
		ImageIO.write(image, "PNG", new File("data.png"));

		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for(int rep : series(repeats))
			exec.execute(new Repeat(rep));

		exec.shutdown();
		
		while(! exec.isTerminated());
		
		System.out.println("All threads finished");
		
		File dir = new File("./best/");
		dir.mkdirs();
		
		Writer out = new BufferedWriter(new FileWriter(new File("likelihoods.csv")));
		for(double ll  : likelihoods)
			out.write(ll + "\n");
		out.close();
		
		for(int i : series(iterations))
			write(bestHistory.get(i), bestDepths.get(i), bestPosts.get(i), String.format("iteration.%06d", i));
		
		try
		{
			FileIO.python(new File("."), "fractals/histogram.py");
		} catch (Exception e) {
			System.out.println("Failed to run plot script. " + e);
		}
	}
	
	public synchronized void done(double likelihood, List<IFS<Similitude>> history, List<List<Double>> depths, List<Similitude> posts)
	{
		likelihoods.add(likelihood);
		
		if(likelihood < bestLikelihood)
		{
			bestLikelihood = likelihood;
			bestHistory = history;
			bestDepths = depths;
			bestPosts = posts;
		}
	}

	public <M extends Map & Parametrizable> void write(IFS<Similitude> ifs, List<Double> depths, Similitude post, String name) throws IOException
	{
		int div = highQuality ? 1 : 4;
		int its = highQuality ? (int) 1000000 : 10000;
				
		BufferedImage image;
		
		image = Draw.draw(ifs, its, 1000/div, true, depths, post);
		ImageIO.write(image, "PNG", new File(name+".png"));
		
		image = Draw.draw(ifs.generator(), its, 1000/div, true, post);
		ImageIO.write(image, "PNG", new File(name+".deep.png"));
	}
	
	private class Repeat extends Thread
	{		
		private int rep;
		
		public Repeat(int rep)
		{
			this.rep = rep;
		}

		@Override
		public void run()
		{
			List<IFS<Similitude>> history = new ArrayList<IFS<Similitude>>(iterations);
			List<List<Double>> dHistory = new ArrayList<List<Double>>(iterations);			
			List<Similitude> pHistory = new ArrayList<Similitude>(iterations);			
			
			IFS<Similitude> initial = IFSs.initialSphere(data.get(0).dimensionality(), numComponents, 1.0, 0.5, true);

			if(rep == 0)
				initial = IFSs.sierpinskiSim();

			
			EM em = new EM(data, sampleSize, initial, depth, true);
			
			history.add(em.model());
			dHistory.add(em.depths());
						
			for(int i : series(iterations))
			{
				
				try
				{ 
					write(em.model(), em.depths(), em.post(), String.format("rep.%04d.%04d", rep, i));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
				em.iterate();
				
				history.add(em.model());
				dHistory.add(em.depths());
				pHistory.add(em.post());
				

			}
			
			double likelihood = - em.logLikelihood(data);
			
			System.out.println(rep + " " + likelihood);
			
			done(likelihood, history, dHistory, pHistory);	
			
			System.out.println("Thread " + rep + " finished");
		}
	}
}
