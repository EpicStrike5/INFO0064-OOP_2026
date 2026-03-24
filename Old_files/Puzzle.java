import java.util.ArrayList;

public class Puzzle {

    private int width;
    private int height;
    private ArrayList<Element> list;
    private Placement [][] grid;
    private boolean [] used_e;


    public Puzzle(int width, int height, ArrayList<Element> ListElements){

        this.width = width;
        this.height = height;
        this.list = ListElements;
    }
    
    public void setUsed(int i, boolean b){ this.used_e[i] = b; }
    public boolean getUsed(int i){ return used_e[i]; }

    public void setPlacement(Placement p, int i, int j){ this.grid[i][j] = p; }
    public Placement getPlacement(int i, int j){ return this.grid[i][j]; }

    public int getWidth() { return this.width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return this.height; }
    public void setHeight(int height) { this.height = height; }

    public ArrayList<Element> getList() { return this.list; }
    public void setList(ArrayList<Element> list) { this.list = list; }

    public static void main(String args[]) {

        String filename = args[0];

        Puzzle puzzle = new Puzzle(0, 0, null);
        if(Functions.Read_doc(filename, puzzle)){

            int size = puzzle.getHeight()*puzzle.getWidth();
            for (int i = 0; i < size; i++) {
                System.out.print(puzzle.getList().get(i).getTop());
                System.out.print(puzzle.getList().get(i).getRight());
                System.out.print(puzzle.getList().get(i).getBottom());
                System.out.print(puzzle.getList().get(i).getLeft());
                System.out.println();
            }
            if(Functions.solve_puzzle(puzzle)){
                System.out.println("Puzzle solved");
            }
        }
        
    }

}