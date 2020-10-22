package SRCDAO;
import java.util.*;

import javafx.util.Pair;
import source.*;

public class Beam {
    //Representacion del collimator
    private Collimator collimator;
    //Representacion de la matriz de intensidad
    private Matrix I;
    //ID BEAM
    private int angle;

    //Informacion de la configuracion tecnica del angulo
    private int max_apertures;
    private int max_intensity;
    private int min_intensity;
    private int initial_intensity;
    private int step_intensity;
    private int open_apertures;
    private int setup;

    //Numero de volumenes
    private int n_volumes;

    //Informacion de la tecnica
    private boolean apertureChange;
    private int move;
    private int totalBeamlets;

    /* Apertures (representation 1):
     * Each aperture is represented by a vector of pairs A[i] = (x_ini, x_fin)
     * and an intensity
    Range open (x_ini+1, x_fin-1) of row "r" for aperture d: A[d][r](x_ini, x_fin)
    */
    private Vector<Aperture> A;
    private HashMap<Pair<Integer,Integer>, Integer> pos2beam;
    private HashMap<Integer, Pair<Integer,Integer>> beam2pos;

    private SortedMap<Integer, Matrix> D;


    /* ------------------------ GENERAL METHODS ------------------------------------------------------- */
    public Beam(int angle, int max_apertures, int max_intensity, int initial_intensity, int step_intensity, int open_apertures, int setup, Vector<Volumen> volumes, Collimator collimator){
        setAngle(angle);
        setMax_apertures(max_apertures);
        setMax_intensity(max_intensity);
        setInitial_intensity(initial_intensity);
        setStep_intensity(step_intensity);
        setOpen_apertures(open_apertures);
        setSetup(setup);
        setMin_intensity(1);
        setN_volumes(volumes.size());

        this.collimator = collimator;
        this.A = new Vector<>();
        this.pos2beam = new HashMap<>();
        this.beam2pos = new HashMap<>();
        this.D = new TreeMap<>();
        this.totalBeamlets = collimator.getNangleBeamlets(angle);

        if(open_apertures==-1)
            setOpen_apertures(max_apertures);

        for(int i = 0; i < n_volumes; i++){
            D.put(i,volumes.get(i).getDepositionMatrix(angle));
        }

        //Declaracion de la matriz de intensidad I y se inicializa
        I = new Matrix(collimator.getxDim(), collimator.getyDim());
        for(int i = 0; i < collimator.getxDim(); i++){
            for(int j = 0; j < collimator.getyDim(); j++){
                if(j >= collimator.getActiveRange(i,angle).getKey() && j <= collimator.getActiveRange(i,angle).getValue() ) {
                    I.setPos(i,j,0);
                }else{
                    I.setPos(i,j,-1);
                }
            }
        }

        // Initialize apertures (PRINCIPAL representation)
        initiliazeBeam(setup,open_apertures);

        //Se construye la Matriz de Intensidad
        generateIntensities();

        //Fill Beam to Pos

    }

    public void initiliazeBeam(int type, int open_apertures){
        Vector<Integer> levels = new Vector<>();
        Random r =  new Random(System.currentTimeMillis());

        int l = ((max_intensity-min_intensity) / step_intensity);

        for(int k = 0; k < max_apertures; k++){
            int i = min_intensity + step_intensity * r.nextInt(l+1);
            levels.add(i);
        }

        for(int i = 0; i < max_apertures; i++){
            Aperture aux = new Aperture(collimator, angle);
            aux.initializeAperture(type, open_apertures);
            aux.initializeIntensity(type,min_intensity,max_intensity,initial_intensity,levels.get(i));
            open_apertures--;
            A.add(aux);
        }
    }

    public void generateIntensities(){
        Pair<Integer,Integer> aux;
        clearIntensity();
        for(Aperture x : A) {
            for (int i = 0; i < collimator.getxDim(); i++) {
                aux = collimator.getActiveRange(i, angle);
                if (aux.getKey() < 0)
                    continue;
                for (int j = x.getOpBeam(i).getKey()+1 ; j < x.getOpBeam(i).getValue(); j++) {
                    double newIntensity = I.getPos(i, j) + x.getIntensity();
                    changeIntensity(i, j, newIntensity);
                }

            }
        }
    }

    public void changeIntensity(int i, int j, double intensity){
        double x = I.getPos(i,j);
        if(Double.compare(x,intensity) != 0 )
            I.setPos(i,j,intensity);

    }

    public void clearIntensity(){
        Pair<Integer,Integer> aux;
        for(int i = 0; i < collimator.getxDim(); i++){
            aux = collimator.getActiveRange(i,angle);
            if(aux.getKey()<0)continue;
            for(int j = aux.getKey(); j <= aux.getValue(); j++) I.setPos(i,j,0);
        }
    }


