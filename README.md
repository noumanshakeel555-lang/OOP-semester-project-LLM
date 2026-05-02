# QuizSecureX 🛡️📚

A full-featured, desktop-based **secure online quiz system** built entirely in **Java Swing**.
Designed for schools and labs — teachers manage classes and quizzes, students take them under
a multi-layer anti-cheat system, and results are stored persistently in a local SQLite database.

---

## Screenshots

> Login → Student Dashboard → Quiz Attempt (Fullscreen) → Results  
> Teacher Dashboard → Class Management → Mark Written Answers

---

## Features

### 👨‍🎓 Student
- Sign up and log in as a Student
- See only the quizzes assigned to your enrolled class
- Take quizzes in a **locked fullscreen** environment with a live countdown timer
- Navigate between questions (Previous / Next) with answers preserved
- Get an instant score for MCQ questions; written answers marked by the teacher
- View full attempt history — scores, percentages, dates, and grade labels — in **My Results**
- Cannot attempt the same quiz twice (completed quizzes are locked on the dashboard)

### 👩‍🏫 Teacher
- Sign up and log in as a Teacher
- **My Classes** — create school classes, enrol students by username, assign quizzes
- **My Quizzes** — view all created quizzes with class assignment and question count
- **Create New Quiz** — step-by-step wizard to build MCQ and/or Subjective quizzes,
  assign them to a class in the same flow
- **Student Results** — see every attempt from enrolled students: score, percentage,
  pending marks, violations, and date; click any row to open the full answer review
- **Mark Answers** — grade written (Subjective) answers with a marks spinner per question;
  the student's score updates in the database the moment you save

### 🛡️ Anti-Cheat System (7 Layers)

#### Layer 1 — Fullscreen Lock
OS-level exclusive fullscreen via `GraphicsDevice.setFullScreenWindow()`. The window is
`setUndecorated(true)` so there is no title bar, no close button, and no taskbar. Falls back
to `MAXIMIZED_BOTH` + `setAlwaysOnTop(true)` on systems where exclusive fullscreen is not
supported.

