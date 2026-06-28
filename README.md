# Java Quiz Application

A console-based quiz application built in Java that supports multiple question types, difficulty levels, high score tracking, and concurrent input handling.

---

## Features

- Three question types: MCQ, MSQ (multiple correct), and True/False
- Three difficulty levels: Easy, Medium, Hard
- Score tracking with persistent high score saved to file
- Input handled on a separate thread using `BlockingQueue`
- Custom exceptions for invalid answers and file errors
- Factory pattern to load questions dynamically from a text file

---

## Tech Stack

- Language: Java
- Concepts used: OOP, Abstract Classes, Interfaces, Enums, Multithreading, Factory Pattern, Custom Exceptions, File I/O

---

## Project Structure

```
newjavaproject/
├── QuizApp.java       # Main source file (all classes)
├── Questions          # Question bank (text file)
├── .gitignore
└── README.md
```

---

## How to Run

### Prerequisites
- Java JDK 8 or above installed
- Terminal or Command Prompt

### Compile
```bash
javac QuizApp.java
```

### Run
```bash
java QuizApp
```

---

## How It Works

1. On launch, you enter your player name
2. Select a difficulty level (Easy / Medium / Hard)
3. Questions are loaded from `Questions.txt` and filtered by difficulty
4. Each question is timed — input is read concurrently using a background thread
5. At the end, your score is displayed and compared against the saved high score
6. If you beat the high score, it gets updated in a local file

---

## Question Format (in `Questions` file)

```
MCQ|Easy|What is the size of int in Java?|4 bytes|2 bytes|8 bytes|1 byte|A
TF|Medium|Java supports multiple inheritance through classes.|False
MSQ|Hard|Which are valid access modifiers in Java?|public|private|protected|friend|A,B,C
```

---

## OOP Concepts Used

| Concept | Where Used |
|---|---|
| Abstract Class | `Question` base class |
| Interface | `Scorable`, `Persistable` |
| Enum | `Difficulty` (EASY, MEDIUM, HARD) |
| Factory Pattern | `QuestionFactory` loads and returns question objects |
| Custom Exceptions | `InvalidAnswerException`, `QuestionFileException` |
| Multithreading | `InputThread` uses `BlockingQueue` for timed input |

---

## Sample Output

```
Welcome to the Java Quiz App!
Enter your name: Varsha
Select difficulty: EASY / MEDIUM / HARD
> MEDIUM

Question 1 (MCQ): What does JVM stand for?
A. Java Virtual Machine
B. Java Variable Method
C. Java Verified Module
D. None of the above
Your answer: A
Correct!

...

Your Score: 7/10
High Score: 9/10 (held by Varsha)
```

---

## Author

**Varsha Shivanand K**  
B.Tech CSE (AI & ML) — RV University, Bengaluru  
[GitHub](https://github.com/varshashivanandk)
