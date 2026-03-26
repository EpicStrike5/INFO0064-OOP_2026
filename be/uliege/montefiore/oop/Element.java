package be.uliege.montefiore.oop;

// represents a single puzzle piece (4 sides: F=flat, B=bump, P=pit)
// sides stored clockwise: top, right, bottom, left
public class Element {

    private char top;
    private char right;
    private char bottom;
    private char left;

    // expects array in clockwise order: [top, right, bottom, left]
    public Element(char[] c) {
        this.top    = c[0];
        this.right  = c[1];
        this.bottom = c[2];
        this.left   = c[3];
    }

    // corner = exactly 2 adjacent flat sides
    public static boolean check_corner(Element e) {
        return (e.top == 'F' && e.right  == 'F') ||
               (e.right  == 'F' && e.bottom == 'F') ||
               (e.bottom == 'F' && e.left   == 'F') ||
               (e.left   == 'F' && e.top    == 'F');
    }

    // true if at least one side is flat
    public static boolean check_edge(Element e) {
        return e.top    == 'F' ||
               e.right  == 'F' ||
               e.bottom == 'F' ||
               e.left   == 'F';
    }

    // returns a new rotated copy, never modifies the original
    public Element rotate_element(int times) {
        char[] copy = { this.top, this.right, this.bottom, this.left };
        Element rotated = new Element(copy);
        for (int i = 0; i < times; i++) {
            rotated.rotate90();
        }
        return rotated;
    }

    // single 90-degree clockwise rotation in-place
    private void rotate90() {
        char temp   = this.right;
        this.right  = this.top;
        this.top    = this.left;
        this.left   = this.bottom;
        this.bottom = temp;
    }

    // direction: 0=above, 1=right, 2=below, 3=left
    public boolean check_neighbour(Element neighbour, int direction) {
        switch (direction) {
            case 0: return areCompatible(this.top,    neighbour.bottom);
            case 1: return areCompatible(this.right,  neighbour.left);
            case 2: return areCompatible(this.bottom, neighbour.top);
            case 3: return areCompatible(this.left,   neighbour.right);
            default: return false;
        }
    }

    // valid pairs: F-F, B-P, P-B
    private boolean areCompatible(char side1, char side2) {
        return (side1 == 'F' && side2 == 'F') ||
               (side1 == 'B' && side2 == 'P') ||
               (side1 == 'P' && side2 == 'B');
    }

    public char getTop()               { return top; }
    public void setTop(char top)       { this.top = top; }

    public char getRight()             { return right; }
    public void setRight(char right)   { this.right = right; }

    public char getBottom()            { return bottom; }
    public void setBottom(char bottom) { this.bottom = bottom; }

    public char getLeft()              { return left; }
    public void setLeft(char left)     { this.left = left; }
}
