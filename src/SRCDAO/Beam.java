package SRCDAO;
import java.util.*;

import source.Pair;
import source.*;

public class Beam {
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
    private int collimatorDim;
    private int totalBeamlets;
    private int aperturesUnused;

    //Representacion del collimator
    private final Collimator collimator;

    //Representacion de la matriz de intensidad
    private final Matrix I;

    /* Apertures (representation 1):
     * Each aperture is represented by a vector of pairs A[i] = (x_ini, x_fin)
     * and an intensity range open (x_ini+1, x_fin-1) of row "r" for aperture d: A[d][r](x_ini, x_fin)
    */
    private ArrayList<Aperture> A;
    private ArrayList<Double> fluenceMap;



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
        setCollimatorDim(collimator.getyDim());
        setTotalBeamlets(collimator.getNangleBeamlets(angle));

        this.collimator = collimator;
        this.A = new ArrayList<>();
        this.fluenceMap = new ArrayList<>();
        this.aperturesUnused = 0;

        if(openApertures==-1)
            setOpenApertures(maxApertures);

        // Declaracion de la matriz de intensidad I y se inicializa
        this.I = new Matrix(collimator.getxDim(), collimator.getyDim());

        // Rellenado de la matriz de intensidad
        clearIntensity();

        // Initialize apertures
        initiliazeBeam(setup,openApertures);

