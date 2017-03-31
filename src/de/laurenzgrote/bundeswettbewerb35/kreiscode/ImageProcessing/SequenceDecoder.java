package de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing;

import java.io.*;
import java.util.ArrayList;

public class SequenceDecoder {
    class Pair {
        private int asciiNo;
        private boolean[] bool;
        private int boolNo;
        private String result;

        public Pair(int asciiNo, boolean[] bool, String result) {
            this.asciiNo = asciiNo;
            this.bool = bool;
            this.result = result;

            this.boolNo = toInt(bool);
        }

        public int getAsciiNo() {
            return asciiNo;
        }

        public boolean[] getBool() {
            return bool;
        }

        public int getBoolNo() {
            return boolNo;
        }

        public String getResult() {
            return result;
        }
    }

    private ArrayList<Pair> pairs;

    public SequenceDecoder(File file) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException | NullPointerException e) {
            // Kein Dict ausgewählt --> Fallback auf Default-Dict aus dem Classpath
            System.err.println("Kein Dict ausgewählt. Fallback auf BwInf-Default");
            InputStream is = this.getClass().getResourceAsStream("dict.txt");
            br = new BufferedReader(new InputStreamReader(is));
        }
        readPairs(br);
    }

    private void readPairs(BufferedReader br) {
        pairs = new ArrayList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");

                boolean[] bool = new boolean[16];
                for (int i = 0; i < 16; i++) {
                    char here = parts[0].charAt(i);
                    if (here != '0') {
                        bool[i] = true;
                    } else {
                        bool[i] = false;
                    }
                }
                int asciiNo = Integer.parseInt(parts[1]);

                Pair newPair = new Pair(asciiNo, bool, parts[2]);
                pairs.add(newPair);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String decode(boolean[] input) {
        int length = input.length;

        String result = "???";

        for (int i = 0; i < length; i++) {
            int boolNo = toInt(input);
            Pair pair = search(boolNo);

            if (pair != null) {
                result = pair.getResult();
                break;
            } else {
                // Jetzt wird geshiftet...
                boolean[] newInput = new boolean[length];

                newInput[0] = input[length-1];
                for (int j = 1; j < length; j++) {
                    newInput[j] = input[j-1];
                }

                input = newInput;
            }
        }

        return result;
    }

    // Assert auf Boolgröße einbauen!
    private int toInt(boolean[] bool) {
        int result = 0;

        for (int i = 0; i < bool.length; i++) {
            if (bool[i]) {
                int exp = bool.length - 1 - i;
                result += Math.pow(2, exp);
            }
        }

        return result;
    }

    // Auf binäre Suche umrüsten!
    // Returns null
    private Pair search (int boolNo) {
        for (Pair pair : pairs) {
            if (pair.getBoolNo() == boolNo) {
                return pair;
            }
        }
        return null;
    }
}
