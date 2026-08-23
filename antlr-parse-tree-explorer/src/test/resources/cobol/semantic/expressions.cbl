       IDENTIFICATION DIVISION.
       PROGRAM-ID. EXPTEST.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 A PIC 9.
       01 B PIC 9.
       01 TARGET PIC X.
       PROCEDURE DIVISION.
           IF (A + B * 2) > 10 AND NOT A = 0
               MOVE FUNCTION MAX(A, B) TO TARGET
           ELSE
               MOVE RETURN-CODE TO TARGET
           END-IF.
           IF A > 1 OR 2
               MOVE A TO B
           END-IF.
           EVALUATE A + 1 ALSO B
             WHEN 2 ALSO 3
               CONTINUE
             WHEN OTHER
               CONTINUE
           END-EVALUATE.
           PERFORM UNTIL A > LENGTH OF TARGET
               MOVE A TO B
           END-PERFORM.
           GOBACK.
