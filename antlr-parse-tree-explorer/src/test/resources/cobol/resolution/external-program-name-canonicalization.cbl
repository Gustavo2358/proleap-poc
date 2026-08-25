       IDENTIFICATION DIVISION.
       PROGRAM-ID. EXTERNAL-NAME-CALLER.
       PROCEDURE DIVISION.
           CALL 'PROG-A'.
           CALL 'LONG-NAME-ABC'.
           CALL '1PROG'.
           CALL '$PROG'.
           CALL '-PROG'.
           CALL 'mixed-Child'.
           GOBACK.
       END PROGRAM EXTERNAL-NAME-CALLER.
