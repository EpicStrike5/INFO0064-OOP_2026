// Required package declaration — see Element.java for explanation.
package be.uliege.montefiore.oop;

// java.io.* gives us FileReader, BufferedReader, FileNotFoundException, IOException
// — everything needed to open and read a text file line by line.
import java.io.*;
// ArrayList is the resizable list we use to collect pieces while reading.
import java.util.ArrayList;
// List is the interface used for the borderCells parameter (more flexible than ArrayList).
import java.util.List;

// =============================================================================
// Functions — contains ALL the algorithm logic. No state, only static methods.
//
// It has two responsibilities:
//   1. READ   : Read_doc() parses the .pzl file and populates a Puzzle object.
//   2. SOLVE  : solve_puzzle() fills the puzzle grid using a two-phase backtracking.
//
// Why no fields?
//   Functions modifies the Puzzle object it receives. All solver state (the grid,
//   the used[] flags) lives inside Puzzle. This makes the code easier to reason
//   about — there are no hidden static variables accumulating between calls.
//
// BUG REMOVED: the original class had static fields `grid`, `edge`, `corner`,
//   `inside` that were never initialised (→ NullPointerException on first use)
//   and were never reset between calls. They have been converted to local
//   variables inside the methods where they are needed.
// =============================================================================
public class Functions {

    // =========================================================================
    // FILE READING
    // =========================================================================

