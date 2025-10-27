package Test;

import java.util.ArrayList;
import source.EvaluationFunction;
import Swarms.Particle;
import robust.EscenarioController;
import source.Volumen;

public class EvaluationAlg {

    private EvaluationFunction evaluation;
    private double particle_value;

    public EvaluationAlg(ArrayList<Volumen> volumes, Particle p, ArrayList<Double> w, ArrayList<Double> Zmin,
            ArrayList<Double> Zmax) {

        // Genero la instancia de la funcion de evaluación
        this.evaluation = new EvaluationFunction(volumes);
        int indexScenario = 0; // Escenario nominal

        // Obtiene valor de evaluacion y fluence map respectivamente
        ArrayList<Double> fluenceMap = p.getPlanFromScenario(indexScenario).getFluenceMap();
        
        // Evaluation de la particula
        double evaluation_value = this.evaluation.evalIntensityVector(fluenceMap,
        EscenarioController.getCollimatorFromIndexScenario(indexScenario));
        
        ArrayList<Double> scenarioValues = p.getFitnessScenarios();
        this.particle_value = scenarioValues.get(indexScenario);

        if (this.particle_value != evaluation_value) {
            System.out.println("Los valores de evaluation son distintos");
        } else {
            System.out.println("Los valores de evaluation son iguales");
        }
        System.out.println("Valor de evaluación: " + evaluation_value);
        System.out.println("Valor de partícula: " + this.particle_value);

        double fitness = p.getFitness();
        System.out.println("Fitness de la partícula: " + fitness);
        boolean match = false;
        for (Double x: scenarioValues) {
            double diff = x - fitness;
            match = match || (x == fitness || Math.abs(diff) < 0.0001);
        }

        if (match) {
            System.out.println("Fitness coincide con al menos un valor de escenario");
        } else {
            System.out.println("Fitness NO coincide con ningun valor de escenario");
        }
    }
}
