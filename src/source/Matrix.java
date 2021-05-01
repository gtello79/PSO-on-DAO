package source;
import SRCDAO.Beam;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Vector;

public class Matrix {
    private int rows;
    private int cols;
    public double [][]p;

    public Matrix(int x, int y){
        this.rows = x;
        this.cols = y;
        p = new double[x][y];
        for(int i = 0; i < x; i++){
            for(int j = 0; j < y; j++){
                p[i][j] = 0.0;
            }
        }
    }

    public void sumPos(int x, int y, int val){
        for(int i = 0; i < rows ; i++){
            if(i == x){
                for(int j = 0; j < cols; j++){
                    if(j == y){
                        p[x][y] +=val;
                        break;
                    }
                }
            break;
            }
        }
    }

    public void resPos(int x, int y, int val){
        for(int i = 0; i < rows ; i++){
            if(i == x){
                for(int j = 0; j < cols; j++){
                    if(j == y){
                        p[x][y] -=val;
                        break;
                    }
                }
            break;
            }
        }
    }

    public void setPos(int x, int y, double val){
        p[x][y] = val;
    }

    public double getPos(int x, int y){
        return p[x][y];
    }

    public void printMatrix(){
        for(int i = 0; i < rows ; i++){
            for(int j = 0; j < cols; j++){
                System.out.print(p[i][j] + " ");
            }
            System.out.println();
        }
    }


    public void printShape(){
        System.out.println("("+rows + "," + cols + ")");
    }

    //Metodo para exportar matriz
    public void exportMatrix(String dataFile){
        FileWriter data = null;
        PrintWriter pw = null;
        try{
            data = new FileWriter(dataFile+".txt");
            pw = new PrintWriter(data);
            for(int i = 0; i < rows ; i++){
                String sRows = "";
                for(int j = 0; j < cols; j++){
                    sRows += p[i][j] + " ";
                }
                sRows += "\n";
                pw.write(sRows);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            // Nuevamente aprovechamos el finally para
            // asegurarnos que se cierra el fichero.
            try {
                if (data != null)
                    data.close();
            }catch (Exception e2){
                e2.printStackTrace();
            }
        }
    }

    public int getX(){
        return this.cols;
    }

    public int getY(){
        return this.rows;
    }

}
