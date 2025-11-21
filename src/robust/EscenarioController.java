package robust;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import source.Volumen;
import source.Collimator;

public class EscenarioController {

    public static String INSTANCE_FILE = "./data/index_instances.txt";
    public static String DATA_FILE = "./data/<instanceID>/Instance.txt";
    private static String COORDINATE_INSTANCE_PATH = "./data/<instanceID>/<ScenarioName>/coordinates_instance.txt";

    private static ArrayList<Escenario> escenarios = new ArrayList<>();
    private static Vector<Integer> angles = new Vector<>();

    private static int nScenarios = 0;
    /**
     * Starts a scenario based on the index provided.
     * It reads the instance file, maps scenarios to their paths, and creates
     * Escenario objects for each unique scenario found.
     *
     * @param indexData The index of the instance to start.
     * @throws IOException If an error occurs while reading the instance file.
     */
    public static void startScenario(int indexData) throws IOException {
        // Clean previous scenarios
        EscenarioController.SetDefaultValues();

        // Gets the instance file path based on the index provided
        getInstanceById(indexData);

        // List the involved scenarios in the instance file
        Map<String, List<String>> uniqueScenarios = EscenarioController.mapScenariosToPaths(DATA_FILE);


        int id_scenario = 0;
        // Create a set to hold unique scenario names
        for (Map.Entry<String, List<String>> entry : uniqueScenarios.entrySet()) {
            String scenarioName = entry.getKey();
            List<String> filePaths = entry.getValue();

            // Create Scenario from filepaths org
            boolean isNominal = 0 == id_scenario;
            String scenarioCoordinatePath = COORDINATE_INSTANCE_PATH.replace("<ScenarioName>", scenarioName);
            Escenario escenario = new Escenario(scenarioName, filePaths, isNominal, scenarioCoordinatePath);

            // Add the scenario to the list of scenarios
            EscenarioController.escenarios.add(escenario);

            id_scenario++;
        }
        
        EscenarioController.nScenarios = id_scenario;
        
        // Activate beamlets with ideal values from the nominal scenario
        Collimator collimator = getCollimatorFromNominalScenario();
        for (Escenario escenario : escenarios) {
            escenario.activateBeamletsWithIdeal(collimator);
        }

    }


    /**
     * Evaluates the fluence map for each scenario and returns a list of evaluations.
     *
     * @param fluenceMap The fluence map to evaluate.
     * @param w          The weights for the evaluation.
     * @param zMin       The minimum z-values for the evaluation.
     * @param zMax       The maximum z-values for the evaluation.
     * @return A list of evaluations for each scenario.
     */
    public static ArrayList<Double> evaluateFluenceMap(ArrayList<Double> fluenceMap) {
        
        // Evaluate the fluence map on each scenario and store the results
        ArrayList<Double> evals = new ArrayList<>();

        for (Escenario escenario : EscenarioController.escenarios) {
            double evalScenario = escenario.evaluateFluenceMap(fluenceMap);
            evals.add(evalScenario);
        }
        return evals;
    }

    /**
     * Evaluates the fluence map over all scenarios and returns the average evaluation.
     *
     * @param fluenceMap The fluence map to evaluate.
     * @param w          The weights for the evaluation.
     * @param zMin       The minimum z-values for the evaluation.
     * @param zMax       The maximum z-values for the evaluation.
     * @return The average evaluation of the fluence map across all scenarios.
     */
    public static double evaluateOverFeatureEscenario(ArrayList<Double> fluenceMap, int indexScenario) {

        // Evaluate the fluence map over all scenarios and return the average
        double totalEval = EscenarioController.escenarios.get(indexScenario).evaluateFluenceMap(fluenceMap);
        return totalEval;
    }

