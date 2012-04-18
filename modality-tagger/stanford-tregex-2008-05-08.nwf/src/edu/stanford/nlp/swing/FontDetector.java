package edu.stanford.nlp.swing;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects which Fonts can be used to display unicode characters in a given language.
 *
 * @author Huy Nguyen (htnguyen@cs.stanford.edu)
 * @author Christopher Manning
 */
public class FontDetector {

  public static final int NUM_LANGUAGES = 2;
  public static final int CHINESE = 0;
  public static final int ARABIC = 1;

  
  public static final String[][] unicodeRanges = new String[NUM_LANGUAGES][];

  static {
    unicodeRanges[CHINESE] = new String[]{"\u3001", "\uFF01", "\uFFEE", "\u0374", "\u3126"};
    unicodeRanges[ARABIC] = new String[]{"\uFB50", "\uFE70","\uFEFF"};
  }


  private FontDetector() {}


  /**
   * Returns which Fonts on the system can display the sample string.
   *
   * @param language the numerical code for the language to check
   * @return a list of Fonts which can display the sample String
   */
  public static List<Font> supportedFonts(int language) {
    if (language < 0 || language > NUM_LANGUAGES) {
      throw new IllegalArgumentException();
    }

    List<Font> fonts = new ArrayList<Font>();
    Font[] systemFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    for (int i = 0; i < systemFonts.length; i++) {
      boolean canDisplay = true;
      for (int j = 0; j < unicodeRanges[language].length; j++) {
        if (systemFonts[i].canDisplayUpTo(unicodeRanges[language][j]) != -1) {
          canDisplay = false;
          break;
        }
      }
      if (canDisplay) {
        fonts.add(systemFonts[i]);
      }
    }
    return fonts;
  }

  public static boolean hasFont(String fontName) {
    Font[] systemFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    for (int i = 0; i < systemFonts.length; i++) {
      if (systemFonts[i].getName().equalsIgnoreCase(fontName)) {
        return true;
      }
    }
    return false;
  }

  public static void main(String[] args) {
    List<Font> fonts = supportedFonts(ARABIC);
    System.err.println("Has MS Mincho? " + hasFont("MS Mincho"));
    for (Font font : fonts) {
      System.out.println(font.getName());
    }
  }

}
