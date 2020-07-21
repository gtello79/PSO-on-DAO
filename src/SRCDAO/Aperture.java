package SRCDAO;

import javafx.util.Pair;
import source.Collimator;
import source.Matrix;

import java.util.Vector;

public class Aperture {
    private Collimator collimator;
    private int angle;
    private int n_aperture;
    private int intensity;
    private int veloc_intensity;
    private Vector<Pair<Integer,Integer>> A;
    private Vector<Pair<Integer,Integer>> velo_A;

    public Aperture(Vector<Pair<Integer,Integer>> A, int min_intensity, Collimator collimator, int angle){
        this.collimator = collimator;
        this.angle = angle;
        this.intensity = min_intensity;
        this.A = A;
        veloc_intensity = 0;
        velo_A = new Vector<>();
        for(int i = 0; i < collimator.getyDim(); i++)
            velo_A.add(new Pair(0,0));
    }

    public void velAperture(double w, double c1, double c2, Vector<Pair<Integer,Integer>> BG, Vector<Pair<Integer,Integer>> BP){
        int counter = 0;
        double r1 = c1;
        double r2 = c2;
        /*
        * Inicializar r1 y r2
        * */
        for (Pair<Integer,Integer> this_move : velo_A){
            Pair<Integer,Integer> aux_G = BG.get(counter);
            Pair<Integer,Integer> aux_P = BP.get(counter);

            int first = (int) (w*this_move.getKey()-c1*r1*(this_move.getKey()-aux_G.getKey())-c2*r2*(this_move.getKey() - aux_P.getKey()));
            int second = (int) (w*this_move.getValue()-c1*r1*(this_move.getValue()-aux_G.getValue())-c2*r2*(this_move.getValue() - aux_P.getValue()));

            velo_A.set(counter, new Pair(first,second));
            counter++;
        }
    }

    public void movAperture(){
        for(int i = 0; i < velo_A.size(); i++){
            int limit_inf = collimator.getActiveRange(i,angle).getValue();
            int limit_sup =collimator.getActiveRange(i,angle).getKey();
            int first = velo_A.get(i).getKey() + A.get(i).getKey();
            int second = velo_A.get(i).getValue() + A.get(i).getValue();
            if(first > limit_sup) first = limit_sup;
            if(first < limit_inf) first = limit_inf;

            if(second > limit_sup) second = limit_sup;
            if(second < limit_inf) second = limit_inf;

            if(first >= second) {
                double val = (first + second) / 2;
                first = (int) (val);
                second = (int) (val) + 1;
            }
            A.set(i, new Pair(first,second));
        }
    }

    public Matrix buildedShape(){
        Matrix aux = new Matrix(collimator.getxDim(),collimator.getyDim());
        Pair<Integer,Integer> aux1;
        for(int i = 0; i < collimator.getxDim(); i++){
            aux1 = collimator.getActiveRange(i,angle);
            if(aux1.getKey() <0 )continue;
            for(int j = aux1.getKey(); j <= aux1.getValue(); j++) aux.setPos(i,j,0);
        }
        return aux;
    }

    public Pair<Integer,Integer> getOpBeam(int i){
        return A.get(i);
    };

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getN_aperture() {
        return n_aperture;
    }

    public void setN_aperture(int n_aperture) {
        this.n_aperture = n_aperture;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public int getVeloc_intensity() {
        return veloc_intensity;
    }

    public void setVeloc_intensity(int veloc_intensity) {
        this.veloc_intensity = veloc_intensity;
    }
}
