package SRCDAO;

import source.Collimator;
import javafx.util.Pair;

import java.util.ArrayList;

public class Aperture {

    private int angle;
    private double intensity;
    private ArrayList<Pair<Integer,Integer>> A;

    private ArrayList<Pair<Integer,Integer>> velocityA;
    private double veloc_intensity;
    private int openedBeamlets;
    private int collimatorDim;

    /*--------------------------------------------------METHODS----------------------------------------------------------------------------*/
    public Aperture(Collimator collimator, int angle){
        setAngle(angle);
        setIntensity(0.0);
        setVeloc_intensity(1.0);
        setOpenedBeamlets(0);

        this.velocityA = new ArrayList<>();
        this.A = new ArrayList<>();
        this.collimatorDim = collimator.getxDim();

        for(int i = 0; i < this.collimatorDim; i++)
            velocityA.add(new Pair<>(0,0)); // -> Velocidad 0 para todas las hojas
    }

    public Aperture(Aperture a){
        setAngle(a.angle);                          // Copy the angle of the aperture
        setIntensity(a.intensity);                  // Used to cp the intensity of the aperture
        setVeloc_intensity(a.veloc_intensity);      // Used to cp the velocity of the intensity
        setVelocityA(a.velocityA);                  // Used to copy the velocity aperture shape rows
        setApertures(a.A);                          // Used to copy the aperture shape rows
        setOpenedBeamlets(a.openedBeamlets);
    }

    public void initializeAperture(int type, int open_apertures, int id_aperture, ArrayList<Pair<Integer,Integer>> activeRange){

        ArrayList<Pair<Integer,Integer>> aux = new ArrayList<>();

        for(int i = 0; i < this.collimatorDim; i++){

            if(activeRange.get(i).getKey() < 0){ //<-1,-1>
                //Cerrada completamente por inactividad
                aux.add(new Pair<>(-2, -2));
            }
            else{
                //Todos los beamlets de la fila i en el angulo 'angle'
                int fLeaf = activeRange.get(i).getKey() - 1;
                int sLeaf = activeRange.get(i).getValue() + 1;

                if(type == ApertureEnum.OPEN_MAX_SETUP.getValue() || type == ApertureEnum.OPEN_MIN_SETUP.getValue()) {
                    //Abierta completamente para los beamlets activos
                    aux.add(new Pair<>(fLeaf,sLeaf));

                }else if(type == ApertureEnum.CLOSED_MAX_SETUP.getValue() || type == ApertureEnum.CLOSED_MIN_SETUP.getValue()) {
                    //Cerrada completamente
                    aux.add(new Pair<>(fLeaf,fLeaf+1));

                }else if(type == ApertureEnum.RAND_RAND_SETUP.getValue()){
                    ///Se abre aleatoriamente las hojas
                    int middle = Math.floorDiv((fLeaf+sLeaf),2);
                    int index1 = fLeaf + (int)(Math.random()*(middle - fLeaf));

                    if( index1 == sLeaf ){
                        aux.add(new Pair<>(fLeaf, sLeaf));
                    }else{
                        int index2 = (middle) + (int)(Math.random()*(sLeaf-middle+1));
                        aux.add(new Pair<>(index1,index2));
                    }
                }else if(type == ApertureEnum.STATIC_SETUP.getValue()){
                    // Se posiciona
                    int index_1 = fLeaf;
                    int index_2 = fLeaf + id_aperture + 2;

                    if (index_2 > sLeaf)
                        index_2 = sLeaf;

                    //Se mantiene la apertura inicial
                    aux.add(new Pair<>(index_1,index_2));
                }
                else{

                    if(open_apertures > 0){
                        aux.add(new Pair<>(fLeaf,sLeaf));
                    }else{
                        //Cerrada completamente por inactividad
                        aux.add(new Pair<>(-2,-2));
                    }
                }
            }
        }

        setApertures(aux);
    }

    public void initializeIntensity(int type, int min_intensity, int max_intensity, int initial_intensity, double r_intensity){
        // Usar switch con enum
        if (type == ApertureEnum.OPEN_MIN_SETUP.getValue() || type == ApertureEnum.CLOSED_MIN_SETUP.getValue()) {
            setIntensity(min_intensity);
        }else if (type == ApertureEnum.OPEN_MAX_SETUP.getValue() || type == ApertureEnum.CLOSED_MAX_SETUP.getValue()) {
            setIntensity(max_intensity);
        }else if (type == ApertureEnum.RAND_RAND_SETUP.getValue()) {
            setIntensity(r_intensity);
        }else if( type == ApertureEnum.STATIC_SETUP.getValue()) {
            final double STATIC_INTENSITY_VALUE = 1; // Constante con nombre
            setIntensity(STATIC_INTENSITY_VALUE);
        }else{
            setIntensity(initial_intensity);
        }
    }

