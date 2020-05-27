package SRCDAO;
import java.util.ArrayList;
import java.util.Vector;

public class Plan {
    private double eval = 0;
    private ArrayList<Beam> Angle_beam;
    private int n_beam;
    private Vector<Double> w;
    private Vector<Double> Zmin;
    private Vector<Double> Zmax;
    private int max_apertures;
    private int max_intensity;
    private int initial_intensity;
    private int step_intensity;
    private int open_apertures;
    private int setup;

    public Plan(Vector<Double> w, Vector<Double> Zmin, Vector<Double> Zmax, int max_apertures, int max_intensity, int initial_intensity, int step_intensity, int open_apertures, int setup){
        Angle_beam = new ArrayList<Beam>();
        System.out.println("##Initilizing plan.");
        /*aqui se deben cargar las instancias*/
        this.w = w;
        this.Zmin = Zmin;
        this.Zmax = Zmax;
        this.max_apertures = max_apertures;
        this.max_intensity = max_intensity;
        this.initial_intensity = initial_intensity;
        this.step_intensity = step_intensity;
        this.open_apertures = open_apertures;
        this.setup = setup;

        for(int i = 0; i < n_beam ; i++){
            //Cambiar el iterador i por el beam que corresponde
            Beam new_beam = new Beam(i, max_apertures, max_intensity, initial_intensity, step_intensity, open_apertures, setup);
            Angle_beam.add(new_beam);
        }
    }

    public void CalculateVelocity(double c1, double c2, double w, Plan Bsolution, Plan Bpersonal){
        for (Beam actual: Angle_beam){
            Beam B_Bsolution = getByID(actual.getId_beam());
            Beam B_BPersonal = getByID(actual.getId_beam());
            actual.CalculateVelocity(c1 ,c2 ,w ,B_Bsolution ,B_BPersonal);
        }
    }

    public void CalculatePosition(){
        for (Beam actual: Angle_beam){
            actual.CalculatePosition();
        }
    }

    public Beam getByID(int to_search){
        for(Beam pivote: Angle_beam){
            if(pivote.getId_beam() == to_search){
                return pivote;
            }
        }
        return null;
    }

    public double getEval() {
        return eval;
    }

    public void setEval(double eval) {
        this.eval = eval;
    }

    public ArrayList<Beam> getBeams(){
        return Angle_beam;
    }

}
