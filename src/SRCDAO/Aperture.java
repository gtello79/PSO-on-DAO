package SRCDAO;

import source.Collimator;
import source.Pair;

import java.util.ArrayList;

public class Aperture {

    private Collimator collimator;
    private int angle;
    private double intensity;
    private ArrayList<Pair<Integer,Integer>> A;

    private ArrayList<Pair<Integer,Integer>> velocityA;
    private double veloc_intensity;

    protected int OPEN_MIN_SETUP = 0;
    protected int OPEN_MAX_SETUP = 1;
    protected int CLOSED_MIN_SETUP = 2 ;
    protected int CLOSED_MAX_SETUP = 3;
    protected int RAND_RAND_SETUP = 4;

    /*--------------------------------------------------METHODS----------------------------------------------------------------------------*/
    public Aperture(Collimator collimator, int angle){
        setAngle(angle);
        setIntensity(0.0);
        setVeloc_intensity(1.0);
        this.collimator = collimator;
        this.velocityA = new ArrayList<>();
        this.A = new ArrayList<>();

        for(int i = 0; i < collimator.getxDim(); i++)
            velocityA.add(new Pair(0,0)); // -> Velocidad 0 para todas las filas
    }

    public Aperture(Aperture a){
        setAngle(a.angle);
        setIntensity(a.intensity);
        setVeloc_intensity(a.veloc_intensity);
        setVelocityA(a.velocityA);
        setApertures(a.A);

    }

    public void initializeAperture(int type, int open_apertures){
        ArrayList<Pair<Integer,Integer>> aux = new ArrayList<>();

        for(int i = 0; i < collimator.getxDim(); i++){

            if(collimator.getActiveRange(i,angle).getFirst() < 0){ //<-1,-1>
                //Cerrada completamente por inactividad
                aux.add(new Pair(-2, -2));
            }
            else{
                //Todos los beamlets de la fila i en el angulo 'angle'
                int fLeaf = collimator.getActiveRange(i,angle).getFirst() - 1;
                int sLeaf = collimator.getActiveRange(i,angle).getSecond() + 1;

                if(type == OPEN_MAX_SETUP || type == OPEN_MIN_SETUP){
                    //Abierta completamente para los beamlets activos
                    aux.add(new Pair(fLeaf,sLeaf));

                }else if(type == CLOSED_MAX_SETUP || type == CLOSED_MIN_SETUP) {
                    //Cerrada completamente (MEJORAR)
                    aux.add(new Pair(fLeaf,fLeaf+1)); //<5,6>

                }else if(type == RAND_RAND_SETUP){
                    ///Se abre aleatoriamente las hojas
                    int middle = Math.floorDiv((fLeaf+sLeaf),2);
                    int index1 = fLeaf + (int)(Math.random()*(middle - fLeaf));

                    if( index1 == sLeaf ){
                        aux.add(new Pair(fLeaf, sLeaf));
                    }else{
                        int index2 = (middle) + (int)(Math.random()*(sLeaf-middle));
                        aux.add(new Pair(index1,index2));
                    }
                }else{

                    if(open_apertures > 0){
                        aux.add(new Pair(fLeaf,sLeaf));
                    }else{
                        //Cerrada completamente por inactividad
                        aux.add(new Pair(-2,-2));
                    }
                }
            }
        }

        setApertures(aux);
    }

