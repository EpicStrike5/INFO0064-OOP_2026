package be.uliege.montefiore.oop;

import java.util.ArrayList;

public class Puzzle {

    private int width;
    private int height;

    // original pieces as read from the file, never modified after loading
    private ArrayList<Element> list;

    // solution grid: grid[row][col] = which piece + rotation, null if empty
    private Placement[][] grid;

    // tracks which pieces are already placed (used during backtracking)
    private boolean[] used_e;

    // best partial grid snapshot — captured whenever the solver reaches a new depth record
    private Placement[][] bestGrid;
    private int bestCount   = 0;
    private int placedCount = 0;

    public Puzzle(int width, int height, ArrayList<Element> listElements) {
        this.width  = width;
        this.height = height;
        this.list   = listElements;
    }

    // allocates grid and used_e — must be called after Read_doc, before solve_puzzle
    public void initialize() {
        this.grid        = new Placement[this.height][this.width];
        this.used_e      = new boolean[this.list.size()];
        this.bestGrid    = null;
        this.bestCount   = 0;
        this.placedCount = 0;
    }

    public void    setUsed(int i, boolean b) { this.used_e[i] = b; }
    public boolean getUsed(int i)            { return used_e[i]; }

    // passing null clears the cell (backtracking)
    public void      setPlacement(Placement p, int row, int col) { this.grid[row][col] = p; }
    public Placement getPlacement(int row, int col)              { return this.grid[row][col]; }

    // called by the solver after each placement / unplacement
    public void incrementPlaced() { placedCount++; }
    public void decrementPlaced() { placedCount--; }

    // snapshots the grid whenever we reach a new maximum number of placed pieces
    public void tryUpdateBest() {
        if (placedCount > bestCount) {
            bestCount = placedCount;
            bestGrid = new Placement[height][width];
            for (int r = 0; r < height; r++)
                for (int c = 0; c < width; c++)
                    bestGrid[r][c] = grid[r][c];
        }
    }

    // restores the deepest partial state reached during backtracking
    public void restoreBest() {
        if (bestGrid == null) return;
        for (int r = 0; r < height; r++)
            for (int c = 0; c < width; c++)
                grid[r][c] = bestGrid[r][c];
        // rebuild used_e to match the restored grid
        for (int i = 0; i < used_e.length; i++) used_e[i] = false;
        for (int r = 0; r < height; r++)
            for (int c = 0; c < width; c++)
                if (grid[r][c] != null) used_e[grid[r][c].getIndex()] = true;
    }

    public int  getWidth()           { return this.width; }
    public void setWidth(int width)  { this.width = width; }

    public int  getHeight()          { return this.height; }
    public void setHeight(int height){ this.height = height; }

    public ArrayList<Element> getList()               { return this.list; }
    public void setList(ArrayList<Element> list)      { this.list = list; }

    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("Usage: java be.uliege.montefiore.oop.Puzzle <puzzle_file>");
            return;
        }
        String filename = args[0];

        Puzzle puzzle = new Puzzle(0, 0, null);
        if (!Functions.Read_doc(filename, puzzle)) {
            return;
        }

        puzzle.initialize();

        boolean showDisplay = args.length >= 2 && args[1].equals("--display");

        if (Functions.solve_puzzle(puzzle)) {

            // print solution: "1-based index  rotation" for each cell, row by row
            for (int row = 0; row < puzzle.getHeight(); row++) {
                for (int col = 0; col < puzzle.getWidth(); col++) {
                    Placement p = puzzle.getPlacement(row, col);
                    System.out.println((p.getIndex() + 1) + " " + p.getRotation());
                }
            }
            if (showDisplay) PuzzleDisplay.show(puzzle);

        } else {
            System.err.println("No solution found for: " + filename);
            // still open the display to show the best partial state + unplaced pieces
            if (showDisplay) PuzzleDisplay.show(puzzle);
        }
    }
}
