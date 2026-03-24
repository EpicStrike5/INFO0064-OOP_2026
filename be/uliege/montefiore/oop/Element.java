// The package groups all our classes under one namespace.
// It also dictates the folder structure:
//   be/uliege/montefiore/oop/Element.java
// The spec requires compiling with `javac be/uliege/montefiore/oop/*.java`
// so this declaration must be present in every file.
package be.uliege.montefiore.oop;

// =============================================================================
// Element — represents a single jigsaw puzzle piece.
//
// A piece is a square with 4 sides. Each side is one of:
//   'F' = Flat  → must face the outer boundary of the rectangle
//   'B' = Bump  → protrudes outward; must match a pit on the adjacent piece
//   'P' = Pit   → concave notch;    must match a bump on the adjacent piece
//
// Sides are stored in clockwise order starting from the top:
//   top, right, bottom, left
//
// This class only models ONE piece. It knows nothing about the grid or the
// overall puzzle — that is Puzzle's job.
// =============================================================================
public class Element {

    // =========================================================================
    // FIELDS — the four sides of this piece
    // =========================================================================

    // Each side is stored as a single char: 'F', 'B', or 'P'.
    // They are private so that no other class can change them directly;
    // all reads and writes go through the getters/setters at the bottom of
    // this file, which makes it easy to add validation later if needed.
    private char top;
    private char right;
    private char bottom;
    private char left;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    // -------------------------------------------------------------------------
    // Element(char[] c) — builds a piece from a 4-character array.
    //
    // The array must be in clockwise order: [top, right, bottom, left].
    // Called by Read_doc in Functions.java after reading one line of the file.
    // -------------------------------------------------------------------------
    public Element(char[] c) {
        // Assign each position of the array to the matching field.
        // c[0] = top, c[1] = right, c[2] = bottom, c[3] = left
        // (clockwise order, as defined by the spec).
        this.top    = c[0];
        this.right  = c[1];
        this.bottom = c[2];
        this.left   = c[3];
    }

    // =========================================================================
    // PIECE-TYPE CLASSIFICATION
    // =========================================================================

    // -------------------------------------------------------------------------
    // check_corner — returns true if this piece belongs in a corner position.
    //
    // A corner piece has exactly 2 ADJACENT flat sides.
    // There are 4 possible adjacent pairs to test (going clockwise):
    //   top+right, right+bottom, bottom+left, left+top.
    // If ANY of them are both 'F', the piece is a corner.
    //
    // We only check ADJACENT pairs, never OPPOSITE ones (top+bottom or
    // left+right), because a piece with flat sides on opposite faces would
    // need to face two boundaries at once, which is geometrically impossible.
    // -------------------------------------------------------------------------
    public static boolean check_corner(Element e) {
        // Test all four adjacent pairs with ||.
        // If the piece is top-left corner:  top==F && left==F
        // If the piece is top-right corner: top==F && right==F
        // etc.
        return (e.top == 'F' && e.right  == 'F') ||   // top-right corner
               (e.right  == 'F' && e.bottom == 'F') || // bottom-right corner
               (e.bottom == 'F' && e.left   == 'F') || // bottom-left corner
               (e.left   == 'F' && e.top    == 'F');   // top-left corner
    }

    // -------------------------------------------------------------------------
    // check_edge — returns true if this piece has AT LEAST one flat side.
    //
    // This is intentionally broad: it returns true for both corner pieces
    // (2 flat sides) and true edge pieces (exactly 1 flat side).
    // In solve_puzzle we always call check_corner first in an if/else chain,
    // so corner pieces never reach check_edge — effectively check_edge means
    // "has exactly 1 flat side" in that context.
    //
    // BUG FIXED: the original code was:
    //   e.top=='F' || e.right=='F' || e.left=='F' && e.bottom=='F'
    // Java's operator precedence evaluates && before ||, turning this into:
    //   e.top=='F' || e.right=='F' || (e.left=='F' && e.bottom=='F')
    // So a piece whose ONLY flat side was bottom or left (alone) was silently
    // classified as an interior piece, making the puzzle appear unsolvable.
    // Fix: list all four sides individually, each connected by ||.
    // -------------------------------------------------------------------------
    public static boolean check_edge(Element e) {
        // One || per side — no operator-precedence trap.
        // If ANY side is flat, this is a boundary piece.
        return e.top    == 'F' ||
               e.right  == 'F' ||
               e.bottom == 'F' ||
               e.left   == 'F';
    }

    // =========================================================================
    // ROTATION
    // =========================================================================

    // -------------------------------------------------------------------------
    // rotate_element — returns a NEW Element rotated clockwise by `times`
    // quarter-turns (0, 1, 2 or 3).
    //
    // Critically, this method returns a COPY — it never modifies `this`.
    // The solver calls this repeatedly during backtracking; the original pieces
    // stored in puzzle.getList() must stay unchanged throughout.
    //
    // BUG FIXED: the original wrote `Element rotated = this;`
    // In Java, object variables are references (pointers), not copies.
    // That line made `rotated` point to the SAME object as `this` in memory,
    // so calling rotated.rotate90() mutated the original piece in the list,
    // corrupting every future lookup. Fix: copy the 4 chars into a new array
    // and build a fresh Element from it.
    // -------------------------------------------------------------------------
    public Element rotate_element(int times) {

        // --- Step 1: copy this piece's sides into a brand-new char array ---
        // We cannot just pass `this` to new Element() because the constructor
        // reads from the array at construction time — we need an actual array.
        char[] copy = { this.top, this.right, this.bottom, this.left };

        // --- Step 2: build a fresh Element from that copy ---
        // `this` is completely untouched from here on.
        Element rotated = new Element(copy);

        // --- Step 3: apply the requested number of 90-degree clockwise turns ---
        // Each call to rotate90() modifies `rotated` in place (not `this`),
        // which is safe because `rotated` is the fresh copy we just created.
        for (int i = 0; i < times; i++) {
            rotated.rotate90();
        }

        // --- Step 4: return the rotated copy ---
        return rotated;
    }