    /*----------------------------------------------------PSO METHODS--------------------------------------------------------------------------------*/
    public void velAperture(double wAperture, double c1Aperture, double c2Aperture, double cnAperture ,Aperture BGlobal, Aperture BPersonal){
        double r1 = Math.random();
        double r2 = Math.random();
        ArrayList<Pair<Integer,Integer>> BG = BGlobal.getApertures();
        ArrayList<Pair<Integer,Integer>> BP = BPersonal.getApertures();
        for(int i = 0; i < this.collimatorDim; i++){
            Pair<Integer,Integer> aux_G = BG.get(i);
            Pair<Integer,Integer> aux_P = BP.get(i);

            // Se mueven unicamente las que estan disponible para moverse
            if( aux_G.getKey() < 0 )
                continue;

            // Velocidad de la primera hoja (izquierda)
            int first = (int)( cnAperture*(
                                    wAperture*velocityA.get(i).getKey() +
                                    c1Aperture*r1*( aux_G.getKey() - A.get(i).getKey() ) +
                                    c2Aperture*r2*(aux_P.getKey() - A.get(i).getKey() )
                                )
                            );

            // Velocidad de la segunda hoja (derecha)
            int second = (int)( cnAperture*(
                                        wAperture*velocityA.get(i).getValue() +
                                        c1Aperture*r1*( aux_G.getValue() - A.get(i).getValue() )  +
                                        c2Aperture*r2*( aux_P.getValue() - A.get(i).getValue() )
                                )
                            );

                            velocityA.set(i, new Pair<>(first, second));
        }
    }

    public void velIntensity(double wIntensity, double c1Intensity, double c2Intensity, double cnIntensity, Aperture BGlobal, Aperture BPersonal){
        double bG = BGlobal.getIntensity();
        double bP = BPersonal.getIntensity();
        double r1 = Math.random();
        double r2 = Math.random();

        double val = cnIntensity*(wIntensity*veloc_intensity + r1*c1Intensity*(bG - intensity) + r2*c2Intensity*(bP - intensity));
        
        setVeloc_intensity(val);
    }

    // Movement function used by pso to translate the apertures
    public void moveAperture(ArrayList<Pair<Integer,Integer>> activeRange){

        setOpenedBeamlets(0);
        int counterOpenedBeamLets = 0;
        for(int i = 0; i < A.size(); i++){

            Pair<Integer,Integer> limits = activeRange.get(i);
            //Par <-2,-2> cerradas de forma definitiva
            if(limits.getKey() < 0)
                continue;

            int first = (velocityA.get(i).getKey() + A.get(i).getKey());
            int second = (velocityA.get(i).getValue() + A.get(i).getValue());

            int limit_inf = limits.getKey();
            int limit_sup = limits.getValue();

            if(first < limit_inf  || first > limit_sup){
                first = limit_inf-1;
            }

            if(second < limit_inf  || second > limit_sup ){
                second = limit_sup+1;
            }

            if(first >= second) {
                int val = (first + second)/2;
                first = (val);
                second = (val)+1;
            }

            Pair<Integer, Integer> newApertures = new Pair<>(first, second);
            A.set(i, newApertures);

            counterOpenedBeamLets += (second-first-1);
        }
        setOpenedBeamlets(counterOpenedBeamLets);
    }

    // Movement Function used of PSO considering the intensities
    public void moveIntensity(double max_intensity){
        double newIntensity = getIntensity() + getVeloc_intensity();

        if(newIntensity > max_intensity){
            newIntensity = max_intensity;
        }else if(newIntensity < 0.0 ) {
            newIntensity = 0.0;
        }

        setIntensity(newIntensity);
    }


    /*---------------------------------------------------GETTERS AND SETTERS------------------------------------------------------------*/

    public void setApertures(ArrayList<Pair<Integer,Integer>> Aperture){
        ArrayList<Pair<Integer,Integer>> newAperture = new ArrayList<>();

        for(Pair<Integer,Integer> row: Aperture){
            Pair<Integer, Integer> newRow = new Pair<>(row.getKey(), row.getValue());
            newAperture.add(newRow);
        }
        this.A = newAperture;
    }

    public void setVelocityA(ArrayList<Pair<Integer,Integer>> velocAperture){
        ArrayList<Pair<Integer,Integer>> newVelocity = new ArrayList<>();

        for(Pair<Integer,Integer> row: velocAperture){
            Pair<Integer,Integer> newRow = new Pair<>(row.getKey(), row.getValue());
            newVelocity.add(newRow);
        }

        this.velocityA = newVelocity;
    }

    public void setRow(int indexRow, Pair<Integer,Integer> newRow, ArrayList<Pair<Integer,Integer>> activeRange){
        Pair<Integer,Integer> limits = activeRange.get(indexRow);
        if( limits.getKey() != -1 ){
            this.A.set(indexRow, newRow);
            this.setApertures(this.A);
        }
    }

    //Consulta si el beamlet de la apertura es proyectado o no
    public boolean getProyectedBeamLet(Beamlet beamlet){
        Pair<Integer,Integer> beamLetsCoords = beamlet.getPosition();
        int x = beamLetsCoords.getKey();
        int y = beamLetsCoords.getValue();
        Pair<Integer,Integer> row = A.get(x);

        return (y > (row.getKey()) && y < (row.getValue()));

    }

    public Pair<Integer,Integer> getOpBeam(int i){
        return A.get(i);
    };

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public double getIntensity() { return intensity; }

    public void setIntensity(double intensity) { this.intensity = intensity; }

    public double getVeloc_intensity() {
        return veloc_intensity;
    }

    public void setVeloc_intensity(double veloc_intensity) {
        this.veloc_intensity = veloc_intensity;
    }

    public void setOpenedBeamlets(int opBeamLets) {
        this.openedBeamlets = opBeamLets;
    }

    public ArrayList<Pair<Integer,Integer>> getApertures(){
        return A;
    }

}