    /* ------------------------------------ PSO METHODS ----------------------------------*/
    public void CalculateVelocity(double c1, double c2, double w, Beam BGlobal, Beam BPersonal){
        for(int i = 0; i < A.size() ; i++){
            Aperture x = A.get(i);
            Aperture bG = BGlobal.getAperture(i);
            Aperture bP = BPersonal.getAperture(i);
            x.velAperture(w,c1,c2,bG,bP);
            x.velIntensity(w,c1,c2,bG,bP);
        }
    }

    public void CalculatePosition(){
        for(Aperture x: A){
            x.movAperture();
            x.moveIntensity(max_intensity);
        }
        generateIntensities();
    }

    /* ------------------------------------------------- GETTER Y SETTERS ---------------------------------------------------*/

    /* Function to be used to get the position in the matrix I of a beam column of matrix D*/
    public Pair<Integer,Integer> getPos(int index){

        if(!beam2pos.containsValue(index)){
            beam2pos.put(index, collimator.indexToPos(index,angle));
            Pair<Integer,Integer> r = collimator.indexToPos(index,angle);
            pos2beam.put(collimator.indexToPos(index,angle), index);
            return beam2pos.get(index);
        }
        else return beam2pos.get(index);
    }

    public Pair<Integer,Integer> getPos2(int index){
        return null;
    }

    public Vector<Double> getIntensityVector(){
        Vector<Double> v = new Vector<Double>();
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> x = collimator.getActiveRange(i,angle);
            if(x.getKey() < 0 )continue;
            for(int j = x.getKey(); j <= x.getValue(); j++){
                v.add(I.getPos(i,j));
            }
        }
        return v;
    }


    public int getPosToIndex(int i,int j){
        int id = -10;
        for(Integer x: beam2pos.keySet()){
            Pair<Integer,Integer> piv = beam2pos.get(x);
            if(piv.getKey() == i && piv.getValue() == j){
                id = x;
                break;
            }
        }
        if(id == -10){
            System.out.println("NO SE ENCONTRO EL VALOR");
        }
        return id;
    }

    public double getIntensity(int beamlet){
        Pair<Integer,Integer> p = getPos(beamlet);
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

    public Matrix getDepositionMatrix(int o){
        int index = 0;
        for(Matrix m: D.values()){
            if(index == o) return m;
            index++;
        }
        return null;
    }
    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getMax_apertures() {
        return max_apertures;
    }

    public void setMax_apertures(int max_apertures) {
        this.max_apertures = max_apertures;
    }

    public int getMax_intensity() {
        return max_intensity;
    }

    public void setMax_intensity(int max_intensity) {
        this.max_intensity = max_intensity;
    }

    public int getMin_intensity() {
        return min_intensity;
    }

    public void setMin_intensity(int min_intensity) {
        this.min_intensity = min_intensity;
    }

    public int getInitial_intensity() {
        return initial_intensity;
    }

    public void setInitial_intensity(int initial_intensity) {
        this.initial_intensity = initial_intensity;
    }

    public int getStep_intensity() {
        return step_intensity;
    }

    public void setStep_intensity(int step_intensity) {
        this.step_intensity = step_intensity;
    }

    public int getOpen_apertures() {
        return open_apertures;
    }

    public void setOpen_apertures(int open_apertures) {
        this.open_apertures = open_apertures;
    }

    public int getSetup() {
        return setup;
    }

    public void setSetup(int setup) {
        this.setup = setup;
    }

    public int getN_volumes() {
        return n_volumes;
    }

    public void setN_volumes(int n_volumes) {
        this.n_volumes = n_volumes;
    }

    public boolean isApertureChange() {
        return apertureChange;
    }

    public void setApertureChange(boolean apertureChange) {
        this.apertureChange = apertureChange;
    }

    public int getMove() {
        return move;
    }

    public void setMove(int move) {
        this.move = move;
    }

    public Aperture getAperture(int id){
        return A.get(id);
    }

    /*-------------------------------------------------------- PRINTERS -------------------------------------- */

    public void printIntensityMatrix(){
        System.out.println(angle + ": ");
        for(int i = 0; i < collimator.getxDim(); i++){
            for(int j = 0; j < collimator.getyDim(); j++){
                System.out.print(I.getPos(i,j) + " ");
            }
            System.out.println("");
        }
    }

    public void printApertures(){
        System.out.println(angle + ": ");
        for(Aperture x : A){
            x.printAperture();
        }
    }

    public void printIdBeam(){
        System.out.println("ANGLE: " + angle + " TOTAL ANGLE BEAMLETS:" + getNbBeamlets());
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> x = collimator.getActiveRange(i,angle);
            if(x.getKey() < 0 )continue;
            for(int j = x.getKey(); j <= x.getValue(); j++){
                System.out.println( getPosToIndex(i,j)+1 + " " + I.getPos(i,j) );
            }
        }
    }

    public void printIdBeamtoVector(){
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> x = collimator.getActiveRange(i,angle);
            if(x.getKey() < 0 )continue;
            for(int j = x.getKey(); j <= x.getValue(); j++){
                System.out.print(I.getPos(i,j)+ " " );
            }
        }
    }


}
