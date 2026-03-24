
public class Element {

    private  char bottom;
    private  char left;
    private  char right;
    private  char top;
    
    public Element(char [] c){

        this.top = c[0];
        this.right = c[1];
        this.bottom = c[2];
        this.left = c[3];
    }

    public static boolean check_corner(Element e){
        return (e.top == 'F' && e.right == 'F') ||
        (e.right == 'F' && e.bottom == 'F') ||
        (e.bottom == 'F' && e.left == 'F')||
        (e.left == 'F' && e.top == 'F');
    }

    public static boolean check_edge(Element e){
        return e.top == 'F' || e.right == 'F' ||
        e.left == 'F' && e.bottom == 'F';
    }

    public Element rotate_element(int times){

        Element rotated = this;
        for (int i = 0; i < times; i++) {
            rotated.rotate90();
        }
        return rotated;
    }

    //rotate element 90° clockwise
    private void rotate90(){

        char temp = this.right;
        this.right = this.top;
        this.top = this.left;
        this.left = this.bottom;
        this.bottom = temp;
    }

    /**
     * Checks if this element can be placed next to another element
     * @param neighbour The adjacent element
     * @param direction The direction of the neighbour (0=top, 1=right, 2=bottom, 3=left)
     */
    public boolean check_neighbour(Element neighbour, int direction) {
        
        switch (direction) {
            case 0:
                return areCompatible(this.top, neighbour.bottom);
                
            case 1:
                return areCompatible(this.right, neighbour.left);
                
            case 2:
                return areCompatible(this.bottom, neighbour.top);
                
            case 3:
                return areCompatible(this.left, neighbour.right);
                
            default:
                return false;
        }
    }
    
    private boolean areCompatible(char side1, char side2) {

        return (side1 == 'F' && side2 == 'F') || 
        (side1 == 'B' && side2 == 'P') || 
        (side1 == 'P' && side2 == 'B');
        
    }

    public char getTop() { return top; }
    public void setTop(char top) { this.top = top; }
    public char getBottom() { return bottom; }
    public void setBottom(char bottom) { this.bottom = bottom; }
    public char getLeft() { return left; }
    public void setLeft(char left) { this.left = left; }
    public char getRight() { return right; }
    public void setRight(char right) { this.right = right; }
    

}