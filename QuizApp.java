import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.awt.Toolkit;

// ENUM
enum Difficulty {
    EASY(1), MEDIUM(2), HARD(3);
    private final int marks;
    Difficulty(int marks) { this.marks = marks; }
    public int getMarks() { return marks; }
}
// INTERFACES
interface Scorable {
    int  getScore();
    void updateScore(int delta);
}

interface Persistable {
    void save() throws IOException;
    void load() throws IOException;
}

abstract class Question {
    protected final String     questionText;
    protected final String[]   options;
    protected final int        correctAnswer;
    protected final Difficulty difficulty;

    protected Question(String questionText, String[] options,
                       int correctAnswer, Difficulty difficulty) {
        this.questionText  = questionText;
        this.options       = options;
        this.correctAnswer = correctAnswer;
        this.difficulty    = difficulty;
    }

    public abstract int     getMarks();
    public abstract String  getHint();
    public abstract boolean checkAnswer(String answer);
    public abstract String  getCorrectAnswerText();
    public abstract String  getType();

    public String     getQuestionText()  { return questionText; }
    public String[]   getOptions()       { return options; }
    public Difficulty getDifficulty()    { return difficulty; }
}

class MCQQuestion extends Question {
    private final String hint;

    public MCQQuestion(String questionText, String[] options,
                       int correctAnswer, Difficulty difficulty, String hint) {
        super(questionText, options, correctAnswer, difficulty);
        this.hint = hint;
    }

    @Override public int    getMarks() { return difficulty.getMarks(); }
    @Override public String getHint()  { return hint; }
    @Override public String getType()  { return "MCQ"; }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null || answer.isEmpty()) return false;
        String lower = answer.trim().toLowerCase();
        if (lower.length() != 1) return false;
        char ch = lower.charAt(0);
        if (ch < 'a' || ch > 'd') return false;
        return (ch - 'a' + 1) == correctAnswer;
    }

    @Override
    public String getCorrectAnswerText() {
        return String.valueOf((char) ('a' + correctAnswer - 1));
    }
}

class MSQQuestion extends Question {
    private final String       hint;
    private final Set<Integer> correctAnswers;

    public MSQQuestion(String questionText, String[] options,
                       Set<Integer> correctAnswers, Difficulty difficulty, String hint) {
        super(questionText, options, 0, difficulty);
        this.correctAnswers = correctAnswers;
        this.hint           = hint;
    }

    @Override public int    getMarks() { return difficulty.getMarks(); }
    @Override public String getHint()  { return hint; }
    @Override public String getType()  { return "MSQ"; }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null || answer.isEmpty()) return false;
        String[] tokens = answer.split("[,\\s]+");
        Set<Integer> provided = new HashSet<>();
        for (String token : tokens) {
            String lower = token.trim().toLowerCase();
            if (lower.isEmpty()) continue;
            if (lower.length() != 1) return false;
            char ch = lower.charAt(0);
            if (ch < 'a' || ch > 'd') return false;
            provided.add(ch - 'a' + 1);
        }
        return !provided.isEmpty() && provided.equals(correctAnswers);
    }

    @Override
    public String getCorrectAnswerText() {
        List<Integer> sorted = new ArrayList<>(correctAnswers);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append((char) ('a' + sorted.get(i) - 1));
        }
        return sb.toString();
    }
}

class TrueFalseQuestion extends Question {
    private static final String[] TF_OPTIONS = {"a. True", "b. False"};

    public TrueFalseQuestion(String questionText, int correctAnswer) {
        super(questionText, TF_OPTIONS, correctAnswer, Difficulty.EASY);
    }

    @Override public int    getMarks() { return 1; }
    @Override public String getHint()  { return "Only two options - True or False!"; }
    @Override public String getType()  { return "True/False"; }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null || answer.isEmpty()) return false;
        String lower = answer.trim().toLowerCase();
        if (lower.length() != 1) return false;
        char ch = lower.charAt(0);
        if (ch != 'a' && ch != 'b') return false;
        return (ch - 'a' + 1) == correctAnswer;
    }

    @Override
    public String getCorrectAnswerText() {
        return correctAnswer == 1 ? "a (True)" : "b (False)";
    }
}

class InvalidAnswerException extends Exception {
    public InvalidAnswerException(String message) { super(message); }
}

class QuestionFileException extends Exception {
    public QuestionFileException(String message) { super(message); }
}

class Player implements Scorable, Persistable {
    private final String       name;
    private       int          score;
    private       int          highScore;
    private       boolean[]    attempted;
    private final List<String> reviewLog;