        //Se construye la Matriz de Intensidad
        generateIntensities();

    }

    public Beam(Beam b){
        setAngle(b.angle);                  // Setea el ID del Beam
        setMax_apertures(b.maxApertures);
        setMax_intensity(b.maxIntensity);
        setInitialIntensity(b.initialIntensity);
        setStep_intensity(b.stepIntensity);
        setOpenApertures(b.openApertures);
        setSetup(b.setup);
        setMinIntensity(b.minIntensity);
        setCollimatorDim(b.getCollimatorDim());
        setTotalBeamlets(b.getTotalBeamlets());

        this.collimator = new Collimator(b.collimator);
        this.A = new ArrayList<>();
        this.fluenceMap = new ArrayList<>();
        this.aperturesUnused = b.aperturesUnused;

        if(openApertures==-1)
            setOpenApertures(maxApertures);

        // Declaracion de la matriz de intensidad I y se inicializa
        I = new Matrix(collimator.getxDim(), collimator.getyDim());

        // Rellenado de la matriz de intensidad
        clearIntensity();

        for(Aperture a: b.A){
            Aperture copyAperture = new Aperture(a);
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
            int i = minIntensity + stepIntensity *(int)(Math.random()*(l+1));
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
        this.aperturesUnused = 0;
        Pair<Integer,Integer> limits;
        clearIntensity();

        for(int a = 0; a < A.size(); a++) {
            Aperture ap = A.get(a);
            double apIntensity = ap.getIntensity();

            if(apIntensity < 1.0){
                this.aperturesUnused++;
            }

            for (int i = 0; i < collimator.getxDim(); i++) {
                limits = collimator.getActiveRange(i, angle);

                if ( limits.getFirst() == -1)
                     continue;

                Pair<Integer, Integer> apertureRow = ap.getOpBeam(i);

                for (int j = apertureRow.getFirst()+1 ; j < apertureRow.getSecond(); j++) {
                    double newIntensity = (this.I.getPos(i, j) + apIntensity);
                    I.setPos(i,j, newIntensity);
                }
            }
        }
        buildIntensityVector();
    }

    public void clearIntensity(){
        // Rellenado de la matriz de intensidad
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> limits = collimator.getActiveRange(i, angle);

            for(int j = 0; j < collimator.getyDim(); j++){
                if(j >= limits.getFirst() && j <= limits.getSecond() ) {
                    this.I.setPos(i,j,0);
                }else{
                    this.I.setPos(i,j,-1);
                }
            }
        }
    }

    public void buildIntensityVector(){
        this.fluenceMap = new ArrayList<>();

        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> limit = collimator.getActiveRange(i,angle);
            if( limit.getFirst() == -1 )
                continue;

            for(int j = limit.getFirst(); j <= limit.getSecond(); j++){
                fluenceMap.add(I.getPos(i,j));
            }
        }
    }

    public void regenerateApertures(){
        ArrayList<Pair<Integer,Integer>> functionalApertures = this.getTransposeMatrix();
        ArrayList<Integer> aperturesUnUsedList = new ArrayList<>();

        //Reconocimiento de aperturas con baja intensidad
        for(int a = 0; a < A.size(); a++){
            Aperture aperture = A.get(a);
            if(aperture.getIntensity() < 1.0)
                aperturesUnUsedList.add(a);
        }

        for(int a = 0; a < aperturesUnUsedList.size(); a++){
            int indexAperture = aperturesUnUsedList.get(a);
            Aperture aperture = A.get(indexAperture);

            for(int indexRow = 0; indexRow < collimator.getxDim(); indexRow++){
                Pair<Integer,Integer> limits = collimator.getActiveRange(indexRow, angle);
                if(limits.getFirst() == -1)
                    continue;

                Pair<Integer, Integer> functionalRow = functionalApertures.get(indexRow);

                if(functionalRow.getFirst()+1 == limits.getFirst() && functionalRow.getSecond()-1 == limits.getSecond()){
                    //La fila es completamente irradiada -> Se ciera en las apertura inutilizadas
                    Pair<Integer,Integer> newRow =  new Pair(limits.getFirst(), limits.getFirst()+1);
                    aperture.setRow(indexRow, newRow);
                    continue;

                }else if(functionalRow.getFirst()+1 > limits.getFirst() && functionalRow.getSecond()-1 < limits.getSecond()){
                    //Falta irradiar por la izquierda y por la derecha
                    if(Math.random() <= 0.5){

                        //De forma aleatoria, la apertura irradiara por la izquierda
                        int apertureLeft = limits.getFirst() - 1;
                        int apertureRight = functionalRow.getFirst() + 1;
                        Pair<Integer,Integer> newRow = new Pair(apertureLeft, apertureRight);

                        aperture.setRow(indexRow, newRow);

                        functionalRow = new Pair(apertureLeft, functionalRow.getSecond());

                    }else{
                        //De forma aleatoria, la apertura irradiara por la derecha
                        int apertureLeft = functionalRow.getSecond() - 1;
                        int apertureRight = limits.getSecond() + 1;

                        Pair<Integer,Integer> newRow = new Pair(apertureLeft, apertureRight);

                        aperture.setRow(indexRow, newRow);

                        functionalRow = new Pair(functionalRow.getFirst(), apertureRight);
                    }
                }else{

                    // Falta irradiar unicamente por la izquierda
                    if(functionalRow.getFirst()+1 > limits.getFirst() ) {
                        //System.out.println("Moviendo hoja a la izquierda");

                        int apertureLeft = limits.getFirst() - 1;
                        int apertureRight = functionalRow.getFirst() + 1;

                        Pair<Integer,Integer> newRow = new Pair(apertureLeft, apertureRight);
                        aperture.setRow(indexRow, newRow);

                        functionalRow = new Pair(apertureLeft, functionalRow.getSecond());

                    }else if(functionalRow.getSecond()-1 < limits.getSecond() ){
                        // Falta irradiar unicamente por la derecha

                        int apertureLeft = functionalRow.getSecond() - 1;
                        int apertureRight = limits.getSecond() + 1;
                        Pair<Integer,Integer> newRow = new Pair(apertureLeft, apertureRight);

                        aperture.setRow(indexRow, newRow);
                        functionalRow = new Pair(functionalRow.getFirst(), apertureRight);

                    }
                }
                functionalApertures.set(indexRow, functionalRow);
            }


        }

    }

    // Get a general aperture with the shapes adjusted using intensity > 1
    public ArrayList<Pair<Integer,Integer>> BuildTransposeAperture(){
        ArrayList<Pair<Integer,Integer>> aperturesOnOperation = new ArrayList<>();

        for(int a = 0; a < A.size(); a++){
            Aperture aperture = A.get(a);

            if(aperture.getIntensity() >= 1.0){
                if(aperturesOnOperation.size() == 0){
                    //Get a template of the first aperture with intensity mayor of 1.0
                    aperturesOnOperation = new ArrayList(aperture.getApertures());
                }else{
                    for(int r = 0; r < aperturesOnOperation.size(); r++ ){

                        //Compare the index of each row on the current aperture with the Template Generated
                        Pair<Integer,Integer> rowLimits = collimator.getActiveRange(r, angle);
                        if(rowLimits.getFirst() == -1)
                            continue;

                        Pair<Integer,Integer> rowAperture = aperture.getOpBeam(r);
                        Pair<Integer,Integer> rowTranspose = aperturesOnOperation.get(r);

                        int firstLeaf = rowTranspose.getFirst();
                        int secondLeaf = rowTranspose.getSecond();

                        if(rowAperture.getFirst() <= firstLeaf ){
                            firstLeaf = rowAperture.getFirst();
                        }

                        if(rowAperture.getSecond() >= secondLeaf){
                            secondLeaf = rowAperture.getSecond();
                        }

                        aperturesOnOperation.set(r, new Pair(firstLeaf, secondLeaf));

                    }
                }
            }
        }

        //Se asegura que no exista un sector intermedio sin irradiar
        for(int r = 0; r < aperturesOnOperation.size(); r++){
            Pair<Integer,Integer> row = aperturesOnOperation.get(r);
            ArrayList<Integer> beamletsOnRow = new ArrayList<>();

            if(row.getFirst() == -2)
                continue;

            //Beamlets utilizados en la fila actual
            for(int x = row.getFirst()+1; x < row.getSecond(); x++){
                beamletsOnRow.add(x);
            }

            //Check if the beamlet is proyected at least one time for an Aperture
            ArrayList<Integer> copyBeamlets = new ArrayList<>();
            for(int a = 0; a < A.size(); a++) {
                Aperture aperture = A.get(a);
                if (aperture.getIntensity() >= 1.0) {
                    Pair<Integer,Integer> apertureRow = aperture.getOpBeam(r);
                    for(int b = 0; b < beamletsOnRow.size(); b++){
                        int beamlet = beamletsOnRow.get(b);
                        if(beamlet >= apertureRow.getFirst() && beamlet <= apertureRow.getSecond()){
                            copyBeamlets.add(beamlet);
                        }
                    }
                }
            }
            //Delete the proyected beamlets and only stay the beamlets hide between leaf
            for(Integer b: copyBeamlets)
                beamletsOnRow.remove(b);

            //Translate the Leaf's to irradiate the beamLets abandoned
            if(beamletsOnRow.size() != 0){
                Pair<Integer,Integer> newRow = null;
                Collections.sort(beamletsOnRow);

                int maxindex = beamletsOnRow.get(beamletsOnRow.size() - 1);
                int minindex = beamletsOnRow.get(0);

                int d1 = Math.abs(minindex - row.getSecond() - 1);
                int d2 = Math.abs(maxindex - row.getFirst() + 1 );

                if( d1 <= d2){
                    newRow = new Pair(row.getFirst(), minindex+1);
                }else{
                    newRow = new Pair(row.getSecond(), maxindex-1);
                }
                aperturesOnOperation.set(r, newRow);

            }
        }

        return aperturesOnOperation;
    }

    /* ------------------------------------------------- GETTER Y SETTERS ---------------------------------------------------*/

    public void setApertures(ArrayList<Aperture> lastApertures){
        ArrayList<Aperture> newApertures = new ArrayList<>();
        for(Aperture aperture: lastApertures){
            Aperture newAper = new Aperture(aperture);
            newApertures.add(newAper);
        }
    }

    public void setIntensityByAperture(double[] intensitySolver){
        for (int a = 0; a < A.size(); a++) {
            Aperture aperture = A.get(a);
            aperture.setIntensity(intensitySolver[a]);
        }
    }

    public boolean getProyectedBeamLetByAperture(int idAperture, int indexBeamlet){
        Aperture ap = A.get(idAperture);
        return ap.getProyectedBeamLet(indexBeamlet);
    }


    public Double getIntensityByAperture(int apertureIndex) throws Exception {
        double intensity;
        try {
            intensity = A.get(apertureIndex).getIntensity();
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("La intensidad no ha sido encontrada ");
        }
        return intensity;
    }

    public int getAperturesUnused(){ return this.aperturesUnused; }

    public ArrayList<Double> getIntensityVector(){ return fluenceMap; }

    public int getIdBeam() { return angle; }

    public void setAngle(int angle) { this.angle = angle; }

    public void setMax_apertures(int maxApertures) { this.maxApertures = maxApertures; }

    public void setMax_intensity(int max_intensity) { this.maxIntensity = max_intensity; }

    public void setMinIntensity(int minIntensity) { this.minIntensity = minIntensity; }

    public void setInitialIntensity(int initialIntensity) { this.initialIntensity = initialIntensity; }

    public void setStep_intensity(int step_intensity) { this.stepIntensity = step_intensity; }

    public void setOpenApertures(int openApertures) { this.openApertures = openApertures; }

    public void setSetup(int setup) { this.setup = setup; }

    public Matrix getIntensitisMatrix(){ return I; }

    public Aperture getAperture(int id){ return A.get(id); }

    public ArrayList<Aperture> getApertures(){ return this.A; }

    public int getCollimatorDim() { return collimatorDim; }

    public void setCollimatorDim(int collimatorDim) { this.collimatorDim = collimatorDim; }

    public int getTotalBeamlets() { return totalBeamlets; }

    public void setTotalBeamlets(int totalBeamlets) { this.totalBeamlets = totalBeamlets; }

    public ArrayList<Pair<Integer, Integer>> getTransposeMatrix(){ return this.BuildTransposeAperture(); }


    /* ------------------------------------------------ PSO METHODS ----------------------------------*/
    public void CalculateVelocity(double c1Aperture, double c2Aperture, double wAperture, double cnAperture, double c1Intensity, double c2Intensity, double wIntensity, double cnIntensity, Beam BGlobal, Beam BPersonal){
        for(int i = 0; i < A.size() ; i++){
            Aperture x = A.get(i);
            Aperture bG = BGlobal.getAperture(i);
            Aperture bP = BPersonal.getAperture(i);

            x.velAperture(wAperture, c1Aperture, c2Aperture, cnAperture, bG, bP);
            x.velIntensity(wIntensity, c1Intensity, c2Intensity, cnIntensity, bG, bP);
        }
    }

    public void CalculatePosition(){
        for(Aperture x: A){
            x.movAperture();
            x.moveIntensity(maxIntensity);
        }
        generateIntensities();
    }



}