    // -------------------------------------------------------------------------
    // rotate90 — rotates THIS element 90 degrees clockwise, in-place.
    //
    // Mental model: imagine physically rotating a square card clockwise.
    // Each side slides to the next position:
    //   LEFT  side becomes the new TOP
    //   TOP   side becomes the new RIGHT
    //   RIGHT side becomes the new BOTTOM
    //   BOTTOM side becomes the new LEFT
    //
    // Private — the only caller is rotate_element, which already works on a
    // copy, so it is safe to mutate here without corrupting the original piece.
    // -------------------------------------------------------------------------
    private void rotate90() {

        // --- Save one value to break the circular dependency ---
        // If we overwrote right = top first, we'd lose the old value of right.
        // By saving right in a temporary variable first, we can do all
        // assignments without losing any data.
        char temp   = this.right;   // save the old RIGHT value

        // --- Perform the three remaining assignments in order ---
        this.right  = this.top;     // old TOP   slides to the RIGHT position
        this.top    = this.left;    // old LEFT  slides to the TOP position
        this.left   = this.bottom;  // old BOTTOM slides to the LEFT position
        this.bottom = temp;         // saved old RIGHT slides to the BOTTOM position
    }

    // =========================================================================
    // COMPATIBILITY CHECK
    // =========================================================================

    // -------------------------------------------------------------------------
    // check_neighbour — returns true if `this` piece is compatible with
    // `neighbour` when `neighbour` is placed in the given direction.
    //
    // `direction` tells us WHERE the neighbour sits relative to this piece:
    //   0 → neighbour is ABOVE  → we compare this.top    with neighbour.bottom
    //   1 → neighbour is RIGHT  → we compare this.right  with neighbour.left
    //   2 → neighbour is BELOW  → we compare this.bottom with neighbour.top
    //   3 → neighbour is LEFT   → we compare this.left   with neighbour.right
    //
    // The rule: the two touching sides must be a valid pair (see areCompatible).
    // Delegates the actual rule logic to the private areCompatible() helper.
    // -------------------------------------------------------------------------
    public boolean check_neighbour(Element neighbour, int direction) {

        // Use a switch to pick the correct pair of touching sides based on
        // where the neighbour is. Each case reads one side of `this` and the
        // OPPOSITE side of `neighbour` (because they face each other).
        switch (direction) {
            case 0:
                // Neighbour is ABOVE: this piece's TOP touches neighbour's BOTTOM.
                return areCompatible(this.top,    neighbour.bottom);
            case 1:
                // Neighbour is RIGHT: this piece's RIGHT touches neighbour's LEFT.
                return areCompatible(this.right,  neighbour.left);
            case 2:
                // Neighbour is BELOW: this piece's BOTTOM touches neighbour's TOP.
                return areCompatible(this.bottom, neighbour.top);
            case 3:
                // Neighbour is LEFT: this piece's LEFT touches neighbour's RIGHT.
                return areCompatible(this.left,   neighbour.right);
            default:
                // Invalid direction — should never happen in normal use.
                return false;
        }
    }

    // -------------------------------------------------------------------------
    // areCompatible — the core matching rule for two sides that touch.
    //
    // Only three combinations are valid:
    //   F - F : two flat sides → legal only at the outer boundary
    //           (isValidPlacement in Functions ensures flat sides only face
    //           outward, so F-F between two pieces is only possible at the
    //           border, which is fine)
    //   B - P : bump meets pit  → they physically interlock correctly
    //   P - B : pit  meets bump → same interlock, other way around
    //
    // All other combos (B-B, P-P, F-B, F-P, …) are invalid because:
    //   B-B : two bumps collide, they cannot occupy the same space
    //   P-P : two pits leave a gap, they don't interlock
    //   F-B / F-P : a flat side cannot receive a bump or pit
    //
    // Private — only check_neighbour should call this.
    // -------------------------------------------------------------------------
    private boolean areCompatible(char side1, char side2) {
        // Test all three valid pairs explicitly.
        // Using == on char is correct and efficient (primitive comparison).
        return (side1 == 'F' && side2 == 'F') ||  // flat meets flat (boundary)
               (side1 == 'B' && side2 == 'P') ||  // bump meets pit  (interlock)
               (side1 == 'P' && side2 == 'B');    // pit  meets bump (interlock)
    }

    // =========================================================================
    // GETTERS AND SETTERS
    // =========================================================================

    // Standard Java bean accessors — allow other classes (Functions,
    // PuzzleDisplay, ...) to read or change the sides without touching
    // the private fields directly. One getter + one setter per side.

    public char getTop()               { return top; }
    public void setTop(char top)       { this.top = top; }

    public char getRight()             { return right; }
    public void setRight(char right)   { this.right = right; }

    public char getBottom()            { return bottom; }
    public void setBottom(char bottom) { this.bottom = bottom; }

    public char getLeft()              { return left; }
    public void setLeft(char left)     { this.left = left; }
}
