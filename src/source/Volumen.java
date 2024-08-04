package source;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Volumen {
    // Nombre del cuerpo descrito
    private String data;
    // Tiene dimension voxels x beamlets, contiene la DDM del organo
    private Matrix DDM;
    // Tiene asociado el numero de voxels asociado al organo
    private int nbVoxels;
    // Tiene el numero de beamlets que influyen en el organo
    private int nb_beamlets;

    //
    private Hashtable<Integer, ArrayList<Integer>> indexDAODDM;

    //
    private Hashtable<String, Double> valueDAODDM;

    //Constructor del volumen asociado
    public Volumen(String data_path) throws FileNotFoundException {
        System.out.println("DATA: " + data_path);
        this.indexDAODDM = new Hashtable<>();
        this.valueDAODDM = new Hashtable<>();
        this.data = data_path;
        this.nb_beamlets = 0;
        this.nbVoxels = 0;

        this.setData(data_path);
    }

    public Volumen(Volumen v){

        this.data = v.data;
        this.DDM = new Matrix(v.getDDM());

        this.nbVoxels = v.getNb_voxels();
        this.nb_beamlets = v.getNb_beamlets();

        this.indexDAODDM = v.getIndexDAODDM();
        this.valueDAODDM = v.getValueDAODDM();
    }

    private void setData(String data) throws FileNotFoundException {
        Vector<String> lines = new Vector<>();
        File cordFile = new File(data);
        
        if(!cordFile.exists()){
            throw new FileNotFoundException("FILE "+ data + " DON'T FOUND");
        }
        //Lectura de archivo data
        Scanner reader = new Scanner(cordFile);
            
        //Lee cada linea del .dat asociasdo
        int count = 0;
        while (reader.hasNextLine()){
            if(count == 0) {
                //No considera la primera linea, solo la cantidad de beamlets
                String bladderDates = reader.nextLine();
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


        //Total de Voxels asociados al organo/tumor
        this.nbVoxels = lines.size()-1;

        //Se crea la DDM inicializada en 0
        this.DDM = new Matrix(this.nbVoxels, this.nb_beamlets);

        //Recorre por voxel(fila) y luego por beamLet(columna)
        for(int i = 0; i < this.nbVoxels; i++){

            ArrayList<Integer> affectionBeamLets = new ArrayList<>();
            String[] dosesVector = lines.get(i).split("\t");

            //Rellena para cada beamLet i
            for(int j = 0; j < this.nb_beamlets; j++){
                double dosesDelivered = Double.parseDouble(dosesVector[j+1]);
                this.DDM.setPos(i,j,dosesDelivered);

                if(dosesDelivered > 0.0){
                    affectionBeamLets.add(j);
                }
                String indexKey = i + "-" + j;
                this.valueDAODDM.put(indexKey, dosesDelivered);
            }
            this.indexDAODDM.put(i, affectionBeamLets);
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

    public Hashtable<Integer, ArrayList<Integer>> getIndexDAODDM(){
        return this.indexDAODDM;
    }

    public Hashtable<String, Double> getValueDAODDM() {
        return this.valueDAODDM;
    }

}
