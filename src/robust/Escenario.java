package robust;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.List;

import source.EvaluationFunction;
import source.Volumen;

public class Escenario {

    private boolean isNominal = false; // Indicates if the scenario is nominal

    private ArrayList<Volumen> volumes;
    private String scenarioName;
    private EvaluationFunction evaluationFunction;


    public Escenario(String scenarioName, List<String> orgFiles, boolean isNominal){
        // Initialize the scenario with a name and a list of original files
        this.scenarioName = scenarioName;
        this.isNominal = isNominal;

        // Initialize the volumes list and populate it with Volumen objects created from
        // the original files
        this.volumes = new ArrayList<>();
        try {
            for (String orgFile : orgFiles) {
                Volumen to_add = new Volumen(orgFile);
                this.volumes.add(to_add);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Initialize the evaluation function with the created volumes
        this.evaluationFunction = new EvaluationFunction(this.volumes);
    }

    public double evaluateFluenceMap(ArrayList<Double> fluenceMap, ArrayList<Double> w, ArrayList<Double> zMin, ArrayList<Double> zMax) {
        this.evaluationFunction = new EvaluationFunction(this.volumes);

        // Evaluate the fluence map on the scenario using the evaluation function
        double evalScenario = this.evaluationFunction.evalIntensityVector(fluenceMap, w, zMin, zMax);
        return evalScenario;
    }



    public static Vector<Integer> get_angles(String nameFile) throws FileNotFoundException {
        File testInstance = new File(nameFile);
        Vector<Integer> angles = new Vector<>();
        Scanner reading = new Scanner(testInstance);
        int nLine = 0;
        while (reading.hasNextLine()) {
            if (nLine == 0) {
                String linea = reading.nextLine();
                String[] angles_list_str = linea.split(" ");
                for (String angle_str : angles_list_str) {
                    Integer angle = Integer.parseInt(angle_str);
                    angles.add(angle);
                }
            } else {
                break;
            }
            nLine++;
        }
        reading.close();
        return angles;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public ArrayList<Volumen> getVolumes() {
        return volumes;
    }

    public boolean isNominal() {
        return isNominal;
    }
}
