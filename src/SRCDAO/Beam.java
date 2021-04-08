package SRCDAO;
import java.util.*;

import source.Pair;
import source.*;

public class Beam {
    //Representacion del collimator
    private Collimator collimator;

    //Representacion de la matriz de intensidad
    private Matrix I;
    //ID BEAM
    private int angle;

    //Informacion de la configuracion tecnica del angulo
    private int maxApertures;
    private int maxIntensity;
    private int minIntensity;
    private int initialIntensity;
    private int stepIntensity;
    private int openApertures;
    private int setup;

    /* Apertures (representation 1):
     * Each aperture is represented by a vector of pairs A[i] = (x_ini, x_fin)
     * and an intensity range open (x_ini+1, x_fin-1) of row "r" for aperture d: A[d][r](x_ini, x_fin)
    */
    private Vector<Aperture> A;
    private HashMap<Pair<Integer,Integer>, Integer> pos2beam;
    private HashMap<Integer, Pair<Integer,Integer>> beam2pos;

    private Vector<Double> fluenceMap;


    /* ------------------------------------------- GENERAL METHODS ------------------------------------------------------- */
    public Beam(int angle, int maxApertures, int maxIntensity, int initialIntensity, int stepIntensity, int openApertures, int setup, Collimator collimator){
        setAngle(angle);
        setMax_apertures(maxApertures);
        setMax_intensity(maxIntensity);
        setInitialIntensity(initialIntensity);
        setStep_intensity(stepIntensity);
        setOpenApertures(openApertures);
        setSetup(setup);
        setMinIntensity(1);

        this.collimator = collimator;
        this.A = new Vector<>();
        this.pos2beam = new HashMap<>();
        this.beam2pos = new HashMap<>();
        this.fluenceMap = new Vector<>();

        if(openApertures==-1)
            setOpenApertures(maxApertures);

        // Declaracion de la matriz de intensidad I y se inicializa
        I = new Matrix(collimator.getxDim(), collimator.getyDim());

        // Rellenado de la matriz de intensidad
        for(int i = 0; i < collimator.getxDim(); i++){
            for(int j = 0; j < collimator.getyDim(); j++){
                if(j >= collimator.getActiveRange(i,angle).getFirst() && j <= collimator.getActiveRange(i,angle).getSecond() ) {
                    I.setPos(i,j,0);
                }else{
                    I.setPos(i,j,-1);
                }
            }
        }

        // Initialize apertures (PRINCIPAL representation)
        initiliazeBeam(setup,openApertures);

        //Se construye la Matriz de Intensidad
        generateIntensities();
    }

    public Beam(Beam b){
        setAngle(b.angle);
        setMax_apertures(b.maxApertures);
        setMax_intensity(b.maxIntensity);
        setInitialIntensity(b.initialIntensity);
        setStep_intensity(b.stepIntensity);
        setOpenApertures(b.openApertures);
        setSetup(b.setup);
        setMinIntensity(b.minIntensity);

        this.collimator = new Collimator(b.collimator);

        this.A = new Vector<>();
        this.pos2beam = new HashMap<>();
        this.beam2pos = new HashMap<>();
        this.fluenceMap = new Vector<>();

        if(openApertures==-1)
            setOpenApertures(maxApertures);

        // Declaracion de la matriz de intensidad I y se inicializa
        I = new Matrix(collimator.getxDim(), collimator.getyDim());

        // Rellenado de la matriz de intensidad
        for(int i = 0; i < this.collimator.getxDim(); i++){
            for(int j = 0; j < this.collimator.getyDim(); j++){
                if(j >= collimator.getActiveRange(i,angle).getFirst() && j <= this.collimator.getActiveRange(i,angle).getSecond() ) {
                    I.setPos(i,j,0);
                }else{
                    I.setPos(i,j,-1);
                }
            }
        }

        for(int i = 0; i < maxApertures; i++){
            Aperture copyAperture = new Aperture(b.getAperture(i));
            this.A.add(copyAperture);
        }

        //Se construye la Matriz de Intensidad
        generateIntensities();

    }