    private static int totalPlayers = 0;

    static {
        System.out.println("[System] Quiz engine loaded.");
        totalPlayers = 0;
    }

    {
        reviewLog = new ArrayList<>();
        totalPlayers++;
    }

    public Player(String name) {
        this.name      = name;
        this.score     = 0;
        this.highScore = 0;
    }

    public void    initAttempted(int n) { this.attempted = new boolean[n]; }
    public void    markAttempted(int i) { if (attempted != null && i >= 0 && i < attempted.length) attempted[i] = true; }
    public boolean wasAttempted(int i)  { return attempted != null && i >= 0 && i < attempted.length && attempted[i]; }

    @Override public int  getScore()             { return score; }
    @Override public void updateScore(int delta) { score = Math.max(0, score + delta); }

    public String       getName()          { return name; }
    public int          getHighScore()     { return highScore; }
    public List<String> getReview()        { return reviewLog; }
    public void         addReview(String r){ reviewLog.add(r); }
    public boolean      isNewHighScore()   { return score > highScore; }

    public static int getTotalPlayers() { return totalPlayers; }

    @Override
    public void save() throws IOException {
        if (score > highScore) highScore = score;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(name + "_highscore.txt"))) {
            bw.write(String.valueOf(highScore));
        }
    }

    @Override
    public void load() throws IOException {
        File file = new File(name + "_highscore.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line != null) {
                try {
                    highScore = Integer.parseInt(line.trim());
                } catch (NumberFormatException e) {
                    System.out.println("Note: High score file corrupt - starting fresh.");
                    highScore = 0;
                }
            }
        }
    }
}

class QuestionFactory {

    public static List<Question> loadFromFile(String filename) throws QuestionFileException {
        List<Question> list = new ArrayList<>();
        File file = new File(filename);
        if (!file.exists()) throw new QuestionFileException("File not found: " + filename);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|");

                switch (p[0].trim().toUpperCase()) {
                    case "MCQ": {
                        if (p.length < 9) { System.out.println("Warning: Malformed MCQ at line " + lineNumber); continue; }
                        String[] opts = {"a. "+p[2].trim(), "b. "+p[3].trim(), "c. "+p[4].trim(), "d. "+p[5].trim()};
                        String cs = p[6].trim().toLowerCase();
                        if (cs.length() != 1 || cs.charAt(0) < 'a' || cs.charAt(0) > 'd') { System.out.println("Warning: Invalid MCQ answer at line " + lineNumber); continue; }
                        list.add(new MCQQuestion(p[1].trim(), opts, cs.charAt(0)-'a'+1, Difficulty.valueOf(p[7].trim().toUpperCase()), p[8].trim()));
                        break;
                    }
                    case "MSQ": {
                        if (p.length < 9) { System.out.println("Warning: Malformed MSQ at line " + lineNumber); continue; }
                        String[] opts = {"a. "+p[2].trim(), "b. "+p[3].trim(), "c. "+p[4].trim(), "d. "+p[5].trim()};
                        Set<Integer> cs = new HashSet<>();
                        for (String t : p[6].split(",")) {
                            String l = t.trim().toLowerCase();
                            if (l.length() == 1 && l.charAt(0) >= 'a' && l.charAt(0) <= 'd') cs.add(l.charAt(0)-'a'+1);
                        }
                        if (cs.isEmpty()) { System.out.println("Warning: No valid answers at line " + lineNumber); continue; }
                        list.add(new MSQQuestion(p[1].trim(), opts, cs, Difficulty.valueOf(p[7].trim().toUpperCase()), p[8].trim()));
                        break;
                    }
                    case "TF": {
                        if (p.length < 3) { System.out.println("Warning: Malformed TF at line " + lineNumber); continue; }
                        String cs = p[2].trim().toLowerCase();
                        if (cs.length() != 1 || (cs.charAt(0) != 'a' && cs.charAt(0) != 'b')) { System.out.println("Warning: Invalid TF answer at line " + lineNumber); continue; }
                        list.add(new TrueFalseQuestion(p[1].trim(), cs.charAt(0)-'a'+1));
                        break;
                    }
                    default:
                        System.out.println("Warning: Unknown type '" + p[0] + "' at line " + lineNumber);
                }
            }
        } catch (IOException e) {
            throw new QuestionFileException("Error reading file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new QuestionFileException("Invalid difficulty in file: " + e.getMessage());
        }

        if (list.isEmpty()) throw new QuestionFileException("No valid questions found in " + filename);
        return list;
    }
}

class InputThread extends Thread {
    private final Scanner               scanner;
    private final BlockingQueue<String> queue;

