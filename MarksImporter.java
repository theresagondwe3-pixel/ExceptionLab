import java.io.*;
import java.util.*;
import java.util.logging.*;

/**
 * ICT261 - Unit 4 Lab: Exception Handling and Error Management
 * Project: ExceptionLab
 *
 * RECOVERY STRATEGY (decision): a bad record is SKIPPED, not fatal.
 * Each record is validated inside its own try/catch inside the loop, so one
 * bad line (non-numeric mark, out-of-range mark, wrong field count, duplicate
 * ID) is reported and the importer carries on with the next line. A missing
 * file, however, cannot be recovered from, so it stops the import.
 */

public class MarksImporter {


    private static final Logger LOGGER = Logger.getLogger(MarksImporter.class.getName());

    /** Technical details go to a log FILE; the console only gets friendly messages. */
    private static void setupLogger() {
        try {
            LOGGER.setUseParentHandlers(false);          // keep technical logs off the console
            FileHandler fh = new FileHandler("import_errors.log", false); // overwrite per run
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            System.out.println("Warning: could not create the log file; technical details will not be saved.");
        }
    }

    /** Declares 'throws IOException' because file access can fail (checked exception). */
    static void importMarks(String filename) throws IOException {
        Set<String> seen = new HashSet<>();
        int accepted = 0, rejected = 0, lineNo = 0;

        // try-with-resources: 'br' implements AutoCloseable, so Java calls br.close()
        // automatically when the block ends - whether it finishes normally or an
        // exception is thrown - so the file is never left open (no finally needed).
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) {
                    continue; // ignore blank lines
                }
                try {
                    String[] parts = line.split(",", -1);

                    // TODO 1: check there are exactly 2 fields
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                                "Expected 2 fields (ID, mark) but found " + parts.length);
                    }
                    String id = parts[0].trim();
                    String markText = parts[1].trim();
                    if (id.isEmpty()) {
                        throw new IllegalArgumentException("Student number is empty");
                    }

                    // TODO 2: parse mark and validate range 0..100
                    int mark = Integer.parseInt(markText);   // may throw NumberFormatException
                    if (mark < 0 || mark > 100) {
                        throw new IllegalArgumentException(
                                "Mark " + mark + " is out of range (must be 0-100)");
                    }

                    // TODO 3: detect repeated student ID using seen
                    if (!seen.add(id)) {                      // add() returns false if already present
                        throw new DuplicateStudentException(id);
                    }

                    // TODO 4: display accepted record
                    System.out.println("Accepted: " + id + " -> " + mark);
                    accepted++;

                } catch (DuplicateStudentException e) {
                    rejected++;
                    System.out.println("Line " + lineNo + " skipped: student " + e.getStudentId()
                            + " appears more than once.");
                    LOGGER.log(Level.WARNING, "Line " + lineNo + " [" + line + "] "
                            + e.getClass().getName() + ": " + e.getMessage(), e);
                } catch (NumberFormatException e) {          // must come BEFORE IllegalArgumentException
                    rejected++;
                    System.out.println("Line " + lineNo + " skipped: the mark is not a valid number.");
                    LOGGER.log(Level.WARNING, "Line " + lineNo + " [" + line + "] "
                            + e.getClass().getName() + ": " + e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                    rejected++;
                    System.out.println("Line " + lineNo + " skipped: " + e.getMessage() + ".");
                    LOGGER.log(Level.WARNING, "Line " + lineNo + " [" + line + "] "
                            + e.getClass().getName() + ": " + e.getMessage(), e);
                }
            }
        }
        System.out.println("Summary: " + accepted + " accepted, " + rejected + " skipped.");
    }

    public static void main(String[] args) {
        setupLogger();
        // Defaults to marks.txt; an optional argument lets us run the T1-T5 test files.
        String filename = (args.length > 0) ? args[0] : "marks.txt";

        try {
            importMarks(filename);
        } catch (FileNotFoundException e) {                   // more specific: BEFORE IOException
            System.out.println("Marks file not found; check the file name.");
            LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (IOException e) {
            System.out.println("A problem occurred while reading the marks file. Please try again.");
            LOGGER.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (DuplicateStudentException e) {               // safety net (normally handled per record)
            System.out.println("Duplicate student number found: " + e.getStudentId());
            LOGGER.log(Level.WARNING, e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {                // safety net for invalid data
            System.out.println("Invalid data found in the marks file.");
            LOGGER.log(Level.WARNING, e.getClass().getName() + ": " + e.getMessage(), e);
        } finally {
            System.out.println("Import attempt finished");
        }
    }
}

/** Custom UNCHECKED exception: extends RuntimeException, so no 'throws' is forced. */
class DuplicateStudentException extends RuntimeException {
    private final String studentId;

    public DuplicateStudentException(String studentId) {
        super("Duplicate student number: " + studentId);
        this.studentId = studentId;
    }

    public String getStudentId() {
        return studentId;
    }
}
