       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'Outer'.
       PROCEDURE DIVISION.
           CALL 'mixed-Child'.
           CALL 'MIXED-CHILD'.
           GOBACK.
       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'mixed-Child'.
       PROCEDURE DIVISION.
           GOBACK.
       END PROGRAM 'mixed-Child'.
       END PROGRAM 'Outer'.
