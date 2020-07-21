package source;

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
}
