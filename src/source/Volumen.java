package source;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Volumen {
    /* Information of the general beamlet configuration */
    private Collimator collimator;

    /*D[a](k,b): Dose delivered to voxel k of the organ by the beamlet b and angle a */
    private SortedMap <Integer, Matrix> D = new TreeMap<>();
    private int nb_voxels;

    public Volumen(Collimator  collimator,String data) throws FileNotFoundException {
        this.collimator = collimator;
            set_data(data);
    }

    private void set_data(String data) throws FileNotFoundException {
        String line;
        File coord = new File("src/"+data);
        String ss;
        String bladder_dates = null;
        int count = 0;
        nb_voxels=-1;
        if(!coord.exists()){
            System.out.println("NO SE ENCUENTRA EL ARCHIVO "+data);
        }else {
            Scanner lectura = new Scanner(coord);
            Vector<String> lines = new Vector<>();
            while (lectura.hasNextLine()) {
                if(count == 0){
                    bladder_dates = lectura.nextLine();
                }else{
                    lines.add(lectura.nextLine());
                }
                count++;
            }

            lectura.close();
            nb_voxels = lines.size()-1;

            for(Integer angle: collimator.getAngles()){
                D.put(angle, new Matrix( nb_voxels , collimator.getNangleBeamlets(angle) ) );
            }
            for(int i = 0; i < nb_voxels; i++){
                int a = 0;
                int j = 0;
                boolean nb_flag = false;
                String actual_voxels = lines.get(i);
                String [] ArrayLinea = actual_voxels.split("\t");

                for(String actual: ArrayLinea){
                    if(nb_flag){
                        if(j >= collimator.getNangleBeamlets(collimator.getAngle(a))){
                            a++;
                            j=0;
                        }
                        double toAdd = Double.parseDouble(actual);
                        (D.get(collimator.getAngle(a))).setPos(i,j,toAdd);
                        j++;
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
}
