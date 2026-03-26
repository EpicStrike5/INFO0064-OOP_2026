package be.uliege.montefiore.oop;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// all algorithm logic: file reading + backtracking solver (no fields, only static methods)
public class Functions {

    // reads the .pzl file and fills the puzzle object
    // format: first line = "width height", then one piece per line (4 chars: top right bottom left)
    public static boolean Read_doc(String filename, Puzzle puzzle) {

        ArrayList<Element> pieceList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {

            String[] size = reader.readLine().split(" ");
            int width     = Integer.parseInt(size[0]);
            int height    = Integer.parseInt(size[1]);
            int nb_lines  = width * height;
            puzzle.setWidth(width);
            puzzle.setHeight(height);

            char[] tableCaracteres = new char[4];
            String line;

            while ((line = reader.readLine()) != null && pieceList.size() < nb_lines) {
                if (line.length() >= 4) {
                    for (int j = 0; j < 4; j++) {
                        tableCaracteres[j] = line.charAt(j);
                        if (tableCaracteres[j] != 'F' && tableCaracteres[j] != 'P'
                                && tableCaracteres[j] != 'B') {
                            System.err.println("Incorrect character in file: " + filename);
                            return false;
                        }
                    }
                    pieceList.add(new Element(tableCaracteres));
                } else {
                    System.err.println("File has incorrect format (line too short): " + filename);
                    return false;
                }
            }

            if (pieceList.size() < nb_lines) {
                System.err.println("Not enough pieces: expected " + nb_lines
                        + ", got " + pieceList.size() + " in " + filename);
                return false;
            }

            puzzle.setList(pieceList);

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filename);
            return false;
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
            return false;
        }

