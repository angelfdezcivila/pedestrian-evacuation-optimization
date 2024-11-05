package es.uma.lcc.caesium.pedestrian.evacuation.optimization.ea;

import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import java.io.File;
import java.util.*;

import es.uma.lcc.caesium.ea.base.Genotype;
import es.uma.lcc.caesium.ea.base.Individual;
import es.uma.lcc.caesium.ea.fitness.ContinuousObjectiveFunction;
import es.uma.lcc.caesium.ea.fitness.OptimizationSense;
import es.uma.lcc.caesium.pedestrian.evacuation.optimization.Double2AccessDecoder;
import es.uma.lcc.caesium.pedestrian.evacuation.optimization.ExitEvacuationProblem;
import es.uma.lcc.caesium.pedestrian.evacuation.simulator.environment.Access;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


/**
 * Objective function of the EA in order to find the location of a certain fixed
 * number of exits in the perimeter of a given environment, so that the evacuation 
 * performance is optimized.
 * @author ccottap, ppgllrd
 * @version 1.2
 *
 */
public class PerimetralExitOptimizationFunction extends ContinuousObjectiveFunction {
	/**
	 * number of exits
	 */
	private final int numExits;
	/**
	 * length of the perimeter
	 */
	private final double perimeterLength;
	/**
	 * the instance of the evacuation problem
	 */
	private final ExitEvacuationProblem eep;
	/**
	 * decoder of accesses
	 */
	private Double2AccessDecoder decoder;
	/**
	 * cache of fitness evaluations
	 */
	private HashMap<TreeSet<Double>, Double> cache;
	/**
	 * granularity in the location of exits
	 */
	private static final double EXIT_PRECISION = 0.1;
	/**
	 * used to round off location values
	 */
	private static final double FACTOR = 1.0 / EXIT_PRECISION;
	
		
	/**
	 * Basic constructor
	 * @param eep the evacuation problem
	 */
	public PerimetralExitOptimizationFunction(ExitEvacuationProblem eep) {
		super(eep.getNumExits(), 0.0, 1.0);
		numExits = eep.getNumExits();
		perimeterLength = eep.getPerimeterLength();
		this.eep = eep;
		decoder = new Double2AccessDecoder(eep);
		cache = null;
	}
	
	@Override
	public void newRun() {
		super.newRun();
		cache = new HashMap<TreeSet<Double>, Double>();
	}
	
	
	/**
	 * Returns the exit evacuation problem being solved
	 * @return the exit evacuation problem being solved
	 */
	public ExitEvacuationProblem getExitEvacuationProblem() {
		return eep;
	}
	
	
	/**
	 * Indicates whether the goal is maximization or minimization
	 * @return the optimization sense
	 */
	public OptimizationSense getOptimizationSense()
	{
		return OptimizationSense.MINIMIZATION;
	}


	@Override
	protected double _evaluate(Individual ind) {
		TreeSet<Double> genes = individualToTreeSet (ind);
		Double val = cache.get(genes);
		if (val == null) {
//			System.out.println("Puertas representadas con un float en el rango [0,1]:");
//			for (int i = 0; i < ind.getGenome().length(); i++) {
//				System.out.println("Gene: " + ind.getGenome().getGene(i));
//			}

//			val = eep.fitness (eep.simulate (decode (ind)));
            try {
                val = FitnessFromPrediction(ind.getGenome());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            cache.put(genes, val);
		}
		
		return val;
	}

	// Load the Keras model and tries to predict fitness
	private double FitnessFromPrediction(Genotype genome) throws Exception {

		//Se necesita la version 1 o 2 de keras
//		String simpleMlp = new ClassPathResource("../../../neural_network_750V1(198582).h5").getFile().getPath(); // no funciona
//		String simpleMlp = new File("neural_network_750(18447).h5").getAbsolutePath();
//		String simpleMlp = new File("neural_network_750V1(198582).h5").getAbsolutePath();
		String simpleMlp = new File("neural_network_750V2(198582).h5").getAbsolutePath();
		MultiLayerNetwork model = KerasModelImport.
				importKerasSequentialModelAndWeights(simpleMlp, false);

//		INDArray features = Nd4j.zeros(1, genome.length()); // Para que sea una matriz de 1xlongitud_genotipo, contiene un único array de la longitud del genotipo
		double[][] genes = new double[1][genome.length()];
		List<Double> sortedGenome = SortedGenotype(genome);
		for (int i = 0; i < genome.length(); i++) {
//			System.out.println("Gene: " + (double)genome.getGene(i));
//			features.putScalar(new int[]{i}, (double)genome.getGene(i)); // el transpaso hace que los decimales varien, y no se por qué
//			System.out.println("Gene readed: " + features.getDouble(i));

//			genes[0][i] = (double)genome.getGene(i);
			genes[0][i] = sortedGenome.get(i);
		}
		INDArray features = Nd4j.create(genes);
		// Para testear que la conversión a INDArray funciona correctamente
//		for (int i = 0; i < genome.length(); i++) {
//			System.out.println("Gene readed: " + features.getDouble(i));
//		}

		double prediction = model.output(features).getDouble(0);
//		System.out.println("Puertas: " + features.toString());
//		System.out.println("Fitness: " + prediction);

		return prediction;

//		String simpleMlp = new File("neural_network").getAbsolutePath();
//		System.out.println(simpleMlp);
//		try(SavedModelBundle model = SavedModelBundle.load(simpleMlp, "serve")){
//			Tensor tensor = model.session().runner().fetch("xy").feed("x", Tensor.create(5.0f)).feed("y", Tensor.create(2.0f)).run().get(0);
//			System.out.println("valor Tensor: " + tensor.floatValue());
//			return tensor.floatValue();
//		}

//		return 0;
	}

	private List<Double> SortedGenotype(Genotype genome) {
		List<Double> sortedGenome = new ArrayList<>(genome.length());
		for (int i = 0; i < genome.length(); i++) {
			sortedGenome.add((double)genome.getGene(i));
		}
		sortedGenome.sort(Double::compareTo);
		return sortedGenome;
	}

	/**
	 * Transforms an individual's genome into a tree set (because genome ordering is irrelevant
	 * when it comes to compare solutions).
	 * @param ind an individual
	 * @return a tree set with the individual's genes
	 */
	private TreeSet<Double> individualToTreeSet (Individual ind) {
		TreeSet<Double> genes = new TreeSet<Double>();
		Genotype g = ind.getGenome();
		for (int exit=0; exit<numExits; exit++) {
			genes.add(roundLocation(g, exit));
		}
		return genes;
	}
	


	/**
	 * Decodes an individual, transforming each gene into the corresponding access(es).
	 * @param ind an individual
	 * @return the list f accesses encoded in the individual's genome.
	 */
	public List<Access> decode (Individual ind) {
		List<Access> exits = new ArrayList<>(numExits);
		Genotype g = ind.getGenome();
		var id = 0;
		for (int exit=0; exit<numExits; exit++) {
			// locations have a precision of 1cm
			double location = roundLocation(g, exit);
			exits.addAll(decoder.decodeAccess(location, exit, id));
			id = exits.size();
		}
		return exits;
	}
	
	/**
	 * Rounds off the value of a certain exit to the desired precision
	 * @param g the genotype
	 * @param index index of the exit
	 * @return the location along the perimeter (rounded off).
	 */
	private double roundLocation (Genotype g, int index) {
		return Math.round(((double)g.getGene(index)) * perimeterLength * FACTOR)/FACTOR;
	}
	
}
