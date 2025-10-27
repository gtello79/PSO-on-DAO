package SRCDAO;

public enum ApertureEnum {
    OPEN_MIN_SETUP(0),
    OPEN_MAX_SETUP(1),
    CLOSED_MIN_SETUP(2),
    CLOSED_MAX_SETUP(3),
    RAND_RAND_SETUP(4),
    STATIC_SETUP(5);

    private final int value;

    ApertureEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}