    public InputThread(Scanner scanner, BlockingQueue<String> queue) {
        this.scanner = scanner;
        this.queue   = queue;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String line = scanner.nextLine().trim();
                queue.put(line);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (NoSuchElementException | IllegalStateException e) {
        }
    }
}

public class QuizApp {

    private static final int    TIME_PER_QUESTION = 15;
    private static final String QUESTIONS_FILE    = "questions.txt";
    private static final String BIG   = "=".repeat(100);
    private static final String SMALL = "-".repeat(100);

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println(BIG);
        System.out.println("                         JAVA QUIZ APPLICATION");
        System.out.println(BIG);

        List<Question> questions;
        try {
            questions = QuestionFactory.loadFromFile(QUESTIONS_FILE);
            System.out.println("[OK] Loaded " + questions.size() + " questions.\n");
        } catch (QuestionFileException e) {
            System.out.println("[ERROR] " + e.getMessage());
            sc.close();
            return;
        }

        System.out.print("Enter your name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) name = "Anonymous";

        Player player = new Player(name);
        player.initAttempted(questions.size());

        try { player.load(); }
        catch (IOException e) { System.out.println("Note: Could not load previous high score."); }

        System.out.println("\nWelcome, " + player.getName() + "!");
        System.out.println("Previous High Score : " + player.getHighScore());
        System.out.println("Total Players       : " + Player.getTotalPlayers());
        System.out.println("\n" + SMALL);
        System.out.println("RULES:");
        System.out.println(SMALL);
        System.out.println("  - Type your answer letter (a/b/c/d) and press Enter.");
        System.out.println("  - For MSQ, separate with commas: e.g. a,c");
        System.out.println("  - Type 'h' to get a hint.");
        System.out.println("  - Type 'q' to quit.");
        System.out.println("  - Wrong answer: -1 mark (score floor 0)");
        System.out.println("  - Timer: " + TIME_PER_QUESTION + "s per question.");
        System.out.println(SMALL);
        System.out.print("\nPress Enter to start...");
        System.out.flush();
        sc.nextLine();

        Collections.shuffle(questions);
        int maxScore = questions.stream().mapToInt(Question::getMarks).sum();

        BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
        new InputThread(sc, inputQueue).start();

        boolean quit = false;

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);

            System.out.println("\n" + SMALL);
            System.out.printf("Q%d of %d  [%s | %s | +%d mark%s]%n",
                i + 1, questions.size(),
                q.getType(), q.getDifficulty(),
                q.getMarks(), q.getMarks() > 1 ? "s" : "");
            System.out.println(q.getQuestionText());

            if (q instanceof MSQQuestion)
                System.out.println("(Select ALL correct - e.g. a,c)");

            for (String opt : q.getOptions())
                System.out.println("  " + opt);
            System.out.println();

            String answerLine = readWithTimeout(inputQueue, TIME_PER_QUESTION);

            if (answerLine == null) {
                System.out.println("  [TIME] Time's up! No answer recorded.");
                player.addReview(String.format("Q%d [%s|%s]: Timed Out  |  Correct Answer: %s",
                    i+1, q.getType(), q.getDifficulty(), q.getCorrectAnswerText()));
                continue;
            }

            if (answerLine.equalsIgnoreCase("q")) {
                System.out.println("  Quiz exited early.");
                quit = true;
                break;
            }

            if (answerLine.equalsIgnoreCase("h")) {
                System.out.println("  [HINT] " + q.getHint());
                System.out.print("  Your answer: ");
                System.out.flush();
                answerLine = readWithTimeout(inputQueue, TIME_PER_QUESTION);
                if (answerLine == null) {
                    System.out.println("  [TIME] Time's up after hint!");
                    player.addReview(String.format("Q%d [%s|%s]: Timed Out  |  Correct: %s",
                        i+1, q.getType(), q.getDifficulty(), q.getCorrectAnswerText()));
                    continue;
                }
            }

            player.markAttempted(i);

            try {
                validateFormat(answerLine, q);
            } catch (InvalidAnswerException e) {
                System.out.println("  [X] Invalid answer: " + answerLine + " | " + e.getMessage());
                player.addReview(String.format("Q%d [%s|%s]: Invalid Input  |  Correct Answer: %s",
                    i+1, q.getType(), q.getDifficulty(), q.getCorrectAnswerText()));
                continue;
            }