    public void initializeIntensity(int type, int min_intensity, int max_intensity, int initial_intensity, int r_intensity){
        if( type==OPEN_MIN_SETUP || type==CLOSED_MIN_SETUP ){
            setIntensity(min_intensity);
        }else if( type==OPEN_MAX_SETUP || type==CLOSED_MAX_SETUP ){
            setIntensity(max_intensity);
        }else if( type == RAND_RAND_SETUP){
            setIntensity(r_intensity);
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
        for(int i = 0; i < collimator.getxDim(); i++){
            Pair<Integer,Integer> aux_G = BG.get(i);
            Pair<Integer,Integer> aux_P = BP.get(i);

            // Se mueven unicamente las que estan disponible para moverse
            if( collimator.getActiveRange(i,angle).getFirst() < 0 )
                continue;

            // Velocidad de la primera hoja (izquierda)
            Integer first = (int)(cnAperture*(wAperture*velocityA.get(i).getFirst() + c1Aperture*r1*( aux_G.getFirst() - A.get(i).getFirst() )  +  c2Aperture*r2*(aux_P.getFirst() - A.get(i).getFirst() ) ));

            // Velocidad de la segunda hoja (derecha)
            Integer second = (int)(cnAperture*(wAperture*velocityA.get(i).getSecond() + c1Aperture*r1*( aux_G.getSecond() - A.get(i).getSecond() )  +  c2Aperture*r2*(aux_P.getSecond() - A.get(i).getSecond() ) ));

            velocityA.set(i, new Pair(first, second));
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

    public void movAperture(){

        for(int i = 0; i < A.size(); i++){

            //Par <-2,-2> cerradas de forma definitiva
            if(A.get(i).getFirst() < -1)
                continue;

            int limit_inf = collimator.getActiveRange(i,angle).getFirst();
            int limit_sup = collimator.getActiveRange(i,angle).getSecond();

            int first = (velocityA.get(i).getFirst() + A.get(i).getFirst());
            int second = (velocityA.get(i).getSecond() + A.get(i).getSecond());

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

            Pair<Integer, Integer> newApertures = new Pair(first, second);
            A.set(i, newApertures);
        }
    }

    public void moveIntensity(double max_intensity){
        double val = getIntensity() + getVeloc_intensity();

        if(val > max_intensity){
            val = max_intensity;
        }else if(val < 0.0 ) {
            val = 0.0;
        }

        double newIntensity = ((double)Math.round(val*1000)/1000);
        setIntensity(newIntensity);
    }

    public boolean getProyectedBeamLet(int indexBeamlet){
        int counterLocal = 0;
        for(int i = 0; i < collimator.getxDim(); i++){

            Pair<Integer, Integer> x = collimator.getActiveRange(i, angle);
            Pair<Integer, Integer> positionApertures = A.get(i);

            if(x.getFirst() < 0 ) {
                continue;
            }
            for(int j = x.getFirst(); j <= x.getSecond(); j++){
                if(counterLocal == indexBeamlet){
                    if(j > positionApertures.getFirst() && j < positionApertures.getSecond() ){
                        return true;
                    }else{
                        return false;
                    }
                }
                counterLocal ++;
            }
        }
        if(counterLocal != collimator.getNangleBeamlets(angle)){
            System.out.println("ERROR: LA CANTIDAD DE BEAMLETS NO COINCIDE");
            System.out.println("CONTADOS: "+ counterLocal + " REGISTRADOS: "+ collimator.getNangleBeamlets(angle));
            System.exit(0);
        }
        return false;
    }
    /*---------------------------------------------------GETTERS AND SETTERS------------------------------------------------------------*/

    public Pair<Integer,Integer> getOpBeam(int i){
        return A.get(i);
    };

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public double getVeloc_intensity() {
        return veloc_intensity;
    }

    public void setVeloc_intensity(double veloc_intensity) {
        this.veloc_intensity = veloc_intensity;
    }

    public void setApertures(ArrayList<Pair<Integer,Integer>> Aperture){
        ArrayList<Pair<Integer,Integer>> newAper = new ArrayList<>();

        for(Pair<Integer,Integer> x: Aperture){
            Pair<Integer, Integer> newShape = new Pair(x.getFirst(), x.getSecond());
            newAper.add(newShape);
        }
        this.A = new ArrayList(newAper);
    }

    public void setVelocityA(ArrayList<Pair<Integer,Integer>> velocAperture){
        ArrayList<Pair<Integer,Integer>> newVelocity = new ArrayList<>();

        for(Pair<Integer,Integer> x: velocAperture){
            Pair<Integer,Integer> newShape = new Pair(x.getFirst(), x.getSecond());
            newVelocity.add(newShape);
        }
        this.velocityA = new ArrayList(newVelocity);
    }

    public ArrayList<Pair<Integer,Integer>> getApertures(){
        return A;
    }

    public void setOpenRow(int indexRow){
        Pair<Integer,Integer> limits = collimator.getActiveRange(indexRow, angle);
        if( limits.getFirst() > -1){
            Pair<Integer,Integer> row = A.get(indexRow);
            int firstLeft = limits.getFirst()-1;
            int secondLeft = limits.getSecond()+1;
            Pair<Integer, Integer> newRow = new Pair(firstLeft, secondLeft);
            A.set(indexRow, newRow);
        }

    }

}
