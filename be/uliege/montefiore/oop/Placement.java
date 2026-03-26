package be.uliege.montefiore.oop;

// stores what's in one grid cell: which piece (0-based index) and how many times it was rotated
public class Placement {

    private int index;
    private int rotation;

    public Placement(int index, int rotation) {
        this.index    = index;
        this.rotation = rotation;
    }

    public int  getIndex()                { return this.index; }
    public void setIndex(int index)       { this.index = index; }

    public int  getRotation()             { return this.rotation; }
    public void setRotation(int rotation) { this.rotation = rotation; }
}