            if (q.checkAnswer(answerLine)) {
                int earned = q.getMarks();
                player.updateScore(earned);
                System.out.println("  [OK] Your answer: " + answerLine + " | Correct! +" + earned + " mark" + (earned > 1 ? "s" : ""));
                player.addReview(String.format("Q%d [%s|%s]: Correct  +%d",
                    i+1, q.getType(), q.getDifficulty(), earned));
            } else {
                player.updateScore(-1);
                System.out.println("  [X] Wrong! Your answer: " + answerLine + " | Correct: " + q.getCorrectAnswerText());
                player.addReview(String.format("Q%d [%s|%s]: Wrong -1  |  Correct Answer: %s",
                    i+1, q.getType(), q.getDifficulty(), q.getCorrectAnswerText()));
            }
            System.out.println("  Score: " + player.getScore());
        }

        if (!quit) {
            for (int i = 0; i < questions.size(); i++) {
                if (!player.wasAttempted(i)) {
                    Question q = questions.get(i);
                    player.addReview(String.format("Q%d [%s|%s]: Not Attempted  |  Correct Answer: %s",
                        i+1, q.getType(), q.getDifficulty(), q.getCorrectAnswerText()));
                }
            }
        }

        System.out.println("\n" + BIG);
        System.out.println("                              RESULTS");
        System.out.println(BIG);
        System.out.println("Player        : " + player.getName());
        System.out.println("Your Score    : " + player.getScore() + " / " + maxScore);
        System.out.println("Previous Best : " + player.getHighScore());

        try {
            boolean wasNew = player.isNewHighScore();
            player.save();
            if (wasNew) System.out.println("New High Score saved!");
        } catch (IOException e) {
            System.out.println("Error saving: " + e.getMessage());
        }

        double pct = maxScore > 0 ? (double) player.getScore() / maxScore * 100 : 0;
        if      (pct >= 80) System.out.println("Performance   : Outstanding!");
        else if (pct >= 60) System.out.println("Performance   : Excellent!");
        else if (pct >= 40) System.out.println("Performance   : Good Job!");
        else                System.out.println("Performance   : Keep Practicing!");

        System.out.println("\n" + BIG);
        System.out.println("                           ANSWER REVIEW");
        System.out.println(BIG);
        List<String> review = player.getReview();
        for (int i = 0; i < review.size(); i++)
            System.out.println((i + 1) + ". " + review.get(i));

        sc.close();
        System.out.println("\nThanks for playing, " + player.getName() + "!");
        System.exit(0);
    }

    static String readWithTimeout(BlockingQueue<String> inputQueue, int timeoutSeconds) {

        System.out.println("[TIMER] " + timeoutSeconds + "s remaining  ");
        System.out.print("Your answer (h=hint, q=quit): ");
        System.out.flush();

        long deadline    = System.currentTimeMillis() + (long) timeoutSeconds * 1000;
        int  lastPrinted = timeoutSeconds;

        try {
            while (true) {
                long remainMs = deadline - System.currentTimeMillis();
                int  remInt   = (int) Math.max(0, (remainMs + 999) / 1000);

                if (remInt <= 0) {
                    eraseTimerLine();
                    Toolkit.getDefaultToolkit().beep();
                    inputQueue.clear();
                    return null;
                }

                if (remInt != lastPrinted) {
                    lastPrinted = remInt;
                    System.out.print("\u001b7\u001b[1A\u001b[2K[TIMER] " + remInt + "s remaining  \u001b8");
                    System.out.flush();
                }

                String line = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (line != null) {
                    eraseTimerLine();
                    return line;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            eraseTimerLine();
            inputQueue.clear();
            return null;
        }
    }

    private static void eraseTimerLine() {
        System.out.print("\u001b7\u001b[1A\u001b[2K\u001b8");
        System.out.flush();
    }

    private static void validateFormat(String answer, Question q) throws InvalidAnswerException {
        if (answer == null || answer.trim().isEmpty())
            throw new InvalidAnswerException("Answer cannot be empty.");

        if (q instanceof MSQQuestion) {
            String[] tokens = answer.split("[,\\s]+");
            for (String t : tokens) {
                if (t.isEmpty()) continue;
                String lower = t.trim().toLowerCase();
                if (lower.length() != 1 || lower.charAt(0) < 'a' || lower.charAt(0) > 'd')
                    throw new InvalidAnswerException("Enter letters a-d separated by commas (e.g. a,c,d).");
            }
        } else {
            String lower = answer.trim().toLowerCase();
            char   last  = (char) ('a' + q.getOptions().length - 1);
            if (lower.length() != 1 || lower.charAt(0) < 'a' || lower.charAt(0) > last)
                throw new InvalidAnswerException("Enter a single letter from a to " + last + ".");
        }
    }
}