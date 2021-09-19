package source;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Volumen {

    //Tiene dimension voxels x beamlets, contiene la DDM del organo
    private Matrix DDM;
    // Tiene asociado el numero de voxels asociado al organo
    private int nbVoxels;
    // Tiene el numero de beamlets que influyen en el organo
    private int nb_beamlets;


    //Constructor del volumen asociado
    public Volumen(String data) throws FileNotFoundException {
        this.nb_beamlets = 0;
        this.nbVoxels = 0;

        this.setData(data);
    }

    //set data
    private void setData(String data) throws FileNotFoundException {
        File cordFile = new File("src/"+data);
        String bladderDates = null;
        int count = 0;
        Vector<String> lines = new Vector<>();
        if(!cordFile.exists()){
            System.out.println("FILE "+ data + " DON'T FOUND");
        }else{
            //Lectura de archivo data
            Scanner reader = new Scanner(cordFile);

            //Lee cada linea del .dat asociasdo
            while (reader.hasNextLine()){
                if(count == 0) {
                    //No considera la primera linea, solo la cantidad de beamlets
                    bladderDates = reader.nextLine();
                    String[] arrayLine = bladderDates.split("\t");
                    String nBeamlets = arrayLine[arrayLine.length - 1];
                    nBeamlets = nBeamlets.split(" ")[0];

                    this.nb_beamlets = Integer.parseInt(nBeamlets);

                }else{
                    //Almacena cada linea correspondiente a la informacion de los voxels
                    lines.add(reader.nextLine());
                }
                count+=1;
            }
            reader.close();
        }

        //Total de Voxels asociados al organo/tumor
        this.nbVoxels = lines.size()-1;

        //Se crea la DDM inicializada en 0
        this.DDM = new Matrix(this.nbVoxels, this.nb_beamlets);

        //Recorre por voxel(fila) y luego por beamLet(columna)
        for(int i = 0; i < this.nbVoxels; i++){

            String arrayLine = lines.get(i);
            String [] dosesVector = arrayLine.split("\t");

            //Rellena para cada beamLet i
            for(int j = 0; j < this.nb_beamlets; j++){
                double dosesDelivered = Double.parseDouble(dosesVector[j+1]);
                this.DDM.setPos(i,j,dosesDelivered);
            }
        }
    }

    public int getNb_voxels(){
        return nbVoxels;
    }

    public Matrix getDDM(){
        return DDM;
    }

    public int getNb_beamlets(){
        return this.nb_beamlets;
    }
    //------------------------------------------------------------------- PRINTERS -------------------------------------------------

}
