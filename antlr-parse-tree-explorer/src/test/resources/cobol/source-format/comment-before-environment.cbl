       IDENTIFICATION DIVISION.
       PROGRAM-ID. COMMENTBUG.
       AUTHOR.
      * An ordinary fixed-format comment follows the empty comment entry.
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SOURCE-COMPUTER. TEST-MACHINE.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 VALUE-A PIC X.
       PROCEDURE DIVISION.
           MOVE 'X' TO VALUE-A.
           GOBACK.