    public void initiliazeBeam(int type, int openApertures){
        Vector<Integer> levels = new Vector<>();

        //Calculate levels for random Intensity
        int l = (maxIntensity-minIntensity)/stepIntensity ;
        for(int k = 0; k < maxApertures; k++){
            int i = minIntensity + stepIntensity *(int) (Math.random()*(l+1));
            levels.add(i);
        }

        //Inicializacion de cada apertura
        for(int i = 0; i < maxApertures; i++){
            Aperture aux = new Aperture(collimator, angle);
            aux.initializeAperture(type, openApertures);
            aux.initializeIntensity(type, minIntensity, maxIntensity, initialIntensity, levels.get(i));
            openApertures--;
            A.add(aux);
        }
    }

    public void generateIntensities(){

        Pair<Integer,Integer> aux;
        clearIntensity();

        for(Aperture ap : A) {
            int apIntensity = ap.getIntensity();
            for (int i = 0; i < collimator.getxDim(); i++) {
                aux = collimator.getActiveRange(i, angle);

                if (aux.getFirst() < 0 || ap.getOpBeam(i).getFirst() < -1)
                    continue;

                for (int j = ap.getOpBeam(i).getFirst()+1 ; j < ap.getOpBeam(i).getSecond(); j++) {
                    Integer newIntensity = (int)(I.getPos(i, j) + apIntensity);
                    I.setPos(i,j, newIntensity );
                }
            }
        }
        buildIntensityVector();

    }

    public void buildIntensityVector(){
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> x = collimator.getActiveRange(i,angle);
            if(x.getFirst() < 0 )
                continue;
            for(int j = x.getFirst(); j <= x.getSecond(); j++){
                fluenceMap.add(I.getPos(i,j));
            }
        }
    }

    public void clearIntensity(){
        Pair<Integer,Integer> aux;
        for(int i = 0; i < collimator.getxDim(); i++){
            aux = collimator.getActiveRange(i,angle);
            if( aux.getFirst()<0 )
                continue;
            for(int j = aux.getFirst(); j <= aux.getSecond(); j++)
                I.setPos(i,j,0);
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
            x.moveIntensity(maxIntensity);
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



    public Vector<Double> getIntensityVector(){
        return fluenceMap;
    }

    public int getPosToIndex(int i,int j){
        int id = -10;
        for(Integer x: beam2pos.keySet()){
            Pair<Integer,Integer> piv = beam2pos.get(x);
            if(piv.getFirst() == i && piv.getSecond() == j){
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
        return I.getPos(p.getFirst(), p.getSecond() );
    }

    public int getNbBeamlets()
    {
        return collimator.getNangleBeamlets(angle);
    }

    public int getIdBeam() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public void setMax_apertures(int maxApertures) {
        this.maxApertures = maxApertures;
    }

    public void setMax_intensity(int max_intensity) {
        this.maxIntensity = max_intensity;
    }

    public void setMinIntensity(int minIntensity) {
        this.minIntensity = minIntensity;
    }

    public void setInitialIntensity(int initialIntensity) {
        this.initialIntensity = initialIntensity;
    }

    public void setStep_intensity(int step_intensity) {
        this.stepIntensity = step_intensity;
    }

    public void setOpenApertures(int openApertures) {
        this.openApertures = openApertures;
    }

    public void setSetup(int setup) {
        this.setup = setup;
    }

    public Aperture getAperture(int id){
        return A.get(id);
    }

    public Collimator getCollimator(){
        return collimator;
    }

    public void setCollimator(Collimator collimator){
        this.collimator = collimator;
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
            if(x.getFirst() < 0 )continue;
            for(int j = x.getFirst(); j <= x.getSecond(); j++){
                System.out.println( getPosToIndex(i,j)+1 + " " + I.getPos(i,j) );
            }
        }
    }

    public void printIdBeamtoVector(){
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> x = collimator.getActiveRange(i,angle);
            if(x.getFirst() < 0 )continue;
            for(int j = x.getFirst(); j <= x.getSecond(); j++){
                System.out.print(I.getPos(i,j)+ " " );
            }
        }
    }


}
