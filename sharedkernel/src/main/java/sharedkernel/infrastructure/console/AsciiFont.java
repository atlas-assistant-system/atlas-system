package sharedkernel.infrastructure.console;

import java.util.ArrayList;
import java.util.List;

final class AsciiFont {

    static final int HEIGHT = 5;
    static final int WIDTH = 5;

    private static final String[] BLANK = {"     ", "     ", "     ", "     ", "     "};

    private static final String[][] GLYPHS = {
        {" ### ", "#   #", "#####", "#   #", "#   #"},
        {"#### ", "#   #", "#### ", "#   #", "#### "},
        {" ####", "#    ", "#    ", "#    ", " ####"},
        {"#### ", "#   #", "#   #", "#   #", "#### "},
        {"#####", "#    ", "#### ", "#    ", "#####"},
        {"#####", "#    ", "#### ", "#    ", "#    "},
        {" ####", "#    ", "#  ##", "#   #", " ####"},
        {"#   #", "#   #", "#####", "#   #", "#   #"},
        {"#####", "  #  ", "  #  ", "  #  ", "#####"},
        {"#####", "   # ", "   # ", "#  # ", " ##  "},
        {"#   #", "#  # ", "###  ", "#  # ", "#   #"},
        {"#    ", "#    ", "#    ", "#    ", "#####"},
        {"#   #", "## ##", "# # #", "#   #", "#   #"},
        {"#   #", "##  #", "# # #", "#  ##", "#   #"},
        {" ### ", "#   #", "#   #", "#   #", " ### "},
        {"#### ", "#   #", "#### ", "#    ", "#    "},
        {" ### ", "#   #", "# # #", "#  # ", " ## #"},
        {"#### ", "#   #", "#### ", "#  # ", "#   #"},
        {" ####", "#    ", " ### ", "    #", "#### "},
        {"#####", "  #  ", "  #  ", "  #  ", "  #  "},
        {"#   #", "#   #", "#   #", "#   #", " ### "},
        {"#   #", "#   #", "#   #", " # # ", "  #  "},
        {"#   #", "#   #", "# # #", "## ##", "#   #"},
        {"#   #", " # # ", "  #  ", " # # ", "#   #"},
        {"#   #", " # # ", "  #  ", "  #  ", "  #  "},
        {"#####", "   # ", "  #  ", " #   ", "#####"}
    };

    private AsciiFont() {}

    static List<String> render(String text) {
        var rows = new ArrayList<String>(HEIGHT);

        for (var row = 0; row < HEIGHT; row++) {
            rows.add(rowOf(text, row));
        }

        return rows;
    }

    static String[] glyphFor(char character) {
        var upper = Character.toUpperCase(character);

        if (upper < 'A' || upper > 'Z') {
            return BLANK;
        }

        return GLYPHS[upper - 'A'];
    }

    private static String rowOf(String text, int row) {
        var line = new StringBuilder();

        for (var index = 0; index < text.length(); index++) {
            if (index > 0) {
                line.append(' ');
            }

            line.append(glyphFor(text.charAt(index))[row]);
        }

        return line.toString();
    }
}
