import util.HMM;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        HMM HMM = null;
        String input;
        System.out.println("\n\n\n\n\n");
        do {
            System.out.print("\nEnter Command (h - list of commands): ");
            input = sc.nextLine();
            switch (input) {
                case "train":
                    HMM = train(sc);
                    break;
                case "ti":
                    if (isTrained(HMM)) computeSentenceProbability_Input(sc, HMM);
                    break;
                case "tf":
                    if (isTrained(HMM)) assignPosTagsToSentence_File(sc, HMM);
                    break;
                case "p":
                    if (isTrained(HMM)) HMM.prettyPrintEverything2Files("./pretty-print/");
                    break;
                case "q":
                    System.exit(0);
                    break;
                case "h":
                    printHelp();
                    break;
            }
        } while(true);
    }

    private static boolean isTrained(HMM HMM) throws InterruptedException {
        if (HMM == null) {
            System.err.println("\nTraining Required (use `train` command)");
            Thread.sleep(500);
            return false;
        } else {
            return true;
        }
    }

    private static HMM train(Scanner sc) throws InterruptedException {
        boolean repeat;
        do {
            repeat = false;
            System.out.print("\nEnter File Name (default ./NLP6320_POSTaggedTrainingSet-Unix.txt): ");
            String fileName = sc.nextLine();
            if (fileName.isEmpty()) {
                fileName = "NLP6320_POSTaggedTrainingSet-Unix.txt";
            }

            try (BufferedReader brTest = new BufferedReader(new FileReader(fileName))) {
                brTest.close();
                System.out.println("Training Started");
                HMM hmm = new HMM("./NLP6320_POSTaggedTrainingSet-Unix.txt");
                System.out.println("Training Ended");
                return hmm;
            } catch (FileNotFoundException e) {
                System.err.println("\nFile Not Found");
                Thread.sleep(500);
                repeat = true;
            } catch (IOException e) {
                System.err.println("\nIOException (aborting back to main menu)");
                Thread.sleep(500);
            }
        } while(repeat);
        return null;
    }

    private static void assignPosTagsToSentence_File(Scanner sc, HMM HMM) throws InterruptedException {
        boolean repeat;
        do {
            repeat = false;
            System.out.print("\nEnter File Name (default ./sentence.txt): ");
            String fileName = sc.nextLine();
            if (fileName.isEmpty()) {
                fileName = "./sentence.txt";
            }

            try (BufferedReader brTest = new BufferedReader(new FileReader(fileName))) {
                String sentence = brTest.readLine();
                System.out.println("\nSentence Got:\n" + sentence);

                printAssignPosTags(sentence, HMM);
            } catch (FileNotFoundException e) {
                System.err.println("\nFile Not Found");
                Thread.sleep(500);
                repeat = true;
            } catch (IOException e) {
                System.err.println("\nIOException (aborting back to main menu)");
                Thread.sleep(500);
            }
        } while(repeat);
    }

    private static void computeSentenceProbability_Input(Scanner sc, HMM HMM) throws InterruptedException {
        String defaultSentence = "Brainpower , not physical plant , is now a firm 's chief asset .";
        System.out.print("\nEnter Test Sentence (default `" + defaultSentence + "`): ");
        String sentence = sc.nextLine();
        if (sentence.isEmpty()) {
            sentence = defaultSentence;
        }
        System.out.println("\nSentence Got:\n" + sentence);

        printAssignPosTags(sentence, HMM);
    }

    private static void printAssignPosTags(String sentence, HMM HMM) throws InterruptedException {
        try {
            String[] tags = HMM.getPosTags(sentence);
            System.out.println("\nAssigned Tags: " + Arrays.toString(tags));
        } catch (Exception e) {
            System.err.println(e.toString());
            Thread.sleep(500);
        }
    }

    private static void printHelp() {
        System.out.println(
                "  train - train bigram model\n" +
                "  ti - input sentence to compute probability (<s> & </s> added automatically)\n" +
                "  tf - input file to compute probability of sentence in the first line of file\n" +
                "  p - save 6 files into pretty-print directory (WARNING TAKES ~1-2 minutes AND REQUIRES ~7GB of file-space)\n" +
                "      files saved:\n" +
                "       - 0-token-to-index.txt\n" +
                "       - 1-bigram-counts.txt\n" +
                "       - 2-unigram-counts.txt\n" +
                "       - 3-prob-no-smoothing.txt\n" +
                "       - 4-prob-add-one-smoothing.txt\n" +
                "       - 5-prob-gt-discount.txt\n" +
                "  h - help menu\n" +
                "  q - quit");
    }
}
