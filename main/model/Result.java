package main.model;

public class Result {

    private int score;
    private int totalMarks;
    private int pendingMarks;   // marks from subjective Qs not yet graded by teacher

    /** Constructor used when all marks are finalised (MCQ only quizzes). */
    public Result(int score, int totalMarks) {
        this(score, totalMarks, 0);
    }

    /** Constructor used when some subjective marks are still pending. */
    public Result(int score, int totalMarks, int pendingMarks) {
        this.score        = score;
        this.totalMarks   = totalMarks;
        this.pendingMarks = pendingMarks;
    }

    public int    getScore()        { return score; }
    public int    getTotalMarks()   { return totalMarks; }
    public int    getPendingMarks() { return pendingMarks; }
    public boolean hasPending()     { return pendingMarks > 0; }

    /** Score as a percentage of the marks that HAVE been graded. */
    public double getPercentage() {
        int graded = totalMarks - pendingMarks;
        if (graded <= 0) return 0;
        return (score * 100.0) / totalMarks;
    }

    /** Called by TeacherDashboardFrame after manually grading a subjective answer. */
    public void addManualScore(int marks) {
        this.score        += marks;
        this.pendingMarks -= marks;
        if (this.pendingMarks < 0) this.pendingMarks = 0;
    }

    public void display() {
        System.out.println("\n====================");
        System.out.println("SCORE: " + score + " / " + totalMarks);
        if (pendingMarks > 0)
            System.out.println("PENDING (subjective): " + pendingMarks + " marks awaiting teacher review");
        System.out.println("PERCENTAGE: " + String.format("%.2f", getPercentage()) + "%");
        System.out.println("====================");
    }
}