    public static ArrayList<Double> evaluateOverAllEscenarios(ArrayList<Double> fluenceMap, int indexScenario) {

        // Evaluate the fluence map over all scenarios and return the average
        ArrayList<Double> totalEval = new ArrayList<>();
        for (Escenario escenario : EscenarioController.escenarios) {
            totalEval.add(escenario.evaluateFluenceMap(fluenceMap));
        }
        return totalEval;
    }

    /**
     * Returns the list of angles used in the scenarios.
     *
     * @return A vector containing the angles used in the scenarios.
     */
    public static void getInstanceById(Integer index) {

        File of = new File(INSTANCE_FILE);
        Scanner reading = null;
        try {
            reading = new Scanner(of);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int nLine = 1;
        while (reading.hasNextLine()) {
            String linea = reading.nextLine();
            if (nLine == index) {
                EscenarioController.DATA_FILE = EscenarioController.DATA_FILE.replace("<instanceID>", linea);
                EscenarioController.COORDINATE_INSTANCE_PATH = EscenarioController.COORDINATE_INSTANCE_PATH.replace("<instanceID>", linea);
                break;
            }
            nLine++;
        }
        reading.close();
    }

    /**
     * Lee un archivo de texto con rutas de escenarios y las mapea.
     * La clave del mapa será el nombre del escenario (ej. "Scenario_1"),
     * y el valor será una lista de todas las rutas de archivo asociadas a ese
     * escenario.
     * La primera línea del archivo se ignorará.
     *
     * @param filePath La ruta del archivo a leer.
     * @return Un mapa donde la clave es el nombre del escenario (String) y el valor
     *         es una List de Strings con las rutas de archivo de ese escenario.
     * @throws IOException Si ocurre un error al leer el archivo.
     */
    public static Map<String, List<String>> mapScenariosToPaths(String filePath) throws IOException {
        Map<String, List<String>> scenarioMap = new LinkedHashMap<>();
        Pattern scenarioPattern = Pattern.compile("(Scenario_\\d+)");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLineSkipped = false;

            while ((line = reader.readLine()) != null) {
                if (!firstLineSkipped) {
                    // Split line with angles
                    String[] angles_list_str = line.split(" ");

                    for (String angle_str : angles_list_str) {
                        Integer angle = Integer.parseInt(angle_str);
                        angles.add(angle);
                    }
                    firstLineSkipped = true;
                    continue;
                }

                Matcher matcher = scenarioPattern.matcher(line);

                if (matcher.find()) {
                    String scenarioName = matcher.group(1);

                    scenarioMap.computeIfAbsent(scenarioName, k -> new ArrayList<>()).add(line);
                }
            }
            reader.close();
        }
        return scenarioMap;
    }

    public static Vector<Integer> getAngles() {
        return angles;
    }

    public static ArrayList<Volumen> getDDMFromNominalScenario() {
        Escenario ddmScenarios = null;
        for (Escenario escenario : escenarios) {
            if (escenario.isNominal()) {
                ddmScenarios = escenario;
            }
        }
        return ddmScenarios != null ? ddmScenarios.getVolumes() : new ArrayList<>();
    }

    public static Collimator getCollimatorFromNominalScenario() {
        Collimator collimator = null;
        for (Escenario escenario : escenarios) {
            if (escenario.isNominal()) {
                collimator = escenario.getCollimators();
            }
        }
        return collimator;
    }

    public static Collimator getCollimatorFromIndexScenario(int index) {
        return escenarios.get(index).getCollimators();
    }

    public static int getnScenarios() {
        return nScenarios;
    }

    public static Escenario getScenarioByIndex(int index) {
        return escenarios.get(index);
    }

    private static void SetDefaultValues(){
        INSTANCE_FILE = "./data/index_instances.txt";
        DATA_FILE = "./data/<instanceID>/Instance.txt";
        COORDINATE_INSTANCE_PATH = "./data/<instanceID>/<ScenarioName>/coordinates_instance.txt";
        escenarios.clear();
        angles.clear();
        nScenarios = 0;
    }

}


