package SRCDAO;

import javafx.util.Pair;

public class Beamlet {
    private int id;
    private int localId;
    private int angle;
    private Pair<Integer, Integer> position;

    public Beamlet(int id, int localId, int angle, Pair<Integer, Integer> position) {
        this.id = id;
        this.localId = localId;
        this.angle = angle;
        this.position = position;

    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public double getAngle() {
        return angle;
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
}
