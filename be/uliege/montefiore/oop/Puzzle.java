// Required package declaration — see Element.java for explanation.
package be.uliege.montefiore.oop;

// ArrayList is needed to store the list of all pieces read from the file.
import java.util.ArrayList;

// =============================================================================
// Puzzle — the central state object AND the program entry point.
//
// ─── PURPOSE OF EACH CLASS IN THIS PROJECT ────────────────────────────────────
//
//  Element         One puzzle piece. Knows its 4 sides (F/B/P), can rotate.
//                  → Think of it as a physical cardboard piece in your hand.
//
//  Placement       A tiny record: "piece #i, rotated r times".
//                  Stored in the grid once a piece is successfully placed.
//                  → Think of it as a sticky note on a board square.
//
//  Puzzle  ◄ YOU ARE HERE
//                  The shared state container. Holds:
//                    - list  : all pieces read from the file (never changes)
//                    - grid  : the 2-D solution grid (filled by the solver)
//                    - used_e: which pieces have already been placed
//                  Also holds main() — the program's entry point.
//                  → Think of it as the puzzle box + the table you're working on.
//
//  Functions       All algorithm logic: file reading + backtracking solver.
//                  It has NO fields of its own — it receives a Puzzle and modifies it.
//                  → Think of it as the rule book + your hands doing the solving.
//
//  PuzzleDisplay   Optional Swing window that draws the solved puzzle graphically.
//                  Only created when --display is passed on the command line.
//                  → Think of it as a camera that photographs the finished puzzle.
//
// ─── EXECUTION FLOW ───────────────────────────────────────────────────────────
//
//  main()
//    ├─ Functions.Read_doc()     reads the .pzl file → fills puzzle.list
//    ├─ puzzle.initialize()      allocates grid[][] and used_e[]
//    ├─ Functions.solve_puzzle() fills grid[][] via two-phase backtracking
//    │     ├─ fill_outside()     places corners + edge pieces on the border
//    │     └─ fill_inside()      places interior pieces
//    ├─ print solution           loops over grid, prints index+rotation per spec
//    └─ PuzzleDisplay.show()     (optional) opens the graphical window
//
// =============================================================================
public class Puzzle {

    // -------------------------------------------------------------------------
    // width, height — dimensions of the assembled rectangle, in number of pieces.
    //   e.g. a 4x5 puzzle has width=4 columns and height=5 rows → 20 pieces total.
    //   Set by Read_doc when it parses the first line of the .pzl file.
    // -------------------------------------------------------------------------
    private int width;
    private int height;

    // -------------------------------------------------------------------------
    // list — the original pieces exactly as read from the file, in file order.
    //   Index 0 = first piece in file, index 1 = second piece, etc.
    //   NEVER modified after Read_doc fills it — the solver only reads from it.
    // -------------------------------------------------------------------------
    private ArrayList<Element> list;

    // -------------------------------------------------------------------------
    // grid — the 2-D solution board, indexed as grid[row][col].
    //   grid[0][0] = top-left cell, grid[height-1][width-1] = bottom-right cell.
    //   Each cell holds a Placement (which piece, which rotation) once filled,
    //   or null if the cell hasn't been filled yet (or was backtracked).
    //   Allocated in initialize() once we know the dimensions.
    // -------------------------------------------------------------------------
    private Placement[][] grid;

    // -------------------------------------------------------------------------
    // used_e — boolean array, one entry per piece.
    //   used_e[i] = true  → piece i is already placed somewhere in the grid.
    //   used_e[i] = false → piece i is still available for the solver to try.
    //   The solver sets/clears this during backtracking to avoid placing the
    //   same piece twice.
    //   Allocated in initialize() once we know the piece count.
    // -------------------------------------------------------------------------
    private boolean[] used_e;

    // -------------------------------------------------------------------------
    // Constructor — called once in main() before reading the file.
    // We pass (0, 0, null) because we don't know width, height or list yet;
    // those come from Read_doc. grid and used_e are left null on purpose and
    // allocated later by initialize().
    // -------------------------------------------------------------------------
    public Puzzle(int width, int height, ArrayList<Element> listElements) {
        this.width  = width;
        this.height = height;
        this.list   = listElements;
    }

