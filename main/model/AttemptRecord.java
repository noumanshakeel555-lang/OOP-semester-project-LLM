package main.model;

import java.util.*;

/**
 * A complete record of one student's quiz attempt.
 * Stored in the DB so teachers can replay every answer.
 */
public class AttemptRecord {

    private int                  id;           // DB primary key (0 until saved)
    private final int            studentId;
    private final String         studentName;
    private final int            quizId;
    private final String         quizTitle;
    private final Map<Integer,String> answers;  // questionId → answer text
    private final Result         result;
    private final int            violations;    // anti-cheat violation count
    private final long           timestamp;     // epoch millis

    /** Constructor used when saving a new attempt. */
    public AttemptRecord(int studentId, String studentName,
                         int quizId,   String quizTitle,
                         Map<Integer,String> answers,
                         Result result, int violations) {
        this.id          = 0;
        this.studentId   = studentId;
        this.studentName = studentName;
        this.quizId      = quizId;
        this.quizTitle   = quizTitle;
        this.answers     = new LinkedHashMap<>(answers);
        this.result      = result;
        this.violations  = violations;
        this.timestamp   = System.currentTimeMillis();
    }

    /** Constructor used when loading from the database. */
    public AttemptRecord(int id, int studentId, String studentName,
                         int quizId, String quizTitle,
                         Map<Integer,String> answers,
                         Result result, int violations, long timestamp) {
        this.id          = id;
        this.studentId   = studentId;
        this.studentName = studentName;
        this.quizId      = quizId;
        this.quizTitle   = quizTitle;
        this.answers     = new LinkedHashMap<>(answers);
        this.result      = result;
        this.violations  = violations;
        this.timestamp   = timestamp;
    }

    public int                  getId()          { return id; }
    public void                 setId(int id)    { this.id = id; }
    public int                  getStudentId()   { return studentId; }
    public String               getStudentName() { return studentName; }
    public int                  getQuizId()      { return quizId; }
    public String               getQuizTitle()   { return quizTitle; }
    public Map<Integer,String>  getAnswers()     { return Collections.unmodifiableMap(answers); }
    public Result               getResult()      { return result; }
    public int                  getViolations()  { return violations; }
    public long                 getTimestamp()   { return timestamp; }
}
