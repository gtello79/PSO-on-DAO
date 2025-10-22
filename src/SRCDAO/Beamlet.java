package SRCDAO;

import javafx.util.Pair;

public class Beamlet {
    private int id;
    private int localId;
    private int angle;
    private Pair<Integer, Integer> position;
    private Pair<Double, Double> realPosition;

    private boolean usedInIdeal;

    public Beamlet(int id, int localId, int angle, Pair<Integer, Integer> position, Pair<Double, Double> realPosition) {
        this.id = id;
        this.localId = localId;
        this.angle = angle;
        this.position = position;
        this.realPosition = realPosition;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public Pair<Integer, Integer> getPosition() {
        return position;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isUsedInIdeal() {
        return usedInIdeal;
    }

    public void setUsedInIdeal(boolean usedInIdeal) {
        this.usedInIdeal = usedInIdeal;
    }

    // Genera un metodo que permita reconocer cuando dos beamlets son iguales a
    // partir de su posicion
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof Beamlet))
            return false;

        Beamlet other = (Beamlet) obj;

        boolean positionVal = this.realPosition.getKey().compareTo(other.realPosition.getKey()) == 0 &&
                this.realPosition.getValue().compareTo(other.realPosition.getValue()) == 0;

        return positionVal;
    }

}
