package es.uma.lcc.caesium.pedestrian.evacuation.optimization.ea;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import es.uma.lcc.caesium.ea.base.EvolutionaryAlgorithm;
import es.uma.lcc.caesium.ea.config.EAConfiguration;
import es.uma.lcc.caesium.pedestrian.evacuation.optimization.ExitEvacuationProblem;
import es.uma.lcc.caesium.pedestrian.evacuation.simulator.configuration.SimulationConfiguration;
import es.uma.lcc.caesium.pedestrian.evacuation.simulator.environment.Environment;

/**
 * Class for running the evacuation optimization algorithm
 * @author ccottap, ppgllrd
 * @version 1.0
 */
public class RunEvacuationOptimization {
	/**
	 * environment filename prefix
	 */
//	private static final String ENVIRONMENT_FILENAME = "base-";
	private static final String ENVIRONMENT_FILENAME = "afterTFG/enviroments/base-";
	/**
	 * stats filename prefix
	 */
//	private static final String STATS_FILENAME = "ea-model(18447)-stats-";
	private static final String STATS_FILENAME = "ea-stats-";
	private static final String STATS_FILENAME_DOORS = "_1Door";

	/**
	 * Main method
	 * @param args command-line arguments
	 * @throws JsonException if the configuration file is not correctly formatted
	 * @throws IOException if files cannot be read/written
	 */
	public static void main(String[] args) throws JsonException, IOException {
		int numExits = Integer.parseInt(args[2]);

		// set US locale
		Locale.setDefault(Locale.US);

		EAConfiguration conf;
		if (args.length < 4) {
			System.out.println ("Required parameters: <ea-configuration-trainingDataFile> <environment-name> <num-exits> <simulation-configuration>");
			System.out.println ("\nNote that the environment configuration trainingDataFile will be sought as " + ENVIRONMENT_FILENAME + "<environment-name>.json,");
//			System.out.println ("and the statistics will be dumped to a trainingDataFile named " + STATS_FILENAME + "<environment-name>.json");
			if(numExits == 1)
				System.out.println ("and the statistics will be dumped to a trainingDataFile named " + STATS_FILENAME + "<environment-name>_" + numExits + "Door.json");
			else
				System.out.println ("and the statistics will be dumped to a trainingDataFile named " + STATS_FILENAME + "<environment-name>_" + numExits + "Doors.json");
			System.exit(1);
		}
		
		// Configure the EA
		FileReader reader = new FileReader(args[0]);
		conf = new EAConfiguration((JsonObject) Jsoner.deserialize(reader));
		int numruns = conf.getNumRuns();
		long firstSeed = conf.getSeed();
		System.out.println(conf);
		EvolutionaryAlgorithm myEA = new EvolutionaryAlgorithm(conf);
		myEA.setVerbosityLevel(1);
		
		// Configure the problem
//		String fileName = STATS_FILENAME + args[1] + STATS_FILENAME_DOORS;
		String fileName = STATS_FILENAME + args[1] + "_" + args[2] + "Door";
		if(numExits != 1)
			fileName = STATS_FILENAME + args[1] + "_" + args[2] + "Doors";

	    Environment environment = Environment.fromFile(ENVIRONMENT_FILENAME + args[1] + ".json");
		SimulationConfiguration simulationConf = SimulationConfiguration.fromFile(args[3]);
//	    int numExits = Integer.parseInt(args[2]);
	    ExitEvacuationProblem eep = new ExitEvacuationProblem (environment, numExits, simulationConf);
		myEA.setObjectiveFunction(new PerimetralExitOptimizationFunction(eep));	// OF original de Pepe (es en caso de que eep ya tenga el entorno)
//		myEA.setObjectiveFunction(new PerimetralExitOptimizationFunction(eep, "model-" + fileName, args[4]));	// En caso de que el entorno no sea necesario, en la configuración carga el nombre del modelo
		myEA.getStatistics().setDiversityMeasure(new CircularSetDiversity(1.0));
		System.out.println(eep);
		
		for (int i=0; i<numruns; i++) {
			long seed = firstSeed + i;
			myEA.run(seed);
			System.out.println ("Run " + i + ": " + 
								String.format("%.2f", myEA.getStatistics().getTime(i)) + "s\t" +
								myEA.getStatistics().getBest(i).getFitness());
		}
//		PrintWriter trainingDataFile = new PrintWriter("afterTFG/trainingData/" + fileName + ".json");
		PrintWriter trainingDataFile = new PrintWriter("afterTFG/trainingData/separatedByRun/" + fileName + ".json");
		PrintWriter eaDataFile = new PrintWriter("afterTFG/eaData/" + fileName + ".json");
//		PrintWriter modelData = new PrintWriter("afterTFG/modelData/model-" + fileName + ".json");
//		trainingDataFile.print(myEA.getStatistics().toJSON().toJson());		// El JSON por defecto de Pepe Gallardo
		JsonArray jsonArray = myEA.getStatistics().toJSON();
		eaDataFile.print(jsonArray.toJson());	//El JSON para el entrenamiento del modelo subrogado
//		JsonObject jsonObject = myEA.getStatistics().toJSONObject();
		jsonArray = myEA.getStatistics().toJSONObjectSeparatedByRuns(); //TODO: El otro proyecto necesita compilar, además de que es necesario comprobar si funciona
		trainingDataFile.print(jsonArray.toJson());	//El JSON para el entrenamiento del modelo subrogado

		/** Para saber cuantos genomas hay en toda la ejecución. Solo funciona poara el fichero con los listados genome y fitness directamente
		int numGenomes = 0;
		for (int i=0; i<numruns; i++) {
			JsonObject currentRun = (JsonObject) jsonArray.get(i);
			JsonObject rundata = (JsonObject) ((JsonArray) currentRun.get("rundata")).get(0);
			numGenomes += ((JsonArray) rundata.get("genome")).size();
		}
		 System.out.println("Tamaño array genomas: " + numGenomes);
		 */

		System.out.println("EA terminado");

		trainingDataFile.close();
		eaDataFile.close();
	}
}
