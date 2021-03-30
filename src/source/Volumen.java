package source;
import javax.swing.text.MutableAttributeSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Volumen {
    /* Information of the general beamlet configuration */
    private Collimator collimator;
    /*D[a](k,b): Dose delivered to voxel k of the organ by the beamlet b and angle a */
    private SortedMap <Integer, Matrix> D;
    //Tiene dimension voxels x beamlets, contiene la DDM del organo
    private Matrix DDM;
    // Tiene asociado el numero de voxels asociado al organo
    private int nb_voxels;
    // Tiene el numero de beamlets que influyen en el organo
    private int nb_beamlets;

    //Constructor del volumen asociado
    public Volumen(Collimator collimator,String data) throws FileNotFoundException {
        D = new TreeMap<>();
        this.nb_beamlets = collimator.getNbBeamlets();
        this.collimator = collimator;
        set_data(data);
    }

    //Loading DDM using angles of a angle
    private void set_data(String data) throws FileNotFoundException {
        File coord = new File("src/"+data);
        String bladder_dates = null;
        int count = 0;
        nb_voxels=0;

        if(!coord.exists()){
            System.out.println("NO SE ENCUENTRA EL ARCHIVO "+data);
        }else {
            //Accede a la lectura de la DDM para un tejido
            Scanner lectura = new Scanner(coord);
            Vector<String> lines = new Vector<>();
            //Lee linea por linea del .dat
            while (lectura.hasNextLine()) {
                if(count == 0){
                    //No almacena la primera linea del archivo DDM, solo conserva la cantidad de beams del volumen
                    //Cantidad de beams en esta ddm
                    bladder_dates = lectura.nextLine();
                    String[] ArrayLinea = bladder_dates.split("\t");
                    String nbeams = ArrayLinea[ArrayLinea.length-1];
                    nbeams = nbeams.split(" ")[0];

                    this.nb_beamlets = Integer.parseInt(nbeams);

                }else{
                    //Almacena cada linea correspondiente a la informacion de los voxels
                    lines.add(lectura.nextLine());
                }
                count++;
            }
            lectura.close();

            //Cantidad total de voxels
            nb_voxels = lines.size()-1;

            //Se crea una matriz para cada angulo de la actual DDM
            for(Integer angle: collimator.getAngles())
                D.put(angle, new Matrix( nb_voxels , collimator.getNangleBeamlets(angle) ) );
            this.DDM = new Matrix(nb_voxels,nb_beamlets);

            //Recorre por voxel(fila) y luego por beamlet (columna)
            for(int i = 0; i < nb_voxels; i++){
                int a = 0; //angulo
                int j = 0;
                int k = 0;
                boolean nb_flag = false;
                String actual_voxels = lines.get(i);
                String [] ArrayLinea = actual_voxels.split("\t");

                // Segmenta x beam el impacto de la intensidad en cada voxel
                for(String actual: ArrayLinea){
                    //Se omite el primer valor (ID VOXEL)
                    if(nb_flag){
                        //Contador de beamlets x angulos
                        //Si j supera la cantidad, setear para seguir con el siguiente beam
                        if(j >= collimator.getNangleBeamlets(collimator.getAngle(a))){
                            a++;
                            j=0;
                        }
                        double toAdd = Double.parseDouble(actual);
                        (D.get(collimator.getAngle(a))).setPos(i,j,toAdd);
                        DDM.setPos(i,k,toAdd);
                        j++;
                        k++;
                    }else{
                        nb_flag=true;
                    }

                }
            }
        }
    }

    public int getNb_voxels(){
        return nb_voxels;
    }

    public Matrix getDepositionMatrix(int angle){
        return D.get(angle);
    }

    public Matrix getDDM(){
        return DDM;
    }

    public int getNb_beamlets(){
        return this.nb_beamlets;
    }
    //------------------------------------------------------------------- PRINTERS -------------------------------------------------
    public void printNbbeamlets(){
        for(Integer angle: collimator.getAngles()){
            System.out.println("ANGLE: "+ angle + " - "+collimator.getNangleBeamlets(angle));
        }
    }

    public void printD(){
        for(Integer angle: collimator.getAngles()){
            System.out.println("ANGLE: " +angle);
            Matrix x = D.get(angle);
            x.printMatrix();
        }
    }

}