#### Layer 2 — Expanded Key Blocker
A `KeyEventDispatcher` is installed at JVM level — it fires before any component processes
the event. Instead of checking modifier bits (which don't work for the Windows key), the
blocker **manually tracks Windows key state** with a `volatile boolean windowsKeyDown` flag:

- `VK_WINDOWS KEY_PRESSED` → sets flag to `true`, consumes event
- `VK_WINDOWS KEY_RELEASED` → clears flag to `false`
- **Any key while `windowsKeyDown == true` → consumed and blocked**

This single rule catches every Win+X combo: `Win+Tab` (Task View), `Win+D` (Show Desktop),
`Win+Ctrl+D` (New Virtual Desktop), `Win+Ctrl+Left/Right` (Switch Virtual Desktop),
`Win+Ctrl+F4` (Close Virtual Desktop), `Win+R`, `Win+anything`.

Additional blocks: `Alt+Tab`, `Cmd+Tab`, `Alt+F4`, `Cmd+Q`, `Escape`, `PrintScreen`.

#### Layer 3 — Focus Loss Detection
A `WindowFocusListener` fires `windowLostFocus()` the instant the quiz window loses focus
for **any reason** — including kernel-level shortcuts the key blocker couldn't fully stop,
notification popups, or virtual desktop switches. Each loss-of-focus counts as one violation.
The window immediately calls `toFront()` + `requestFocus()` to fight back.

#### Layer 4 — Blackout Overlay
A separate `JWindow` (not a JFrame — so it has no taskbar button) is created at startup and
pre-sized to cover **every connected monitor** by taking the union of all `GraphicsDevice`
bounds. The instant `windowLostFocus` fires:

1. The blackout window becomes visible and calls `setAlwaysOnTop(true)`
2. A `Timer` hammers `toFront()` + `requestFocus()` **every 100 ms for 1.5 seconds**
   so Windows Task View animation cannot stay on top of it

The blackout shows a plain black screen with a white "⚠ Quiz Paused — this has been
recorded as a violation" message. The student **cannot read any quiz content, cannot see any
copied text, and cannot take a useful screenshot** while away. The blackout disappears the
moment focus returns.

#### Layer 5 — Clipboard Wiper
A background **daemon thread** starts before the first question loads and runs a tight loop:
every **300 ms** it checks whether the system clipboard contains any text — if it does, it
immediately replaces the contents with an empty `StringSelection`. The thread also fires
once at the very start (to clear anything already on the clipboard before the quiz begins)
and fires once more in `windowGainedFocus` to catch the window between the student's last
copy and the thread's next 300 ms tick.

The attack this defeats: copy answer from browser → Win+Tab to quiz → Ctrl+V. By the time
they switch back, the clipboard is already empty.

#### Layer 6 — Paste Blocker on Answer Fields
The subjective answer `JTextArea` has its `InputMap` overridden with dead no-op actions:

| Keystroke | Blocked |
|---|---|
| `Ctrl+V` / `Cmd+V` | Paste |
| `Ctrl+C` / `Cmd+C` | Copy |
| `Ctrl+X` / `Cmd+X` | Cut |
| `Ctrl+A` / `Cmd+A` | Select All |

`setComponentPopupMenu(null)` removes the right-click context menu entirely. Mouse press
events that would trigger a popup are consumed. Students must type their own answers —
there is no way to inject copied text even if the clipboard wiper somehow missed a window.

#### Layer 7 — Warning Overlay + Auto-Submit
A `JPanel` on `JLayeredPane.PALETTE_LAYER` (above all content) slides in as a red gradient
banner with a shake animation on each violation. Three red dot indicators in the nav bar
fill permanently. After **3 violations** the quiz is force-submitted with a 1.8-second
countdown message, and the result dialog shows a red note with the violation count for the
teacher to see.

---

**Summary table:**

| Layer | Defeats |
|---|---|
| Fullscreen + no title bar | Clicking out, taskbar access |
| Win key state tracker | Win+Tab, Win+D, Win+Ctrl+D, all virtual desktop shortcuts |
| Focus loss detection | Any remaining OS-level switch |
| Blackout overlay (all monitors, persistent toFront) | Reading quiz content, screenshotting, Task View |
| Clipboard wiper (300 ms daemon) | Copy from browser → paste into quiz |
| Paste blocker + no right-click menu | Pasting even if clipboard wiper missed a tick |
| Auto-submit at 3 violations | Repeated switching attempts |

Auto-submit fires after **3 violations** (each focus-loss = 1 violation).

### 💾 Database (SQLite)
All data persists across sessions in a local `quizpro.db` file:

| Table | Stores |
|---|---|
| `users` | Teachers and students |
| `classes` | School classes with teacher ownership |
| `class_students` | Student enrolment (many-to-many) |
| `quizzes` | Quiz titles and class assignments |
| `class_quizzes` | Quiz-to-class assignments |
| `questions` | MCQ and Subjective questions |
| `question_options` | MCQ answer options |
| `attempts` | Score, total, pending marks, violations, timestamp |
| `attempt_answers` | Each student answer + awarded marks per question |

Schema is **auto-migrated** on startup — existing data is never lost when columns are added.

---

## Project Structure

```
QuizSecureX/
│
├── src/
│   └── main/
│       ├── app/
│       │   └── Launcher.java              # Entry point
│       │
│       ├── db/
│       │   └── DatabaseManager.java       # SQLite init, migration, helpers
│       │
│       ├── model/
│       │   ├── User.java                  # Abstract base
│       │   ├── Student.java
│       │   ├── Teacher.java
│       │   ├── Quiz.java
│       │   ├── Question.java              # Abstract base
│       │   ├── MCQQuestion.java
│       │   ├── SubjectiveQuestion.java    # evaluate() always returns 0
│       │   ├── SchoolClass.java
│       │   ├── Attempt.java
│       │   ├── AttemptRecord.java         # Full attempt with answers + violations
│       │   └── Result.java               # score + totalMarks + pendingMarks
│       │
│       ├── dao/
│       │   ├── UserDAO.java
│       │   ├── QuizDAO.java               # Saves questions + options in one transaction
│       │   ├── ClassDAO.java
│       │   ├── AttemptDAO.java            # saveFullAttempt, awardSubjectiveMarks
│       │   └── AccessDAO.java            # Class-based quiz access control
│       │
│       ├── service/
│       │   ├── AuthService.java
│       │   ├── QuizService.java
│       │   ├── ClassService.java          # Enrolment, quiz assignment, student queries
│       │   └── AttemptService.java        # Console fallback
│       │
│       └── gui/
│           ├── UITheme.java               # Central design system (colors, fonts, components)
│           ├── LoginFrame.java
│           ├── StudentDashboardFrame.java # Two tabs: My Quizzes + My Results
│           ├── TeacherDashboardFrame.java # Sidebar nav with 5 panels
│           ├── QuizAttemptFrame.java      # Fullscreen + all anti-cheat layers
│           └── QuizCreateFrame.java
│
└── lib/
    ├── sqlite-jdbc-3.43.2.2.jar
    ├── slf4j-api-2.0.9.jar
    └── slf4j-simple-2.0.9.jar
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| GUI | Java Swing (`javax.swing`, `java.awt`) |
| Database | SQLite via `sqlite-jdbc` |
| Build | Manual `javac` / any Java IDE |
| Persistence | Local file `quizpro.db` (auto-created) |

---

## Setup & Installation

### Prerequisites
- Java 17 or higher
- The three JAR files in your `lib/` folder (see below)

### Step 1 — Download the SQLite driver

Download these three JARs and place them in `lib/`:

| JAR | Download |
|---|---|
| `sqlite-jdbc-3.43.2.2.jar` | https://github.com/xerial/sqlite-jdbc/releases/tag/3.43.2.2 |
| `slf4j-api-2.0.9.jar` | https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/ |
| `slf4j-simple-2.0.9.jar` | https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/ |

### Step 2 — Add to classpath (IntelliJ IDEA)
1. `File → Project Structure → Modules → Dependencies`
2. Click `+` → `JARs or Directories`
3. Select all three JARs → OK → Apply

### Step 3 — Add to classpath (Eclipse)
1. Right-click project → `Build Path → Add External Archives`
2. Select all three JARs → Open

### Step 4 — Compile & run (Command Line)

**Windows:**
```bat
javac -cp ".;lib\sqlite-jdbc-3.43.2.2.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-simple-2.0.9.jar" ^
      -d . ^
      src\main\db\*.java src\main\model\*.java src\main\dao\*.java ^
      src\main\service\*.java src\main\app\*.java src\main\gui\*.java

java -cp ".;lib\sqlite-jdbc-3.43.2.2.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-simple-2.0.9.jar" ^
     main.app.Launcher
```

**macOS / Linux:**
```bash
javac -cp ".:lib/sqlite-jdbc-3.43.2.2.jar:lib/slf4j-api-2.0.9.jar:lib/slf4j-simple-2.0.9.jar" \
      -d . \
      src/main/db/*.java src/main/model/*.java src/main/dao/*.java \
      src/main/service/*.java src/main/app/*.java src/main/gui/*.java

java -cp ".:lib/sqlite-jdbc-3.43.2.2.jar:lib/slf4j-api-2.0.9.jar:lib/slf4j-simple-2.0.9.jar" \
     main.app.Launcher
```

> **Tip:** Create a `run.bat` (Windows) or `run.sh` (Mac/Linux) with the above commands so you only type `run` each time.

On first launch you will see:
```
[DB] Initialized — quizpro.db
```
The login window opens and `quizpro.db` is created in your working directory.

---

## Usage Walkthrough

### As a Teacher

1. **Sign up** → select `TEACHER` → log in
2. **My Classes** → `Create Class` → e.g. `Grade 10 — Section A`
3. Select the class → **Students tab** → `Enrol Student` → enter a student's username
4. **Create New Quiz** → Launch Quiz Builder → enter title → set question count → choose the class to assign → add questions (MCQ or Subjective)
5. **Student Results** → table of all attempts → click a row to view every answer with colour-coded marks
6. **Mark Answers** → all attempts with ungraded written answers → click a row → enter marks with the spinner → `Save Marks`

### As a Student

1. **Sign up** → select `STUDENT` → log in
2. If not enrolled: a placeholder card is shown — ask your teacher to add you
3. Once enrolled: only your class's quizzes appear → click `Start Quiz`
4. Quiz opens in **fullscreen anti-cheat mode** — 2-minute timer, violation warnings
5. On submit: MCQ score shown instantly; written answers show "⏳ pending teacher review"
6. **My Results tab** shows your full history: score, percentage (colour-coded), grade label, date, and whether any marks are still pending

---

## Design System

All visual styling lives in `UITheme.java`. To change the colour scheme or fonts, edit it once and every screen updates automatically.

Key design tokens:

```java
UITheme.SIDEBAR_BG   // #0F1535  — dark navy sidebar
UITheme.PRIMARY      // #3D6FFF  — main blue
UITheme.ACCENT       // #845EFF  — purple accent
UITheme.SUCCESS      // #00C48C  — green
UITheme.DANGER       // #FF4757  — red
UITheme.WARNING      // #FFB300  — amber
UITheme.BG           // #F0F4FF  — page background
UITheme.SURFACE      // #FFFFFF  — card surface
```

---

## Architecture

The project follows a clean **4-layer architecture**:

```
GUI (Swing frames)
      ↓
Service layer (business logic)
      ↓
DAO layer (database access — prepared statements only)
      ↓
SQLite (quizpro.db)
```

- **No raw SQL string concatenation** for user data — all queries use `PreparedStatement`
- **Transactions** used for multi-table writes (quiz save, attempt save, mark awarding)
- **Schema migration** runs at every startup via `safeAddColumn()` — safe to run on existing databases

---

## Known Limitations

- Passwords are stored as plain text — add bcrypt (e.g. `jBCrypt`) for production use
- Data lives in a local file — for multi-machine use, swap SQLite for a networked DB (MySQL/PostgreSQL); the DAO interfaces won't need to change
- Win+Tab Task View is a Windows kernel shortcut and cannot be fully blocked from Java; the blackout window and clipboard wiper mitigate it effectively
- Quiz timer is 2 minutes (hardcoded in `QuizAttemptFrame.TIME_LIMIT_SECONDS`) — make this configurable per quiz for production

---

## Contributing

Pull requests welcome. Before opening one:
1. Keep the 4-layer separation — GUI must not talk to DAOs directly
2. Use `PreparedStatement` for all queries
3. Wrap multi-table writes in `DatabaseManager.beginTransaction()` / `commit()` / `rollback()`
4. All new UI components should use `UITheme` factories (`UITheme.primaryBtn()`, `UITheme.card()`, etc.)

---

## License

This project was built as a university lab assignment. Feel free to use, modify, and extend it.

---

*Built with Java Swing · SQLite · No external UI frameworks*