    // -------------------------------------------------------------------------
    // Read_doc — opens the puzzle specification file and fills the Puzzle object.
    //
    // File format (from the spec):
    //   Line 1 :  "width height"          e.g.  "4 5"
    //   Lines 2…: one piece per line, 4 chars    e.g.  "FBBP"
    //             in clockwise order: top, right, bottom, left.
    //
    // Returns true on success, false on any error (file not found, bad format,
    // wrong number of pieces, invalid characters). Error details go to stderr.
    // -------------------------------------------------------------------------
    public static boolean Read_doc(String filename, Puzzle puzzle) {

        // Accumulate pieces here before handing them to the Puzzle object.
        ArrayList<Element> pieceList = new ArrayList<>();

        // try-with-resources: the BufferedReader is automatically closed when
        // the block exits (whether normally or via exception).
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            // --- Read the first line: "width height" ---
            String[] size  = reader.readLine().split(" ");
            int width      = Integer.parseInt(size[0]);
            int height     = Integer.parseInt(size[1]);
            int nb_lines   = width * height; // total pieces expected
            puzzle.setWidth(width);
            puzzle.setHeight(height);

            // --- Read one piece per subsequent line ---
            // tableCaracteres is reused each iteration; Element copies the values
            // (not the array reference), so reuse is safe.
            char[] tableCaracteres = new char[4];
            String line;

            // Stop as soon as we have all expected pieces OR reach end of file.
            while ((line = reader.readLine()) != null && pieceList.size() < nb_lines) {

                if (line.length() >= 4) {

                    // Read the 4 side characters and validate each one.
                    for (int j = 0; j < 4; j++) {
                        tableCaracteres[j] = line.charAt(j);
                        if (tableCaracteres[j] != 'F' && tableCaracteres[j] != 'P'
                                && tableCaracteres[j] != 'B') {
                            System.err.println("Incorrect character in file: " + filename);
                            return false;
                        }
                    }
                    // Create the piece and add it to the list.
                    pieceList.add(new Element(tableCaracteres));

                } else {
                    // A line shorter than 4 characters is a format error.
                    System.err.println("File has incorrect format (line too short): " + filename);
                    return false;
                }
            }

            // --- Verify we got the right number of pieces ---
            if (pieceList.size() < nb_lines) {
                System.err.println("Not enough pieces: expected " + nb_lines
                        + ", got " + pieceList.size() + " in " + filename);
                return false;
            }

            // --- Hand the completed list to the Puzzle ---
            puzzle.setList(pieceList);

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filename);
            return false;
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
            return false;
        }

        return true; // everything went well
    }


    // =========================================================================
    // MAIN SOLVER ENTRY POINT
    // =========================================================================

    // -------------------------------------------------------------------------
    // solve_puzzle — orchestrates the two-phase backtracking solver.
    //
    // Phase 0 — Pre-checks (fast):
    //   Classify pieces into corner / edge / inside categories and verify
    //   that enough of each type exist. If not, fail immediately without
    //   running the expensive backtracking.
    //
    // Phase 1 — fill_outside:
    //   Place the 4 corner pieces and all edge pieces around the border,
    //   going clockwise. Backtracking is used but is fast because rotation
    //   is almost always forced (only ~1 valid rotation per piece per position).
    //
    // Phase 2 — fill_inside:
    //   Place all interior pieces row by row, left to right.
    //   Full backtracking with 4 rotations per piece.
    //
    // Returns true if a valid solution was found, false otherwise.
    // -------------------------------------------------------------------------
    public static boolean solve_puzzle(Puzzle puzzle) {

        int sizePuzzle = puzzle.getList().size();
        int height     = puzzle.getHeight();
        int width      = puzzle.getWidth();

        // --- Phase 0a: classify pieces ---
        // These lists are LOCAL variables — not static fields — so they are
        // fresh every time solve_puzzle is called (no stale data).
        ArrayList<Element> corner = new ArrayList<>();
        ArrayList<Element> edge   = new ArrayList<>();
        ArrayList<Element> inside = new ArrayList<>();

        int counterCorners = 0;
        int counterEdges   = 0;
        // Formula: border perimeter minus the 4 corner slots
        // = (top row - 2) + (bottom row - 2) + (left col - 2) + (right col - 2)
        // = 2*(width-2) + 2*(height-2)
        int minEdges = 2 * (width - 2) + 2 * (height - 2);

        for (int i = 0; i < sizePuzzle; i++) {
            Element e = puzzle.getList().get(i);
            // check_corner is tested first so corner pieces don't also count as edges.
            if (Element.check_corner(e)) {
                counterCorners++;
                corner.add(e);
            } else if (Element.check_edge(e)) {
                counterEdges++;
                edge.add(e);
            } else {
                inside.add(e);
            }
        }

        // --- Phase 0b: early failure checks ---
        // If we don't have enough corners or edges, no solution can exist.
        // We detect this before backtracking to give a clear error message.
        if (counterCorners < 4) {
            System.err.println("Not enough corner pieces: need 4, found " + counterCorners);
            return false;
        }
        if (counterEdges < minEdges) {
            System.err.println("Not enough edge pieces: need " + minEdges
                    + ", found " + counterEdges);
            return false;
        }

        // --- Phase 1: build the ordered list of border cells ---
        // We traverse the border CLOCKWISE starting from (0,0):
        //   top row left→right     (0,0) … (0,W-1)
        //   right col top→bottom   (1,W-1) … (H-1,W-1)
        //   bottom row right→left  (H-1,W-2) … (H-1,0)
        //   left col bottom→top    (H-2,0) … (1,0)
        //
        // This order is important: when fill_outside places cell N, every cell
        // placed so far has exactly one neighbour already on the board to
        // check compatibility against — never zero, never two at the same time.
        List<int[]> borderCells = new ArrayList<>();
        for (int c = 0;          c < width;      c++)  borderCells.add(new int[]{0,          c});
        for (int r = 1;          r < height;     r++)  borderCells.add(new int[]{r,          width - 1});
        for (int c = width - 2;  c >= 0;         c--)  borderCells.add(new int[]{height - 1, c});
        for (int r = height - 2; r >= 1;         r--)  borderCells.add(new int[]{r,          0});

        // Run phase 1.
        if (!fill_outside(puzzle, 0, borderCells)) {
            return false;
        }

        // --- Phase 2: fill interior cells (only if they exist) ---
        // A puzzle with width<=2 or height<=2 has no interior cells at all —
        // every cell is already a border cell, so fill_outside handled everything.
        if (width > 2 && height > 2) {
            if (!fill_inside(puzzle, 1, 1)) {
                return false;
            }
        }

        // Both phases succeeded — puzzle is solved.
        // BUG FIXED: the original had `return false` here, so the solver always
        // reported failure even after successfully filling the grid.
        return true;
    }


    // =========================================================================
    // PHASE 1 — FILL THE BORDER (corners + edges)
    // =========================================================================

    // -------------------------------------------------------------------------
    // fill_outside — recursive backtracking over the ordered border cell list.
    //
    // At each call we try to fill border cell number `cellIndex`.
    // We try every unused piece of the correct type (corner or edge),
    // in all 4 rotations, and recurse for the next cell.
    // If no piece fits, we return false so the caller can try its next option.
    //
    // Parameters:
    //   puzzle      — shared state (grid, used_e, piece list)
    //   cellIndex   — which border cell we are currently trying to fill
    //                 (index into the borderCells list built in solve_puzzle)
    //   borderCells — the ordered list of (row,col) pairs for all border cells
    // -------------------------------------------------------------------------
    private static boolean fill_outside(Puzzle puzzle, int cellIndex, List<int[]> borderCells) {

        // Base case: all border cells have been placed successfully.
        if (cellIndex == borderCells.size()) return true;

        // Get the grid coordinates of the current border cell.
        int row = borderCells.get(cellIndex)[0];
        int col = borderCells.get(cellIndex)[1];
        int w   = puzzle.getWidth();
        int h   = puzzle.getHeight();

        // A cell is a corner cell if it is at one of the four physical corners.
        boolean isCornerCell = (row == 0 || row == h - 1) && (col == 0 || col == w - 1);

        // Try every piece that hasn't been placed yet.
        for (int i = 0; i < puzzle.getList().size(); i++) {
            if (puzzle.getUsed(i)) continue; // already placed elsewhere

            Element original = puzzle.getList().get(i);

            // Filter by piece type: only corner pieces at corner cells,
            // only edge pieces (not corners) at edge cells.
            // This cuts the search space dramatically.
            if ( isCornerCell && !Element.check_corner(original)) continue;
            if (!isCornerCell && (!Element.check_edge(original) || Element.check_corner(original))) continue;

            // Try all 4 rotations for this piece.
            for (int rot = 0; rot < 4; rot++) {

                // rotate_element returns a NEW element — the original is untouched.
                Element rotated = original.rotate_element(rot);

                // Check 1: flat sides must face outward; non-flat sides must face inward.
                // For most border pieces only ONE rotation passes this check.
                if (!isValidPlacement(rotated, row, col, w, h)) continue;

                // Check 2: the sides that touch already-placed neighbours must match
                // (B↔P or P↔B). Cells not yet placed are simply skipped (null-safe).
                if (!checkNeighbors(rotated, row, col, puzzle)) continue;

                // Both checks passed — tentatively place the piece.
                puzzle.setPlacement(new Placement(i, rot), row, col);
                puzzle.setUsed(i, true);

                // Recurse for the next border cell.
                if (fill_outside(puzzle, cellIndex + 1, borderCells)) return true;

                // Recursion returned false — this placement was a dead end.
                // Undo it (backtrack) and try the next piece/rotation.
                puzzle.setPlacement(null, row, col);
                puzzle.setUsed(i, false);
            }
        }

        // No piece fitted this border cell — tell the caller to backtrack.
        return false;
    }


    // =========================================================================
    // PHASE 2 — FILL THE INTERIOR
    // =========================================================================

    // -------------------------------------------------------------------------
    // fill_inside — recursive backtracking over interior cells, row by row.
    //
    // Interior cells are those NOT on any border:
    //   row ∈ [1, height-2]  and  col ∈ [1, width-2]
    //
    // We process them left-to-right, top-to-bottom, starting at (1,1).
    // When we reach the right border column, we wrap to the next row.
    // When we advance past the last interior row, we are done (base case).
    //
    // At each interior cell the top neighbour (row-1,col) and left neighbour
    // (row,col-1) are always already placed (because of our traversal order),
    // so we get two strong constraints that prune the search space quickly.
    //
    // BUG FIXED: the original fill_inside had no rotation loop, referenced a
    // variable outside its scope, and called the recursion unconditionally
    // (decoupled from the placement logic). Completely rewritten.
    // -------------------------------------------------------------------------
    private static boolean fill_inside(Puzzle puzzle, int row, int col) {

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        // Base case: we've moved past the last interior row — all done.
        if (row == h - 1) return true;

        // Compute the next interior cell.
        // Moving right: if the next column would be the right border, wrap.
        int nextCol = col + 1;
        int nextRow = row;
        if (nextCol == w - 1) { // reached the right border column
            nextCol = 1;        // jump back to the first interior column
            nextRow = row + 1;  // of the next row
        }

        // Try every unused interior piece.
        for (int i = 0; i < puzzle.getList().size(); i++) {
            if (puzzle.getUsed(i)) continue;

            Element original = puzzle.getList().get(i);

            // Interior pieces must have NO flat sides.
            // Skip corners and edge pieces immediately to avoid pointless work.
            if (Element.check_corner(original) || Element.check_edge(original)) continue;

            // Try all 4 rotations.
            for (int rot = 0; rot < 4; rot++) {

                Element rotated = original.rotate_element(rot);

                // For an interior cell, isValidPlacement rejects any flat side
                // (since all four directions face another piece, not the border).
                if (!isValidPlacement(rotated, row, col, w, h)) continue;

                // Check compatibility with all already-placed neighbours.
                // Top and left are always placed; right and bottom may not be yet.
                if (!checkNeighbors(rotated, row, col, puzzle)) continue;

                // Tentatively place the piece.
                puzzle.setPlacement(new Placement(i, rot), row, col);
                puzzle.setUsed(i, true);

                // Recurse for the next interior cell.
                if (fill_inside(puzzle, nextRow, nextCol)) return true;

                // Dead end — backtrack.
                puzzle.setPlacement(null, row, col);
                puzzle.setUsed(i, false);
            }
        }

        // No piece fitted this interior cell — backtrack.
        return false;
    }


    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    // -------------------------------------------------------------------------
    // isValidPlacement — checks the boundary and orientation rules for one cell.
    //
    // Rule A (boundary): a side that faces the outside of the rectangle MUST be F.
    //   → row==0       : top    must be F
    //   → row==height-1: bottom must be F
    //   → col==0       : left   must be F
    //   → col==width-1 : right  must be F
    //
    // Rule B (interior): a side that faces another cell must NOT be F.
    //   → row>0        : top    must NOT be F
    //   → row<height-1 : bottom must NOT be F
    //   → col>0        : left   must NOT be F
    //   → col<width-1  : right  must NOT be F
    //
    // These two rules together automatically enforce the correct piece type
    // AND the correct orientation, without separate checks.
    // -------------------------------------------------------------------------
    private static boolean isValidPlacement(Element e, int row, int col, int width, int height) {
        // Rule A — outer sides must be flat
        if (row == 0          && e.getTop()    != 'F') return false;
        if (row == height - 1 && e.getBottom() != 'F') return false;
        if (col == 0          && e.getLeft()   != 'F') return false;
        if (col == width - 1  && e.getRight()  != 'F') return false;
        // Rule B — interior-facing sides must NOT be flat
        if (row > 0           && e.getTop()    == 'F') return false;
        if (row < height - 1  && e.getBottom() == 'F') return false;
        if (col > 0           && e.getLeft()   == 'F') return false;
        if (col < width - 1   && e.getRight()  == 'F') return false;
        return true;
    }

    // -------------------------------------------------------------------------
    // checkNeighbors — verifies that `e` is compatible with every already-placed
    // adjacent piece in all four directions.
    //
    // A neighbour is "already placed" when puzzle.getPlacement(r,c) != null.
    // We check all four directions; those that aren't placed yet are simply
    // skipped. This makes the method safe to call from both fill_outside (where
    // some neighbours haven't been placed yet) and fill_inside (where top and
    // left are guaranteed but right and bottom may not be).
    //
    // direction encoding (see Element.check_neighbour):
    //   0 = neighbour above   → compare e.top    with neighbour.bottom
    //   1 = neighbour right   → compare e.right  with neighbour.left
    //   2 = neighbour below   → compare e.bottom with neighbour.top
    //   3 = neighbour left    → compare e.left   with neighbour.right
    // -------------------------------------------------------------------------
    private static boolean checkNeighbors(Element e, int row, int col, Puzzle puzzle) {
        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        // Check the neighbour ABOVE (direction 0)
        if (row > 0     && puzzle.getPlacement(row - 1, col) != null)
            if (!e.check_neighbour(getEffectiveElement(puzzle, row - 1, col), 0)) return false;

        // Check the neighbour to the LEFT (direction 3)
        if (col > 0     && puzzle.getPlacement(row, col - 1) != null)
            if (!e.check_neighbour(getEffectiveElement(puzzle, row, col - 1), 3)) return false;

        // Check the neighbour to the RIGHT (direction 1)
        if (col < w - 1 && puzzle.getPlacement(row, col + 1) != null)
            if (!e.check_neighbour(getEffectiveElement(puzzle, row, col + 1), 1)) return false;

        // Check the neighbour BELOW (direction 2)
        if (row < h - 1 && puzzle.getPlacement(row + 1, col) != null)
            if (!e.check_neighbour(getEffectiveElement(puzzle, row + 1, col), 2)) return false;

        return true;
    }

    // -------------------------------------------------------------------------
    // getEffectiveElement — retrieves the piece placed at (row,col) already
    // rotated to its placed orientation.
    //
    // The grid stores Placement(originalIndex, rotation) — not a pre-rotated
    // Element — so we reconstruct the oriented piece on demand here.
    //
    // This replaces the old static `Element[][] grid` field that was intended
    // to cache rotated elements but was never initialised or written to.
    // -------------------------------------------------------------------------
    private static Element getEffectiveElement(Puzzle puzzle, int row, int col) {
        Placement p = puzzle.getPlacement(row, col);
        // Look up the original piece by index, then return a rotated copy.
        return puzzle.getList().get(p.getIndex()).rotate_element(p.getRotation());
    }
}
