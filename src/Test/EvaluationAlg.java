package Test;

import java.util.ArrayList;
import source.EvaluationFunction;
import Swarms.Particle;
import source.Volumen;

public class EvaluationAlg {

    private EvaluationFunction evaluation;
    private double particle_value;

    public EvaluationAlg(ArrayList<Volumen> volumes, Particle p, ArrayList<Double> w, ArrayList<Double> Zmin, ArrayList<Double> Zmax) {
        
        // Genero la instancia de la funcion de evaluación
        this.evaluation = new EvaluationFunction(volumes);
        
        // Obtiene valor de evaluacion y fluence map respectivamente
        this.particle_value = p.getFitness();
        ArrayList<Double> fluenceMap = p.getCurrentPlan().getFluenceMap();

        // Evaluation de la particula
        double evaluation_value = this.evaluation.evalIntensityVector(fluenceMap, w, Zmin, Zmax);

        if(evaluation_value != this.particle_value){
            System.out.println("Los valores de evaluation son distintos");
        }

    }
}