        return true;
    }

    // two-phase solver: phase 1 fills the border, phase 2 fills the interior
    public static boolean solve_puzzle(Puzzle puzzle) {

        int sizePuzzle = puzzle.getList().size();
        int height     = puzzle.getHeight();
        int width      = puzzle.getWidth();

        // classify pieces
        ArrayList<Element> corner = new ArrayList<>();
        ArrayList<Element> edge   = new ArrayList<>();
        ArrayList<Element> inside = new ArrayList<>();

        int counterCorners = 0;
        int counterEdges   = 0;
        int minEdges = 2 * (width - 2) + 2 * (height - 2);

        for (int i = 0; i < sizePuzzle; i++) {
            Element e = puzzle.getList().get(i);
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

        // early failure if not enough corners/edges
        if (counterCorners < 4) {
            System.err.println("Not enough corner pieces: need 4, found " + counterCorners);
            return false;
        }
        if (counterEdges < minEdges) {
            System.err.println("Not enough edge pieces: need " + minEdges
                    + ", found " + counterEdges);
            return false;
        }

        // build the border cell list in clockwise order starting from (0,0)
        List<int[]> borderCells = new ArrayList<>();
        for (int c = 0;          c < width;      c++)  borderCells.add(new int[]{0,          c});
        for (int r = 1;          r < height;     r++)  borderCells.add(new int[]{r,          width - 1});
        for (int c = width - 2;  c >= 0;         c--)  borderCells.add(new int[]{height - 1, c});
        for (int r = height - 2; r >= 1;         r--)  borderCells.add(new int[]{r,          0});

        if (!fill_outside(puzzle, 0, borderCells)) {
            puzzle.restoreBest();
            return false;
        }

        if (width > 2 && height > 2) {
            if (!fill_inside(puzzle, 1, 1)) {
                puzzle.restoreBest();
                return false;
            }
        }

        return true;
    }

    // recursive backtracking for border cells (corners + edges)
    private static boolean fill_outside(Puzzle puzzle, int cellIndex, List<int[]> borderCells) {

        if (cellIndex == borderCells.size()) return true;

        int row = borderCells.get(cellIndex)[0];
        int col = borderCells.get(cellIndex)[1];
        int w   = puzzle.getWidth();
        int h   = puzzle.getHeight();

        boolean isCornerCell = (row == 0 || row == h - 1) && (col == 0 || col == w - 1);

        for (int i = 0; i < puzzle.getList().size(); i++) {
            if (puzzle.getUsed(i)) continue;

            Element original = puzzle.getList().get(i);

            // only try the right piece type for this cell
            if ( isCornerCell && !Element.check_corner(original)) continue;
            if (!isCornerCell && (!Element.check_edge(original) || Element.check_corner(original))) continue;

            for (int rot = 0; rot < 4; rot++) {
                Element rotated = original.rotate_element(rot);

                if (!isValidPlacement(rotated, row, col, w, h)) continue;
                if (!checkNeighbors(rotated, row, col, puzzle)) continue;

                puzzle.setPlacement(new Placement(i, rot), row, col);
                puzzle.setUsed(i, true);
                puzzle.incrementPlaced();
                puzzle.tryUpdateBest();

                if (fill_outside(puzzle, cellIndex + 1, borderCells)) return true;

                // backtrack
                puzzle.setPlacement(null, row, col);
                puzzle.setUsed(i, false);
                puzzle.decrementPlaced();
            }
        }

        return false;
    }

    // recursive backtracking for interior cells, row by row left to right
    private static boolean fill_inside(Puzzle puzzle, int row, int col) {

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        if (row == h - 1) return true;

        // compute next interior cell, wrapping to next row at the right border
        int nextCol = col + 1;
        int nextRow = row;
        if (nextCol == w - 1) {
            nextCol = 1;
            nextRow = row + 1;
        }

        for (int i = 0; i < puzzle.getList().size(); i++) {
            if (puzzle.getUsed(i)) continue;

            Element original = puzzle.getList().get(i);

            // interior pieces have no flat sides
            if (Element.check_corner(original) || Element.check_edge(original)) continue;

            for (int rot = 0; rot < 4; rot++) {
                Element rotated = original.rotate_element(rot);

                if (!isValidPlacement(rotated, row, col, w, h)) continue;
                if (!checkNeighbors(rotated, row, col, puzzle)) continue;

                puzzle.setPlacement(new Placement(i, rot), row, col);
                puzzle.setUsed(i, true);
                puzzle.incrementPlaced();
                puzzle.tryUpdateBest();

                if (fill_inside(puzzle, nextRow, nextCol)) return true;

                // backtrack
                puzzle.setPlacement(null, row, col);
                puzzle.setUsed(i, false);
                puzzle.decrementPlaced();
            }
        }

        return false;
    }

    // outer sides must be F, inner-facing sides must not be F
    private static boolean isValidPlacement(Element e, int row, int col, int width, int height) {
        if (row == 0          && e.getTop()    != 'F') return false;
        if (row == height - 1 && e.getBottom() != 'F') return false;
        if (col == 0          && e.getLeft()   != 'F') return false;
        if (col == width - 1  && e.getRight()  != 'F') return false;
        if (row > 0           && e.getTop()    == 'F') return false;
        if (row < height - 1  && e.getBottom() == 'F') return false;
        if (col > 0           && e.getLeft()   == 'F') return false;
        if (col < width - 1   && e.getRight()  == 'F') return false;
        return true;
    }

    // checks compatibility with all already-placed neighbours in 4 directions
    private static boolean checkNeighbors(Element e, int row, int col, Puzzle puzzle) {
        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        if (row > 0     && puzzle.getPlacement(row - 1, col) != null)
            if (!e.check_neighbour(getEffectiveElement(puzzle, row - 1, col), 0)) return false;
        if (col > 0     && puzzle.getPlacement(row, col - 1) != null)
            if (!e.check_neighbour(getEffectiveElement(puzzle, row, col - 1), 3)) return false;
        if (col < w - 1 && puzzle.getPlacement(row, col + 1) != null)
            if (!e.check_neighbour(getEffectiveElement(puzzle, row, col + 1), 1)) return false;
        if (row < h - 1 && puzzle.getPlacement(row + 1, col) != null)
            if (!e.check_neighbour(getEffectiveElement(puzzle, row + 1, col), 2)) return false;

        return true;
    }

    // returns the piece at (row,col) with its stored rotation applied
    private static Element getEffectiveElement(Puzzle puzzle, int row, int col) {
        Placement p = puzzle.getPlacement(row, col);
        return puzzle.getList().get(p.getIndex()).rotate_element(p.getRotation());
    }
}
