package es.uma.lcc.caesium.pedestrian.evacuation;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import es.uma.lcc.caesium.ea.base.EvolutionaryAlgorithm;
import es.uma.lcc.caesium.ea.base.Genotype;
import es.uma.lcc.caesium.ea.base.Individual;
import es.uma.lcc.caesium.ea.config.EAConfiguration;
import es.uma.lcc.caesium.pedestrian.evacuation.optimization.ExitEvacuationProblem;
import es.uma.lcc.caesium.pedestrian.evacuation.optimization.ea.CircularSetDiversity;
import es.uma.lcc.caesium.pedestrian.evacuation.optimization.ea.PerimetralExitOptimizationFunction;
import es.uma.lcc.caesium.pedestrian.evacuation.simulator.configuration.SimulationConfiguration;
import es.uma.lcc.caesium.pedestrian.evacuation.simulator.environment.Environment;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * Class for running the evacuation optimization algorithm
 * @author ccottap, ppgllrd
 * @version 1.0
 */
public class JsonToCsv {
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
		// set US locale
		Locale.setDefault(Locale.US);

		EAConfiguration conf;
		if (args.length < 3) {
			System.out.println ("Required parameters: <path-to-save-file> <path-to-read-file> <file-name-to-convert>");
			System.out.println ("\nNote that the environment configuration trainingDataFile will be sought as " + args[2] + ".csv,");
			System.out.println ("and the statistics will be dumped to " + args[0]);
			System.exit(1);
		}

		String fileName = args[2] + ".csv";
		FileReader reader = new FileReader(args[1] + args[2] + ".json");
		JsonArray deserialize = (JsonArray) Jsoner.deserialize(reader);
		System.out.println("JIJI " + deserialize.toJson());

		PrintWriter solsim = new PrintWriter(args[0] + fileName);
		JsonObject jsonObjectDeserialized = (JsonObject) deserialize.get(0);
		JsonArray rundata = (JsonArray) jsonObjectDeserialized.get("rundata");
		JsonObject firstRun = (JsonObject) rundata.get(0);
		JsonArray genomes = (JsonArray) firstRun.get("genome");
		int numExits = ((JsonArray) genomes.get(0)).size();
		int numRuns = deserialize.size();


		for (int i=0; i<numExits; i++)
			solsim.print("genome_" + i + ",");
		solsim.print("fitness,seed,run,time");
		solsim.println();

		for (int i=0; i<numRuns; i++) {
			JsonObject currentDeserializedRun = (JsonObject) deserialize.get(i);
			int currentSeed = Integer.parseInt(currentDeserializedRun.get("seed").toString());
			double currentTime = Double.parseDouble(currentDeserializedRun.get("time").toString());
			JsonArray currentRundata = (JsonArray) currentDeserializedRun.get("rundata");
			JsonObject currentRun = (JsonObject) currentRundata.get(0);
			JsonArray currentGenomes = (JsonArray) currentRun.get("genome");
			JsonArray currentFitnesses = (JsonArray) currentRun.get("fitness");

			for (int j=0; j<currentGenomes.size(); j++) {
				JsonArray currentGenome = (JsonArray) currentGenomes.get(j);
				for (int k=0; k<currentGenome.size(); k++) {
					double door = Double.parseDouble(currentGenome.get(k).toString());
					solsim.print(door + ",");
				}

				double currentFitness = Double.parseDouble(currentFitnesses.get(j).toString());
				solsim.print(currentFitness + ",");
				solsim.print(currentSeed + ","); //seed = run+1
				solsim.print(i + ","); //seed = run+1
				solsim.print(currentTime);
				solsim.println();
			}
		}
		solsim.close();
	}
}
