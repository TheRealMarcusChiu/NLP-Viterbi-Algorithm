package util;

import lombok.Data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Data
public class HMM {

    public static final String SOS_MARKER = "<s>";
    public static final String EOS_MARKER = "</s>";
    public static final String SOS_SENTENCE = SOS_MARKER + "_" + SOS_MARKER + " ";
    public static final String EOS_SENTENCE = " " + EOS_MARKER + "_" + EOS_MARKER;

    ArrayList<String> tokenWordSet;
    ArrayList<String> tokenPosSet; // new ArrayList<>(Arrays.asList("CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB", "$", "#", "“", "”", "(", ")", ",", ".", ":"));

    public int[][] transitionCounts; // Pos * PoS
    public int[][] emissionCounts;   // Pos * Word
    public int[] posCounts;

    public double[][] transitionProbabilities; // Pos * PoS
    public double[][] emissionProbabilities;   // Pos * Word

    public HMM(String fileName) {
        setTokenSets(fileName);
        setCounts(fileName);
        setProbabilities();
    }

    // HMM's Viterbi Algorithm
    public String[] getPosTags(String sentence) throws Exception {
        sentence = SOS_MARKER + " " +  sentence.trim() + " " + EOS_MARKER;
        String[] words = sentence.split("\\s+");

        // COMPUTE TRELLIS MATRIX & BACKTRACE
        double[][] trellisMatrixProbabilities = new double[words.length][tokenPosSet.size()];
        int[][] trellisMatrixBackTrace = new int[words.length][tokenPosSet.size()];
        for (int i = 0; i < words.length; i++) {
            trellisMatrixProbabilities[i] = new double[tokenPosSet.size()];
            trellisMatrixBackTrace[i] = new int[tokenPosSet.size()];
            // INITIALIZE BACKTRACE
            for (int j = 0; j < tokenPosSet.size(); j++) {
                trellisMatrixBackTrace[i][j] = -1;
            }
        }
        // INITIALIZE TRELLIS MATRIX
        trellisMatrixProbabilities[0][0] = 1.0; // initial pos probability P(<s>) = 1

        for (int i = 1; i < words.length; i++) {
            int wordIndex = tokenWordSet.indexOf(words[i]);
            for (int j = 0; j < tokenPosSet.size(); j++) {
                double maxProb = 0.0;
                int previousPosIndex = -1;
                for (int k = 0; k < tokenPosSet.size(); k++) {
                    double pp = trellisMatrixProbabilities[i-1][k];
                    double tp = transitionProbabilities[k][j];
                    double ep = emissionProbabilities[j][wordIndex];
                    double prob = tp * ep * pp;
                    if (prob > maxProb) {
                        maxProb = prob;
                        previousPosIndex = k;
                    }
                }
                trellisMatrixProbabilities[i][j] = maxProb;
                trellisMatrixBackTrace[i][j] = previousPosIndex;
            }
        }


        // DECODE TRELLIS MATRIX
        int[] tagIndices = new int[words.length]; // remove the <s> and </s>

        double maxProb = 0.0;
        int posIndex = -1;
        for (int i = 0; i < tokenPosSet.size(); i++) {
            if (trellisMatrixProbabilities[words.length - 1][i] > maxProb) {
                maxProb = trellisMatrixProbabilities[words.length - 1][i];
                posIndex = i;
            }
        }
        if (posIndex == -1) {
            throw new Exception("zero probability");
        }

        tagIndices[words.length - 1] = trellisMatrixBackTrace[words.length - 1][posIndex];

        for (int i = words.length - 1; i >= 1; i--) {
            if (tagIndices[i] == -1) {
                throw new Exception("zero probability");
            }
            tagIndices[i-1] = trellisMatrixBackTrace[i-1][tagIndices[i]];
        }

        String[] tags = new String[words.length - 2]; // remove the <s> and </s>
        for (int i = 2; i < words.length; i++) {
            tags[i-2] = tokenPosSet.get(tagIndices[i]);
        }

        return tags;
    }

