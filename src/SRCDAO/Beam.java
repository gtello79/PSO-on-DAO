package SRCDAO;
import java.util.*;

import javafx.util.Pair;
import source.*;

public class Beam {
    private Collimator collimator;
    private Matrix I;
    private Matrix lastIter;
    private int angle;
    private int max_apertures;
    private int max_intensity;
    private int min_intensity;
    private int initial_intensity;
    private int step_intensity;
    private int open_apertures;
    private int setup;
    private int n_volumes;
    private boolean apertureChange;
    private int move;

    protected int OPEN_MAX_SETUP = 0;
    protected int OPEN_MIN_SETUP = 1;
    protected int CLOSED_MIN_SETUP = 2 ;
    protected int CLOSED_MAX_SETUP = 3;
    protected int RAND_RAND_SETUP = 6;

    /* Apertures (representation 1):
     * Each aperture is represented by a vector of pairs A[i] = (x_ini, x_fin)
     * and an intensity
    Range open (x_ini, x_fin) of row "r" for aperture d: A[d][r](x_ini, x_fin)
    */

    private Vector<Aperture> A;
    private HashMap<Pair<Integer,Integer>, Integer> pos2beam;
    private HashMap<Integer, Pair<Integer,Integer>> beam2pos;

    private SortedMap<Integer, Integer> int2nb;
    private SortedMap<Integer, Matrix>D;

    public Beam(int angle, int max_apertures, int max_intensity, int initial_intensity, int step_intensity, int open_apertures, int setup, Vector<Volumen> volumes, Collimator collimator){
        this.angle = angle;
        this.max_apertures = max_apertures;
        this.max_intensity = max_intensity;
        this.initial_intensity = initial_intensity;
        this.step_intensity = step_intensity;
        this.open_apertures = open_apertures;
        this.setup = setup;
        this.collimator = collimator;
        A = new Vector<>();
        pos2beam = new HashMap<>();
        beam2pos = new HashMap<>();
        D = new TreeMap<>();

        min_intensity = 1;
        n_volumes = volumes.size();

        if(open_apertures==-1)
            open_apertures = max_apertures;

        for(int i = 0; i < n_volumes; i++){
            D.put(i,volumes.get(i).getDepositionMatrix(angle));
        }

        //Initialize empty matrix of intensity
        I = new Matrix(collimator.getxDim(), collimator.getyDim());
        lastIter = I;

        // Iniatialize apertures (PRINCIPAL representation)
        initiliazeBeam(setup,open_apertures);

        // Iniatialize intensity Matrix
        for(int i = 0; i < collimator.getxDim(); i++){
            for(int j = 0; j < collimator.getyDim(); j++){
                if(!collimator.isActiveBeamAngle(i,j,angle)) {
                    I.setPos(i,j,-1);
                }
            }
        }
    };

    public void initiliazeBeam(int type, int open_apertures){
        int intensity = 0;
        //Creating the apertures of the shapes and intensities
        if(type == OPEN_MAX_SETUP || type == OPEN_MIN_SETUP){
            for(int i = 0; i < max_apertures; i++){
                Vector<Pair<Integer,Integer>> aux = new Vector<>();
                for(int j = 0; j < collimator.getxDim(); j++)
                    aux.add(collimator.getActiveRange(j,angle));
                intensity = min_intensity;
                Aperture toAdd = new Aperture(aux,intensity,collimator,angle);
                A.add(toAdd);
            }
        } else if(type == CLOSED_MAX_SETUP || type == CLOSED_MIN_SETUP){
            for(int i = 0; i < max_apertures; i++){
                Vector<Pair<Integer,Integer>> aux = new Vector<>();
                for(int j = 0; j < collimator.getxDim(); i++){
                    aux.add(new Pair(-1,-1));
                }
                intensity = max_intensity;
                Aperture toAdd = new Aperture(aux,intensity,collimator,angle);
                A.add(toAdd);
            }
        } else if(type == RAND_RAND_SETUP){
            for(int i = 0; i < max_apertures; i++){
                //Making the random shapes
                Vector<Pair<Integer,Integer>> aux = new Vector<>();
                for(int j = 0; j < collimator.getxDim(); j++){
                    Pair<Integer,Integer> auxRange = collimator.getActiveRange(j,angle);
                    if(auxRange.getKey() < 0){
                        aux.add(new Pair(-1,-1));
                        continue;
                    }
                    int index1 = auxRange.getKey() + (int)Math.random()%(auxRange.getValue() - auxRange.getKey() + 1);
                    if(index1 == auxRange.getValue()){
                        //Está cerrada
                        aux.add(new Pair(auxRange.getValue(),auxRange.getValue()));
                    }else{
                        int index2 = index1 + (int) Math.random()%(auxRange.getValue() - index1 + 1);
                        aux.add(new Pair(index1,index2));
                    }
                }

                //Making the random intensity
                Vector<Integer> levels = new Vector<>();
                for(int k = 0; k <= max_intensity; k=k+step_intensity){
                    levels.add(k);
                }
                int sel;
                for(int r = 0; r < max_apertures; r++){
                    sel = (int)(Math.random() % levels.size());
                    intensity = levels.get(sel);
                }
                Aperture toAdd = new Aperture(aux,intensity,collimator,angle);
                A.add(toAdd);
            }
        }else{
            for(int i = 0; i < max_apertures; i++){
                Vector<Pair<Integer,Integer>> aux = new Vector<>();
                for(int j = 0; j < collimator.getxDim(); j++){
                    if(open_apertures>0){
                        aux.add(collimator.getActiveRange(j,angle));
                    }else
                        aux.add(new Pair(-1,-1));
                }
                open_apertures--;
                Aperture toAdd = new Aperture(aux,min_intensity,collimator,angle);
                A.add(toAdd);
            }
        }
        generateIntensities();
    }