    // -------------------------------------------------------------------------
    // initialize() — allocates grid and used_e with the correct sizes.
    //
    // Must be called AFTER Read_doc (which sets width, height and list) and
    // BEFORE solve_puzzle (which writes into grid and used_e).
    //
    // BUG FIXED: in the original code these arrays were never allocated, so
    // any access threw NullPointerException at runtime.
    // -------------------------------------------------------------------------
    public void initialize() {
        // height rows, each with width columns — matches the physical grid
        this.grid   = new Placement[this.height][this.width];
        // one boolean per piece; Java initialises boolean[] to all false
        this.used_e = new boolean[this.list.size()];
    }

    // -------------------------------------------------------------------------
    // used_e accessors — let Functions mark/unmark pieces during backtracking.
    // -------------------------------------------------------------------------
    public void    setUsed(int i, boolean b) { this.used_e[i] = b; }
    public boolean getUsed(int i)            { return used_e[i]; }

    // -------------------------------------------------------------------------
    // grid accessors — let Functions read/write the solution grid.
    // Passing null to setPlacement clears a cell (used during backtracking).
    // -------------------------------------------------------------------------
    public void      setPlacement(Placement p, int row, int col) { this.grid[row][col] = p; }
    public Placement getPlacement(int row, int col)              { return this.grid[row][col]; }

    // -------------------------------------------------------------------------
    // Dimension accessors — used throughout Functions and PuzzleDisplay.
    // -------------------------------------------------------------------------
    public int  getWidth()           { return this.width; }
    public void setWidth(int width)  { this.width = width; }

    public int  getHeight()          { return this.height; }
    public void setHeight(int height){ this.height = height; }

    // -------------------------------------------------------------------------
    // List accessors — Read_doc calls setList() to hand over the parsed pieces;
    // everyone else calls getList() to read them.
    // -------------------------------------------------------------------------
    public ArrayList<Element> getList()               { return this.list; }
    public void setList(ArrayList<Element> list)      { this.list = list; }

    // -------------------------------------------------------------------------
    // main() — the program entry point. Orchestrates the whole pipeline:
    //   1. validate arguments
    //   2. read the puzzle file
    //   3. initialise the state arrays
    //   4. run the solver
    //   5. print the solution (or an error)
    //   6. optionally open the graphical display
    // -------------------------------------------------------------------------
    public static void main(String[] args) {

        // Step 1 — validate command-line arguments.
        // The spec says the program takes exactly one argument: the filename.
        // BUG FIXED: the original code accessed args[0] without this check,
        // throwing ArrayIndexOutOfBoundsException when called with no arguments.
        if (args.length < 1) {
            System.err.println("Usage: java be.uliege.montefiore.oop.Puzzle <puzzle_file>");
            return;
        }
        String filename = args[0];

        // Step 2 — create an empty Puzzle object, then let Read_doc fill it.
        // We start with (0, 0, null) because we don't know anything yet.
        Puzzle puzzle = new Puzzle(0, 0, null);
        if (!Functions.Read_doc(filename, puzzle)) {
            // Read_doc already printed the specific error message; just exit.
            return;
        }

        // Step 3 — now that Read_doc has set width, height and list,
        // we can allocate the grid and the used_e tracking array.
        puzzle.initialize();

        // Step 4 — check for the optional --display flag BEFORE solving,
        // so we know whether to open the window after a successful solve.
        // The spec requires text-only output for grading, so the GUI is
        // always opt-in and never replaces the required console output.
        boolean showDisplay = args.length >= 2 && args[1].equals("--display");

        // Step 5 — run the solver.
        if (Functions.solve_puzzle(puzzle)) {

            // Solution found: print it row by row, left to right within each row.
            // Format per spec: "<1-based index> <rotation>" on each line.
            // We add 1 to p.getIndex() because Placement stores 0-based indices
            // but the spec numbers pieces starting at 1.
            for (int row = 0; row < puzzle.getHeight(); row++) {
                for (int col = 0; col < puzzle.getWidth(); col++) {
                    Placement p = puzzle.getPlacement(row, col);
                    System.out.println((p.getIndex() + 1) + " " + p.getRotation());
                }
            }

            // Step 6 — open the graphical window if requested.
            // PuzzleDisplay.show() internally uses SwingUtilities.invokeLater
            // to open the window on the correct thread, so it is safe to call here.
            if (showDisplay) {
                PuzzleDisplay.show(puzzle);
            }

        } else {
            // No solution exists for this puzzle — print an error to stderr.
            System.err.println("No solution found for: " + filename);
        }
    }
}