    private void setTokenSets(String fileName) {
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            Set<String> wordSet = new HashSet<>();
            Set<String> posSet = new HashSet<>();
            stream.forEach(sentence -> {
                String[] tokens = sentence.split("\\s+");
                for (String token : tokens) {
                    int index = token.lastIndexOf("_");
                    wordSet.add(token.substring(0, index));
                    posSet.add(token.substring(index + 1));
                }
            });
            tokenWordSet = new ArrayList<>(wordSet);
            tokenPosSet = new ArrayList<>(posSet);
            Collections.sort(tokenWordSet);
            Collections.sort(tokenPosSet);
            tokenWordSet.add(0, EOS_MARKER); // add to front of list
            tokenWordSet.add(0, SOS_MARKER); // add to front of list
            tokenPosSet.add(0, EOS_MARKER); // add to front of list
            tokenPosSet.add(0, SOS_MARKER); // add to front of list
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setCounts(String fileName) {
        int posSetSize = tokenPosSet.size();
        int wordSetSize = tokenWordSet.size();

        posCounts = new int[posSetSize];

        transitionCounts = new int[posSetSize][];
        for (int i = 0; i < posSetSize; i++) {
            transitionCounts[i] = new int[posSetSize];
        }

        emissionCounts = new int[posSetSize][];
        for (int i = 0; i < posSetSize; i++) {
            emissionCounts[i] = new int[wordSetSize];
        }

        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(sentence -> {
                sentence = SOS_SENTENCE + sentence + EOS_SENTENCE;
                String[] tokens = sentence.split("\\s+");

                int[] posIndices = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    int index = tokens[i].lastIndexOf("_");
                    String pos = tokens[i].substring(index + 1);
                    String word = tokens[i].substring(0, index);
                    int posIndex = tokenPosSet.indexOf(pos);
                    int wordIndex = tokenWordSet.indexOf(word);
                    emissionCounts[posIndex][wordIndex]++;
                    posCounts[posIndex]++;
                    posIndices[i] = posIndex;
                }
                for (int i = 1; i < tokens.length; i++) {
                    transitionCounts[posIndices[i-1]][posIndices[i]]++;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setProbabilities() {
        int posSetSize = tokenPosSet.size();
        int wordSetSize = tokenWordSet.size();

        transitionProbabilities = new double[posSetSize][];
        for (int i = 0; i < posSetSize; i++) {
            transitionProbabilities[i] = new double[posSetSize];
        }

        emissionProbabilities = new double[posSetSize][];
        for (int i = 0; i < posSetSize; i++) {
            emissionProbabilities[i] = new double[wordSetSize];
        }

        for (int i = 0; i < posSetSize; i++) {
            for (int j = 1; j < posSetSize; j++) {
                transitionProbabilities[i][j] = (double)transitionCounts[i][j] / (double)posCounts[i];
            }
            for (int j = 0; j < wordSetSize; j++) {
                emissionProbabilities[i][j] = (double)emissionCounts[i][j] / (double)posCounts[i];
            }
        }
    }

    // WARNING THIS METHOD TAKES ~1-2 minutes AND REQUIRES ~7GB of file-space
    public void prettyPrintEverything2Files(String directory) throws IOException {
        // WRITE POS-TO-INDEX
        BufferedWriter tokenToIndexWriter = new BufferedWriter(new FileWriter(directory + "0-pos-to-index.txt"));
        StringBuilder builder = new StringBuilder();
        builder.append("index = pos\n");
        for (int i = 0; i < tokenPosSet.size(); i++) {
            builder.append(i).append(" = ").append(tokenPosSet.get(i)).append("\n");
        }
        tokenToIndexWriter.write(builder.toString());
        tokenToIndexWriter.flush();
        tokenToIndexWriter.close();
        System.out.println("FINISHED WRITING 0-pos-to-index.txt");

        // WRITE WORD-TO-INDEX
        BufferedWriter wordToIndexWriter = new BufferedWriter(new FileWriter(directory + "1-word-to-index.txt"));
        builder.setLength(0); // clear/empty buffer
        builder.append("index = word\n");
        for (int i = 0; i < tokenWordSet.size(); i++) {
            builder.append(i).append(" = ").append(tokenWordSet.get(i)).append("\n");
        }
        wordToIndexWriter.write(builder.toString());
        wordToIndexWriter.flush();
        wordToIndexWriter.close();
        System.out.println("FINISHED WRITING 1-word-to-index.txt");

        // WRITE EMISSION COUNTS
        BufferedWriter emissionCountsWriter = new BufferedWriter(new FileWriter(directory + "2-emission-counts.txt"));
        builder.setLength(0); // clear/empty buffer
        builder.append("pos-index-1 word-index-2 = emission-count\n");
        for (int i = 0; i < tokenPosSet.size(); i++) {
            for (int j = 0; j < tokenWordSet.size(); j++) {
                builder.append("\n").append(i).append(" ").append(j).append(" = ").append(emissionCounts[i][j]);
            }
            emissionCountsWriter.write(builder.toString());
            builder.setLength(0); // clear/empty buffer
        }
        emissionCountsWriter.close();
        System.out.println("FINISHED WRITING 2-emission-counts.txt");

        // WRITE TRANSITION COUNTS
        BufferedWriter transitionCountsWriter = new BufferedWriter(new FileWriter(directory + "3-transition-counts.txt"));
        builder.setLength(0); // clear/empty buffer
        builder.append("pos-index-1 pos-index-2 = transition-count\n");
        for (int i = 0; i < tokenPosSet.size(); i++) {
            for (int j = 0; j < tokenPosSet.size(); j++) {
                builder.append("\n").append(i).append(" ").append(j).append(" = ").append(transitionCounts[i][j]);
            }
            transitionCountsWriter.write(builder.toString());
            builder.setLength(0); // clear/empty buffer
        }
        transitionCountsWriter.close();
        System.out.println("FINISHED WRITING 3-transition-counts.txt");

        // WRITE EMISSION PROBABILITIES
        BufferedWriter emissionProbWriter = new BufferedWriter(new FileWriter(directory + "4-emission-probabilities.txt"));
        builder.setLength(0); // clear/empty buffer
        builder.append("P(word-index|pos-index) = emission-probability\n");
        for (int i = 0; i < tokenPosSet.size(); i++) {
            for (int j = 0; j < tokenWordSet.size(); j++) {
                builder.append("\nP(").append(j).append("|").append(i).append(") = ").append(emissionProbabilities[i][j]);
            }
            emissionProbWriter.write(builder.toString());
            builder.setLength(0); // clear/empty buffer
        }
        emissionProbWriter.close();
        System.out.println("FINISHED WRITING 4-emission-probabilities.txt");

        // WRITE TRANSITION PROBABILITIES
        BufferedWriter transitionProbWriter = new BufferedWriter(new FileWriter(directory + "5-transition-probabilities.txt"));
        builder.setLength(0); // clear/empty buffer
        builder.append("P(pos-index-2|pos-index-1) = transition-probability\n");
        for (int i = 0; i < tokenPosSet.size(); i++) {
            for (int j = 0; j < tokenPosSet.size(); j++) {
                builder.append("\nP(").append(j).append("|").append(i).append(") = ").append(transitionProbabilities[i][j]);
            }
            transitionProbWriter.write(builder.toString());
            builder.setLength(0); // clear/empty buffer
        }
        transitionProbWriter.close();
        System.out.println("FINISHED WRITING 5-transition-probabilities.txt");
        System.out.println("if files are not shown, then possibly need to exit program");
    }
}