    public void generateIntensities(){
        Pair<Integer,Integer> aux;
        clearIntensity();
        for(int a = 0; a < max_apertures; a++){
            Matrix sum = A.get(a).buildedShape();
            for(int i = 0; i < collimator.getxDim(); i++){
                for(int j = 0; j < collimator.getyDim(); j++){
                    if(sum.getPos(i,j) == -1) continue;
                    I.sumPos(i,j,(int)sum.getPos(i,j));
                }
            }

        }
    }

    public void clearIntensity(){
        Pair<Integer,Integer> aux;
        for(int i = 0; i < collimator.getxDim(); i++){
            aux = collimator.getActiveRange(i,angle);
            if(aux.getKey() <0 )continue;
            for(int j = aux.getKey(); j <= aux.getValue(); j++) I.setPos(i,j,0);
        }
    };

    public void setUniformIntensity(double intensity){
        for(int a = 0; a < max_apertures; a++){
            for(int i = 0; i < collimator.getxDim(); i++){
                Pair<Integer,Integer> aux = collimator.getActiveRange(i,angle);
                if(aux.getKey() < 0 )continue;
                for(int j = aux.getKey(); j <= aux.getValue(); j++){
                    changeIntensity(i,j,intensity, null);
                }
            }
        }
    }

    public void CalculateVelocity(double c1, double c2, double w, Beam BGlobal, Beam BPersonal){

    }
    public void CalculatePosition(){

    }

    //To Change intensity on Intensity Matrix
    public void changeIntensity(int i, int j, double intensity, ArrayList<Pair<Integer, Double>> diff){
        if(intensity == I.getPos(i,j)) return;
        if(!diff.equals(null)){
            int x = pos2beam.get(new Pair(i,j));
            double rest = intensity - I.getPos(i,j);
            diff.add(new Pair(x,rest) );
        }
        if(I.getPos(i,j)>0.0){
            double x = I.getPos(i,j);
            if(int2nb.get(I.getPos(i,j) + 0.5) == 1)
                int2nb.remove(I.getPos(i,j));
            else{
                int2nb.replace( (int)(x + 0.5) , int2nb.get(x + 0.5), int2nb.get(x+0.5) -1 );
            }
            I.setPos(i,j,intensity);
            if(I.getPos(i,j)>0.0) int2nb.replace( (int)(x + 0.5) , int2nb.get(x + 0.5), int2nb.get(x+0.5) +1 );
        }

    }

    public ArrayList<Integer> openBeamlets(int a){
        ArrayList<Integer> ob = new ArrayList<>();
        Pair<Integer,Integer> aux;

        for(int i = 0; i < collimator.getxDim(); i++){
            aux = collimator.getActiveRange(i,angle);

            if(aux.getKey()<0) continue;
            for(int j = aux.getKey();  j<=aux.getValue();j++){
                Pair<Integer,Integer> auxBeam = A.get(a).getOpBeam(i);
                if(j >= auxBeam.getKey() && j <= auxBeam.getValue())
                    ob.add(pos2beam.get(new Pair(i,j)));
            }
        }
        return ob;
    }

    public ArrayList<Integer> closedBeamlets(int a){
        ArrayList<Integer> ob = new ArrayList<>();
        Pair <Integer,Integer> aux;
        for(int i = 0; i < collimator.getxDim(); i++){
            aux = collimator.getActiveRange(i,angle);
            if(aux.getKey()<0) continue;
            for (int j =  aux.getKey(); j <= aux.getValue(); j++){
                if(j < A.get(a).getOpBeam(i).getKey() || j > A.get(a).getOpBeam(i).getValue()){
                    int x = pos2beam.get(new Pair(i,j));
                    ob.add(x);
                }
            }
        }
        return ob;
    }

    public int getSumAlpha() {
        int i = 0;
        for (Aperture a : A) {
            i += a.getIntensity();
        }
        return i;
    }
    
    /* Function to be used to get the position in the matrix I of a beam column of matrix D*/
    public Pair<Integer,Integer> getPos(int index){
        System.out.println(beam2pos.size());
        if(!beam2pos.containsValue(index)){
            beam2pos.put(index, collimator.indexToPos(index,angle));
            Pair<Integer,Integer> r = collimator.indexToPos(index,angle);
            pos2beam.put(collimator.indexToPos(index,angle), index);
            return beam2pos.get(index);
        }
        else return beam2pos.get(index);
    };

    public double getIntensity(int beam){
        Pair<Integer,Integer> p = getPos(beam);
        return I.getPos(p.getKey(),p.getValue());
    }

    public int getNbApertures()
    {
        return max_apertures;
    }

    public int getNbBeamlets()
    {
        return collimator.getNangleBeamlets(angle);
    }

    public int getId_beam() {
        return angle;
    }

    public void setId_beam(int angle) {
        this.angle = angle;
    }

    public Matrix getDepositionMatrix(int o){
        int index = 0;
        for(Matrix m: D.values()){
            if(index == o) return m;
            index++;
        }
        return null;
    }
}
